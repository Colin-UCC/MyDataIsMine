package com.fyp.mydataismine.packetcapture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SimpleEventBusTest {

    private SimpleEventBus.PacketListener mockListener;

    @Before
    public void setUp() {
        mockListener = mock(SimpleEventBus.PacketListener.class);
    }

    @After
    public void tearDown() {
        SimpleEventBus.unregisterListener(mockListener);
    }

    @Test
    public void registerListener_ShouldAddListener() {
        SimpleEventBus.registerListener(mockListener);

        PacketInfo packetInfo = new PacketInfo("192.168.1.1", "8.8.8.8", 60, "TCP");
        SimpleEventBus.postPacket(packetInfo);

        verify(mockListener).onPacketReceived(packetInfo);
    }

    @Test
    public void unregisterListener_ShouldRemoveListener() {
        SimpleEventBus.registerListener(mockListener);
        SimpleEventBus.unregisterListener(mockListener);

        PacketInfo packetInfo = new PacketInfo("192.168.1.1", "8.8.8.8", 60, "TCP");
        SimpleEventBus.postPacket(packetInfo);

        verify(mockListener, never()).onPacketReceived(packetInfo);
    }

    @Test
    public void postPacket_ShouldNotifyRegisteredListeners() {
        SimpleEventBus.PacketListener anotherMockListener = mock(SimpleEventBus.PacketListener.class);
        SimpleEventBus.registerListener(mockListener);
        SimpleEventBus.registerListener(anotherMockListener);

        PacketInfo packetInfo = new PacketInfo("192.168.1.1", "8.8.8.8", 60, "TCP");
        SimpleEventBus.postPacket(packetInfo);

        verify(mockListener).onPacketReceived(packetInfo);
        verify(anotherMockListener).onPacketReceived(packetInfo);
    }
}
