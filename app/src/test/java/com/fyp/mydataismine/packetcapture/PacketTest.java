package com.fyp.mydataismine.packetcapture;

import static com.fyp.mydataismine.packetcapture.IP4Header.IP4_HEADER_SIZE;

import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class PacketTest {
    private ByteBuffer ipv4Buffer;
    private ByteBuffer ipv6Buffer;

    @Before
    public void setUp() {
        byte[] ipv4Payload = {1, 2, 3, 4, 5};
        byte[] ipv6Payload = {1, 2, 3, 4, 5};

        ipv4Buffer = ByteBuffer.allocate(Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE + ipv4Payload.length);
        ipv6Buffer = ByteBuffer.allocate(40 + Packet.TCP_HEADER_SIZE + ipv6Payload.length);

        initializeIPv4Buffer(ipv4Buffer, ipv4Payload);
        initializeIPv6Buffer(ipv6Buffer, ipv6Payload);
    }

    private void initializeIPv4Buffer(ByteBuffer buffer, byte[] payload) {
        buffer.clear();
        int totalLength = Packet.IP4_HEADER_SIZE + payload.length;

        buffer.put((byte) 0x45); // IPv4 version and IHL
        buffer.put((byte) 0x00); // Type of Service
        buffer.putShort((short) totalLength); // Total length
        buffer.putInt(0); // Identification, Flags, Fragment Offset
        buffer.putInt(0); // TTL, Protocol, Header Checksum
        buffer.putInt(0); // Source IP
        buffer.putInt(0); // Destination IP
        buffer.put(payload); // Payload
        buffer.flip();
    }

    private void initializeIPv6Buffer(ByteBuffer buffer, byte[] payload) {
        buffer.put((byte) 0x60); // IPv6 version and traffic class
        buffer.put(new byte[3]); // Flow label
        buffer.putShort((short) (payload.length)); // Payload length
        buffer.put((byte) 0x06); // Next header (TCP, for example)
        buffer.put((byte) 0xFF); // Hop limit
        buffer.put(new byte[16]); // Source address (16 bytes)
        buffer.put(new byte[16]); // Destination address (16 bytes)
        buffer.put(payload); // Payload
        buffer.flip();
    }

    @Test
    public void testIPv6PacketParsing() throws UnknownHostException {
        Packet packet = new Packet(ipv6Buffer);
        assertNotNull("IPv6 Header should not be null", packet.ip6Header);
        assertNotNull("Payload should not be null", packet.getPayload());
    }

    @Test
    public void testIPv4PacketParsing() throws UnknownHostException {
        Packet packet = new Packet(ipv4Buffer);
        assertNotNull("IPv4 Header should not be null", packet.ip4Header);
        assertNotNull("IPv4 Payload should not be null", packet.getPayload());
    }

    @Test
    public void testPayloadRetrievalIPv4() {
        byte[] payload = {0x68, 0x65, 0x6C, 0x6C, 0x6F}; // "hello" in ASCII
        ByteBuffer ipv4Buffer = ByteBuffer.allocate(Packet.IP4_HEADER_SIZE + payload.length);
        initializeIPv4Buffer(ipv4Buffer, payload);

        Packet ipv4Packet = new Packet(ipv4Buffer);
        ByteBuffer ipv4Payload = ipv4Packet.getPayload();

        assertNotNull("IPv4 payload should not be null", ipv4Payload);
        assertEquals("IPv4 payload size should be 5 bytes", 5, ipv4Payload.remaining());
        byte[] actualPayload = new byte[ipv4Payload.remaining()];
        ipv4Payload.get(actualPayload);

        assertArrayEquals("IPv4 payload content should match", payload, actualPayload);
    }

    @Test
    public void testUpdateTCPBuffer() {
        int payloadSize = 500; // Mock payload size for testing

        // Create the combined buffer with enough space for headers and mock payload
        ByteBuffer combinedBuffer = ByteBuffer.allocate(Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE + payloadSize);

        // Initialize the IPv4 header
        combinedBuffer.put((byte) 0x45); // Version (4) and Header Length (5)
        combinedBuffer.put((byte) 0x00); // Type of Service
        combinedBuffer.putShort((short) (Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE + payloadSize)); // Total Length
        combinedBuffer.putInt(0); // ID, Flags, Fragment Offset
        combinedBuffer.put((byte) 0x40); // TTL
        combinedBuffer.put((byte) 0x06); // Protocol (TCP)
        combinedBuffer.putShort((short) 0); // Header checksum (mocked)
        combinedBuffer.putInt(0xC0A80001); // Source address
        combinedBuffer.putInt(0xC0A80002); // Destination address

        // Initialize the TCP header
        combinedBuffer.putShort((short) 1234); // Source port
        combinedBuffer.putShort((short) 80); // Destination port
        combinedBuffer.putInt(100); // Sequence number
        combinedBuffer.putInt(0); // Acknowledgment number
        combinedBuffer.put((byte) (5 << 4)); // Data offset
        combinedBuffer.put((byte) 0); // Flags
        combinedBuffer.putShort((short) 0); // Window size
        combinedBuffer.putShort((short) 0); // Checksum
        combinedBuffer.putShort((short) 0); // Urgent pointer

        // Add mock payload to match the total length
        byte[] mockPayload = new byte[payloadSize];
        combinedBuffer.put(mockPayload);

        combinedBuffer.flip(); // Reset the position for reading

        Packet packet = new Packet(combinedBuffer);

        byte flags = (byte) (TCPHeader.SYN | TCPHeader.ACK);
        long sequenceNum = 1000;
        long ackNum = 2000;

        // Ensure tcpHeader is not null before calling updateTCPBuffer
        assertNotNull(packet.tcpHeader);

        // Safely call updateTCPBuffer
        packet.updateTCPBuffer(combinedBuffer, flags, sequenceNum, ackNum, payloadSize);

        // Assertions to verify the TCP header fields
        assertEquals(flags, packet.tcpHeader.flags);
        assertEquals(sequenceNum, packet.tcpHeader.sequenceNumber);
        assertEquals(ackNum, packet.tcpHeader.acknowledgementNumber);
    }

    @Test
    public void testUpdateUDPBuffer() {
        // Setup mock payload size for UDP
        int mockPayloadSize = 50; // Mock payload size for testing
        int udpTotalLength = Packet.UDP_HEADER_SIZE + mockPayloadSize;

        // Create a buffer with enough space for the IP header, UDP header, and the mock payload
        ByteBuffer buffer = ByteBuffer.allocate(Packet.IP4_HEADER_SIZE + udpTotalLength);

        // Initialize IPv4 header
        buffer.put((byte) 0x45); // Version (4) and IHL (5)
        buffer.put((byte) 0x00); // Type of Service
        buffer.putShort((short) (Packet.IP4_HEADER_SIZE + udpTotalLength)); // Total Length
        buffer.putInt(0); // Identification, Flags, Fragment Offset
        buffer.put((byte) 0x40); // TTL
        buffer.put((byte) 0x11); // Protocol (UDP)
        buffer.putShort((short) 0); // Header checksum (mocked)
        buffer.putInt(0xC0A80001); // Source IP
        buffer.putInt(0xC0A80002); // Destination IP

        // Initialize UDP header
        buffer.putShort((short) 1234); // Source port
        buffer.putShort((short) 80); // Destination port
        buffer.putShort((short) udpTotalLength); // Length field of the UDP header
        buffer.putShort((short) 0); // Checksum (mocked)

        // Fill the rest of the buffer with mock payload data to match the expected total length
        byte[] mockPayload = new byte[mockPayloadSize];
        buffer.put(mockPayload);

        buffer.flip(); // Reset the position for reading

        // Create the packet instance
        Packet packet = new Packet(buffer);

        // Ensure udpHeader is not null before calling updateUDPBuffer
        assertNotNull(packet.udpHeader);

        // Update the UDP buffer
        packet.updateUDPBuffer(buffer, mockPayloadSize);

        // Assertions to verify the UDP length field and the total length field in the IPv4 header
        assertEquals("UDP total length should match", udpTotalLength, packet.udpHeader.length);
        assertEquals("IPv4 total length should match", Packet.IP4_HEADER_SIZE + udpTotalLength, packet.ip4Header.totalLength);
    }

    @Test
    public void testSwapSourceAndDestination() {
        // Setup the test environment
        int payloadSize = 20; // Example payload size
        int totalPacketSize = Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE + payloadSize;

        ByteBuffer buffer = ByteBuffer.allocate(totalPacketSize);

        // Sample IP addresses and ports for testing
        int sourceIp = 0xC0A80001; // 192.168.0.1
        int destinationIp = 0xC0A80002; // 192.168.0.2
        short sourcePort = 1234;
        short destinationPort = 80;

        // Initialize IPv4 header
        buffer.put((byte) 0x45); // IPv4 version and IHL
        buffer.put((byte) 0x00); // Type of Service
        buffer.putShort((short) totalPacketSize); // Total Length
        buffer.putInt(0); // ID, Flags, Fragment Offset
        buffer.put((byte) 0x40); // TTL
        buffer.put((byte) 0x06); // Protocol TCP
        buffer.putInt(sourceIp); // Source IP
        buffer.putInt(destinationIp); // Destination IP

        // Initialize TCP header
        buffer.putShort(sourcePort); // Source port
        buffer.putShort(destinationPort); // Destination port
        buffer.putInt(0); // Sequence number
        buffer.putInt(0); // Acknowledgment number
        buffer.putShort((short) 0x5000); // Data offset and flags
        buffer.putShort((short) 0); // Window size
        buffer.putShort((short) 0); // Checksum
        buffer.putShort((short) 0); // Urgent pointer

        // Add payload to match the specified total length
        for (int i = 0; i < payloadSize; i++) {
            buffer.put((byte) i);
        }

        buffer.flip(); // Prepare the buffer for reading

        // Create a Packet object from the buffer
        Packet originalPacket = new Packet(buffer);

        // Perform the source and destination swap
        Packet swappedPacket = originalPacket.swapSourceAndDestination();

        // Verify that the source and destination addresses and ports have been swapped
        assertEquals("Source IP should be swapped", destinationIp, ByteBuffer.wrap(swappedPacket.getIp4Header().sourceAddress.getAddress()).getInt() & 0xFFFFFFFF);
        assertEquals("Destination IP should be swapped", sourceIp, ByteBuffer.wrap(swappedPacket.getIp4Header().destinationAddress.getAddress()).getInt() & 0xFFFFFFFF);

        assertEquals("Source port should be swapped", destinationPort, swappedPacket.getTcpHeader().sourcePort);
        assertEquals("Destination port should be swapped", sourcePort, swappedPacket.getTcpHeader().destinationPort);
    }



    @Test
    public void testUpdateTCPChecksum() {
        int payloadSize = 20; // Example payload size
        int totalPacketLength = Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE + payloadSize;

        ByteBuffer buffer = ByteBuffer.allocate(totalPacketLength);

        // Initialize IPv4 header
        buffer.put((byte) 0x45); // Version (4) and Header Length (5)
        buffer.put((byte) 0x00); // Type of Service
        buffer.putShort((short) totalPacketLength); // Total Length
        buffer.putInt(0); // ID, Flags, Fragment Offset
        buffer.put((byte) 0x40); // TTL
        buffer.put((byte) 0x06); // Protocol (TCP)
        buffer.putInt(0xC0A80001); // Source IP
        buffer.putInt(0xC0A80002); // Destination IP

        // Initialize TCP header
        buffer.position(Packet.IP4_HEADER_SIZE);
        buffer.putShort((short) 1234); // Source port
        buffer.putShort((short) 80); // Destination port
        buffer.putInt(0); // Sequence number
        buffer.putInt(0); // Acknowledgment number
        buffer.putShort((short) (5 << 4)); // Data offset
        buffer.putShort((short) 0); // Checksum placeholder
        buffer.putShort((short) 0); // Urgent pointer

        // Fill with dummy payload
        buffer.position(Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE);
        buffer.put(new byte[payloadSize]);

        buffer.flip();

        Packet packet = new Packet(buffer);

        // Ensure the buffer is writable before updating the checksum
        ByteBuffer writableBuffer = buffer.duplicate();
        packet.updateTCPChecksum(payloadSize);

    }








}
