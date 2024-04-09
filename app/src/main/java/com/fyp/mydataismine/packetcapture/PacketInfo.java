package com.fyp.mydataismine.packetcapture;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents the information of a network packet, including source and destination IPs, payload size, and protocol used.
 */
public class PacketInfo {
    // Member variables for packet details
    private String sourceIp;
    private String destinationIp;
    private int payloadSize;
    private String protocol;
    private String location;
    private String timestamp;

    /**
     * Constructor to initialize the packet information.
     *
     * @param sourceIp      The source IP address of the packet.
     * @param destinationIp The destination IP address of the packet.
     * @param payloadSize   The size of the packet's payload.
     * @param protocol      The protocol used by the packet.
     */
    public PacketInfo(String sourceIp, String destinationIp, int payloadSize, String protocol) {
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.payloadSize = payloadSize;
        this.protocol = protocol;
        this.timestamp = generateTimestamp();
    }

    /**
     * Sets the geographical location where the packet was captured.
     *
     * @param location The location of the packet capture.
     */


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
                '}';
    }

    // Getters ans setters
    public String getSourceIp() {
        return sourceIp;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public String getProtocol() {
        return protocol;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public String getLocation() {
        return location;
    }
    public String getTimestamp() {
        return timestamp; // Return the stored timestamp
    }

}

