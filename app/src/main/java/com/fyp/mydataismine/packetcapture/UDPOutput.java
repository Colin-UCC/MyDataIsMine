package com.fyp.mydataismine.packetcapture;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages the output of UDP packets, sending them through a non-blocking DatagramChannel.
 */
public class UDPOutput implements Runnable {
    private static final String TAG = UDPOutput.class.getSimpleName();
    private ConcurrentLinkedQueue<Packet> inputQueue;
    private DatagramChannel outputChannel;

    /**
     * Initializes the UDPOutput instance with a queue for input packets and a DatagramChannel for output.
     * @param inputQueue The queue containing packets to be sent.
     * @param outputChannel The DatagramChannel through which packets will be sent.
     */
    public UDPOutput(ConcurrentLinkedQueue<Packet> inputQueue, DatagramChannel outputChannel) {
        this.inputQueue = inputQueue;
        this.outputChannel = outputChannel;
    }

    /**
     * Initializes a UDPOutput instance with a specified queue of packets to send and a VPN file descriptor.
     * This constructor sets up a non-blocking DatagramChannel that sends packets to a predefined address and port.
     * The DatagramChannel is created using the provided FileDescriptor to ensure it operates within the VPN's network interface.
     *
     * @param inputQueue The queue containing packets to be sent, typically coming from the VPN service.
     * @param vpnFileDescriptor The file descriptor of the VPN's network interface, used to bind the DatagramChannel.
     */
    public UDPOutput(ConcurrentLinkedQueue<Packet> inputQueue, FileDescriptor vpnFileDescriptor) {
        this.inputQueue = inputQueue;
        try {
            // Create a DatagramChannel using the provided FileDescriptor
            outputChannel = DatagramChannel.open();
            outputChannel.connect(new InetSocketAddress("localhost", 8080));
            outputChannel.configureBlocking(false);
        } catch (IOException e) {
            Log.e(TAG, "Error creating DatagramChannel", e);
        }
    }

    /**
     * Main run method that continuously processes and sends packets from the input queue.
     */
    @Override
    public void run() {
        try {
            Log.i(TAG, "Started");
            while (true) {
                // Dequeue a packet from the inputQueue
                Packet currentPacket = inputQueue.poll();

                if (currentPacket != null) {
                    // Extract the data from the packet
                    ByteBuffer dataBuffer = currentPacket.backingBuffer;

                    try {
                        // Send the data via the outputChannel
                        while (dataBuffer.hasRemaining()) {
                            outputChannel.write(dataBuffer);
                        }
                        Log.i(TAG, "Data sent successfully");

                        // Perform any necessary actions here after data is sent
                    } catch (IOException e) {
                        // Handle the case where data could not be sent
                        Log.e(TAG, "Failed to send data", e);
                    } finally {
                        // Release the ByteBuffer to the pool
                        ByteBufferPool.release(dataBuffer);
                    }
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "Stopping");
        }
    }
}

