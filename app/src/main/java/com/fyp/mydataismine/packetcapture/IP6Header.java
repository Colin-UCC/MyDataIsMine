package com.fyp.mydataismine.packetcapture;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Represents the IPv6 header of a network packet. This class provides methods to parse
 * and access IPv6 header fields such as version, traffic class, flow label, and addresses.
 */
public class IP6Header {
    // IPv6 specific fields
    private int version;
    private int trafficClass;
    private int flowLabel;
    private int payloadLength;
    private int nextHeader;
    private int hopLimit;
    private InetAddress sourceAddress;
    private InetAddress destinationAddress;

    /**
     * Constructs an IP6Header instance by parsing data from the given ByteBuffer.
     *
     * @param buffer The ByteBuffer containing the raw IPv6 header data.
     * @throws UnknownHostException if IP address conversion fails.
     */
    public IP6Header(ByteBuffer buffer) throws UnknownHostException {
        // Parse the first 4 bytes for version, traffic class, and flow label
        int firstFourBytes = buffer.getInt();
        this.version = (firstFourBytes >> 28) & 0x0F;
        this.trafficClass = (firstFourBytes >> 20) & 0xFF;
        this.flowLabel = firstFourBytes & 0xFFFFF;

        // Next 2 bytes for payload length
        this.payloadLength = buffer.getShort() & 0xFFFF;

        // Next header (protocol) and hop limit
        this.nextHeader = buffer.get() & 0xFF;
        this.hopLimit = buffer.get() & 0xFF;

        // Source and destination addresses (16 bytes each)
        byte[] addressBytes = new byte[16];
        buffer.get(addressBytes);
        this.sourceAddress = InetAddress.getByAddress(addressBytes);

        buffer.get(addressBytes);
        this.destinationAddress = InetAddress.getByAddress(addressBytes);
    }

    // Getters and setters

    public int getVersion() {
        return version;
    }
    public int getTrafficClass() {
        return trafficClass;
    }
    public int getFlowLabel() {
        return flowLabel;
    }
    public int getPayloadLength() {
        return payloadLength;
    }
    public int getNextHeader() {
        return nextHeader;
    }
    public int getHopLimit() {
        return hopLimit;
    }
    public InetAddress getSourceAddress() {
        return sourceAddress;
    }
    public InetAddress getDestinationAddress() {
        return destinationAddress;
    }

    @Override
    public String toString() {
        return "IP6Header{" +
                "version=" + version +
                ", trafficClass=" + trafficClass +
                ", flowLabel=" + flowLabel +
                ", payloadLength=" + payloadLength +
                ", nextHeader=" + nextHeader +
                ", hopLimit=" + hopLimit +
                ", sourceAddress=" + sourceAddress.getHostAddress() +
                ", destinationAddress=" + destinationAddress.getHostAddress() +
                '}';
    }
}
