package com.fyp.mydataismine.packetcapture;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.InetSocketAddress;

/**
 * Handles the input of UDP packets, reading them from a network channel and queuing them for further processing.
 */
public class UDPInput implements Runnable {
    // Class fields and constructor
    private static final String TAG = UDPInput.class.getSimpleName();
    private static final int HEADER_SIZE = Packet.IP4_HEADER_SIZE + Packet.UDP_HEADER_SIZE;
    private Selector selector;
    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;

    public UDPInput(ConcurrentLinkedQueue<ByteBuffer> outputQueue, Selector selector) {
        this.outputQueue = outputQueue;
        this.selector = selector;
    }

    /**
     * Runs the main loop to continuously read from the UDP channel and process incoming packets.
     */
    @Override
    public void run() {
        try {
            Log.i(TAG, "Started");
            while (!Thread.interrupted()) {
                int readyChannels = selector.select();

                if (readyChannels == 0) {
                    Thread.sleep(10);
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();

                while (keyIterator.hasNext() && !Thread.interrupted()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid() && key.isReadable()) {
                        keyIterator.remove();

                        DatagramChannel inputChannel = (DatagramChannel) key.channel();
                        ByteBuffer receiveBuffer = ByteBuffer.allocate(Packet.MAX_PACKET_SIZE);

                        try {
                            // Read from the inputChannel into the receiveBuffer
                            receiveBuffer.clear();
                            InetSocketAddress senderAddress = (InetSocketAddress) inputChannel.receive(receiveBuffer);
                            receiveBuffer.flip();

                            if (senderAddress != null) {
                                // Process the received packet
                                Packet referencePacket = (Packet) key.attachment();
                                referencePacket.updateUDPBuffer(receiveBuffer, receiveBuffer.remaining());
                                receiveBuffer.position(HEADER_SIZE);

                                // Offer the receiveBuffer to the outputQueue
                                outputQueue.offer(receiveBuffer);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading UDP packet", e);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Stopping");
        } catch (IOException e) {
            Log.w(TAG, e.toString(), e);
        }
    }
}


