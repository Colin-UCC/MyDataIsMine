package com.fyp.mydataismine.packetcapture;

/**
 * Interface defining the contract for packet processing.
 * Classes implementing this interface are responsible for processing and uploading network packets.
 */
public interface PacketProcessor {
    /**
     * Processes the captured packets and uploads them to the designated storage or analysis service.
     */
    void processAndUploadPackets();
}
