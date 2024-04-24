package com.fyp.mydataismine.packetcapture;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.nio.channels.SocketChannel;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class TCBTest {
    private TCB tcb;
    private String ipAndPort = "192.168.1.1:80";
    private SocketChannel channel;
    private Packet referencePacket;

    @Before
    public void setUp() throws Exception {
        channel = mock(SocketChannel.class);
        referencePacket = mock(Packet.class);
        tcb = new TCB(ipAndPort, 0L, 0L, 0L, 0L, channel, referencePacket);

        TCB.putTCB(ipAndPort, tcb);
    }

    @Test
    public void testTCBCreationAndRetrieval() {
        TCB retrievedTCB = TCB.getTCB(ipAndPort);

        assertNotNull("TCB should not be null after creation", retrievedTCB);
        assertEquals("Retrieved TCB should match the original", tcb, retrievedTCB);
    }

    @Test
    public void testTCBUpdate() {
        tcb.mySequenceNum = 1L;
        TCB retrievedTCB = TCB.getTCB(ipAndPort);

        assertEquals("Sequence number should be updated", 1L, retrievedTCB.mySequenceNum);
    }

    @Test
    public void testCloseTCB() throws Exception {
        TCB.closeTCB(tcb);

        assertNull("TCB should be removed from cache", TCB.getTCB(ipAndPort));
        verify(channel, times(1)).close();
    }

    @Test
    public void testCloseAll() throws Exception {
        TCB newTCB = new TCB("192.168.1.2:80", 1L, 1L, 1L, 1L, channel, referencePacket);
        TCB.putTCB("192.168.1.2:80", newTCB);

        TCB.closeAll();

        assertNull("All TCBs should be removed from cache", TCB.getTCB(ipAndPort));
        assertNull("All TCBs should be removed from cache", TCB.getTCB("192.168.1.2:80"));
        verify(channel, times(2)).close();
    }

    @After
    public void tearDown() throws Exception {
        TCB.closeAll(); // Ensure all TCBs are closed and removed from the cache
    }
}
