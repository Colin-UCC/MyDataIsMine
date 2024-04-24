package com.fyp.mydataismine.packetcapture;

import static com.fyp.mydataismine.packetcapture.Packet.TCP_HEADER_SIZE;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class TCPHeaderTest {
    private TCPHeader tcpHeader;
    private ByteBuffer buffer;

    @Before
    public void setUp() {
        // Example TCP header data - 20 bytes header without options
        buffer = ByteBuffer.allocate(TCP_HEADER_SIZE);
        buffer.putShort((short) 1234); // Source port
        buffer.putShort((short) 80);   // Destination port
        buffer.putInt(0);              // Sequence number
        buffer.putInt(0);              // Acknowledgment number
        buffer.put((byte) 0x50);       // Data offset and reserved (5 header length)
        buffer.put((byte) 0x12);       // Flags (SYN, ACK)
        buffer.putShort((short) 0);    // Window size
        buffer.putShort((short) 0);    // Checksum
        buffer.putShort((short) 0);    // Urgent pointer

        buffer.flip(); // Reset position to 0 for reading
        tcpHeader = new TCPHeader(buffer);
    }

    @Test
    public void testParsing() {
        assertEquals(1234, tcpHeader.getSourcePort());
        assertEquals(80, tcpHeader.getDestinationPort());
        assertEquals(0, tcpHeader.getSequenceNumber());
        assertEquals(0, tcpHeader.getAcknowledgmentNumber());
        assertEquals(20, tcpHeader.getDataOffset()); // 5 * 4 = 20 bytes
        assertTrue(tcpHeader.isSYN());
        assertTrue(tcpHeader.isACK());
        assertFalse(tcpHeader.isFIN());
    }

    @Test
    public void testSwapSourceAndDestination() {
        tcpHeader.swapSourceAndDestination();
        assertEquals(80, tcpHeader.getSourcePort());
        assertEquals(1234, tcpHeader.getDestinationPort());
    }

    @Test
    public void testFlagChecks() {
        assertTrue(tcpHeader.isSYN());
        assertTrue(tcpHeader.isACK());
        assertFalse(tcpHeader.isRST());
        assertFalse(tcpHeader.isFIN());
        assertFalse(tcpHeader.isPSH());
        assertFalse(tcpHeader.isURG());
    }
}
