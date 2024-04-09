package com.fyp.mydataismine.packetcapture;

import static com.google.firebase.appcheck.internal.util.Logger.TAG;

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Represents the IPv4 header of a network packet. This class provides methods to parse
 * and access IPv4 header fields such as version, header length, total length, protocol, and addresses.
 */
public class IP4Header {

    // Class fields represent IPv4 header components
    public byte version;
    public byte IHL;  // Internet Header Length
    public int headerLength;
    public short typeOfService;
    public int totalLength;
    public int identificationAndFlagsAndFragmentOffset;
    public short TTL;
    private short protocolNum;
    public TransportProtocol protocol;
    public int headerChecksum;
    public InetAddress sourceAddress;
    public InetAddress destinationAddress;

    public static final int IP4_HEADER_SIZE = 20;

    /**
     * Constructs an IP4Header instance by parsing data from the given ByteBuffer.
     *
     * @param buffer The ByteBuffer containing the raw IPv4 header data.
     * @throws UnknownHostException if IP address conversion fails.
     */
    public IP4Header(ByteBuffer buffer) throws UnknownHostException {
        if (buffer.remaining() < IP4_HEADER_SIZE) {
            throw new IllegalArgumentException("Buffer too small for IPv4 header");
        }

        byte versionAndIHL = buffer.get();
        this.version = (byte) (versionAndIHL >> 4);
        if (version != 4) {
            Log.w(TAG, "Not an IPv4 packet");
            return; // Skip processing this packet
        }
        this.IHL = (byte) (versionAndIHL & 0x0F);
        this.headerLength = this.IHL * 4;
        //this.headerLength = this.IHL << 2;

        if (this.version != 4) {
            throw new IllegalArgumentException("Not an IPv4 packet");
        }

        this.typeOfService = BitUtils.getUnsignedByte(buffer.get());
        this.totalLength = BitUtils.getUnsignedShort(buffer.getShort());

        this.identificationAndFlagsAndFragmentOffset = buffer.getInt();

        this.TTL = BitUtils.getUnsignedByte(buffer.get());
        this.protocolNum = BitUtils.getUnsignedByte(buffer.get());
        this.protocol = TransportProtocol.numberToEnum(protocolNum);
        this.headerChecksum = BitUtils.getUnsignedShort(buffer.getShort());

        byte[] sourceAddressBytes = new byte[4];
        buffer.get(sourceAddressBytes);
        //buffer.get(addressBytes, 0, 4);
        this.sourceAddress = InetAddress.getByAddress(sourceAddressBytes);

        byte[] destinationAddressBytes = new byte[4];
        //buffer.get(addressBytes, 0, 4);
        buffer.get(destinationAddressBytes);
        this.destinationAddress = InetAddress.getByAddress(destinationAddressBytes);
    }

    /**
     * Swaps the source and destination addresses of this header, effectively flipping the packet direction.
     *
     * @return The IP4Header instance with swapped addresses.
     */
    public IP4Header swapSourceAndDestination() {
        InetAddress temp = sourceAddress;
        sourceAddress = destinationAddress;
        destinationAddress = temp;
        return this; // Return the updated IP4Header object
    }

    /**
     * Returns the protocol name for this header's transport layer protocol.
     *
     * @return A string representing the transport protocol name.
     */
    public String getTransportProtocolName() {
        return protocol.getProtocolName();
    }

    /**
     * Fills the given ByteBuffer with the header's data, useful for packet reconstruction or forwarding.
     *
     * @param buffer The ByteBuffer to fill with the header data.
     */
    public void fillHeader(ByteBuffer buffer) {
        buffer.put((byte) (this.version << 4 | this.IHL));
        buffer.put((byte) this.typeOfService);
        buffer.putShort((short) this.totalLength);

        buffer.putInt(this.identificationAndFlagsAndFragmentOffset);

        buffer.put((byte) this.TTL);
        buffer.put((byte) this.protocol.getNumber());
        buffer.putShort((short) this.headerChecksum);

        buffer.put(this.sourceAddress.getAddress());
        buffer.put(this.destinationAddress.getAddress());
    }

    // Getters
    public int getIHL() {
        return IHL;
    }

    public int getVersion() { return this.version; }

    public int getTypeOfService() {
        return this.typeOfService & 0xFF;
    }

    public int getTotalLength() {
        return this.totalLength;
    }

    public int getIdentification() {
        return this.identificationAndFlagsAndFragmentOffset >> 16;
    }

    public int getProtocol() {
        return this.protocol.getNumber();  // Assuming you have a method getNumber() in TransportProtocol enum
    }

    public InetAddress getDestinationAddress() {
        return this.destinationAddress;
    }

    public InetAddress getSourceAddress() {
        return this.sourceAddress;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("IP4Header{");
        sb.append("version=").append(version);
        sb.append(", IHL=").append(IHL);
        sb.append(", typeOfService=").append(typeOfService);
        sb.append(", totalLength=").append(totalLength);
        sb.append(", identificationAndFlagsAndFragmentOffset=").append(identificationAndFlagsAndFragmentOffset);
        sb.append(", TTL=").append(TTL);
        sb.append(", protocol=").append(protocolNum).append(":").append(protocol);
        sb.append(", headerChecksum=").append(headerChecksum);
        sb.append(", sourceAddress=").append(sourceAddress.getHostAddress());
        sb.append(", destinationAddress=").append(destinationAddress.getHostAddress());
        sb.append('}');
        return sb.toString();
    }
}