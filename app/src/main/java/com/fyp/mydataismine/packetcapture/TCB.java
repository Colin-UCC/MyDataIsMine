package com.fyp.mydataismine.packetcapture;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents a Transmission Control Block (TCB) which encapsulates the state information of a TCP connection.
 * It is used to manage the state and control flow of TCP connections during packet capture and analysis.
 */
public class TCB
{
    public String ipAndPort;
    public long mySequenceNum, theirSequenceNum;
    public long myAcknowledgementNum, theirAcknowledgementNum;
    public TCBStatus status;

    // TCB status enumeration
    public enum TCBStatus
    {
        SYN_SENT,
        SYN_RECEIVED,
        ESTABLISHED,
        CLOSE_WAIT,
        LAST_ACK,
    }

    public Packet referencePacket;

    public SocketChannel channel;
    public boolean waitingForNetworkData;
    public SelectionKey selectionKey;

    // Static TCB cache management
    private static final int MAX_CACHE_SIZE = 50;
    private static LRUCache<String, TCB> tcbCache =
            new LRUCache<>(MAX_CACHE_SIZE, new LRUCache.CleanupCallback<String, TCB>()
            {
                @Override
                public void cleanup(Map.Entry<String, TCB> eldest)
                {
                    eldest.getValue().closeChannel();
                }
            });

    /**
     * Initializes a new instance of TCB with the specified parameters.
     */
    public TCB(String ipAndPort, long mySequenceNum, long theirSequenceNum, long myAcknowledgementNum, long theirAcknowledgementNum,
               SocketChannel channel, Packet referencePacket)
    {
        this.ipAndPort = ipAndPort;

        this.mySequenceNum = mySequenceNum;
        this.theirSequenceNum = theirSequenceNum;
        this.myAcknowledgementNum = myAcknowledgementNum;
        this.theirAcknowledgementNum = theirAcknowledgementNum;

        this.channel = channel;
        this.referencePacket = referencePacket;
    }

    // Other methods for TCB management and operations
    public static TCB getTCB(String ipAndPort)
    {
        synchronized (tcbCache)
        {
            return tcbCache.get(ipAndPort);
        }
    }

    public static void putTCB(String ipAndPort, TCB tcb)
    {
        synchronized (tcbCache)
        {
            tcbCache.put(ipAndPort, tcb);
        }
    }



    public static void closeTCB(TCB tcb)
    {
        tcb.closeChannel();
        synchronized (tcbCache)
        {
            tcbCache.remove(tcb.ipAndPort);
        }
    }

    public static void closeAll()
    {
        synchronized (tcbCache)
        {
            Iterator<Map.Entry<String, TCB>> it = tcbCache.entrySet().iterator();
            while (it.hasNext())
            {
                it.next().getValue().closeChannel();
                it.remove();
            }
        }
    }

    private void closeChannel() {
        if (channel != null) {
            try {
                if (channel.isOpen()) {
                    channel.close();
                }
            } catch (IOException e) {
                // TODO logs
            }
        }
    }
}