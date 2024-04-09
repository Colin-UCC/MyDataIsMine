package com.fyp.mydataismine.networkmonitor;

public class NetworkTrafficData {
    private String timestamp;
    private String sourceIp;



    private int sourcePort;

    public String getDestinationIp() {
        return destinationIp;
    }

    private String destinationIp;



    private int destinationPort;
    private String protocol;
    private int length;
    private String location;
    private String organisation;
    private boolean isSuspicious;

    // Constructor, getters, and setters

    public NetworkTrafficData(String sourceIp, String destinationIp, String protocol, int length, String timestamp) {
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.protocol = protocol;
        this.length = length;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    // ...

    public void setLocation(String location) {
        this.location = location;
    }

    public void setOrganization(String organisation) {
        this.organisation = organisation;
    }

    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    // Add getters for all fields
    public String getTimestamp() {
        return timestamp;
    }



    public int getSourcePort() {
        return sourcePort;
    }



    public int getDestinationPort() {
        return destinationPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getLength() {
        return length;
    }

    public String getLocation() {
        return location;
    }

    public String getOrganisation() {
        return organisation;
    }

    public boolean isSuspicious() {
        return isSuspicious;
    }


    @Override
    public String toString() {
        return "NetworkTrafficData{" +
                "timestamp=" + timestamp +
                ", sourceIp='" + sourceIp + '\'' +
                ", sourcePort=" + sourcePort +
                ", destinationIp='" + destinationIp + '\'' +
                ", destinationPort=" + destinationPort +
                ", protocol='" + protocol + '\'' +
                ", length=" + length +
                ", location='" + location + '\'' +
                ", organisation='" + organisation + '\'' +
                ", isSuspicious=" + isSuspicious +
                '}';
    }

}
