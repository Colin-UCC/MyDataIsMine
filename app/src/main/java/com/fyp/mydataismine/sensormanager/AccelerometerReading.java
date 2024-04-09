package com.fyp.mydataismine.sensormanager;

/**
 * Represents a single reading from an accelerometer sensor, encapsulating the
 * x, y, and z axis values along with a timestamp for when the reading was taken.
 */
public class AccelerometerReading {
    private float x;
    private float y;
    private float z;
    private String timestamp;

    /**
     * Constructs an accelerometer reading with specified x, y, z values and timestamp.
     *
     * @param x The acceleration along the x-axis.
     * @param y The acceleration along the y-axis.
     * @param z The acceleration along the z-axis.
     * @param timestamp The timestamp when the reading was taken.
     */
    public AccelerometerReading(float x, float y, float z, String timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public String getTime() {
        return timestamp;
    }
}

