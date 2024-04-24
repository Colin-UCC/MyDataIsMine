package com.fyp.mydataismine.packetcapture;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class to manage database creation and version management for packet data storage.
 * This class handles the setup and updates of the database used to store packet information
 * captured during network traffic analysis.
 */
public class PacketDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PacketData.db";
    private static final int DATABASE_VERSION = 2;

    private static PacketDbHelper instance;

    public static synchronized PacketDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PacketDbHelper(context.getApplicationContext());
        }
        return instance;
    }

        private static final String TABLE_CREATE =
            "CREATE TABLE packets (" +
                    "id INTEGER PRIMARY KEY," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "source_ip TEXT," +
                    "destination_ip TEXT," +
                    "protocol_type TEXT," +
                    "payload_size INTEGER)";

    /**
     * Constructs a new helper object to create, open, and/or manage a database.
     * The database is not actually created or opened until one of {@link #getWritableDatabase()} or
     * {@link #getReadableDatabase()} is called.
     *
     * @param context to use for locating paths to the database
     */
    public PacketDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * This is where the creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    /**
     * Called when the database needs to be upgraded.
     * The implementation should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     *
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS packets");
        onCreate(db);
    }

    /**
     * Inserts packet data into the database.
     * This method adds a new row to the packets table with the provided packet information.
     *
     * @param sourceIp      The source IP address of the packet.
     * @param destinationIp The destination IP address of the packet.
     * @param payloadSize   The size of the packet payload.
     * @param protocol      The protocol used by the packet.
     */
//    public void insertPacketData(String sourceIp, String destinationIp, int payloadSize, String protocol) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put("source_ip", sourceIp);
//        values.put("destination_ip", destinationIp);
//        values.put("payload_size", payloadSize);
//        values.put("protocol", protocol);
//
//        db.insert("packets", null, values);
//        db.close();
//    }
    public long insertPacketData(String sourceIp, String destinationIp, int payloadSize, String protocol) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("source_ip", sourceIp);
        values.put("destination_ip", destinationIp);
        values.put("payload_size", payloadSize);
        values.put("protocol_type", protocol);

        long rowId = db.insertOrThrow("packets", null, values);
        //db.close();
        return rowId;
    }

}

