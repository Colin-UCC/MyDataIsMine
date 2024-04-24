package com.fyp.mydataismine.sensormanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fyp.mydataismine.MainActivity;
import com.fyp.mydataismine.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

/**
 * A service that monitors sensor usage on the device, logging access details and uploading the logs to Firebase.
 */
public class SensorMonitorService extends Service {
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "sensor_monitoring_service_channel";
    public static final String ACTION_WRITE_AND_UPLOAD_LOG = "com.fyp.mydataismine.sensormanager.ACTION_WRITE_AND_UPLOAD_LOG";
    private ScheduledExecutorService scheduler;
    private DatabaseReference mDatabase;
    private static final String PREFS_NAME = "SensorServicePrefs";
    private static final String EVENTS_KEY = "LoggedEvents";
    private Map<String, String> sensorMap = new HashMap<>();
    private Map<String, Long> loggedEvents = new HashMap<>();

    /**
     * Initializes the service, setting up the notification channel and starting sensor monitoring.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        loadLoggedEvents();
        startMonitoringSensorUsage();
        initFirebaseDatabase();
        //getAllPackageNames();
        getAllNonSystemPackageNames();
        printLoggedEvents();
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
        saveLoggedEvents();
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
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        scheduler.scheduleAtFixedRate(this::monitorSensorUsage, 0, 1, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::cleanOldLoggedEvents, 1, 1, TimeUnit.HOURS);
    }

    /**
     * Monitors sensor usage, capturing sensor access details and logging them.
     */
    private void monitorSensorUsage() {
        Log.d("SensorMonitor", "Monitoring sensor usage started.");
        Process process = null;
        BufferedReader reader = null;
        FileWriter writer = null;
        File logFile = null;
        // Execute the dumpsys command and process its output
        try {
            process = Runtime.getRuntime().exec("su -c dumpsys sensorservice");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            logFile = new File(getExternalFilesDir(null), "sensor_service_log.txt");
            writer = new FileWriter(logFile, false);

            String line;

            while ((line = reader.readLine()) != null) {
                //Log.d("SensorMonitorService", line);
                //writer.write(line + "\n"); // Write each line to the file
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
        return packageName.startsWith("com.android.") || packageName.startsWith("com.google.") || packageName.startsWith("android.");
    }

    /**
     * Processes a line of sensor usage data, extracting and logging relevant information.
     * @param line A string containing a line of sensor usage data.
     */
    private void parseSensorList(String line) {
        // line format: '0x0000000b) BMI160 Accelerometer | BOSCH'
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

    /**
     * Processes a line of sensor usage information to update sensor logs or handle special cases like resetting the sensor list.
     *
     * @param line the string line from the sensor log to be processed.
     */
    private void processSensorUsageLine(String line) {
        if (line.startsWith("Sensor List:")) {
            // Clear previous sensor list entries
            sensorMap.clear();
            return;
        }

        // Parse the sensor list to fill the sensorMap
        if (line.startsWith("0x")) {
            parseSensorList(line);
            return;
        }

        if (line.contains("pid=") && line.contains("package=")) {// && isNewEntry(line)) {
            try {
                String[] parts = line.split("\\s+");
                String time = parts[0];

                if (isCurrentOrPastTime(time)) {
                    String sensorCode = parts[2];
                    String packageName = line.substring(line.indexOf("package=") + 8);
                    if (isSystemApp(packageName)) {
                        return;
                    }

                    String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
                    String fullTimestamp = currentDate + " " + time; // Combine date and time


                    for (String part : parts) {
                        if (part.startsWith("package=")) {
                            packageName = part.substring("package=".length());
                            break;
                        }
                    }
                    if (!sensorCode.startsWith("0x")) {
                        sensorCode = "0x" + sensorCode;
                    }
                    String sensorName = sensorMap.get(sensorCode);

                    if (sensorName != null) {
                        String eventKey = packageName + "_" + sensorCode + "_" + fullTimestamp;
                        if (addEventToLog(eventKey)) {
                            SensorLogEntry entry = new SensorLogEntry(packageName, sensorName, sensorCode, fullTimestamp);
                            Log.w("New Log", "new log added: " + eventKey);
                            uploadSensorLogEntryToFirebase(entry);

                            // Notify the user with a notification
                            notifyUser(entry);

                            Log.d("SensorMonitorService", "New Event Logged: " + entry.toString());
                        } else {
                            Log.d("SensorMonitorService", "Duplicate Event Skipped: " + eventKey);
                        }
                    }
            } else {
                    Log.d("SensorMonitorService", "Skipped entry from previous day: " + line);
                }
            } catch (Exception e) {
                Log.e("SensorMonitorService", "Error processing sensor usage line", e);
            }
        }
    }

    /**
     * Adds a new event to the logged events if it is not already logged.
     *
     * @param entry the string representing the unique key of the event to log.
     * @return true if the event is added, false if it is a duplicate.
     */
    private boolean addEventToLog(String entry) {
        synchronized (this) {
            long currentTime = System.currentTimeMillis();
            if (!loggedEvents.containsKey(entry)) {
                loggedEvents.put(entry, currentTime);
                Log.d("SensorMonitorService", "Event added to log: " + entry);
                return true;  // Indicates the entry was added
            }
            Log.d("SensorMonitorService", "Attempt to add duplicate event prevented: " + entry);
            return false;  // Indicates the entry was not added because it's a duplicate
        }
    }

    /**
     * Retrieves the unique user identifier from Firebase Authentication.
     *
     * @return the current user's unique ID if logged in, otherwise null.
     */
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    /**
     * Uploads a sensor log entry to Firebase, ensuring no duplicates are created.
     *
     * @param entry the SensorLogEntry object to upload.
     */
    private void uploadSensorLogEntryToFirebase(SensorLogEntry entry) {
        String userId = getCurrentUserId();
        if (userId != null && mDatabase != null) {
            String uniqueKey = entry.createUniqueKey(); // Generate a unique key for the entry
            DatabaseReference entryRef = mDatabase.child("users").child(userId).child("sensorlogs").child(uniqueKey);

            entryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) { // If the entry does not exist, push it
                        entryRef.setValue(entry)
                                .addOnSuccessListener(aVoid -> Log.d("FirebaseDB", "Data uploaded successfully."))
                                .addOnFailureListener(e -> Log.e("FirebaseDB", "Failed to upload data.", e));
                    } else {
                        Log.d("FirebaseDB", "Duplicate entry not uploaded.");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("FirebaseDB", "Firebase database error: " + databaseError.getMessage());
                }
            });
        }
    }

    /**
     * Sends a notification to the user about a sensor event.
     *
     * @param entry the sensor log entry based on which the notification is created.
     */

    private void notifyUser(SensorLogEntry entry) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        String notificationChannelId = "sensor_usage_notification_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    notificationChannelId,
                    "Sensor Usage Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, notificationChannelId)
                .setContentTitle("New Sensor Activity Detected")
                .setContentText("Sensor: " + entry.getSensorName() + " used by " + entry.getPackageName())
                .setSmallIcon(R.drawable.ic_notification)  // Ensure you have an appropriate icon here
                .setAutoCancel(true)
                .build();

        notificationManager.notify(new Random().nextInt(), notification);
    }


    /**
     * Checks if the provided log time is current or in the past compared to the system's current time.
     *
     * @param logTime the time string from the log entry.
     * @return true if the log time is current or past, false otherwise.
     */
    private boolean isCurrentOrPastTime(String logTime) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            Date logDate = timeFormat.parse(logTime); // Parse the log's time
            Date currentDate = new Date(); // Get the current system time

            // Format both dates back to time-only strings to compare just the times
            String currentTime = timeFormat.format(currentDate);

            // If the log time is greater than the current time, it's from the past day
            return logDate.compareTo(timeFormat.parse(currentTime)) <= 0;
        } catch (ParseException e) {
            Log.e("SensorMonitorService", "Failed to parse time: " + logTime, e);
            return false;
        }
    }

    /**
     * Fetches all installed package names on the device that do not belong to system apps.
     *
     * @return a list of non-system application package names.
     */
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

    /**
     * Prints all currently logged events to the debug log.
     */
    public void printLoggedEvents() {
        Log.d("SensorMonitorService", "Current Logged Events:");
        for (Map.Entry<String, Long> entry : loggedEvents.entrySet()) {
            // Log the event along with its timestamp
            Log.d("LOGGED EVENTS", "Event: " + entry.getKey() + ", Timestamp: " + entry.getValue());
        }
    }

    /**
     * Saves the current state of logged events to SharedPreferences.
     */
    private void saveLoggedEvents() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(loggedEvents); // Serialize map to JSON string
        editor.putString(EVENTS_KEY, json);
        editor.apply();
        Log.d("SensorMonitorService", "Logged events saved as JSON.");
    }

    /**
     * Loads logged events from SharedPreferences back into the running application.
     */
    private void loadLoggedEvents() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        try {
            String json = prefs.getString(EVENTS_KEY, null);
            if (json != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<HashMap<String, Long>>(){}.getType();
                HashMap<String, Long> map = gson.fromJson(json, type);
                loggedEvents = map != null ? map : new HashMap<>();
            } else {
                loggedEvents = new HashMap<>();
            }
        } catch (Exception e) {
            Log.e("Loading Error", "Error loading events from SharedPreferences", e);
            loggedEvents = new HashMap<>();
        }
        Log.d("SensorMonitorService", "Logged events loaded from JSON.");
    }

    /**
     * Removes logged events that are older than 24 hours from the current system time.
     */
    private void cleanOldLoggedEvents() {
        long twentyFourHoursAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        synchronized (this) {
            Iterator<Map.Entry<String, Long>> it = loggedEvents.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> entry = it.next();
                if (entry.getValue() < twentyFourHoursAgo) {
                    it.remove();
                    Log.d("SensorMonitorService", "Old event removed from log: " + entry.getKey());
                }
            }
        }
    }

}




