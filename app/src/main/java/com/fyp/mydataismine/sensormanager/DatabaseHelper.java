package com.fyp.mydataismine.sensormanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Database Version and Name
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "sensorData";

    // Table name and Columns names
    private static final String TABLE_NAME = "readings";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_X = "x";
    private static final String COLUMN_Y = "y";
    private static final String COLUMN_Z = "z";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    /**
     * Constructor for DatabaseHelper.
     * @param context The context where the database helper is used.
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_X + " REAL,"
                + COLUMN_Y + " REAL," + COLUMN_Z + " REAL,"
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * Adds a new accelerometer reading to the database.
     * @param x The x-value of the accelerometer reading.
     * @param y The y-value of the accelerometer reading.
     * @param z The z-value of the accelerometer reading.
     */
    public void addReading(float x, float y, float z) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_X, x);
        values.put(COLUMN_Y, y);
        values.put(COLUMN_Z, z);

        long result = db.insert(TABLE_NAME, null, values);

        if (result != -1) {
            Log.d("DatabaseHelper", "Reading added successfully.");
        } else {
            Log.e("DatabaseHelper", "Failed to add reading.");
        }
        db.close();
    }

    /**
     * Retrieves all sensor readings from the database.
     * @return A cursor to the set of readings.
     */
    public Cursor getAllReadings() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    /**
     * Retrieves all sensor readings as a list of AccelerometerReading objects.
     * @return An ArrayList of AccelerometerReading objects.
     */
    public ArrayList<AccelerometerReading> getAllReadingObjects() {
        ArrayList<AccelerometerReading> readings = new ArrayList<>();
        //SQLiteDatabase db = this.getWritableDatabase();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            int columnIndexX = cursor.getColumnIndex(COLUMN_X);
            int columnIndexY = cursor.getColumnIndex(COLUMN_Y);
            int columnIndexZ = cursor.getColumnIndex(COLUMN_Z);
            int columnIndexTime = cursor.getColumnIndex(COLUMN_TIMESTAMP);

            do {
                if (columnIndexX != -1 && columnIndexY != -1 && columnIndexZ != -1) {
                    float x = cursor.getFloat(columnIndexX);
                    float y = cursor.getFloat(columnIndexY);
                    float z = cursor.getFloat(columnIndexZ);
                    String timestamp = cursor.getString(columnIndexTime);

                    readings.add(new AccelerometerReading(x, y, z, timestamp));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return readings;
    }


}

