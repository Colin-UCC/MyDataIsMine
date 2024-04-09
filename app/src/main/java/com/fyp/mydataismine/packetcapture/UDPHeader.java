package com.fyp.mydataismine.packetcapture;

import java.nio.ByteBuffer;

/**
 * Represents the header of a UDP (User Datagram Protocol) packet, encapsulating the essential information such as ports and checksum.
 */
public class UDPHeader {

    public int sourcePort;
    public int destinationPort;
    public int length;
    public int checksum;

    /**
     * Constructs a UDPHeader object by parsing the necessary fields from the given ByteBuffer.
     * @param buffer ByteBuffer containing the UDP header data.
     */
    public UDPHeader(ByteBuffer buffer)
    {
        this.sourcePort = BitUtils.getUnsignedShort(buffer.getShort());
        this.destinationPort = BitUtils.getUnsignedShort(buffer.getShort());

        this.length = BitUtils.getUnsignedShort(buffer.getShort());
        this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
    }

    /**
     * Swaps the source and destination ports, useful for response packets.
     * @return The UDPHeader object with swapped source and destination ports.
     */
    public UDPHeader swapSourceAndDestination() {
        int tempPort = sourcePort;
        sourcePort = destinationPort;
        destinationPort = tempPort;
        return this;
    }

    /**
     * Fills a ByteBuffer with the UDP header information, useful for constructing packet data.
     * @param buffer ByteBuffer to fill with the UDP header data.
     */
    public void fillHeader(ByteBuffer buffer)
    {
        buffer.putShort((short) this.sourcePort);
        buffer.putShort((short) this.destinationPort);

        buffer.putShort((short) this.length);
        buffer.putShort((short) this.checksum);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("UDPHeader{");
        sb.append("sourcePort=").append(sourcePort);
        sb.append(", destinationPort=").append(destinationPort);
        sb.append(", length=").append(length);
        sb.append(", checksum=").append(checksum);
        sb.append('}');
        return sb.toString();
    }

    public int getSourcePort() { return sourcePort;
    }

    public int getDestinationPort() { return destinationPort;
    }
}
