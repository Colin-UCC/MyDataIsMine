package com.fyp.mydataismine.packetcapture;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class IP4HeaderTest {

    @Test
    public void parseBuffer_correctlyParsesIP4Header() throws Exception {
        // Create a byte array representing an IPv4 header
        byte[] ipHeader = {
                (byte) 0x45, // Version and IHL
                (byte) 0x00, // Type of Service
                (byte) 0x00, (byte) 0x14, // Total Length
                (byte) 0x00, (byte) 0x00, // Identification
                (byte) 0x40, (byte) 0x00, // Flags and Fragment Offset
                (byte) 0x40, // TTL
                (byte) 0x06, // Protocol (TCP)
                (byte) 0x00, (byte) 0x00, // Header checksum (just a placeholder)
                (byte) 192, (byte) 168, (byte) 0, (byte) 1, // Source IP
                (byte) 192, (byte) 168, (byte) 0, (byte) 2  // Destination IP
        };

        ByteBuffer buffer = ByteBuffer.wrap(ipHeader);
        IP4Header header = new IP4Header(buffer);

        Assert.assertEquals(4, header.version);
        Assert.assertEquals(5, header.IHL);
        Assert.assertEquals(20, header.headerLength);
        Assert.assertEquals(64, header.TTL);
        Assert.assertEquals(6, header.protocol.getNumber()); // TCP protocol
        Assert.assertEquals("192.168.0.1", header.sourceAddress.getHostAddress());
        Assert.assertEquals("192.168.0.2", header.destinationAddress.getHostAddress());
    }

    @Test
    public void swapSourceAndDestination_swapsAddressesCorrectly() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(IP4Header.IP4_HEADER_SIZE);
        buffer.put((byte) 0x45); // Version and IHL
        buffer.position(12); // Skip to the source address position
        buffer.put(InetAddress.getByName("1.2.3.4").getAddress());
        buffer.put(InetAddress.getByName("5.6.7.8").getAddress());
        buffer.flip();

        IP4Header header = new IP4Header(buffer);
        header.swapSourceAndDestination();

        Assert.assertEquals("5.6.7.8", header.sourceAddress.getHostAddress());
        Assert.assertEquals("1.2.3.4", header.destinationAddress.getHostAddress());
    }

    @Test
    public void fillHeader_correctlyFillsByteBuffer() throws Exception {
        // Mock ByteBuffer with initial IP4 header data
        byte[] mockHeaderData = new byte[IP4Header.IP4_HEADER_SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(mockHeaderData);

        IP4Header header = new IP4Header(buffer);

        header.version = 4;
        header.IHL = 5;
        header.typeOfService = 0;
        header.totalLength = 20;
        header.TTL = 64;
        header.protocol = TransportProtocol.TCP;
        header.sourceAddress = InetAddress.getByName("1.2.3.4");
        header.destinationAddress = InetAddress.getByName("5.6.7.8");

        // Create a new ByteBuffer for filling
        ByteBuffer fillBuffer = ByteBuffer.allocate(IP4Header.IP4_HEADER_SIZE);
        header.fillHeader(fillBuffer);
        fillBuffer.flip();

        // Parsing back the filled buffer to verify the filled data
        IP4Header parsedHeader = new IP4Header(fillBuffer);

        Assert.assertEquals(4, parsedHeader.version);
        Assert.assertEquals(5, parsedHeader.IHL);
        Assert.assertEquals(64, parsedHeader.TTL);
        Assert.assertEquals(TransportProtocol.TCP, parsedHeader.protocol);
        Assert.assertEquals("1.2.3.4", parsedHeader.sourceAddress.getHostAddress());
        Assert.assertEquals("5.6.7.8", parsedHeader.destinationAddress.getHostAddress());
    }

}
