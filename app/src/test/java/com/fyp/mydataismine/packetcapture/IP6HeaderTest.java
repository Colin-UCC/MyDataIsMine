package com.fyp.mydataismine.packetcapture;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class IP6HeaderTest {

    @Test
    public void testIP6HeaderParsing() throws Exception {
        // Prepare a ByteBuffer with mock IPv6 header data
        ByteBuffer buffer = ByteBuffer.allocate(40); // IPv6 header is 40 bytes
        //buffer.putInt(0x60200000); // Version, traffic class, and flow label
        buffer.putInt(0x62000000);
        buffer.putShort((short) 0); // Payload length
        buffer.put((byte) 0x3A); // Next header (58 for ICMPv6)
        buffer.put((byte) 64); // Hop limit

        byte[] srcAddress = InetAddress.getByName("2001:db8::1").getAddress();
        byte[] destAddress = InetAddress.getByName("2001:db8::2").getAddress();
        buffer.put(srcAddress);
        buffer.put(destAddress);

        buffer.flip(); // Reset position to 0 to read

        // Create an IP6Header instance from the buffer
        IP6Header header = new IP6Header(buffer);

        // Assert the values are parsed correctly
        Assert.assertEquals(6, header.getVersion());
        Assert.assertEquals(0x20, header.getTrafficClass());
        Assert.assertEquals(0, header.getFlowLabel());
        Assert.assertEquals(0, header.getPayloadLength());
        Assert.assertEquals(58, header.getNextHeader());
        Assert.assertEquals(64, header.getHopLimit());
        Assert.assertEquals("2001:db8:0:0:0:0:0:1", header.getSourceAddress().getHostAddress());
        Assert.assertEquals("2001:db8:0:0:0:0:0:2", header.getDestinationAddress().getHostAddress());
    }
}
