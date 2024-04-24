package com.fyp.mydataismine.packetcapture;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.fyp.mydataismine.packetcapture.PacketDbHelper;

@RunWith(AndroidJUnit4.class)
public class PacketDBHelperTest {

    private PacketDbHelper dbHelper;
    private SQLiteDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        dbHelper = new PacketDbHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    @Test
    public void testDatabaseCreationAndTable() {
        assertTrue(db.isOpen());
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='packets'", null);
        assertTrue(cursor.moveToFirst());
        assertEquals("packets", cursor.getString(0));
        cursor.close();
    }

    @Test
    public void testInsertPacketData() {
        // Insert data into the database
        long rowId = dbHelper.insertPacketData("192.168.1.1", "192.168.1.2", 500, "TCP");

        // Check that data was inserted successfully
        assertNotEquals(-1, rowId);

        // Query the database to retrieve the inserted data
        Cursor cursor = db.query("packets", null, null, null, null, null, null);

        // Validate the query results
        assertTrue(cursor.moveToFirst());
        assertEquals("192.168.1.1", cursor.getString(cursor.getColumnIndex("source_ip")));
        assertEquals("192.168.1.2", cursor.getString(cursor.getColumnIndex("destination_ip")));
        assertEquals(500, cursor.getInt(cursor.getColumnIndex("payload_size")));
        assertEquals("TCP", cursor.getString(cursor.getColumnIndex("protocol_type")));

        // Close the cursor
        cursor.close();
    }




    @After
    public void tearDown() {
        db.close();
    }
}
