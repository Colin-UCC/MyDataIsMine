package com.fyp.mydataismine.packetcapture;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class UDPHeaderTest {

    @Test
    public void testUDPHeaderParsing() {
        // Creating a ByteBuffer containing example UDP header data
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putShort((short) 12345);  // Source port
        buffer.putShort((short) 54321);  // Destination port
        buffer.putShort((short) 8);      // Length
        buffer.putShort((short) 0);      // Checksum
        buffer.flip(); // Reset position to the start

        // Initialize UDPHeader
        UDPHeader udpHeader = new UDPHeader(buffer);

        // Assertions to ensure the header is parsed correctly
        Assert.assertEquals(12345, udpHeader.getSourcePort());
        Assert.assertEquals(54321, udpHeader.getDestinationPort());
        Assert.assertEquals(8, udpHeader.length);
        Assert.assertEquals(0, udpHeader.checksum);
    }

    @Test
    public void testSwapSourceAndDestination() {
        // Initialize UDPHeader with sample ports
        UDPHeader udpHeader = new UDPHeader(ByteBuffer.wrap(new byte[8]));
        udpHeader.sourcePort = 12345;
        udpHeader.destinationPort = 54321;

        // Swap the ports
        udpHeader.swapSourceAndDestination();

        // Assertions to check if ports were swapped
        Assert.assertEquals(54321, udpHeader.getSourcePort());
        Assert.assertEquals(12345, udpHeader.getDestinationPort());
    }

    @Test
    public void testFillHeader() {
        // Create a UDPHeader and fill it
        UDPHeader udpHeader = new UDPHeader(ByteBuffer.allocate(8));
        udpHeader.sourcePort = 12345;
        udpHeader.destinationPort = 54321;
        udpHeader.length = 8;
        udpHeader.checksum = 0;

        // ByteBuffer to fill
        ByteBuffer buffer = ByteBuffer.allocate(8);
        udpHeader.fillHeader(buffer);
        buffer.flip(); // Reset position to read back data

        // Assertions to check if buffer is filled correctly
        Assert.assertEquals(12345, buffer.getShort() & 0xFFFF);
        Assert.assertEquals(54321, buffer.getShort() & 0xFFFF);
        Assert.assertEquals(8, buffer.getShort() & 0xFFFF);
        Assert.assertEquals(0, buffer.getShort() & 0xFFFF);
    }

}
