package com.fyp.mydataismine.packetcapture;

/**
 * Enumerates the various transport protocols recognized in the packet capture process, each associated with its protocol number.
 */
public enum TransportProtocol {
    TCP(6),
    UDP(17),
    ICMPv6(58),
    Other(0xFF);

    private final int protocolNumber;
    private final IP4Header ip4Header;

    /**
     * Initializes the enumeration with the protocol number.
     * @param protocolNumber The standard protocol number associated with this transport protocol.
     */
    TransportProtocol(int protocolNumber) {
        this.protocolNumber = protocolNumber;
        this.ip4Header = null;
    }

    // Constructor that accepts an IP4Header instance
    TransportProtocol(int protocolNumber, IP4Header ip4Header) {
        this.protocolNumber = protocolNumber;
        this.ip4Header = ip4Header;
    }

    /**
     * Gets the name of the protocol.
     * @return A string representing the protocol name, or "Unknown" if the protocol is not specifically defined.
     */
    public String getProtocolName() {
        if (ip4Header != null) {
            return ip4Header.protocol.name();
        }
        return "Unknown";
    }

    /**
     * Converts a numerical protocol identifier to its corresponding enum value.
     * @param protocolNumber The protocol number to convert.
     * @return The corresponding TransportProtocol enum value, or Other if the number does not match a known protocol.
     */
    public static TransportProtocol numberToEnum(int protocolNumber) {
        switch (protocolNumber) {
            case 6: return TCP;
            case 17: return UDP;
            case 58: return ICMPv6; // Handling ICMPv6
            default: return Other;
        }
    }

    /**
     * Gets the protocol number associated with this transport protocol.
     * @return The protocol number.
     */
    public int getNumber() {
        return this.protocolNumber;
    }
}
