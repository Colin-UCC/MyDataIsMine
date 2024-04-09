package com.fyp.mydataismine.packetcapture;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class ICMPv6HeaderTest {

    @Test
    public void testICMPv6HeaderParsing() {
        byte type = 0x3A;
        byte code = 0x20;
        short checksum = 0x1A2B;

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put(type);
        buffer.put(code);
        buffer.putShort(checksum);
        buffer.flip(); // Reset pointer to the start of the buffer for reading

        // Create ICMPv6Header from the buffer
        ICMPv6Header icmpv6Header = new ICMPv6Header(buffer);

        // Assert tests
        Assert.assertEquals("Type should match", 0xFF & type, icmpv6Header.getType());
        Assert.assertEquals("Code should match", 0xFF & code, icmpv6Header.getCode());
        Assert.assertEquals("Checksum should match", Short.toUnsignedInt(checksum), icmpv6Header.getChecksum());
    }

}
