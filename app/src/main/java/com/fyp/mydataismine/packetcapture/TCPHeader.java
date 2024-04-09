package com.fyp.mydataismine.packetcapture;

import static com.fyp.mydataismine.packetcapture.Packet.TCP_HEADER_SIZE;

import java.nio.ByteBuffer;

/**
 * Represents the TCP (Transmission Control Protocol) header of a network packet.
 * This class provides mechanisms to parse and manipulate fields of the TCP header.
 */
public class TCPHeader {

    // TCP header flags constants
    public static final int FIN = 0x01;
    public static final int SYN = 0x02;
    public static final int RST = 0x04;
    public static final int PSH = 0x08;
    public static final int ACK = 0x10;
    public static final int URG = 0x20;

    // TCP header fields
    public int sourcePort;
    public int destinationPort;
    public long sequenceNumber;
    public long acknowledgementNumber;
    public byte dataOffsetAndReserved;
    private byte dataOffset;
    public int headerLength;
    public byte flags;
    public int window;
    public int checksum;
    public int urgentPointer;
    public byte[] optionsAndPadding;

    /**
     * Parses the TCP header from the given ByteBuffer.
     *
     * @param buffer The buffer containing the TCP header data.
     */
    public TCPHeader(ByteBuffer buffer) {
        this.sourcePort = BitUtils.getUnsignedShort(buffer.getShort());
        this.destinationPort = BitUtils.getUnsignedShort(buffer.getShort());

        this.sequenceNumber = BitUtils.getUnsignedInt(buffer.getInt());
        this.acknowledgementNumber = BitUtils.getUnsignedInt(buffer.getInt());

        this.dataOffsetAndReserved = buffer.get();
        this.headerLength = ((this.dataOffsetAndReserved & 0xF0) >> 4) * 4;
        this.flags = buffer.get();
        this.window = BitUtils.getUnsignedShort(buffer.getShort());

        this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
        this.urgentPointer = BitUtils.getUnsignedShort(buffer.getShort());

        int optionsLength = this.headerLength - TCP_HEADER_SIZE;
        if (optionsLength > 0) {
            optionsAndPadding = new byte[optionsLength];
            buffer.get(optionsAndPadding, 0, optionsLength);
        }
    }

    /**
     * Swaps the source and destination ports to facilitate response packet creation.
     */
    public void swapSourceAndDestination()
    {
        int tempPort = sourcePort;
        sourcePort = destinationPort;
        destinationPort = tempPort;
    }

    /**
     * Gets the data offset field value in bytes.
     *
     * @return Data offset in bytes.
     */
    public int getDataOffset() {
        // Data offset field is the higher 4 bits of the 8-bit field, so shift right by 4.
        // Each unit of the data offset represents 4 bytes, so multiply by 4 to get the offset in bytes.
        return ((this.dataOffsetAndReserved >> 4) & 0x0F) * 4;
    }

    // Flag check methods
    public boolean isFIN()
    {
        return (flags & FIN) == FIN;
    }

    public boolean isSYN()
    {
        return (flags & SYN) == SYN;
    }

    public boolean isRST()
    {
        return (flags & RST) == RST;
    }

    public boolean isPSH()
    {
        return (flags & PSH) == PSH;
    }

    public boolean isACK()
    {
        return (flags & ACK) == ACK;
    }

    public boolean isURG()
    {
        return (flags & URG) == URG;
    }

    /**
     * Fills the provided ByteBuffer with the TCP header data.
     *
     * @param buffer The buffer to be filled with TCP header data.
     */
    public void fillHeader(ByteBuffer buffer)
    {
        buffer.putShort((short) sourcePort);
        buffer.putShort((short) destinationPort);

        buffer.putInt((int) sequenceNumber);
        buffer.putInt((int) acknowledgementNumber);

        buffer.put(dataOffsetAndReserved);
        buffer.put(flags);
        buffer.putShort((short) window);

        buffer.putShort((short) checksum);
        buffer.putShort((short) urgentPointer);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("TCPHeader{");
        sb.append("sourcePort=").append(sourcePort);
        sb.append(", destinationPort=").append(destinationPort);
        sb.append(", sequenceNumber=").append(sequenceNumber);
        sb.append(", acknowledgementNumber=").append(acknowledgementNumber);
        sb.append(", headerLength=").append(headerLength);
        sb.append(", window=").append(window);
        sb.append(", checksum=").append(checksum);
        sb.append(", flags=");
        if (isFIN()) sb.append(" FIN");
        if (isSYN()) sb.append(" SYN");
        if (isRST()) sb.append(" RST");
        if (isPSH()) sb.append(" PSH");
        if (isACK()) sb.append(" ACK");
        if (isURG()) sb.append(" URG");
        sb.append('}');
        return sb.toString();
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }
}

