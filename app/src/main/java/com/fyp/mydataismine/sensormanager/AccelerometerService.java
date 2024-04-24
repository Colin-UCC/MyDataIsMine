package com.fyp.mydataismine.sensormanager;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fyp.mydataismine.MainActivity;
import com.fyp.mydataismine.R;

public class AccelerometerService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final float CHANGE_THRESHOLD = 3.5f;
    private static final long UPDATE_INTERVAL = 1000; // milliseconds
    protected SensorManager sensorManager;
    protected Sensor accelerometerSensor;
    //private Sensor gyroscopeSensor;
    //private Sensor magnetometerSensor;
    protected MovingAverageFilter filterX;
    protected MovingAverageFilter filterY;
    protected MovingAverageFilter filterZ;
    private long lastUpdate = 0;
    private int count = 0;
    private float prevAvgX = 0.0f, prevAvgY = 0.0f, prevAvgZ = 0.0f;
    protected DatabaseHelper databaseHelper;

    // Sensor data arrays
    private float[] accelerometerData = new float[3];
    //private float[] gyroscopeData = new float[3];
    //private float[] magnetometerData = new float[3];

    // Moving Average Filters for each sensor axis
    private MovingAverageFilter filterAccelX, filterAccelY, filterAccelZ;
    //private MovingAverageFilter filterGyroX, filterGyroY, filterGyroZ;
    //private MovingAverageFilter filterMagX, filterMagY, filterMagZ;

    @Override
    public void onCreate() {
        super.onCreate();
        initialiseSensors();
    }

    /**
     * Initializes sensor resources, setting up sensors and filters.
     */
    void initialiseSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        databaseHelper = new DatabaseHelper(this);

        filterX = new MovingAverageFilter(10);
        filterY = new MovingAverageFilter(10);
        filterZ = new MovingAverageFilter(10);

        int filterSize = 10; // Size of the filter (number of values to average)
        filterAccelX = new MovingAverageFilter(filterSize);
        filterAccelY = new MovingAverageFilter(filterSize);
        filterAccelZ = new MovingAverageFilter(filterSize);
//        filterGyroX = new MovingAverageFilter(filterSize);
//        filterGyroY = new MovingAverageFilter(filterSize);
//        filterGyroZ = new MovingAverageFilter(filterSize);
//        filterMagX = new MovingAverageFilter(filterSize);
//        filterMagY = new MovingAverageFilter(filterSize);
//        filterMagZ = new MovingAverageFilter(filterSize);

        registerSensor();
    }

    /**
     * Registers listeners for the accelerometer, gyroscope, and magnetometer sensors.
     */
    private void registerSensor() {
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d("AccelerometerService", "Accelerometer available");
        } else {
            // Handle accelerometer not available
            Log.d("AccelerometerService", "Accelerometer not available");
        }
//        if (gyroscopeSensor != null) {
//            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
//        } else {
//            Log.d("GyroscopeService", "Gyroscope not available");
//        }

//        if (magnetometerSensor != null) {
//            sensorManager.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
//        } else {
//            Log.d("MagnetometerService", "Magnetometer not available");
//        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createForegroundNotification());
        return START_STICKY;
    }

    /**
     * Creates a notification for running the service in the foreground.
     *
     * @return The constructed Notification instance.
     */
    private Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sensor Fusion Service")
                .setContentText("Collecting sensor data")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    /**
     * Logs the content of the database for debugging purposes.
     */
    private void logDatabaseContent() {
        Cursor cursor = databaseHelper.getAllReadings();
        if (cursor.moveToFirst()) {
            StringBuilder builder = new StringBuilder();
            do {
                builder.setLength(0); // Clear the builder
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    builder.append(cursor.getColumnName(i)).append(": ").append(cursor.getString(i)).append(", ");
                }
                Log.d("DatabaseContent", builder.toString());
            } while (cursor.moveToNext());
        } else {
            Log.d("DatabaseContent", "No data found in the database.");
        }
        cursor.close();
    }


    //@Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            processSensorData(event);
        }
    }

    /**
     * Processes and broadcasts the sensor data after applying filters.
     */
//    private void processAndBroadcastSensorData() {
//
//        // Retrieve the average values from the filters
//        float avgAccelX = filterAccelX.getAverage();
//        float avgAccelY = filterAccelY.getAverage();
//        float avgAccelZ = filterAccelZ.getAverage();
//        float avgGyroX = filterGyroX.getAverage();
//        float avgGyroY = filterGyroY.getAverage();
//        float avgGyroZ = filterGyroZ.getAverage();
//        float avgMagX = filterMagX.getAverage();
//        float avgMagY = filterMagY.getAverage();
//        float avgMagZ = filterMagZ.getAverage();
//
//        Log.d("SensorFusionService", "Accel: " + arrayToString(accelerometerData) +
//                " Gyro: " + arrayToString(gyroscopeData) +
//                " Mag: " + arrayToString(magnetometerData));
//
//        // You can now use these averaged values for further processing or fusion
//        // For example, you can implement sensor fusion algorithms here
//
//
//        // ***** maybe put in logic to send the data only when its activated here...
//        // Do i really need to send all the data here, can i not just store it in the db
//        // Send broadcast with averaged sensor data
//        Intent intent = new Intent("SENSOR_FUSION_DATA");
//        intent.putExtra("avgAccelX", avgAccelX);
//        intent.putExtra("avgAccelY", avgAccelY);
//        intent.putExtra("avgAccelZ", avgAccelZ);
//        intent.putExtra("avgGyroX", avgGyroX);
//        intent.putExtra("avgGyroY", avgGyroY);
//        intent.putExtra("avgGyroZ", avgGyroZ);
//        intent.putExtra("avgMagX", avgMagX);
//        intent.putExtra("avgMagY", avgMagY);
//        intent.putExtra("avgMagZ", avgMagZ);
//
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//    }

    /**
     * Processes accelerometer sensor data.
     *
     * @param event The sensor event containing the new accelerometer data.
     */
    private void processSensorData(SensorEvent event) {
        filterX.addValue(event.values[0]);
        filterY.addValue(event.values[1]);
        filterZ.addValue(event.values[2]);

        float avgX = filterX.getAverage();
        float avgY = filterY.getAverage();
        float avgZ = filterZ.getAverage();

        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastUpdate) > UPDATE_INTERVAL) {
            float samplingRate = calculateSamplingRate(currentTime);
            Log.d("Accelerometer SamplingRate", "Rate: " + samplingRate + " Hz");
            lastUpdate = currentTime;
            count = 0;
        }

        count++;
        checkSignificantMotion(avgX, avgY, avgZ);
    }

    /**
     * Calculates and returns the sampling rate based on event timestamps.
     *
     * @param currentTime The current time in milliseconds.
     * @return The calculated sampling rate in Hz.
     */
    private float calculateSamplingRate(long currentTime) {
        return count / ((float)(currentTime - lastUpdate) / 1000f);
    }

    /**
     * Converts an array of floats to a comma-separated string.
     *
     * @param array The array to be converted.
     * @return A string representation of the array.
     */
    private String arrayToString(float[] array) {
        return "[" + array[0] + ", " + array[1] + ", " + array[2] + "]";
    }

    /**
     * Checks for significant motion and updates the database and broadcasts the data if necessary.
     *
     * @param avgX The average x value from the accelerometer.
     * @param avgY The average y value from the accelerometer.
     * @param avgZ The average z value from the accelerometer.
     */
    private void checkSignificantMotion(float avgX, float avgY, float avgZ) {
        if (hasSignificantMotion(avgX, avgY, avgZ)) {
            databaseHelper.addReading(avgX, avgY, avgZ);
            Log.d("DB ACTION", "Reading added: X=" + avgX + ", Y=" + avgY + ", Z=" + avgZ);
            updatePreviousValues(avgX, avgY, avgZ);
        }
        // Send broadcast with accelerometer data
        Intent intent = new Intent("ACCELEROMETER_DATA");
        intent.putExtra("x", avgX);
        intent.putExtra("y", avgY);
        intent.putExtra("z", avgZ);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void checkSignificantMotionWith3Sensors(float avgX, float avgY, float avgZ) {
        // TODO: Decide on how to trigger the db. should it be just accelorometer? may need testing
        if (hasSignificantMotion(avgX, avgY, avgZ)) {
            // TODO: Create a new schema for triple sensors
            //databaseHelper.addReading(avgX, avgY, avgZ);
            //Log.d("DB ACTION", "Reading added: X=" + avgX + ", Y=" + avgY + ", Z=" + avgZ);
            // TODO: create a new function for all values.
            //updatePreviousValues(avgX, avgY, avgZ);
        }

        // TODO: create a new intent
        // Send broadcast with accelerometer data
        Intent intent = new Intent("ACCELEROMETER_DATA");
        intent.putExtra("x", avgX);
        intent.putExtra("y", avgY);
        intent.putExtra("z", avgZ);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Determines if the change in accelerometer data represents significant motion.
     *
     * @param avgX The average x value from the accelerometer.
     * @param avgY The average y value from the accelerometer.
     * @param avgZ The average z value from the accelerometer.
     * @return true if the motion is significant, false otherwise.
     */
    private boolean hasSignificantMotion(float avgX, float avgY, float avgZ) {
        return Math.abs(avgX - prevAvgX) > CHANGE_THRESHOLD ||
                Math.abs(avgY - prevAvgY) > CHANGE_THRESHOLD ||
                Math.abs(avgZ - prevAvgZ) > CHANGE_THRESHOLD;
    }

    /**
     * Updates the previous average values for x, y, and z axes.
     *
     * @param avgX The new average x value.
     * @param avgY The new average y value.
     * @param avgZ The new average z value.
     */
    private void updatePreviousValues(float avgX, float avgY, float avgZ) {
        prevAvgX = avgX;
        prevAvgY = avgY;
        prevAvgZ = avgZ;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Implement if needed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}

