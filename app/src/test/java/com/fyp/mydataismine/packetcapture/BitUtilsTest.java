package com.fyp.mydataismine.packetcapture;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the BitUtils utility methods.
 */
public class BitUtilsTest {

    @Test
    public void getUnsignedByte_correctlyConvertsNegativeByte() {
        byte negativeByte = -1; // 0xFF in two's complement
        short expected = 255;   // Unsigned 0xFF
        assertEquals(expected, BitUtils.getUnsignedByte(negativeByte));
    }

    @Test
    public void getUnsignedByte_correctlyConvertsPositiveByte() {
        byte positiveByte = 127; // 0x7F
        short expected = 127;
        assertEquals(expected, BitUtils.getUnsignedByte(positiveByte));
    }

    @Test
    public void getUnsignedShort_correctlyConvertsNegativeShort() {
        short negativeShort = -1; // 0xFFFF in two's complement
        int expected = 65535;     // Unsigned 0xFFFF
        assertEquals(expected, BitUtils.getUnsignedShort(negativeShort));
    }

    @Test
    public void getUnsignedShort_correctlyConvertsPositiveShort() {
        short positiveShort = 32767; // 0x7FFF
        int expected = 32767;
        assertEquals(expected, BitUtils.getUnsignedShort(positiveShort));
    }

    @Test
    public void getUnsignedInt_correctlyConvertsNegativeInt() {
        int negativeInt = -1;    // 0xFFFFFFFF in two's complement
        long expected = 4294967295L; // Unsigned 0xFFFFFFFF
        assertEquals(expected, BitUtils.getUnsignedInt(negativeInt));
    }

    @Test
    public void getUnsignedInt_correctlyConvertsPositiveInt() {
        int positiveInt = Integer.MAX_VALUE; // 0x7FFFFFFF
        long expected = 2147483647L; // 0x7FFFFFFF
        assertEquals(expected, BitUtils.getUnsignedInt(positiveInt));
    }
}
