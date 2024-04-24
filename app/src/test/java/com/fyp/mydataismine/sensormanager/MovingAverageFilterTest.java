package com.fyp.mydataismine.sensormanager;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class MovingAverageFilterTest {

    private MovingAverageFilter filter;

    @Before
    public void setUp() {
        filter = new MovingAverageFilter(3);
    }

    @Test
    public void testAddValueAndAverage() {
        assertEquals(0.0, filter.getAverage(), 0.0);
        filter.addValue(10);
        assertEquals(10.0, filter.getAverage(), 0.001);
        filter.addValue(20);
        assertEquals(15.0, filter.getAverage(), 0.001);
        filter.addValue(30);
        assertEquals(20.0, filter.getAverage(), 0.001);
        filter.addValue(40);
        assertEquals(30.0, filter.getAverage(), 0.001);
    }

    @Test
    public void testAverageWhenNotFull() {
        filter.addValue(5);
        filter.addValue(10);
        assertEquals(7.5, filter.getAverage(), 0.001);
    }

    @Test
    public void testAverageWithAllSameValues() {
        filter.addValue(5);
        filter.addValue(5);
        filter.addValue(5);
        assertEquals(5.0, filter.getAverage(), 0.0);
        filter.addValue(5);
        assertEquals(5.0, filter.getAverage(), 0.0);
    }

    @Test
    public void testAverageResetsProperly() {
        filter.addValue(10);
        filter.addValue(20);
        filter.addValue(30);

        filter.addValue(100);
        filter.addValue(100);
        filter.addValue(100);
        assertEquals(100.0, filter.getAverage(), 0.001);
    }
}
