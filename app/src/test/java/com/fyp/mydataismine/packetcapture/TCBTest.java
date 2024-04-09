package com.fyp.mydataismine.packetcapture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.SocketChannel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TCBTest {
    private TCB tcb;
    private String ipAndPort = "192.168.1.1:80";
    private SocketChannel channel;

//    @Before
//    public void setUp() throws Exception {
//        channel = mock(SocketChannel.class);
//        // Stub the close method to prevent NullPointerException
//        doNothing().when(channel).close();
//
//        tcb = new TCB(ipAndPort, 1L, 1L, 1L, 1L, channel, null);
//        TCB.putTCB(ipAndPort, tcb);
//
//    }

    @Before
    public void setUp() throws Exception {
        channel = mock(SocketChannel.class);
        tcb = new TCB(ipAndPort, 1L, 1L, 1L, 1L, channel, null);
        TCB.putTCB(ipAndPort, tcb);
    }

    @Test
    public void testTCBCreationAndRetrieval() {
        TCB retrievedTCB = TCB.getTCB(ipAndPort);
        assertNotNull(retrievedTCB);
        assertEquals(tcb, retrievedTCB);
        assertEquals(ipAndPort, retrievedTCB.ipAndPort);
    }

    @Test
    public void testUpdateTCB() {
        // Update TCB values
        long newSequenceNum = 2L;
        tcb.mySequenceNum = newSequenceNum;
        tcb.theirSequenceNum = newSequenceNum;

        TCB updatedTCB = TCB.getTCB(ipAndPort);
        assertNotNull(updatedTCB);
        assertEquals(newSequenceNum, updatedTCB.mySequenceNum);
        assertEquals(newSequenceNum, updatedTCB.theirSequenceNum);
    }

    @Test
    public void testCloseTCB() throws Exception {
        TCB.closeTCB(tcb);
        assertNull(TCB.getTCB(ipAndPort));
        verify(channel, times(1)).close();
    }

    @Test
    public void testCloseAllTCBs() throws Exception {
        TCB anotherTCB = new TCB("192.168.1.2:80", 1L, 1L, 1L, 1L, channel, null);
        TCB.putTCB("192.168.1.2:80", anotherTCB);

        TCB.closeAll();

        assertNull(TCB.getTCB(ipAndPort));
        assertNull(TCB.getTCB("192.168.1.2:80"));
        verify(channel, times(2)).close();
    }

    @After
    public void tearDown() throws Exception {
        TCB.closeAll(); // Ensure all TCBs are closed after tests
        // Verify that close was called, without explicitly stubbing it
        verify(channel, times(1)).close();
    }
}
