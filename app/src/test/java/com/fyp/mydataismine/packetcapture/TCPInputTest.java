package com.fyp.mydataismine.packetcapture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.Iterator;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class TCPInputTest {

    @Mock private Iterator<SelectionKey> mockKeyIterator;

    @Mock private SocketChannel mockChannel;
    @Mock private SelectionKey mockKey;
    @Mock private TCB mockTCB;

    private TCPInput tcpInput;
    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;
    private Packet referencePacket;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        outputQueue = new ConcurrentLinkedQueue<>();
        FileDescriptor fd = mock(FileDescriptor.class); // Use a mock FileDescriptor

        tcpInput = new TCPInput(outputQueue, fd);

        when(mockKey.channel()).thenReturn(mockChannel);
        when(mockKey.attachment()).thenReturn(mockTCB);

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        initializeIPv4Buffer(buffer, new byte[20]); // Assuming 20 bytes payload
        referencePacket = new Packet(buffer);

        mockTCB.referencePacket = referencePacket; // Directly assign the reference packet

        when(mockKeyIterator.hasNext()).thenReturn(true, false); // Ensures the loop runs once
        when(mockKeyIterator.next()).thenReturn(mockKey);
    }


    private void initializeIPv4Buffer(ByteBuffer buffer, byte[] payload) {
        buffer.clear();

        // Example IPv4 header values
        byte versionAndHeaderLength = (byte) ((4 << 4) | 5); // IPv4 and header length 20 bytes
        byte typeOfService = 0;
        int totalLength = Packet.IP4_HEADER_SIZE + payload.length;
        int identification = 0;
        byte flagsAndFragmentOffset = 0;
        byte ttl = 64; // Time to live
        byte protocol = 6; // TCP
        int headerChecksum = 0; // For simplicity, not computed here
        int sourceAddress = 0; // 0.0.0.0
        int destinationAddress = 0; // 0.0.0.0

        buffer.put(versionAndHeaderLength);
        buffer.put(typeOfService);
        buffer.putShort((short) totalLength);
        buffer.putShort((short) identification);
        buffer.putShort((short) ((flagsAndFragmentOffset << 13) | 0)); // Flags and fragment offset
        buffer.put(ttl);
        buffer.put(protocol);
        buffer.putShort((short) headerChecksum);
        buffer.putInt(sourceAddress);
        buffer.putInt(destinationAddress);

        // Add the payload
        buffer.put(payload);

        buffer.flip(); // Reset the position to the start of the buffer
    }



    @Test
    public void testProcessConnect_SuccessfulConnection() throws Exception {
        when(mockChannel.finishConnect()).thenReturn(true);

        tcpInput.processConnect(mockKey, null); // Iterator can be null for testing

        verify(mockTCB).status = TCB.TCBStatus.SYN_RECEIVED;
        assertFalse(outputQueue.isEmpty());
    }

    @Test
    public void testProcessInput_DataRead() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        when(mockChannel.read(buffer)).thenReturn(10); // Assume 10 bytes read

        tcpInput.processInput(mockKey, null); // Iterator can be null for testing

        assertFalse(outputQueue.isEmpty());
    }

    @Test
    public void testProcessInput_EndOfStream() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        when(mockChannel.read(buffer)).thenReturn(-1); // End of stream

        tcpInput.processInput(mockKey, mockKeyIterator); // Pass the mock iterator

        verify(mockTCB).status = TCB.TCBStatus.LAST_ACK;
        assertFalse(outputQueue.isEmpty());
        verify(mockKeyIterator).remove(); // Verify remove() is called on the iterator
    }

    @Test
    public void testProcessInput_ReadError() throws Exception {
        when(mockChannel.read(any(ByteBuffer.class))).thenThrow(new IOException());

        tcpInput.processInput(mockKey, null); // Iterator can be null for testing

        verify(mockTCB).status = TCB.TCBStatus.CLOSE_WAIT;
        assertFalse(outputQueue.isEmpty());
    }
}
