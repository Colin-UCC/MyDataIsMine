package com.fyp.mydataismine.sensormanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        // Ensure the database is deleted before each test
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    @After
    public void finish() {
        db.close();
        dbHelper.close();
    }

    private void clearDatabase() {
        db.delete(DatabaseHelper.TABLE_NAME, null, null);
    }

    @Test
    public void testAddAndRetrieveReadings() {
        clearDatabase(); // Clear the database for the clean start

        // Insert a sample reading
        dbHelper.addReading(5.0f, 6.0f, 7.0f);

        // Verify data is inserted correctly
        ArrayList<AccelerometerReading> readings = dbHelper.getAllReadingObjects();
        assertNotNull(readings);
        assertEquals(1, readings.size());

        AccelerometerReading reading = readings.get(0);
        assertEquals(5.0f, reading.getX(), 0.001);
        assertEquals(6.0f, reading.getY(), 0.001);
        assertEquals(7.0f, reading.getZ(), 0.001);
    }

    @Test
    public void testGetAllReadingObjects() {
        clearDatabase(); // Ensure database is clean before the test

        // Add multiple readings
        dbHelper.addReading(1.0f, 2.0f, 3.0f);
        dbHelper.addReading(4.0f, 5.0f, 6.0f);

        // Retrieve readings as objects
        ArrayList<AccelerometerReading> readings = dbHelper.getAllReadingObjects();
        assertNotNull(readings);
        assertEquals(2, readings.size());

        // Check the first reading
        AccelerometerReading firstReading = readings.get(0);
        assertEquals(1.0f, firstReading.getX(), 0.001);
        assertEquals(2.0f, firstReading.getY(), 0.001);
        assertEquals(3.0f, firstReading.getZ(), 0.001);

        // Check the second reading
        AccelerometerReading secondReading = readings.get(1);
        assertEquals(4.0f, secondReading.getX(), 0.001);
        assertEquals(5.0f, secondReading.getY(), 0.001);
        assertEquals(6.0f, secondReading.getZ(), 0.001);
    }
}
