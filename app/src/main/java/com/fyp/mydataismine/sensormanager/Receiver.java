package com.fyp.mydataismine.sensormanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Broadcast receiver for handling accelerometer data.
 */
public class Receiver extends BroadcastReceiver {

    /**
     * Responds to broadcast messages.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        // Check for the specific action that indicates accelerometer data
        if ("ACCELEROMETER_DATA".equals(action)) {
            // Create and send a local broadcast with the accelerometer data
            Intent localIntent = new Intent("LOCAL_ACCELEROMETER_DATA");
            localIntent.putExtras(intent.getExtras()); // Transfer the received data
            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
        }
    }
}