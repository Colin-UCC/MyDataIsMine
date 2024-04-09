package com.fyp.mydataismine.packetcapture;

/**
 * Provides utility methods for converting signed numbers to unsigned numbers.
 * This class is essential in network packet analysis where data often needs to be interpreted as unsigned.
 */
public class BitUtils {
    /**
     * Converts a signed byte to an unsigned short.
     * @param value The signed byte to be converted.
     * @return The unsigned short representation of the input byte.
     */
    public static short getUnsignedByte(byte value)
    {
        return (short)(value & 0xFF);
    }

    /**
     * Converts a signed short to an unsigned integer.
     * @param value The signed short to be converted.
     * @return The unsigned integer representation of the input short.
     */
    public static int getUnsignedShort(short value)
    {
        return value & 0xFFFF;
    }

    /**
     * Converts a signed integer to an unsigned long.
     * @param value The signed integer to be converted.
     * @return The unsigned long representation of the input integer.
     */
    public static long getUnsignedInt(int value)
    {
        return value & 0xFFFFFFFFL;
    }
}
