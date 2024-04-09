package com.fyp.mydataismine.packetcapture;

import java.nio.ByteBuffer;

/**
 * Represents the header of an ICMPv6 packet, providing access to its type, code, and checksum.
 * This class is used in network traffic analysis to interpret ICMPv6 packets,
 * which are important for error reporting and diagnostic purposes in IPv6 networks.
 */
public class ICMPv6Header {
    private byte type;
    private byte code;
    private int checksum;

    /**
     * Constructs an ICMPv6Header by parsing the necessary fields from the provided ByteBuffer.
     * @param buffer The ByteBuffer containing the raw bytes of the ICMPv6 header.
     */
    public ICMPv6Header(ByteBuffer buffer) {
        this.type = buffer.get();
        this.code = buffer.get();
        this.checksum = Short.toUnsignedInt(buffer.getShort());
    }

    /**
     * Gets the type of the ICMPv6 packet.
     * @return The type as an unsigned integer.
     */
    public int getType() {
        return type & 0xFF;  // Convert to unsigned integer for public access
    }

    /**
     * Gets the code of the ICMPv6 packet.
     * @return The code as an unsigned integer.
     */
    public int getCode() {
        return code & 0xFF;  // Convert to unsigned integer for public access
    }

    /**
     * Gets the checksum of the ICMPv6 packet.
     * @return The checksum as an integer.
     */
    public int getChecksum() {
        return checksum;
    }



    @Override
    public String toString() {
        return "ICMPv6Header{" +
                "type=" + type +
                ", code=" + code +
                ", checksum=" + checksum +
                '}';
    }
}
