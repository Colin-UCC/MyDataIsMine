package com.fyp.mydataismine.packetcapture;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents the information of a network packet, including source and destination IPs, payload size,
 * protocol used, geographical location, and organization name.
 */
public class PacketInfo {
    // Member variables for packet details
    private String sourceIp;
    private String destinationIp;
    private int payloadSize;
    private String protocol;
    private String location;
    private String organization; // Added new field for organization
    private String timestamp;

    /**
     * Constructor to initialize the packet information.
     *
     * @param sourceIp       The source IP address of the packet.
     * @param destinationIp  The destination IP address of the packet.
     * @param payloadSize    The size of the packet's payload.
     * @param protocol       The protocol used by the packet.
     */
    public PacketInfo(String sourceIp, String destinationIp, int payloadSize, String protocol) {
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.payloadSize = payloadSize;
        this.protocol = protocol;
        this.timestamp = generateTimestamp(); // Timestamp for creation moment
    }

    /**
     * Sets the geographical location where the packet was captured.
     *
     * @param location The location of the packet capture.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Sets the organization associated with the packet.
     *
     * @param organization The organization name associated with the packet's IP.
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /**
     * Generates a timestamp for when the packet info is created.
     *
     * @return The timestamp in "yyyy-MM-dd HH:mm:ss" format.
     */
    private String generateTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return dateFormat.format(now);
    }

    @Override
    public String toString() {
        return "PacketInfo{" +
                "sourceIp='" + sourceIp + '\'' +
                ", destinationIp='" + destinationIp + '\'' +
                ", payloadSize=" + payloadSize +
                ", protocol='" + protocol + '\'' +
                ", location='" + location + '\'' +
                ", organization='" + organization + '\'' + // Include organization in the output
                ", timestamp='" + timestamp + '\'' +
                '}';
    }

    // Getters and setters
    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getLocation() {
        return location;
    }

    public String getOrganization() {
        return organization;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
