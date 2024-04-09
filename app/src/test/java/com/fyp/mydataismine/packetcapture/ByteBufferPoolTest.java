package com.fyp.mydataismine.packetcapture;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class ByteBufferPoolTest {

    @After
    public void tearDown() {
        ByteBufferPool.clear();  // Clear the pool after each test to prevent side effects
    }

    @Test
    public void acquire_returnsNonNullBuffer() {
        ByteBuffer buffer = ByteBufferPool.acquire();
        Assert.assertNotNull("Buffer should not be null", buffer);
    }

    @Test
    public void acquire_returnsBufferWithPositiveCapacity() {
        ByteBuffer buffer = ByteBufferPool.acquire();
        Assert.assertTrue("Buffer should have a positive capacity", buffer.capacity() > 0);
    }

    @Test
    public void release_returnsBufferToPool() {
        ByteBuffer buffer = ByteBufferPool.acquire();
        Assert.assertNotNull(buffer);

        ByteBufferPool.release(buffer);
        ByteBuffer buffer2 = ByteBufferPool.acquire();
        Assert.assertTrue("Should get a buffer from the pool after release", buffer2 != null);
    }

    @Test
    public void clear_emptiesThePool() {
        ByteBuffer buffer = ByteBufferPool.acquire();
        ByteBufferPool.release(buffer);
        ByteBufferPool.clear();

        ByteBuffer buffer2 = ByteBufferPool.acquire();
        Assert.assertTrue("Should create a new buffer after clear", buffer2 != null && buffer2 != buffer);
    }
}
