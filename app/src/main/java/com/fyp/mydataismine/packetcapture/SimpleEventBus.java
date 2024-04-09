package com.fyp.mydataismine.packetcapture;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple implementation of an event bus for dispatching packet data to registered listeners.
 * It allows components to register as listeners for packet events and broadcasts new packet information to them.
 */
public class SimpleEventBus {

    /**
     * Defines the interface for listeners interested in packet events.
     */
    public interface PacketListener {
        void onPacketReceived(PacketInfo packetInfo);
    }

    private static final List<PacketListener> listeners = new ArrayList<>();

    /**
     * Registers a listener to receive packet events.
     * @param listener The listener to register.
     */
    public static void registerListener(PacketListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Unregisters a listener from receiving packet events.
     * @param listener The listener to unregister.
     */
    public static void unregisterListener(PacketListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Posts a packet event to all registered listeners.
     * @param packetInfo The packet information to distribute.
     */
    public static void postPacket(PacketInfo packetInfo) {
        synchronized (listeners) {
            for (PacketListener listener : listeners) {
                listener.onPacketReceived(packetInfo);
            }
        }
    }
}

