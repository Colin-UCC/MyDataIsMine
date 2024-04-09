package com.fyp.mydataismine.packetcapture;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fyp.mydataismine.packetcapture.TCPHeader;
import com.fyp.mydataismine.packetcapture.TCB.TCBStatus;

/**
 * Manages outgoing TCP traffic within the VPN service, handling TCP packet processing, connection management, and data forwarding.
 */
public class TCPOutput implements Runnable {
    private static final String TAG = TCPOutput.class.getSimpleName();

    private VPNNetworkService vpnService;
    private ConcurrentLinkedQueue<Packet> inputQueue;
    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;
    private Selector selector;

    private Random random = new Random();

    /**
     * Initializes a new instance of the TCPOutput class.
     *
     * @param inputQueue The queue containing incoming packets to be processed.
     * @param outputQueue The queue for placing processed packets to be sent out.
     * @param selector The selector for managing non-blocking network channels.
     * @param vpnService The VPN service managing network interactions.
     */
    public TCPOutput(ConcurrentLinkedQueue<Packet> inputQueue, ConcurrentLinkedQueue<ByteBuffer> outputQueue,
                     Selector selector, VPNNetworkService vpnService) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.selector = selector;
        this.vpnService = vpnService;
    }

    /**
     * The main processing loop that takes packets from the input queue, processes them, and places responses in the output queue.
     */
    @Override
    public void run() {
        Log.i(TAG, "Started");
        try {
            Thread currentThread = Thread.currentThread();
            while (true) {
                Packet currentPacket;
                // TODO: Block when not connected
                do {
                    currentPacket = inputQueue.poll();
                    if (currentPacket != null)
                        break;
                    Thread.sleep(10);
                } while (!currentThread.isInterrupted());

                if (currentThread.isInterrupted())
                    break;

                ByteBuffer payloadBuffer = currentPacket.backingBuffer;
                currentPacket.backingBuffer = null;
                ByteBuffer responseBuffer = ByteBufferPool.acquire();

                InetAddress destinationAddress = currentPacket.ip4Header.destinationAddress;

                TCPHeader tcpHeader = currentPacket.tcpHeader;
                int destinationPort = tcpHeader.destinationPort;
                int sourcePort = tcpHeader.sourcePort;

                String ipAndPort = destinationAddress.getHostAddress() + ":" +
                        destinationPort + ":" + sourcePort;
                TCB tcb = TCB.getTCB(ipAndPort);
                if (tcb == null)
                    initializeConnection(ipAndPort, destinationAddress, destinationPort,
                            currentPacket, tcpHeader, responseBuffer);
                else if (tcpHeader.isSYN())
                    processDuplicateSYN(tcb, tcpHeader, responseBuffer);
                else if (tcpHeader.isRST())
                    closeCleanly(tcb, responseBuffer);
                else if (tcpHeader.isFIN())
                    processFIN(tcb, tcpHeader, responseBuffer);
                else if (tcpHeader.isACK())
                    processACK(tcb, tcpHeader, payloadBuffer, responseBuffer);


                if (responseBuffer.position() == 0)
                    ByteBufferPool.release(responseBuffer);
                ByteBufferPool.release(payloadBuffer);
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Stopping");
        } catch (IOException e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            TCB.closeAll();
        }
    }

    /**
     * Initializes a new TCP connection for the given packet.
     *
     * @param ipAndPort A string representing the IP address and port of the connection.
     * @param destinationAddress The destination IP address of the connection.
     * @param destinationPort The destination port number of the connection.
     * @param currentPacket The current TCP packet being processed.
     * @param tcpHeader The TCP header of the current packet.
     * @param responseBuffer The buffer for storing the response packet.
     * @throws IOException if an I/O error occurs.
     */
    private void initializeConnection(String ipAndPort, InetAddress destinationAddress, int destinationPort,
                                      Packet currentPacket, TCPHeader tcpHeader, ByteBuffer responseBuffer)
            throws IOException {
        currentPacket.swapSourceAndDestination();
        if (tcpHeader.isSYN()) {
            SocketChannel outputChannel = SocketChannel.open();
            outputChannel.configureBlocking(false);
            vpnService.protect(outputChannel.socket());

            TCB tcb = new TCB(ipAndPort, random.nextInt(Short.MAX_VALUE + 1), tcpHeader.sequenceNumber, tcpHeader.sequenceNumber + 1,
                    tcpHeader.acknowledgementNumber, outputChannel, currentPacket);
            TCB.putTCB(ipAndPort, tcb);

            try {
                outputChannel.connect(new InetSocketAddress(destinationAddress, destinationPort));
                if (outputChannel.finishConnect()) {
                    tcb.status = TCBStatus.SYN_RECEIVED;
                    // TODO: Set MSS for receiving larger packets from the device
                    currentPacket.updateTCPBuffer(responseBuffer, (byte) (TCPHeader.SYN | TCPHeader.ACK),
                            tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                    tcb.mySequenceNum++; // SYN counts as a byte
                } else {
                    tcb.status = TCBStatus.SYN_SENT;
                    selector.wakeup();
                    tcb.selectionKey = outputChannel.register(selector, SelectionKey.OP_CONNECT, tcb);
                    return;
                }
            } catch (IOException e) {
                Log.e(TAG, "Connection error: " + ipAndPort, e);
                currentPacket.updateTCPBuffer(responseBuffer, (byte) TCPHeader.RST, 0, tcb.myAcknowledgementNum, 0);
                TCB.closeTCB(tcb);
            }
        } else {
            currentPacket.updateTCPBuffer(responseBuffer, (byte) TCPHeader.RST,
                    0, tcpHeader.sequenceNumber + 1, 0);
        }
        outputQueue.offer(responseBuffer);
    }

    /**
     * Processes a duplicate SYN packet.
     *
     * @param tcb The Transmission Control Block associated with the connection.
     * @param tcpHeader The TCP header of the incoming packet.
     * @param responseBuffer The buffer for storing the response packet.
     */
    private void processDuplicateSYN(TCB tcb, TCPHeader tcpHeader, ByteBuffer responseBuffer) {
        synchronized (tcb) {
            if (tcb.status == TCBStatus.SYN_SENT) {
                tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + 1;
                return;
            }
        }
        sendRST(tcb, 1, responseBuffer);
    }

    /**
     * Processes a FIN packet.
     *
     * @param tcb The Transmission Control Block associated with the connection.
     * @param tcpHeader The TCP header of the incoming packet.
     * @param responseBuffer The buffer for storing the response packet.
     */
    private void processFIN(TCB tcb, TCPHeader tcpHeader, ByteBuffer responseBuffer) {
        synchronized (tcb) {
            Packet referencePacket = tcb.referencePacket;
            tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + 1;
            tcb.theirAcknowledgementNum = tcpHeader.acknowledgementNumber;

            if (tcb.waitingForNetworkData) {
                tcb.status = TCBStatus.CLOSE_WAIT;
                referencePacket.updateTCPBuffer(responseBuffer, (byte) TCPHeader.ACK,
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
            } else {
                tcb.status = TCBStatus.LAST_ACK;
                referencePacket.updateTCPBuffer(responseBuffer, (byte) (TCPHeader.FIN | TCPHeader.ACK),
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                tcb.mySequenceNum++; // FIN counts as a byte
            }
        }
        outputQueue.offer(responseBuffer);
    }

    /**
     * Processes an ACK packet, forwarding the contained data to the remote server if necessary.
     *
     * @param tcb The Transmission Control Block associated with the connection.
     * @param tcpHeader The TCP header of the incoming packet.
     * @param payloadBuffer The buffer containing the payload of the incoming packet.
     * @param responseBuffer The buffer for storing the response packet.
     * @throws IOException if an I/O error occurs during packet processing.
     */
    private void processACK(TCB tcb, TCPHeader tcpHeader, ByteBuffer payloadBuffer, ByteBuffer responseBuffer) throws IOException {
        int payloadSize = payloadBuffer.limit() - payloadBuffer.position();

        synchronized (tcb) {
            SocketChannel outputChannel = tcb.channel;
            if (tcb.status == TCBStatus.SYN_RECEIVED) {
                tcb.status = TCBStatus.ESTABLISHED;

                selector.wakeup();
                tcb.selectionKey = outputChannel.register(selector, SelectionKey.OP_READ, tcb);
                tcb.waitingForNetworkData = true;
            } else if (tcb.status == TCBStatus.LAST_ACK) {
                closeCleanly(tcb, responseBuffer);
                return;
            }

            if (payloadSize == 0) return; // Empty ACK, ignore

            if (!tcb.waitingForNetworkData) {
                selector.wakeup();
                tcb.selectionKey.interestOps(SelectionKey.OP_READ);
                tcb.waitingForNetworkData = true;
            }

            // Forward to remote server
            try {
                while (payloadBuffer.hasRemaining())
                    outputChannel.write(payloadBuffer);
            } catch (IOException e) {
                Log.e(TAG, "Network write error: " + tcb.ipAndPort, e);
                sendRST(tcb, payloadSize, responseBuffer);
                return;
            }

            tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + payloadSize;
            tcb.theirAcknowledgementNum = tcpHeader.acknowledgementNumber;
            Packet referencePacket = tcb.referencePacket;
            referencePacket.updateTCPBuffer(responseBuffer, (byte) TCPHeader.ACK, tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
        }
        outputQueue.offer(responseBuffer);
    }

    /**
     * Sends a RST (reset) packet to the remote host, effectively closing the connection.
     *
     * @param tcb The Transmission Control Block associated with the connection.
     * @param prevPayloadSize The size of the previous payload, used to calculate sequence numbers.
     * @param buffer The buffer for storing the RST packet.
     */
    private void sendRST(TCB tcb, int prevPayloadSize, ByteBuffer buffer) {
        tcb.referencePacket.updateTCPBuffer(buffer, (byte) TCPHeader.RST, 0, tcb.myAcknowledgementNum + prevPayloadSize, 0);
        outputQueue.offer(buffer);
        TCB.closeTCB(tcb);
    }

    /**
     * Closes the connection cleanly and releases any associated resources.
     *
     * @param tcb The Transmission Control Block associated with the connection.
     * @param buffer The buffer to release back to the pool.
     */
    private void closeCleanly(TCB tcb, ByteBuffer buffer) {
        ByteBufferPool.release(buffer);
        TCB.closeTCB(tcb);
    }
}

