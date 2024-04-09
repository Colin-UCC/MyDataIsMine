package com.fyp.mydataismine.sensormanager;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FirebaseWorker extends Worker {

    /**
     * Constructor for FirebaseWorker.
     * @param context The application context.
     * @param workerParams Parameters for the worker.
     */
    public FirebaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Performs the work needed to extract data from the database, convert it to CSV, and upload it to Firebase.
     * @return The result of the work, either SUCCESS or FAILURE.
     */
    @NonNull
    @Override
    public Result doWork() {
        //String csvData = convertDatabaseToCsv();
        String csvData = convertDatabaseToDailyCsv();

        // Get current user's UID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userUID = currentUser.getUid();

            // Upload CSV to Firebase, specific to the user
            if (csvData != null) {
                uploadCsvToFirebase(csvData, userUID);
                return Result.success();
            }
        }
        return Result.failure();
        //Toast.makeText(FirebaseWorker.this, "Test file upload failed", Toast.LENGTH_SHORT).show();
    }

    /**
     * Converts the SQLite database contents to a CSV string.
     * @return The CSV string representing the database contents.
     */
    private String convertDatabaseToCsv() {
        StringBuilder csvData = new StringBuilder();
        DatabaseHelper databaseHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM readings", null);

        try {
            String[] columnNames = cursor.getColumnNames();
            csvData.append(TextUtils.join(",", columnNames)).append("\n");

            while (cursor.moveToNext()) {
                List<String> rowData = new ArrayList<>();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    rowData.add(cursor.getString(i));
                }
                csvData.append(TextUtils.join(",", rowData)).append("\n");
            }
        } catch (Exception e) {
            Log.e("FirebaseWorker", "Error converting database to CSV", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }

        return csvData.toString();
    }

    /**
     * Converts the SQLite database contents of a specific day to a CSV string.
     * @return The CSV string representing the database contents for that day.
     */
    private String convertDatabaseToDailyCsv() {
        Log.d("FirebaseWorker", "Starting conversion of database to daily CSV");

        // Get yesterday's date
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String yesterdayDate = sdf.format(calendar.getTime());

        StringBuilder csvData = new StringBuilder();
        DatabaseHelper databaseHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        // Set calendar to the start of yesterday
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        String startDayString = sdf.format(calendar.getTime()) + " 00:00:00";

        // Set calendar to the end of yesterday
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        String endDayString = sdf.format(calendar.getTime()) + " 23:59:59";

        Log.d("FirebaseWorker", "Start of day: " + startDayString);
        Log.d("FirebaseWorker", "End of day: " + endDayString);

        String query = "SELECT * FROM readings WHERE timestamp >= ? AND timestamp <= ?";
        Cursor cursor = db.rawQuery(query, new String[]{startDayString, endDayString});

        try {
            String[] columnNames = cursor.getColumnNames();
            csvData.append(TextUtils.join(",", columnNames)).append("\n");

            while (cursor.moveToNext()) {
                List<String> rowData = new ArrayList<>();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    rowData.add(cursor.getString(i));
                }
                csvData.append(TextUtils.join(",", rowData)).append("\n");
            }
        } catch (Exception e) {
            Log.e("FirebaseWorker", "Error converting database to CSV", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }

        return csvData.toString();
    }

    /**
     * Uploads the CSV string data to Firebase under the user's unique storage space.
     * @param csvData The CSV data to upload.
     * @param userUID The user's unique identifier in Firebase.
     */
    private void uploadCsvToFirebase(String csvData, String userUID) {
        if (csvData == null || csvData.isEmpty()) {
            Log.e("FirebaseWorker", "CSV data is empty, cannot upload.");
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1); // Adjust to yesterday
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
        String fileNameDate = sdf.format(calendar.getTime()); // Get yesterday's date for file name

        String fileName = "accelerometer_data_" + fileNameDate + ".csv"; // Naming convention
        StorageReference csvRef = storageRef.child("users/" + userUID + "/" + fileName);

        byte[] csvBytes = csvData.getBytes(StandardCharsets.UTF_8);
        UploadTask uploadTask = csvRef.putBytes(csvBytes);

        uploadTask.addOnSuccessListener(taskSnapshot ->
                Log.d("FirebaseWorker", "CSV upload successful: " + fileName)
        ).addOnFailureListener(exception ->
                Log.e("FirebaseWorker", "CSV upload failed", exception)
        );
    }

    /**
     * Generates a date string for the previous day in 'yyyy_MM_dd' format.
     * @return A string representing yesterday's date.
     */
    private String getYesterdayDateString() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
}

