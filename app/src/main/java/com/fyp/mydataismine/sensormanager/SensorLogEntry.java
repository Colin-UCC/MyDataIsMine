package com.fyp.mydataismine.sensormanager;

/**
 * Represents a single entry in a sensor access log, containing details about the sensor access event.
 */
public class SensorLogEntry {
    private final String packageName;
    private final String sensorName;
    private final String  sensorHandle;
    private final String timeAccessed;

    /**
     * Constructor for creating a sensor log entry.
     * @param packageName The name of the package accessing the sensor.
     * @param sensorName The name of the sensor being accessed.
     * @param sensorHandle The handle or identifier of the sensor.
     * @param timeAccessed The timestamp when the sensor was accessed.
     */
    public SensorLogEntry(String packageName, String sensorName, String sensorHandle, String timeAccessed) {
        this.packageName = packageName;
        this.sensorName = sensorName;
        this.sensorHandle = sensorHandle;
        this.timeAccessed = timeAccessed;
    }
    public String getPackageName() {
        return packageName;
    }

    public String getSensorName() {
        return sensorName;
    }

    public String getSensorHandle() {
        return sensorHandle;
    }

    public String getTimeAccessed() {
        return timeAccessed;
    }

    @Override
    public String toString() {
        return "SensorLogEntry{" +
                "Package Name='" + packageName + '\'' +
                ", Sensor Name='" + sensorName + '\'' +
                ", Sensor Handle='" + sensorHandle + '\'' +
                ", Time Accessed='" + timeAccessed + '\'' +
                '}';
    }
}

