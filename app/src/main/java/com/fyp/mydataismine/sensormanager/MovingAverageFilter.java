package com.fyp.mydataismine.sensormanager;

/**
 * A class to implement a moving average filter.
 */
public class MovingAverageFilter {

    private final float[] queue;
    private int fillCount;
    private int head;

    /**
     * Constructor for MovingAverageFilter.
     *
     * @param size The size of the moving average window.
     */
    public MovingAverageFilter(int size) {
        queue = new float[size];
    }

    /**
     * Adds a new value to the moving average filter.
     *
     * @param value The new value to be added.
     */
    public void addValue(float value) {
        queue[head] = value;
        head = (head + 1) % queue.length;
        if (fillCount < queue.length) {
            fillCount++;
        }
    }

    /**
     * Calculates and returns the average of the values in the filter.
     *
     * @return The average of the values.
     */
    public float getAverage() {
        float sum = 0;
        for (int i = 0; i < fillCount; i++) {
            sum += queue[i];
        }
        return fillCount > 0 ? sum / fillCount : 0;
    }
}
