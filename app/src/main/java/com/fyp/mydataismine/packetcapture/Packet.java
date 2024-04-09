package com.fyp.mydataismine.packetcapture;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Represents a network packet, including both IPv4 and IPv6 packets, and provides methods
 * for parsing and accessing different components of the packet, such as headers and payload.
 */
public final class Packet {

    // Constant declarations
    public static final int IP4_HEADER_SIZE = 20;
    public static final int TCP_HEADER_SIZE = 20;
    public static final int UDP_HEADER_SIZE = 8;
    public static final int MAX_PACKET_SIZE = 1500;

    // Header and protocol objects
    public IP4Header ip4Header;
    public IP6Header ip6Header;
    public TCPHeader tcpHeader;
    public UDPHeader udpHeader;
    private ICMPv6Header icmpv6Header;

    public ByteBuffer backingBuffer;

    private boolean isTCP;
    private boolean isUDP;

    /**
     * Parses the packet data from the given ByteBuffer and initializes the respective headers
     * based on the IP version and transport protocol.
     *
     * @param buffer ByteBuffer containing the raw packet data
     * @throws IllegalArgumentException if the packet data is invalid or unsupported
     */

    public Packet(ByteBuffer buffer) {
        try {
            buffer.mark();
            byte version = (byte) (buffer.get() >> 4);
            buffer.reset();

            if (version == 4) {
                parseIPv4(buffer);
            } else if (version == 6) {
                parseIPv6(buffer);
            } else {
                throw new IllegalArgumentException("Unsupported IP version: " + version);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP address", e);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Packet buffer is too short", e);
        }
        this.backingBuffer = buffer.asReadOnlyBuffer();
    }


//    public Packet(ByteBuffer buffer) throws IllegalArgumentException {
//        try {
//            // Determine if the packet is IPv4 or IPv6 based on the first 4 bits
//            buffer.mark();
//            byte version = (byte) (buffer.get() >> 4);
//            buffer.reset();
//
//            if (version == 4) {
//                // IPv4
//                this.ip4Header = new IP4Header(buffer);
//                parseIPv4(buffer);
//            } else if (version == 6) {
//                // IPv6
//                this.ip6Header = new IP6Header(buffer);
//                parseIPv6(buffer);
//            } else {
//                throw new IllegalArgumentException("Unsupported IP version: " + version);
//            }
//        } catch (UnknownHostException e) {
//            throw new IllegalArgumentException("Invalid IP address", e);
//        }
//
//        this.backingBuffer = buffer.asReadOnlyBuffer();
//    }

    /**
     * Parses the IPv4 header from the given ByteBuffer and initializes the appropriate transport layer header.
     * This method determines whether the transport protocol is TCP or UDP and initializes the corresponding header.
     * If the protocol is unsupported, it logs a warning and skips packet processing.
     *
     * @param buffer ByteBuffer containing the raw packet data positioned at the start of the IPv4 header.
     *               The buffer's position will be advanced to the end of the parsed header.
     */
    private void parseIPv4(ByteBuffer buffer) throws UnknownHostException {
        if (!hasEnoughData(buffer, IP4_HEADER_SIZE)) {
            throw new IllegalArgumentException("Not enough data for IPv4 header");
        }
        this.ip4Header = new IP4Header(buffer);

        // Based on the protocol, decide how much data should be available for the rest of the packet
        int expectedLength = this.ip4Header.totalLength - IP4_HEADER_SIZE;

        if (!hasEnoughData(buffer, expectedLength)) {
            throw new IllegalArgumentException("Not enough data for IPv4 payload");
        }

        // Logic for parsing IPv4 packets
        if (this.ip4Header.protocol == TransportProtocol.TCP) {
            if (!hasEnoughData(buffer, TCP_HEADER_SIZE)) {
                throw new IllegalArgumentException("Not enough data for TCP header");
            }
            this.tcpHeader = new TCPHeader(buffer);
            this.udpHeader = null;

        } else if (ip4Header.protocol == TransportProtocol.UDP) {
            //Log.d(TAG, "test2");
            this.udpHeader = new UDPHeader(buffer);
            this.tcpHeader = null;
        } else {
            //Log.w(TAG, "Unsupported IPv4 protocol encountered, skipping packet.");
            this.tcpHeader = null;
            this.udpHeader = null;
            //Log.w(TAG, buffer.toString());
        }
    }

    /**
     * Retrieves the payload of the packet as a ByteBuffer.
     *
     * @return ByteBuffer containing the packet's payload, or null if there is no payload.
     */
    public ByteBuffer getPayload() {
        int headerLength = 0;
        int totalLength = 0;
        ByteBuffer payload = null;

        // Determine header length based on IP version
        if (this.ip4Header != null) {
            // IPv4
            headerLength = this.ip4Header.headerLength;
            totalLength = this.ip4Header.totalLength;
        } else if (this.ip6Header != null) {
            // IPv6 - initial header is 40 bytes, does not include payload length
            headerLength = 40; // Fixed size for the IPv6 header
            totalLength = this.ip6Header.getPayloadLength() + headerLength; // Total length is payload length + header
        }

        // Adjust header length based on protocol
        if (this.isTCP() && this.tcpHeader != null) {
            headerLength += this.tcpHeader.getDataOffset(); // TCP data offset includes TCP header length
        } else if (this.isUDP()) {
            headerLength += UDP_HEADER_SIZE; // Add UDP header size
        }

        // Calculate payload size
        int payloadSize = totalLength - headerLength;
        if (payloadSize > 0) {
            payload = ByteBuffer.allocate(payloadSize);
            // Set the buffer's position to the start of the payload
            this.backingBuffer.position(headerLength);
            // Create a new limit for the buffer to mark the end of the payload
            this.backingBuffer.limit(headerLength + payloadSize);
            // Copy the payload into the new buffer
            payload.put(this.backingBuffer.slice());
            // Prepare the buffer for reading
            payload.flip();
        }

        return payload; // Return the payload, or null if there's no payload or an error occurred
    }

    /**
     * Logs details about the packet, such as source and destination IP addresses, and payload size.
     */
    public void logPacketDetails() {
        // Method to log details about the packet
        String sourceIp = this.ip4Header.sourceAddress.getHostAddress();
        String destinationIp = this.ip4Header.destinationAddress.getHostAddress();
        int payloadSize = getPayload() != null ? getPayload().remaining() : 0;
        Log.d("PacketDetails", "From: " + sourceIp + " To: " + destinationIp + " Payload size: " + payloadSize + " bytes");
    }

    /**
     * Swaps the source and destination addresses in the IP header.
     *
     * @return A new Packet instance with swapped source and destination addresses.
     */
    public Packet swapSourceAndDestination() {
        IP4Header newIp4Header = ip4Header.swapSourceAndDestination();
        if (isTCP()) {
            tcpHeader.swapSourceAndDestination();
        } else if (isUDP()) {
            udpHeader.swapSourceAndDestination();
        }
        return new Packet(newIp4Header, tcpHeader, udpHeader, backingBuffer);
    }

    /**
     * Updates the TCP buffer with the specified flags, sequence number, acknowledgment number, and payload size.
     * This method prepares the TCP header in the buffer for sending or processing, setting various fields such as
     * flags, sequence number, acknowledgment number, and recalculates the checksums for both IP and TCP headers.
     *
     * @param buffer The ByteBuffer to be updated with the TCP header and payload data.
     * @param flags The control flags for the TCP segment (e.g., SYN, ACK, FIN).
     * @param sequenceNum The sequence number for the TCP segment.
     * @param ackNum The acknowledgment number for the TCP segment.
     * @param payloadSize The size of the payload in bytes that follows the TCP header.
     */
    public void updateTCPBuffer(ByteBuffer buffer, byte flags, long sequenceNum, long ackNum, int payloadSize) {
        buffer.position(0);
        fillHeader(buffer);
        backingBuffer = buffer;

        tcpHeader.flags = flags;
        backingBuffer.put(IP4_HEADER_SIZE + 13, flags);

        tcpHeader.sequenceNumber = sequenceNum;
        backingBuffer.putInt(IP4_HEADER_SIZE + 4, (int) sequenceNum);

        tcpHeader.acknowledgementNumber = ackNum;
        backingBuffer.putInt(IP4_HEADER_SIZE + 8, (int) ackNum);

        // Reset header size, since we don't need options
        byte dataOffset = (byte) (TCP_HEADER_SIZE << 2);
        tcpHeader.dataOffsetAndReserved = dataOffset;
        backingBuffer.put(IP4_HEADER_SIZE + 12, dataOffset);

        updateTCPChecksum(payloadSize);

        int ip4TotalLength = IP4_HEADER_SIZE + TCP_HEADER_SIZE + payloadSize;
        backingBuffer.putShort(2, (short) ip4TotalLength);
        ip4Header.totalLength = ip4TotalLength;

        updateIP4Checksum();
    }

    /**
     * Updates the UDP buffer with the specified payload size.
     * This method prepares the UDP header in the buffer, setting the UDP length field and recalculating
     * the IP header checksum to reflect the new packet size. It also disables the UDP checksum validation
     * by setting it to zero.
     *
     * @param buffer The ByteBuffer to be updated with the UDP header and payload data.
     * @param payloadSize The size of the payload in bytes that follows the UDP header.
     */
    public void updateUDPBuffer(ByteBuffer buffer, int payloadSize) {
        buffer.position(0);
        fillHeader(buffer);
        backingBuffer = buffer;

        int udpTotalLength = UDP_HEADER_SIZE + payloadSize;
        backingBuffer.putShort(IP4_HEADER_SIZE + 4, (short) udpTotalLength);
        udpHeader.length = udpTotalLength;

        // Disable UDP checksum validation
        backingBuffer.putShort(IP4_HEADER_SIZE + 6, (short) 0);
        udpHeader.checksum = 0;

        int ip4TotalLength = IP4_HEADER_SIZE + udpTotalLength;
        backingBuffer.putShort(2, (short) ip4TotalLength);
        ip4Header.totalLength = ip4TotalLength;

        updateIP4Checksum();
    }

    /**
     * Recalculates and updates the IPv4 header checksum.
     * This method computes the checksum for the IPv4 header and updates the checksum field in the header.
     * It is essential to call this method after modifying any part of the IP header that affects the checksum.
     */
    private void updateIP4Checksum() {
        ByteBuffer buffer = backingBuffer.duplicate();
        buffer.position(0);

        // Clear previous checksum
        buffer.putShort(10, (short) 0);

        int ipLength = ip4Header.headerLength;
        int sum = 0;
        while (ipLength > 0) {
            sum += BitUtils.getUnsignedShort(buffer.getShort());
            ipLength -= 2;
        }
        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;
        ip4Header.headerChecksum = sum;
        backingBuffer.putShort(10, (short) sum);
    }

    /**
     * Parses the IPv6 header from the given buffer and initializes the appropriate transport layer header.
     * This method examines the Next Header field of the IPv6 header to determine the type of the
     * transport layer protocol (e.g., TCP, UDP, ICMPv6) and then initializes the corresponding header object.
     *
     * @param buffer ByteBuffer containing the packet data starting from the IPv6 header.
     */
    private void parseIPv6(ByteBuffer buffer) {
//        if (ip6Header == null) {
//            //Log.e(TAG, "parseIPv6: ip6Header is null");
//            return;
//        }
        try {
            this.ip6Header = new IP6Header(buffer);
            int nextHeader = ip6Header.getNextHeader();

            switch (nextHeader) {
                case 6: // Protocol number for TCP
                    this.tcpHeader = new TCPHeader(buffer);
                    this.udpHeader = null;
                    //Log.d(TAG, "parseIPv6: Parsed TCP header");
                    break;
                case 17: // Protocol number for UDP
                    this.udpHeader = new UDPHeader(buffer);
                    this.tcpHeader = null;
                    // Log.d(TAG, "parseIPv6: Parsed UDP header");
                    break;
                case 58: // Protocol number for ICMPv6
                    this.icmpv6Header = new ICMPv6Header(buffer);
                    // You can add additional processing here if needed
                    // Log.d(TAG, "parseIPv6: Parsed ICMPv6 header: " + icmpv6Header.toString());
                    break;
                default:
                    // Log.w(TAG, "parseIPv6: Unsupported IPv6 protocol encountered: " + nextHeader);
                    this.tcpHeader = null;
                    this.udpHeader = null;
                    break;
            }
        } catch (Exception e) {
            //Log.e(TAG, "parseIPv6: Error parsing IPv6 packet", e);
        }
    }

    /**
     * Constructs a packet instance with specified headers and data buffer.
     * This constructor allows for the manual assembly of a packet from its components,
     * useful for scenarios where packet headers and data are constructed or modified programmatically.
     *
     * @param ip4Header The IPv4 header of the packet; can be null if this is an IPv6 packet.
     * @param tcpHeader The TCP header of the packet; should be non-null if the packet is TCP, otherwise null.
     * @param udpHeader The UDP header of the packet; should be non-null if the packet is UDP, otherwise null.
     * @param buffer The ByteBuffer containing the data portion of the packet.
     * @throws IllegalArgumentException if the provided buffer is too small or otherwise invalid.
     */
    public Packet(IP4Header ip4Header, TCPHeader tcpHeader, UDPHeader udpHeader, ByteBuffer buffer) throws IllegalArgumentException {
        this.ip4Header = ip4Header;
        this.tcpHeader = tcpHeader;
        this.udpHeader = udpHeader;
        this.backingBuffer = ByteBuffer.allocate(buffer.remaining());
        this.backingBuffer.put(buffer);
        this.backingBuffer.flip();
    }

    /**
     * Calculates the payload size of the packet.
     * This method computes the size of the packet's data by subtracting the header size
     * from the total length of the packet.
     *
     * @return The size of the payload in bytes, or 0 if no payload is present or if the packet type is unsupported.
     */
    public int calculatePayloadSize() {
        if (ip4Header != null) {
            int headerSize = ip4Header.getIHL() * 4; // IHL to bytes

            if (isTCP() && tcpHeader != null) {
                headerSize += tcpHeader.getDataOffset() * 4; // TCP data offset to bytes
            } else if (isUDP()) {
                headerSize += UDP_HEADER_SIZE;
            }

            int payloadSize = ip4Header.totalLength - headerSize;
            return payloadSize > 0 ? payloadSize : 0;
        }
        return 0; // No payload or unsupported packet type
    }

    private boolean hasEnoughData(ByteBuffer buffer, int requiredLength) {
        return buffer.remaining() >= requiredLength;
    }


    /**
     * Updates the checksum for a TCP packet.
     * This method computes and sets the TCP checksum for the packet, considering the pseudo-header,
     * TCP header, and data payload. The checksum calculation follows the standard TCP checksum algorithm.
     *
     * @param payloadSize The size of the TCP payload in bytes, used to calculate the checksum.
     */
    private void updateTCPChecksum(int payloadSize) {
        int sum = 0;
        int tcpLength = TCP_HEADER_SIZE + payloadSize;

        // Calculate pseudo-header checksum
        ByteBuffer buffer = ByteBuffer.wrap(ip4Header.sourceAddress.getAddress());
        sum = BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());

        buffer = ByteBuffer.wrap(ip4Header.destinationAddress.getAddress());
        sum += BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());

        sum += TransportProtocol.TCP.getNumber() + tcpLength;

        buffer = backingBuffer.duplicate();
        // Clear previous checksum
        buffer.putShort(IP4_HEADER_SIZE + 16, (short) 0);

        // Calculate TCP segment checksum
        buffer.position(IP4_HEADER_SIZE);
        while (tcpLength > 1) {
            sum += BitUtils.getUnsignedShort(buffer.getShort());
            tcpLength -= 2;
        }
        if (tcpLength > 0)
            sum += BitUtils.getUnsignedByte(buffer.get()) << 8;

        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;
        tcpHeader.checksum = sum;
        backingBuffer.putShort(IP4_HEADER_SIZE + 16, (short) sum);
    }

    /**
     * Fills the provided buffer with the packet header information.
     * This method populates the buffer with the header data of the packet. Depending on the packet type,
     * it includes the IPv4/IPv6 header and the TCP/UDP header.
     *
     * @param buffer The ByteBuffer to be filled with the packet's header information.
     */
    private void fillHeader(ByteBuffer buffer) {
        ip4Header.fillHeader(buffer);
        if (isUDP())
            udpHeader.fillHeader(buffer);
        else if (isTCP())
            tcpHeader.fillHeader(buffer);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Packet{");
        if (ip4Header != null) {
            sb.append("IP4Header=").append(ip4Header);
            if (isTCP()) {
                sb.append(", TCPHeader=").append(tcpHeader);
            } else if (isUDP()) {
                sb.append(", UDPHeader=").append(udpHeader);
            }
        } else if (ip6Header != null) {
            sb.append("IP6Header=").append(ip6Header);
            if (isTCP()) {
                sb.append(", TCPHeader=").append(tcpHeader);
            } else if (isUDP()) {
                sb.append(", UDPHeader=").append(udpHeader);
            }
            // Include ICMPv6Header if present and relevant
            if (icmpv6Header != null) {
                sb.append(", ICMPv6Header=").append(icmpv6Header);
            }
        }
        sb.append(", PayloadSize=").append(calculatePayloadSize());
        sb.append('}');
        return sb.toString();
    }
    public IP4Header getIp4Header() {
        return ip4Header;
    }

    public TCPHeader getTcpHeader() {
        return tcpHeader;
    }

    public UDPHeader getUdpHeader() {
        return udpHeader;
    }

    public ByteBuffer getBackingBuffer() {
        return backingBuffer;
    }

    public boolean isTCP() {
        return tcpHeader != null;
    }

    public boolean isUDP() {
        return udpHeader != null;
    }

    public boolean isIPv6() {
        return ip6Header != null;
    }

    public byte[] getSourceAddress() {
        if (isIPv6()) {
            return ip6Header.getSourceAddress().getAddress();
        } else {
            return ip4Header.sourceAddress.getAddress();
        }
    }

    /**
     * Gets the ICMPv6 header from this packet.
     * @return The ICMPv6 header of the packet.
     */
    public ICMPv6Header getIcmpv6Header() {
        return this.icmpv6Header;
    }

    /**
     * Gets the IPv6 header from this packet.
     * @return The IPv6 header of the packet.
     */
    public IP6Header getIp6Header() {
        return this.ip6Header;
    }

    public byte[] getDestinationAddress() {
        if (isIPv6()) {
            return ip6Header.getDestinationAddress().getAddress();
        } else {
            return ip4Header.destinationAddress.getAddress();
        }
    }

    public int getNextHeader() {
        if (isIPv6()) {
            return ip6Header.getNextHeader();
        } else if (ip4Header != null) {
            // Assuming TransportProtocol enum has a method to get the numeric value
            return ip4Header.protocol.getNumber();
        } else {
            return -1; // or some other default value indicating an error or unknown protocol
        }
    }

    public int getPayloadLength() {
        if (isIPv6()) {
            return ip6Header.getPayloadLength();
        } else if (isUDP()) {
            return udpHeader.length;
        } else if (isTCP()) {
            // TCP header length is calculated differently
            return tcpHeader.dataOffsetAndReserved >>> 12; // check if this calculation is correct
        } else {
            return 0; // or throw an exception for unsupported cases
        }
    }

    public int getHeaderLength() {
        if (isIPv6()) {
            return 40; // IPv6 headers are always 40 bytes
        } else {
            return ip4Header.headerLength; // For IPv4
        }
    }

    public int getHopLimit() {
        if (isIPv6()) {
            return ip6Header.getHopLimit();
        }
        return -1; // Return an error value or throw an exception for IPv4 packets
    }
}
