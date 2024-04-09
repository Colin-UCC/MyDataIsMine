package com.fyp.mydataismine.packetcapture;

import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PacketTest {
    private ByteBuffer ipv4Buffer;
    private ByteBuffer ipv6Buffer;

    @Before
    public void setUp() {
        // Assuming a payload size of 20 bytes for both IPv4 and IPv6
        byte[] ipv4Payload = new byte[20]; // You can fill this with actual data if needed
        byte[] ipv6Payload = new byte[20]; // You can fill this with actual data if needed

        ipv4Buffer = ByteBuffer.allocate(Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE + ipv4Payload.length);
        ipv6Buffer = ByteBuffer.allocate(40 + Packet.TCP_HEADER_SIZE + ipv6Payload.length); // IPv6 header is 40 bytes

        initializeIPv4Buffer(ipv4Buffer, ipv4Payload);
        initializeIPv6Buffer(ipv6Buffer, ipv6Payload); // Assuming you have a similar method for IPv6
    }

    private void initializeIPv4Buffer(ByteBuffer buffer, byte[] payload) {
        buffer.clear();
        buffer.put((byte) 0x45); // IPv4 version and IHL
        buffer.put((byte) 0x00); // Type of Service
        buffer.putShort((short) (Packet.IP4_HEADER_SIZE + payload.length)); // Total length
        buffer.putInt(0); // Identification, Flags, Fragment Offset
        buffer.putInt(0); // TTL, Protocol, Header Checksum
        buffer.putInt(0); // Source IP
        buffer.putInt(0); // Destination IP
        buffer.put(payload); // Payload
        buffer.flip();
    }


//    private void initializeIPv4Buffer(ByteBuffer buffer) {
//        buffer.clear();
//        // Example IPv4 header with minimal fields set
//        buffer.put((byte) 0x45); // IPv4 version and IHL
//        buffer.put((byte) 0x00); // Type of Service
//        buffer.putShort((short) (Packet.IP4_HEADER_SIZE + 20)); // Total length
//        buffer.putInt(0); // Identification, Flags, Fragment Offset
//        buffer.putInt(0); // TTL, Protocol, Header Checksum
//        buffer.putInt(0); // Source IP
//        buffer.putInt(0); // Destination IP
//        buffer.put(new byte[20]); // Example TCP/UDP payload
//        buffer.flip();
//    }

    private void initializeIPv6Buffer(ByteBuffer buffer, byte[] payload) {
        buffer.clear();
        buffer.put((byte) 0x60); // IPv6 version and traffic class
        buffer.put(new byte[3]); // Flow label
        buffer.putShort((short) payload.length); // Payload length
        buffer.put((byte) 0x06); // Next header (TCP, for example)
        buffer.put((byte) 0xFF); // Hop limit
        buffer.put(new byte[16]); // Source address (16 bytes)
        buffer.put(new byte[16]); // Destination address (16 bytes)
        buffer.put(payload); // Inserting the payload
        buffer.flip();
    }



    @Test
    public void testIPv6PacketParsing() throws UnknownHostException {
        Packet packet = new Packet(ipv6Buffer);
        assertNotNull("IPv6 Header should not be null", packet.ip6Header);
        assertNotNull("Payload should not be null", packet.getPayload());
        // Additional assertions can be added here to validate the contents of the IPv6 header
    }


    @Test
    public void testIPv4PacketParsing() throws UnknownHostException {
        Packet packet = new Packet(ipv4Buffer);
        assertNotNull(packet.ip4Header);
        assertNotNull(packet.getPayload());
        // Add more assertions to validate IPv4 packet parsing
    }

    @Test
    public void testPayloadRetrievalIPv4() {
        // Initialize the IPv4 buffer with some header and a known payload
        ByteBuffer ipv4Buffer = ByteBuffer.allocate(Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE + 5); // Adjust size for header + payload
        initializeIPv4Buffer(ipv4Buffer, new byte[]{0x68, 0x65, 0x6C, 0x6C, 0x6F}); // "hello" in ASCII

        Packet ipv4Packet = new Packet(ipv4Buffer);
        ByteBuffer ipv4Payload = ipv4Packet.getPayload();

        assertNotNull("IPv4 payload should not be null", ipv4Payload);
        assertEquals("IPv4 payload size should be 5 bytes", 5, ipv4Payload.remaining());

        // Expected payload content
        byte[] expectedPayload = {0x68, 0x65, 0x6C, 0x6C, 0x6F}; // "hello" in ASCII
        byte[] actualPayload = new byte[ipv4Payload.remaining()];
        ipv4Payload.get(actualPayload);

        assertArrayEquals("IPv4 payload content should match", expectedPayload, actualPayload);
    }

    @Test
    public void testIPv6TcpHeaderParsing() throws UnknownHostException {
        // Initialize IPv6 buffer with TCP segment
        byte[] ipv6Payload = new byte[20]; // Mock TCP payload
        ByteBuffer ipv6Buffer = ByteBuffer.allocate(40 + Packet.TCP_HEADER_SIZE + ipv6Payload.length);
        initializeIPv6Buffer(ipv6Buffer, ipv6Payload);

        Packet ipv6Packet = new Packet(ipv6Buffer);
        assertNotNull("IPv6 TCP header should not be null", ipv6Packet.tcpHeader);
        assertEquals("Next header should be TCP", TransportProtocol.TCP.getNumber(), ipv6Packet.getNextHeader());
    }

//    @Test
//    public void testIPv6UdpHeaderParsing() throws UnknownHostException {
//        // Prepare a UDP payload for the IPv6 packet
//        byte[] ipv6Payload = new byte[20]; // Example payload size
//        ByteBuffer ipv6Buffer = ByteBuffer.allocate(40 + Packet.UDP_HEADER_SIZE + ipv6Payload.length);
//
//        // Initialize IPv6 buffer with a UDP segment
//        initializeIPv6Buffer(ipv6Buffer, ipv6Payload, TransportProtocol.UDP.getNumber());
//
//        // Parse the packet from the buffer
//        Packet ipv6Packet = new Packet(ipv6Buffer);
//
//        // Verify that the udpHeader is correctly initialized
//        assertNotNull("IPv6 UDP header should not be null", ipv6Packet.udpHeader);
//        assertEquals("Next header should be UDP", TransportProtocol.UDP.getNumber(), ipv6Packet.getNextHeader());
//    }



}



//
//import static org.junit.Assert.*;
//
//import org.junit.Before;
//import org.junit.Test;
//import java.nio.ByteBuffer;
//import java.net.InetAddress;
//
//public class PacketTest {
//    private Packet ipv4Packet;
//    private Packet ipv6Packet;
//
//    @Before
//    public void setUp() {
//        // Initialize ByteBuffer with test data for IPv4 and IPv6 packets
//        ByteBuffer ipv4Buffer = ByteBuffer.allocate(Packet.MAX_PACKET_SIZE);
//        ByteBuffer ipv6Buffer = ByteBuffer.allocate(Packet.MAX_PACKET_SIZE);
//        // For IPv4
//        initializeIPv4Buffer(ipv4Buffer);
//        // For IPv6
//        initializeIPv6Buffer(ipv6Buffer);
//
//        ipv4Packet = new Packet(ipv4Buffer);
//        ipv6Packet = new Packet(ipv6Buffer);
//    }
//
//        private void initializeIPv4Buffer(ByteBuffer buffer) {
//        buffer.clear();
//
//        // Example data for IPv4 header
//        byte versionAndIHL = 0x45; // IPv4 and 5 * 4 = 20 bytes header length
//        byte typeOfService = 0x00;
//        short totalLength = 20; // header length only for simplicity
//        short identification = 0x00;
//        short flagsAndFragmentOffset = 0x00;
//        byte ttl = 64;
//        byte protocol = 6; // TCP
//        short headerChecksum = 0; // For simplicity, set to 0 here, would be calculated normally
//        int sourceAddress = 0xC0A80001; // 192.168.0.1
//        int destinationAddress = 0xC0A80002; // 192.168.0.2
//
//        buffer.put(versionAndIHL);
//        buffer.put(typeOfService);
//        buffer.putShort(totalLength);
//        buffer.putShort(identification);
//        buffer.putShort(flagsAndFragmentOffset);
//        buffer.put(ttl);
//        buffer.put(protocol);
//        buffer.putShort(headerChecksum);
//        buffer.putInt(sourceAddress);
//        buffer.putInt(destinationAddress);
//
//        buffer.flip(); // Reset buffer position to be ready for reading
//    }
//
//
//    private void initializeIPv6Buffer(ByteBuffer buffer) {
//        buffer.clear();
//
//        // Example data for IPv6 header
//        int version = 6; // IPv6
//        int trafficClass = 0; // Default traffic class
//        int flowLabel = 0; // No specific flow label
//        short payloadLength = 0; // No payload for this example
//        byte nextHeader = 6; // Assuming TCP as the next header
//        byte hopLimit = 64; // Common default value for TTL/hop limit
//        byte[] sourceAddress = new byte[16]; // 128-bit source IPv6 address
//        byte[] destinationAddress = new byte[16]; // 128-bit destination IPv6 address
//
//        // Set IPv6 source and destination addresses to localhost for this example
//        sourceAddress[15] = 1; // ::1
//        destinationAddress[15] = 1; // ::1
//
//        // Combine version, traffic class, and flow label into a single 4-byte field
//        int vtf = (version << 28) | (trafficClass << 20) | flowLabel;
//
//        buffer.putInt(vtf);
//        buffer.putShort(payloadLength);
//        buffer.put(nextHeader);
//        buffer.put(hopLimit);
//        buffer.put(sourceAddress);
//        buffer.put(destinationAddress);
//
//        buffer.flip(); // Reset buffer position to be ready for reading
//    }
//
//
//    @Test
//    public void testIPv4PacketTCPHeaderParsing() {
//        assertNotNull(ipv4Packet.tcpHeader);
//        assertEquals(6, ipv4Packet.ip4Header.getProtocol()); // Ensure protocol is TCP
//
//        // Add assertions to validate TCP header fields
//        // For example:
//        assertEquals(12345, ipv4Packet.tcpHeader.getSourcePort());
//        assertEquals(80, ipv4Packet.tcpHeader.getDestinationPort());
//        // Add more assertions as needed
//    }
//
//    @Test
//    public void testIPv4PacketUDPHeaderParsing() {
//        assertNotNull(ipv4Packet.udpHeader);
//        assertEquals(17, ipv4Packet.ip4Header.getProtocol()); // Ensure protocol is UDP
//
//        // Add assertions to validate UDP header fields
//        // For example:
//        assertEquals(53, ipv4Packet.udpHeader.getSourcePort());
//        assertEquals(123, ipv4Packet.udpHeader.getDestinationPort());
//        // Add more assertions as needed
//    }
//
//    @Test
//    public void testIPv6PacketTCPHeaderParsing() {
//        assertNotNull(ipv6Packet.tcpHeader);
//        assertEquals(6, ipv6Packet.ip6Header.getNextHeader()); // Ensure next header is TCP
//
//        // Add assertions to validate TCP header fields
//        // For example:
//        assertEquals(56789, ipv6Packet.tcpHeader.getSourcePort());
//        assertEquals(443, ipv6Packet.tcpHeader.getDestinationPort());
//        // Add more assertions as needed
//    }
//
//    @Test
//    public void testIPv6PacketUDPHeaderParsing() {
//        assertNotNull(ipv6Packet.udpHeader);
//        assertEquals(17, ipv6Packet.ip6Header.getNextHeader()); // Ensure next header is UDP
//
//        // Add assertions to validate UDP header fields
//        // For example:
//        assertEquals(12345, ipv6Packet.udpHeader.getSourcePort());
//        assertEquals(23456, ipv6Packet.udpHeader.getDestinationPort());
//        // Add more assertions as needed
//    }
//
//    @Test
//    public void testPayloadRetrievalIPv4() {
//        ByteBuffer ipv4Payload = ipv4Packet.getPayload();
//        assertNotNull(ipv4Payload);
//
//        // Add assertions to validate IPv4 payload content if applicable
//        // For example:
//        byte[] expectedPayload = {0x68, 0x65, 0x6C, 0x6C, 0x6F}; // "hello" in ASCII
//        byte[] actualPayload = new byte[ipv4Payload.remaining()];
//        ipv4Payload.get(actualPayload);
//        assertArrayEquals(expectedPayload, actualPayload);
//    }
//
//    @Test
//    public void testPayloadRetrievalIPv6() {
//        ByteBuffer ipv6Payload = ipv6Packet.getPayload();
//        assertNotNull(ipv6Payload);
//
//        // Add assertions to validate IPv6 payload content if applicable
//        // For example:
//        byte[] expectedPayload = {0x77, 0x6F, 0x72, 0x6C, 0x64}; // "world" in ASCII
//        byte[] actualPayload = new byte[ipv6Payload.remaining()];
//        ipv6Payload.get(actualPayload);
//        assertArrayEquals(expectedPayload, actualPayload);
//    }
//
//    // Add more tests as needed
//}







//package com.fyp.mydataismine.packetcapture;
//
//import org.junit.Test;
//import org.junit.Before;
//import static org.junit.Assert.*;
//
//import java.net.InetAddress;
//import java.nio.ByteBuffer;
//
//public class PacketTest {
//    private Packet ipv4Packet;
//    private Packet ipv6Packet;
//
//    private ByteBuffer createMockIPv4PacketBuffer() {
//        byte[] data = new byte[60]; // Adjust size based on needs
//        ByteBuffer buffer = ByteBuffer.wrap(data);
//
//        // Mock IPv4 Header (20 bytes)
//        buffer.put((byte) 0x45); // Version and IHL
//        buffer.put((byte) 0x00); // Type of Service
//        buffer.putShort((short) 60); // Total Length
//        buffer.putShort((short) 0); // Identification
//        buffer.putShort((short) 0); // Flags and Fragment Offset
//        buffer.put((byte) 64); // TTL
//        buffer.put((byte) 6); // Protocol (TCP)
//        buffer.putShort((short) 0); // Header Checksum
//        buffer.putInt(0xC0A80001); // Source IP (192.168.0.1)
//        buffer.putInt(0xC0A80002); // Destination IP (192.168.0.2)
//
//        buffer.rewind();
//        return buffer;
//    }
//
//    @Before
//    public void setUp() {
//        // Initialize ByteBuffer with test data for IPv4 and IPv6 packets
//        ByteBuffer ipv4Buffer = ByteBuffer.allocate(Packet.MAX_PACKET_SIZE);
//        ByteBuffer ipv6Buffer = ByteBuffer.allocate(Packet.MAX_PACKET_SIZE);
//        // For IPv4
//        initializeIPv4Buffer(ipv4Buffer);
//        // For IPv6
//        initializeIPv6Buffer(ipv6Buffer);
//
//        ipv4Packet = new Packet(ipv4Buffer);
//        ipv6Packet = new Packet(ipv6Buffer);
//    }
//
//    private void initializeIPv4Buffer(ByteBuffer buffer) {
//        buffer.clear();
//
//        // Example data for IPv4 header
//        byte versionAndIHL = 0x45; // IPv4 and 5 * 4 = 20 bytes header length
//        byte typeOfService = 0x00;
//        short totalLength = 20; // header length only for simplicity
//        short identification = 0x00;
//        short flagsAndFragmentOffset = 0x00;
//        byte ttl = 64;
//        byte protocol = 6; // TCP
//        short headerChecksum = 0; // For simplicity, set to 0 here, would be calculated normally
//        int sourceAddress = 0xC0A80001; // 192.168.0.1
//        int destinationAddress = 0xC0A80002; // 192.168.0.2
//
//        buffer.put(versionAndIHL);
//        buffer.put(typeOfService);
//        buffer.putShort(totalLength);
//        buffer.putShort(identification);
//        buffer.putShort(flagsAndFragmentOffset);
//        buffer.put(ttl);
//        buffer.put(protocol);
//        buffer.putShort(headerChecksum);
//        buffer.putInt(sourceAddress);
//        buffer.putInt(destinationAddress);
//
//        buffer.flip(); // Reset buffer position to be ready for reading
//    }
//
//
//    private void initializeIPv6Buffer(ByteBuffer buffer) {
//        buffer.clear();
//
//        // Example data for IPv6 header
//        int version = 6; // IPv6
//        int trafficClass = 0; // Default traffic class
//        int flowLabel = 0; // No specific flow label
//        short payloadLength = 0; // No payload for this example
//        byte nextHeader = 6; // Assuming TCP as the next header
//        byte hopLimit = 64; // Common default value for TTL/hop limit
//        byte[] sourceAddress = new byte[16]; // 128-bit source IPv6 address
//        byte[] destinationAddress = new byte[16]; // 128-bit destination IPv6 address
//
//        // Set IPv6 source and destination addresses to localhost for this example
//        sourceAddress[15] = 1; // ::1
//        destinationAddress[15] = 1; // ::1
//
//        // Combine version, traffic class, and flow label into a single 4-byte field
//        int vtf = (version << 28) | (trafficClass << 20) | flowLabel;
//
//        buffer.putInt(vtf);
//        buffer.putShort(payloadLength);
//        buffer.put(nextHeader);
//        buffer.put(hopLimit);
//        buffer.put(sourceAddress);
//        buffer.put(destinationAddress);
//
//        buffer.flip(); // Reset buffer position to be ready for reading
//    }
//
//
//    @Test
//    public void testIPv4PacketParsing() {
//        // Ensure the IP4Header object is not null
//        assertNotNull(ipv4Packet.ip4Header);
//
//        // Check that the IP version is correctly set to 4
//        assertEquals(4, ipv4Packet.ip4Header.getVersion());
//
//        int expectedIHL = 20;
//        int expectedTOS = 0;
//        int expectedTotalLength = 60;
//        int expectedIdentification = 0;
//        int expectedProtocol = 6;
//        String expectedSourceIP = "192.168.1.1";
//        String expectedDestinationIP = "192.168.1.2";
//
//        // Validate other fields in the IP header, for example:
//        assertEquals(expectedIHL, ipv4Packet.ip4Header.getIHL());  // Header length
//        assertEquals(expectedTOS, ipv4Packet.ip4Header.getTypeOfService()); // Type of Service
//        assertEquals(expectedTotalLength, ipv4Packet.ip4Header.getTotalLength()); // Total Length
//        assertEquals(expectedIdentification, ipv4Packet.ip4Header.getIdentification()); // Identification
//        assertEquals(expectedProtocol, ipv4Packet.ip4Header.getProtocol()); // Protocol
//        assertEquals(expectedSourceIP, ipv4Packet.ip4Header.getSourceAddress().getHostAddress()); // Source IP address
//        assertEquals(expectedDestinationIP, ipv4Packet.ip4Header.getDestinationAddress().getHostAddress()); // Destination IP address
//
//
//    }
//
//
//    @Test
//    public void testIPv6PacketParsing() {
//        assertNotNull(ipv6Packet.ip6Header);
//        assertEquals(6, ipv6Packet.ip6Header.getVersion());
//        // Add more assertions to validate IPv6 header fields
//    }
//
//    @Test
//    public void testPayloadRetrieval() {
//        ByteBuffer ipv4Payload = ipv4Packet.getPayload();
//        assertNotNull(ipv4Payload);
//        // Compare with expected payload size or content
//
//        ByteBuffer ipv6Payload = ipv6Packet.getPayload();
//        assertNotNull(ipv6Payload);
//        // Compare with expected payload size or content
//    }
//
//    @Test
//    public void testAddressSwapping() {
//        InetAddress originalSource = ipv4Packet.ip4Header.getSourceAddress();
//        InetAddress originalDest = ipv4Packet.ip4Header.getDestinationAddress();
//
//        ipv4Packet.swapSourceAndDestination();
//
//        assertEquals(originalSource, ipv4Packet.ip4Header.getDestinationAddress());
//        assertEquals(originalDest, ipv4Packet.ip4Header.getSourceAddress());
//    }
//
//}
