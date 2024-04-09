package com.fyp.mydataismine.sensormanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.fyp.mydataismine.MainActivity;
import com.fyp.mydataismine.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A service that monitors sensor usage on the device, logging access details and uploading the logs to Firebase.
 */
public class SensorMonitorService extends Service {
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "sensor_monitoring_service_channel";
    public static final String ACTION_WRITE_AND_UPLOAD_LOG = "com.fyp.mydataismine.sensormanager.ACTION_WRITE_AND_UPLOAD_LOG";
    private ScheduledExecutorService scheduler;
    private DatabaseReference mDatabase;
    private Map<String, String> sensorMap = new HashMap<>();
    private Set<String> loggedEvents = new HashSet<>();

    /**
     * Initializes the service, setting up the notification channel and starting sensor monitoring.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startMonitoringSensorUsage();
        initFirebaseDatabase();
        //getAllPackageNames();
        getAllNonSystemPackageNames();
    }

    /**
     * Starts the service with the intent to either monitor sensor usage or process a specific action.
     * @param intent The Intent supplied to {@code startService}, containing the action to perform.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's current started state.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_WRITE_AND_UPLOAD_LOG.equals(intent.getAction())) {
            writeLogFile();
            uploadLogFileToFirebase(new File(getExternalFilesDir(null), "sensor_service_log.txt"));
            return START_NOT_STICKY;
        }

        // Prepare the notification information
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sensor Monitoring")
                .setContentText("Monitoring sensor activity...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        // Call startForeground() as soon as possible
        startForeground(SERVICE_NOTIFICATION_ID, notification);

        return START_NOT_STICKY;
    }

    /**
     * Cleans up resources and stops the service.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Shut down the scheduler to stop monitoring when service is destroyed
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * Initializes the connection to Firebase Database for data storage.
     */
    private void initFirebaseDatabase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Creates a notification channel for running this service in the foreground on Android O and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.monitor_channel_name);
            String description = getString(R.string.monitor_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Binds to the service when use by in-app components.
     * @param intent The Intent that was used to bind to this service.
     * @return Return an IBinder through which clients can call on to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Starts monitoring sensor usage, setting up a scheduled task to perform monitoring at regular intervals.
     */
    private void startMonitoringSensorUsage() {
        // Initialize the ScheduledExecutorService
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        // Schedule the monitoring task to run every 2 minutes
        scheduler.scheduleAtFixedRate(this::monitorSensorUsage, 0, 2, TimeUnit.MINUTES);
    }

    /**
     * Monitors sensor usage, capturing sensor access details and logging them.
     */
    private void monitorSensorUsage() {
        Process process = null;
        BufferedReader reader = null;
        FileWriter writer = null;
        File logFile = null;
        // Execute the dumpsys command and process its output
        try {
            process = Runtime.getRuntime().exec("su -c dumpsys sensorservice");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            logFile = new File(getExternalFilesDir(null), "sensor_service_log.txt");
            writer = new FileWriter(logFile, false); // true to append, false to overwrite

            String line;

            while ((line = reader.readLine()) != null) {
                //Log.d("SensorMonitorService", line);
                writer.write(line + "\n"); // Write each line to the file
                processSensorUsageLine(line);
            }
        } catch (IOException e) {
            Log.e("SensorMonitorService", "Error executing dumpsys command", e);
        } finally {

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference logFileRef = storageRef.child("sensor_service_logs/" + System.currentTimeMillis() + "_log.txt");

            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                        writer.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (IOException e) {
                Log.e("SensorMonitorService", "Error closing streams", e);
            }
        }
    }

    /**
     * Writes the collected sensor log to a file.
     */
    public void writeLogFile() {
        new Thread(this::monitorSensorUsage).start(); // Call monitorSensorUsage in a separate thread
    }

    /**
     * Uploads the log file to Firebase, associating it with the current user's account.
     * @param logFile The file to be uploaded.
     */
    public void uploadLogFileToFirebase(File logFile) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("SensorMonitorService", "User not logged in, cannot upload log file");
            return;
        }

        if (!logFile.exists()) {
            Log.e("SensorMonitorService", "Log file does not exist, cannot upload.");
            return;
        }

        if (user != null) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();

            // Assuming logFile.getName() includes a unique identifier like a timestamp
            String fileName = "sensor_service_log_" + System.currentTimeMillis() + ".txt";

            // Construct the path in Firebase to store the file
            StorageReference logFileRef = storageRef.child("/users/" + user.getUid() + "/sensor_service_logs/" + fileName);

            Uri fileUri = Uri.fromFile(logFile);
            logFileRef.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> Log.d("SensorMonitorService", "Log file uploaded successfully."))
                    .addOnFailureListener(e -> Log.e("SensorMonitorService", "Error uploading log file", e));
        } else {
            Log.e("SensorMonitorService", "User not logged in, cannot upload log file");
        }
    }

    private boolean isSystemApp(String packageName) {
        return packageName.startsWith("com.android.") || packageName.startsWith("com.google.");
    }

    /**
     * Processes a line of sensor usage data, extracting and logging relevant information.
     * @param line A string containing a line of sensor usage data.
     */
    private void parseSensorList(String line) {
        // Example line format: "0x0000000b) BMI160 Accelerometer | BOSCH ..."
        if (line.contains(")")) {
            String[] parts = line.split("\\) ");
            if (parts.length > 1) {
                String handle = parts[0].trim(); // e.g., "0x0000000b"
                String sensorDetails = parts[1];
                String sensorName = sensorDetails.split("\\|")[0].trim(); // e.g., "BMI160 Accelerometer"
                sensorMap.put(handle, sensorName);
            }
        }
    }

    private void processSensorUsageLine(String line) {
        if (line.startsWith("Sensor List:")) {
            // Clear previous sensor list entries
            sensorMap.clear();
            return;
        }

        // Parse the sensor list to fill the sensorMap
        if (line.startsWith("0x")) {
            parseSensorList(line);
            return; // Exit the method after parsing the sensor list line
        }

        if (line.contains("pid=") && line.contains("package=")) {
            try {
                String[] parts = line.split("\\s+");
                String time = parts[0];
                String sensorCode = parts[2];  // Assuming the sensor code is the third element after splitting by space
                String packageName = line.substring(line.indexOf("package=") + 8);

                if (isSystemApp(packageName)) {
                    return; // Skip system apps
                }

                for (String part : parts) {
                    if (part.startsWith("package=")) {
                        packageName = part.substring("package=".length());
                        break;
                    }
                }

                // Ensure sensorCode has the correct format before looking it up
                if (!sensorCode.startsWith("0x")) {
                    sensorCode = "0x" + sensorCode;
                }

                String sensorName = sensorMap.get(sensorCode);
                String eventKey = packageName + "_" + sensorCode + "_" + time;

                if (sensorName != null && !loggedEvents.contains(eventKey)) {
                    loggedEvents.add(eventKey); // Add to logged events to avoid duplicates
                    SensorLogEntry entry = new SensorLogEntry(packageName, sensorName, sensorCode, time);
                    uploadSensorLogEntryToFirebase(entry);
                    Log.d("SensorMonitorService", entry.toString());
                }
            } catch (Exception e) {
                Log.e("SensorMonitorService", "Error processing sensor usage line", e);
            }
        }
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    private void uploadSensorLogEntryToFirebase(SensorLogEntry entry) {
        String userId = getCurrentUserId();
        if (userId != null && mDatabase != null) {
            String basePath = "users/" + userId + "/sensorlogs";
            String key = mDatabase.child(basePath).push().getKey();
            if (key != null) {
                mDatabase.child(basePath).child(key).setValue(entry)
                        .addOnSuccessListener(aVoid -> Log.d("FirebaseDB", "Data uploaded successfully to path: " + basePath))
                        .addOnFailureListener(e -> Log.e("FirebaseDB", "Failed to upload data to path: " + basePath, e));
            }
        }
    }

    private List<String> getAllPackageNames() {
        List<String> packageNames = new ArrayList<>();
        Process process = null;
        BufferedReader reader = null;

        try {
            // Execute the 'pm list packages' command
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", "pm list packages"});
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {

                // Lines are of the form: package:com.example.package
                String packageName = line.split(":")[1];
                Log.d("package list: ", line);
                packageNames.add(packageName);

            }
        } catch (IOException e) {
            Log.e("SensorMonitorService", "Error fetching package names", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e("SensorMonitorService", "Error closing stream", e);
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
        return packageNames;
    }

    private List<String> getAllNonSystemPackageNames() {
        List<String> packageNames = new ArrayList<>();
        Process process = null;
        BufferedReader reader = null;

        try {
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", "pm list packages"});
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // Extract the package name from each line
                String packageName = line.split(":")[1];

                // Check if the package name does not start with common system app prefixes
                if (!packageName.startsWith("com.android.") &&
                        !packageName.startsWith("com.qualcomm.") &&
                        !packageName.startsWith("com.google.") &&
                        !packageName.startsWith("android")) {

                    // Add the package name to the list if it's a non-system app
                    packageNames.add(packageName);
                    Log.d("NonSystemApp", "Package: " + packageName);
                }
            }
        } catch (IOException e) {
            Log.e("SensorMonitorService", "Error fetching package names", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e("SensorMonitorService", "Error closing stream", e);
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

        return packageNames;
    }

    private void notifyUserAboutSensorUsage(String packageName, String notificationText) {
        int notificationId = packageName.hashCode();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sensor Access Alert")
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }

    private boolean isSystemPackage(PackageManager pm, String packageName) {
        // Broad criteria for system packages
        if (packageName.startsWith("com.android.") || packageName.startsWith("com.google.android.") || packageName.startsWith("android.")) {
            return true;
        }

        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            // Log and treat unrecognized packages potentially as system components
            //Log.d("pm", pm.toString());
            //Log.d("SensorUsage", "Third Party package by found: " + packageName);
            return false; // Treat as system package by default or based on your specific criteria
        }
    }

    private String getAppNameFromPackageName(String packageName) {
        PackageManager packageManager = getApplicationContext().getPackageManager();
        String appName;
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            appName = (String) packageManager.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("SensorMonitorService", "Package name not found: " + packageName, e);
            appName = packageName; // Fallback to package name if the app name is not found
        }
        return appName;
    }

    private String extractSensorInfo(String line) {
        // Implement this method to extract sensor information from the line
        // This could involve parsing the line to find the sensor name or ID
        return line.substring(line.indexOf("Sensor: ") + 8, line.indexOf(", PID:")); // Example extraction logic
    }

    private String extractPackageName(String line) {
        // Implement this method to extract package name from the line
        return line.substring(line.indexOf("package=") + 8).split(" ")[0]; // Example extraction logic
    }

    private void uploadSensorLogToFirebase(SensorLogEntry entry) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("sensorLogs").child(user.getUid());
            databaseReference.push().setValue(entry)
                    .addOnSuccessListener(aVoid -> Log.d("FirebaseUpload", "Sensor log uploaded successfully."))
                    .addOnFailureListener(e -> Log.e("FirebaseUpload", "Failed to upload sensor log.", e));
        }
    }
}
