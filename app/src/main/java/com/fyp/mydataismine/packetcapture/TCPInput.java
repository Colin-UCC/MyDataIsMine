package com.fyp.mydataismine.packetcapture;

import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.fyp.mydataismine.packetcapture.TCB.TCBStatus;

/**
 * Manages the input stream for TCP packets within a VPN service, utilizing a non-blocking I/O mechanism to read and process incoming TCP data.
 */
public class TCPInput implements Runnable
{
    private static final String TAG = TCPInput.class.getSimpleName();
    private static final int HEADER_SIZE = Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE;
    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;
    private Selector selector;
    private FileDescriptor vpnFileDescriptor;

    /**
     * Constructs a TCPInput handler for reading and processing TCP packets from a network channel.
     *
     * @param outputQueue The queue to which processed packets will be added.
     * @param vpnFileDescriptor The file descriptor of the VPN interface.
     */
    public TCPInput(ConcurrentLinkedQueue<ByteBuffer> outputQueue, FileDescriptor vpnFileDescriptor) {
        this.outputQueue = outputQueue;
        this.vpnFileDescriptor = vpnFileDescriptor;
        try {
            // Create a Selector using the provided FileDescriptor
            selector = SelectorProvider.provider().openSelector();
        } catch (IOException e) {
            Log.e(TAG, "Error creating Selector", e);
        }
    }

    /**
     * The main run method that continuously reads and processes incoming TCP packets.
     */
    @Override
    public void run()
    {
        try
        {
            Log.d(TAG, "Started");
            while (!Thread.interrupted())
            {
                int readyChannels = selector.select();

                if (readyChannels == 0) {
                    Thread.sleep(10);
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();

                while (keyIterator.hasNext() && !Thread.interrupted())
                {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid())
                    {
                        if (key.isConnectable())
                            processConnect(key, keyIterator);
                        else if (key.isReadable())
                            processInput(key, keyIterator);
                    }
                }
            }
        }
        catch (InterruptedException e)
        {
            Log.i(TAG, "Stopping");
        }
        catch (IOException e)
        {
            Log.w(TAG, e.toString(), e);
        }
    }

    /**
     * Processes the TCP connections that are ready to be connected.
     *
     * @param key The selection key representing the channel ready for connection.
     * @param keyIterator The iterator for the selection keys.
     */
    void processConnect(SelectionKey key, Iterator<SelectionKey> keyIterator)
    {
        TCB tcb = (TCB) key.attachment();
        Packet referencePacket = tcb.referencePacket;
        try
        {
            if (tcb.channel.finishConnect())
            {
                keyIterator.remove();
                tcb.status = TCBStatus.SYN_RECEIVED;

                // TODO: Set MSS for receiving larger packets from the device
                ByteBuffer responseBuffer = ByteBufferPool.acquire();
                referencePacket.updateTCPBuffer(responseBuffer, (byte) (TCPHeader.SYN | TCPHeader.ACK),
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                outputQueue.offer(responseBuffer);

                tcb.mySequenceNum++; // SYN counts as a byte
                key.interestOps(SelectionKey.OP_READ);
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "Connection error: " + tcb.ipAndPort, e);
            ByteBuffer responseBuffer = ByteBufferPool.acquire();
            referencePacket.updateTCPBuffer(responseBuffer, (byte) TCPHeader.RST, 0, tcb.myAcknowledgementNum, 0);
            outputQueue.offer(responseBuffer);
            TCB.closeTCB(tcb);
        }
    }

    /**
     * Processes the incoming TCP data for a channel.
     *
     * @param key The selection key representing the channel with incoming data.
     * @param keyIterator The iterator for the selection keys.
     */
    void processInput(SelectionKey key, Iterator<SelectionKey> keyIterator)
    {
        keyIterator.remove();
        ByteBuffer receiveBuffer = ByteBufferPool.acquire();
        // Leave space for the header
        receiveBuffer.position(HEADER_SIZE);

        TCB tcb = (TCB) key.attachment();
        synchronized (tcb)
        {
            Packet referencePacket = tcb.referencePacket;
            SocketChannel inputChannel = (SocketChannel) key.channel();
            int readBytes;
            try
            {
                readBytes = inputChannel.read(receiveBuffer);
            }
            catch (IOException e)
            {
                Log.e(TAG, "Network read error: " + tcb.ipAndPort, e);
                referencePacket.updateTCPBuffer(receiveBuffer, (byte) TCPHeader.RST, 0, tcb.myAcknowledgementNum, 0);
                outputQueue.offer(receiveBuffer);
                TCB.closeTCB(tcb);
                return;
            }

            if (readBytes == -1)
            {
                // End of stream
                key.interestOps(0);
                tcb.waitingForNetworkData = false;

                if (tcb.status != TCBStatus.CLOSE_WAIT)
                {
                    ByteBufferPool.release(receiveBuffer);
                    return;
                }

                tcb.status = TCBStatus.LAST_ACK;
                referencePacket.updateTCPBuffer(receiveBuffer, (byte) TCPHeader.FIN, tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                tcb.mySequenceNum++; // FIN counts as a byte
            }
            else  {

                // Calculate payload size (exclude TCP/IP header size if needed)
                int payloadSize = readBytes - HEADER_SIZE; // Adjust if you need to exclude header size
                String sourceIp = tcb.referencePacket.ip4Header.sourceAddress.getHostAddress();
                String destinationIp = tcb.referencePacket.ip4Header.destinationAddress.getHostAddress();

                // Log or store packet data
                logOrStorePacketData(sourceIp, destinationIp, payloadSize);

                referencePacket.updateTCPBuffer(receiveBuffer, (byte) (TCPHeader.PSH | TCPHeader.ACK),
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, readBytes);
                tcb.mySequenceNum += readBytes;
                receiveBuffer.position(HEADER_SIZE + readBytes);
            }
        }
        outputQueue.offer(receiveBuffer);
    }

    /**
     * Logs or stores the packet data for further analysis or debugging.
     *
     * @param sourceIp The source IP address of the packet.
     * @param destinationIp The destination IP address of the packet.
     * @param payloadSize The size of the packet payload.
     */
    private void logOrStorePacketData(String sourceIp, String destinationIp, int payloadSize) {
        // Implementation to log or store the packet data
        Log.d(TAG, "Packet from " + sourceIp + " to " + destinationIp + " with payload size: " + payloadSize + " bytes");
    }
}

