package com.fyp.mydataismine.packetcapture;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages a pool of ByteBuffer instances to optimize memory usage during network packet capture and analysis.
 * This class helps to reduce the overhead associated with the creation and destruction of ByteBuffers,
 * which is critical in high-performance network traffic processing.
 */
public class ByteBufferPool {
    private static final int BUFFER_SIZE = 32768 ; // Adjust as needed
    private static ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();

    /**
     * Acquires a ByteBuffer from the pool or creates a new one if the pool is empty.
     * @return A ByteBuffer ready for use.
     */
    public static ByteBuffer acquire() {
        ByteBuffer buffer = pool.poll();
        if (buffer == null)
            buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        return buffer;
    }

    /**
     * Returns a ByteBuffer to the pool for reuse.
     * @param buffer The ByteBuffer to be released back to the pool.
     */
    public static void release(ByteBuffer buffer) {
        buffer.clear();
        pool.offer(buffer);
    }

    /**
     * Clears the ByteBuffer pool, releasing all held resources.
     */
    public static void clear() {
        pool.clear();
    }
}

