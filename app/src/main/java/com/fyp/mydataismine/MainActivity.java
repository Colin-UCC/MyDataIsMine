package com.fyp.mydataismine;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

// AndroidX imports
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

// Firebase imports
import com.fyp.mydataismine.auth.LoginActivity;
import com.fyp.mydataismine.networkmonitor.NetworkMonitorService;
import com.fyp.mydataismine.packetcapture.PacketInfo;
import com.fyp.mydataismine.packetcapture.PacketStreamActivity;
import com.fyp.mydataismine.packetcapture.SimpleEventBus;
import com.fyp.mydataismine.packetcapture.VPNNetworkService;
import com.fyp.mydataismine.sensormanager.AccelerometerReading;
import com.fyp.mydataismine.sensormanager.AccelerometerService;
import com.fyp.mydataismine.sensormanager.DatabaseHelper;
import com.fyp.mydataismine.sensormanager.FirebaseWorker;
import com.fyp.mydataismine.sensormanager.SensorLogEntry;
import com.fyp.mydataismine.sensormanager.SensorMonitorService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// MPAndroidChart library imports
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

// Java standard library imports
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main activity of the application, serving as the central hub for user interaction and service management.
 */
public class MainActivity extends AppCompatActivity implements SimpleEventBus.PacketListener {

    // Class variables
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private Button startServiceButton, stopServiceButton, signOutButton,
                    testUploadButton, cancelAllWorkButton, btnConvertAndUpload,
                    btnToggleService, goToPacketStreamButton, btnLogDatabase,
                    sensorServiceLogButton;
    private LineChart chart, networkChart, sensorActivityChart;

    //private BarChart sensorActivityChart;
    private LineDataSet xDataSet, yDataSet, zDataSet;
    private LineData lineData;
    private ArrayList<Entry> xValues, yValues, zValues;
    private Button startVpnButton;
    private Button stopVpnButton;
    public static final String ACTION_NEW_PACKET = "com.fyp.mydataismine.NEW_PACKET";
    private static final String PACKET_CHANNEL_ID = "Packet Interceptor";
    private static final int REQUEST_VPN = 1001;
    private boolean isMonitorServiceRunning = false;
    private DatabaseReference mDatabase;
    private boolean isMonitoring = false;

    /**
     * Initializes the activity, setting up the user interface and service components.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeFirebase();
        setupUI();
        checkUserAuthorization();
        scheduleDailyUploadTask();

        if (savedInstanceState != null) {
            // Restore the state of the service running
            boolean isServiceRunning = savedInstanceState.getBoolean("isServiceRunning", false);
            updateButtonState(isServiceRunning);
        } else {
            updateButtonState(isServiceRunning(AccelerometerService.class));
        }


        btnToggleService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMonitorServiceRunning) {
                    startSensorMonitoringService();
                } else {
                    stopSensorMonitoringService();
                }
            }
        });

        goToPacketStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to PacketStreamActivity
                Intent intent = new Intent(MainActivity.this, PacketStreamActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Called when the activity resumes from the paused state, updating the UI state and registering receivers.
     */
    @Override
    protected void onResume() {
        super.onResume();
        updateButtonState(isServiceRunning(AccelerometerService.class));
        registerReceiver();
        isMonitorServiceRunning = isMyServiceRunning(SensorMonitorService.class);
        updateMonitorButtonText();
    }

    /**
     * Called when the activity is paused, unregistering receivers to prevent memory leaks.
     */
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    /**
     * Saves the state of the activity before it's potentially destroyed by the system.
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isServiceRunning", stopServiceButton.isEnabled());
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



    /**
     * Initializes Firebase services used in the application.
     */
    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Sets up the user interface elements and event handlers.
     */
    private void setupUI() {
        setupButtons();
        setupChart();
        setupNetworkChart();
        //setupBarChart();
        setupSensorActivityChart();
        createNotificationChannel();
    }

    private void setupButtons() {
        startServiceButton = findViewById(R.id.startServiceButton);
        stopServiceButton = findViewById(R.id.stopServiceButton);

        startServiceButton.setOnClickListener(v -> handleServiceStart());
        stopServiceButton.setOnClickListener(v -> handleServiceStop());

        signOutButton = findViewById(R.id.signOutButton);
        testUploadButton = findViewById(R.id.testUploadButton);
        cancelAllWorkButton = findViewById(R.id.cancelAllWorkButton);
        btnConvertAndUpload = findViewById(R.id.btnConvertAndUpload);

        startVpnButton = findViewById(R.id.startVpnButton);
        stopVpnButton = findViewById(R.id.stopVpnButton);

        startVpnButton.setOnClickListener(v -> startVpnService());
        stopVpnButton.setOnClickListener(v -> stopVpnService());

        btnToggleService = findViewById(R.id.btnToggleService);

        goToPacketStreamButton = findViewById(R.id.goToPacketStreamButton);

        signOutButton.setOnClickListener(v -> signOut());

        sensorServiceLogButton = findViewById(R.id.sensorServiceLogButton);

        sensorServiceLogButton.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(MainActivity.this, SensorMonitorService.class);
            serviceIntent.setAction(SensorMonitorService.ACTION_WRITE_AND_UPLOAD_LOG);
            startService(serviceIntent);
        });


        btnLogDatabase = findViewById(R.id.btnLogDatabase);
        btnLogDatabase.setOnClickListener(view -> logDatabaseContent());

        //testUploadButton.setOnClickListener(v -> uploadTestFile());
        cancelAllWorkButton.setOnClickListener(v -> cancelAllWork());
        btnConvertAndUpload.setOnClickListener(v -> convertAndUpload());

        updateVpnButtonVisibility();
    }

    private void updateVpnButtonVisibility(boolean isVpnStarted) {
        if (isVpnStarted) {
            startVpnButton.setVisibility(View.GONE);
            stopVpnButton.setVisibility(View.VISIBLE);
        } else {
            startVpnButton.setVisibility(View.VISIBLE);
            stopVpnButton.setVisibility(View.GONE);
        }
    }

    private void logDatabaseContent() {
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        ArrayList<AccelerometerReading> readings = databaseHelper.getAllReadingObjects();
        for (AccelerometerReading reading : readings) {
            Log.d("DatabaseLog", "Timestamp: " + reading.getTime() +
                    ", X: " + reading.getX() +
                    ", Y: " + reading.getY() +
                    ", Z: " + reading.getZ());
        }
    }


    private void startSensorMonitoringService() {
        Intent serviceIntent = new Intent(this, SensorMonitorService.class);
        startForegroundService(serviceIntent);
        isMonitorServiceRunning = true;
        updateMonitorButtonText();
    }

    private void stopSensorMonitoringService() {
        Intent serviceIntent = new Intent(this, SensorMonitorService.class);
        stopService(serviceIntent);
        isMonitorServiceRunning = false;
        updateMonitorButtonText();
    }

    private void updateMonitorButtonText() {
        Button btnToggleService = findViewById(R.id.btnToggleService);
        if (isMonitorServiceRunning) {
            btnToggleService.setText("Stop Monitoring");
        } else {
            btnToggleService.setText("Start Monitoring");
        }
    }


    private void updateVpnButtonVisibility() {
        // This method should check if the VPN is currently active and update button visibility accordingly
        // Assuming `isVpnRunning` is a method that checks if the VPN service is running
        boolean isVpnRunning = VPNNetworkService.isRunning();
        updateVpnButtonVisibility(isVpnRunning);
    }

    private void startVpnService() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN); // VPN consent required
        } else {
            // VPN permission already granted or not required, proceed to start VPN
            proceedToStartVpn();
        }
    }

    private void proceedToStartVpn() {
        Intent vpnStartIntent = new Intent(this, VPNNetworkService.class);
        vpnStartIntent.setAction(VPNNetworkService.ACTION_START_VPN);
        startService(vpnStartIntent); // Starting the VPN service
        updateVpnButtonVisibility(true); // Update button visibility based on VPN state
    }

    private void stopVpnService() {
        Intent stopVpnIntent = new Intent(this, VPNNetworkService.class);
        stopVpnIntent.setAction(VPNNetworkService.ACTION_STOP_VPN);
        startService(stopVpnIntent); // Stopping the VPN service
        updateVpnButtonVisibility(false); // Update button visibility based on VPN state
    }

    // Method to toggle network monitoring
    public void toggleNetworkMonitoring(View view) {
        Button btn = (Button) view;
        Intent serviceIntent = new Intent(this, NetworkMonitorService.class);

        if (!isMonitoring) {
            startService(serviceIntent);
            btn.setText("Stop Monitoring");
        } else {
            stopService(serviceIntent);
            btn.setText("Start Monitoring");
        }

        isMonitoring = !isMonitoring;
    }

    private void setupChart() {
        chart = findViewById(R.id.chart);
        xValues = new ArrayList<>();
        yValues = new ArrayList<>();
        zValues = new ArrayList<>();

        xDataSet = new LineDataSet(xValues, "X Axis");
        yDataSet = new LineDataSet(yValues, "Y Axis");
        zDataSet = new LineDataSet(zValues, "Z Axis");

        xDataSet.setColor(Color.RED);
        yDataSet.setColor(Color.GREEN);
        zDataSet.setColor(Color.BLUE);

        lineData = new LineData(xDataSet, yDataSet, zDataSet);
        chart.setData(lineData);
        chart.invalidate(); // Refresh chart
    }





    /**
     * Checks if the user is currently authorized and redirects to the login screen if not.
     */
    private void checkUserAuthorization() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
        }
    }

    private void redirectToLogin() {
        Intent entryIntent = new Intent(this, LoginActivity.class);
        startActivity(entryIntent);
        finish();
    }

    // Button Click Handlers
    private void handleServiceStart() {
        Intent serviceIntent = new Intent(this, AccelerometerService.class);
        startForegroundService(serviceIntent);
        updateButtonState(true);
    }

    private void handleServiceStop() {
        Intent serviceIntent = new Intent(this, AccelerometerService.class);
        stopService(serviceIntent);
        updateButtonState(false);
    }

    // Utility Methods
    private void signOut() {
        mAuth.signOut();
        redirectToLogin();
    }

    private void uploadTestFile() {
        // Reference to your Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Create a reference to 'test_file.txt' in Firebase Storage
        StorageReference testFileRef = storageRef.child("test_file.txt");

        // Content of the file
        String fileContent = "This is a test file!";
        byte[] data = fileContent.getBytes(StandardCharsets.UTF_8);

        // Upload the file to Firebase Storage
        testFileRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    // Success handling logic
                    Log.d("FirebaseStorage", "Test file uploaded successfully.");
                    Toast.makeText(MainActivity.this, "Test file uploaded successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Failure handling logic
                    Log.e("FirebaseStorage", "Error uploading test file", e);
                    Toast.makeText(MainActivity.this, "Error uploading test file", Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelAllWork() {
        WorkManager.getInstance(this).cancelAllWork();
        Toast.makeText(this, "All enqueued work cancelled", Toast.LENGTH_SHORT).show();
    }

    private void convertAndUpload() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FirebaseWorker.class).build();
        WorkManager.getInstance(MainActivity.this).enqueue(request);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateButtonState(boolean isServiceRunning) {
        if (isServiceRunning) {
            startServiceButton.setVisibility(View.GONE);
            stopServiceButton.setVisibility(View.VISIBLE);
            stopServiceButton.setEnabled(true);
        } else {
            startServiceButton.setVisibility(View.VISIBLE);
            stopServiceButton.setVisibility(View.GONE);
            stopServiceButton.setEnabled(false);
        }
    }

    private void createNotificationChannel() {
        // Check if the Android version is Oreo (API 26) or higher, as NotificationChannel is only available in API 26 and higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Define the channel ID and name. It's better to use a string resource or a static final field for the channel ID.
            CharSequence name = getString(R.string.sensor_channel_name);
            String description = getString(R.string.sensor_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT; // You can choose the importance level as per your need.

            // Create the NotificationChannel object with the above details.
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system.
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void registerReceiver() {
        // Single receiver for multiple sensors
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("ACCELEROMETER_DATA"));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract accelerometer data from the intent
            if (intent.getAction().equals("ACCELEROMETER_DATA")) {
                float x = intent.getFloatExtra("x", 0f);
                float y = intent.getFloatExtra("y", 0f);
                float z = intent.getFloatExtra("z", 0f);

                // Update the chart with the received accelerometer data
                addEntry(x, y, z);
            }
        }
    };


    // Method to update chart with new sensor data
    public void addEntry(float x, float y, float z) {
        // Check if the datasets are already created
        if (xDataSet == null) {
            xDataSet = createSet("X Axis", Color.RED);
            lineData.addDataSet(xDataSet);
        }
        if (yDataSet == null) {
            yDataSet = createSet("Y Axis", Color.GREEN);
            lineData.addDataSet(yDataSet);
        }
        if (zDataSet == null) {
            zDataSet = createSet("Z Axis", Color.BLUE);
            lineData.addDataSet(zDataSet);
        }

        // Add new entries to the dataset
        xValues.add(new Entry(xValues.size(), x));
        yValues.add(new Entry(yValues.size(), y));
        zValues.add(new Entry(zValues.size(), z));

        // Notify the data has changed
        xDataSet.notifyDataSetChanged();
        yDataSet.notifyDataSetChanged();
        zDataSet.notifyDataSetChanged();

        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private LineDataSet createSet(String label, int color) {
        LineDataSet set = new LineDataSet(null, label);
        set.setLineWidth(2.5f);
        set.setColor(color);
        set.setCircleColor(color);
        set.setCircleRadius(5f);
        set.setFillColor(color);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        return set;
    }

    private long calculateInitialDelay() {
        //Calendar calendar = Calendar.getInstance();
        Log.d("INIT DELAY", "Calculate initial delay");

        Calendar now = Calendar.getInstance();
        Calendar nextUploadTime = Calendar.getInstance();

        // Set the time to 00:01 on the next day
        //nextUploadTime.add(Calendar.DAY_OF_YEAR, 1);
        nextUploadTime.set(Calendar.HOUR_OF_DAY, 00);
        nextUploadTime.set(Calendar.MINUTE, 01);
        nextUploadTime.set(Calendar.SECOND, 0);
        nextUploadTime.set(Calendar.MILLISECOND, 0);

        // Adjust the day if the set time has already passed
        if (nextUploadTime.before(now)) {
            nextUploadTime.add(Calendar.DAY_OF_YEAR, 1);

        }

        return nextUploadTime.getTimeInMillis() - now.getTimeInMillis();

    }

    /**
     * Schedules a daily task for uploading data to Firebase using WorkManager.
     */
    private void scheduleDailyUploadTask() {
        long initialDelay = calculateInitialDelay();
        Log.d("FIREBASE TASK", "UPLOAD REQUEST");

        PeriodicWorkRequest uploadWorkRequest = new PeriodicWorkRequest.Builder(FirebaseWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("daily_upload")
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                "upload_data",
                ExistingPeriodicWorkPolicy.UPDATE,
                uploadWorkRequest
        );

        // Notify the user that the scheduling was successful
        Toast.makeText(MainActivity.this, "Daily upload scheduled successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SimpleEventBus.registerListener(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorUsageReceiver, new IntentFilter("com.fyp.mydataismine.SENSOR_USAGE"));
    }

    @Override
    protected void onStop() {
        SimpleEventBus.unregisterListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorUsageReceiver);
        super.onStop();
    }

    @Override
    public void onPacketReceived(PacketInfo packetInfo) {
        //if ("UDP".equals(packetInfo.getProtocol())) {
            runOnUiThread(() -> updateChart(packetInfo));
        //}
    }

    private void setupNetworkChart() {

        networkChart = findViewById(R.id.networkChart);
        networkChart.getDescription().setEnabled(false);
        networkChart.setTouchEnabled(true);
        networkChart.setDragEnabled(true);
        networkChart.setScaleEnabled(true);
        networkChart.setDrawGridBackground(false);

        XAxis x = networkChart.getXAxis();
        x.setDrawLabels(false);  // We might not show X labels for clarity
        x.setDrawGridLines(false);

        YAxis y = networkChart.getAxisLeft();
        y.setLabelCount(6, false);
        y.setTextColor(Color.WHITE);
        y.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        y.setDrawGridLines(true);
        y.setAxisLineColor(Color.WHITE);

        networkChart.getAxisRight().setEnabled(false);

        // Initialize with empty data
        LineData data = new LineData();
        networkChart.setData(data);
    }

    private void updateChart(PacketInfo packetInfo) {
        LineData data = networkChart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByLabel("TCP/UDP Payload Size", true);
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            // Here, we add the entry and assume the X value is the count of entries already present.
            // This will automatically increment X, ensuring each new data point moves the graph forward.
            set.addEntry(new Entry(set.getEntryCount(), packetInfo.getPayloadSize()));

            data.notifyDataChanged();
            networkChart.notifyDataSetChanged();

            // Move the view to the last entry, showing the latest n entries.
            networkChart.setVisibleXRangeMaximum(50); // For example, show 50 entries at a time
            networkChart.moveViewToX(data.getEntryCount() - 51); // Moves to the latest entry
        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "TCP/UDP Payload Size");
        set.setLineWidth(2.5f);
        set.setColor(Color.BLUE);
        set.setCircleColor(Color.RED);
        set.setValueTextColor(Color.WHITE);
        set.setCircleRadius(3f);
        set.setFillAlpha(65);
        set.setFillColor(Color.BLUE);
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }


    private BarDataSet createSet(String sensorName) {
        List<BarEntry> entries = new ArrayList<>();
        BarDataSet set = new BarDataSet(entries, sensorName);
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        return set;
    }

    private void setupSensorActivityChart() {
        sensorActivityChart = findViewById(R.id.sensorActivityChart);
        sensorActivityChart.getDescription().setEnabled(false);
        sensorActivityChart.setTouchEnabled(true);
        sensorActivityChart.setDragEnabled(true);
        sensorActivityChart.setScaleEnabled(true);
        sensorActivityChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Assuming value is a timestamp in milliseconds
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date((long) value));
            }
        });

        LineData data = new LineData();
        sensorActivityChart.setData(data);
        sensorActivityChart.invalidate(); // Refresh the chart
    }

    private BroadcastReceiver sensorUsageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.fyp.mydataismine.SENSOR_USAGE".equals(intent.getAction())) {
                long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
                float value = intent.getFloatExtra("value", 1); // Assuming '1' denotes an activation
                addSensorDataToChart(timestamp, value);
            }
        }
    };


    public void addSensorDataToChart(long timestamp, float value) {
        LineData data = sensorActivityChart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSetSensorActivity();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(timestamp, value), 0);
            data.notifyDataChanged();
            sensorActivityChart.notifyDataSetChanged();
            sensorActivityChart.setVisibleXRangeMaximum(60000); // show for example last minute
            sensorActivityChart.moveViewToX(data.getEntryCount() - 61);
        }
    }

    private LineDataSet createSetSensorActivity() {
        LineDataSet set = new LineDataSet(null, "Sensor Activations");
        set.setLineWidth(2.5f);
        set.setColor(Color.BLUE);
        set.setCircleColor(Color.BLUE);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

}