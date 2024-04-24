package com.fyp.mydataismine.packetcapture;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.fyp.mydataismine.MainActivity;
import com.fyp.mydataismine.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * VPNNetworkService extends Android's VpnService to implement a custom VPN that captures and analyzes network traffic.
 * It establishes a VPN interface to intercept and route the device's network traffic through the service, allowing for packet inspection, logging, and modification.
 * The service manages separate queues for UDP and TCP traffic to process them accordingly.
 */
public class VPNNetworkService extends VpnService {

    private VPNRunnable vpnRunnable;
    private static final String TAG = VPNNetworkService.class.getSimpleName();
    private static final String VPN_ADDRESS = "10.0.0.2";
    private static final String VPN_ROUTE = "0.0.0.0";
    private static final int NOTIFICATION_ID = 1;
    public static final String BROADCAST_VPN_STATE = "com.fyp.packetinterceptor.VPN_STATE";
    public static final String ACTION_START_VPN = "com.fyp.packetinterceptor.START_VPN";
    public static final String ACTION_STOP_VPN = "com.fyp.packetinterceptor.STOP_VPN";
    private static boolean isRunning = false;
    private ParcelFileDescriptor vpnInterface = null;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private ExecutorService executorService;
    private FileDescriptor vpnFileDescriptor;
    private Selector udpSelector;
    private Selector tcpSelector;
    private Thread vpnRunnableThread;

    /**
     * Initializes the VPN service, creating a notification channel for foreground execution and setting up the VPN interface.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "vpn_service_channel",
                    "VPN Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        isRunning = true;
        setupVPN();
        startVPN();
    }

    /**
     * Sets up the VPN with the required parameters and establishes the VPN interface.
     * This method configures the IP address and routes for the VPN and initializes the interface.
     */
    private void setupVPN() {
        if (vpnInterface == null) {
            Builder builder = new Builder();
            builder.addAddress(VPN_ADDRESS, 32);
            builder.addRoute(VPN_ROUTE, 0);
            builder.addRoute("::", 0); // Capture all IPv6 traffic

            //builder.addDnsServer("8.8.8.8");
            //builder.addDnsServer("8.8.4.4");

            try {
                // Establish the VPN interface and get the file descriptor
                vpnInterface = builder.setSession(getString(R.string.app_name)).establish();
                vpnFileDescriptor = vpnInterface.getFileDescriptor();
            } catch (Exception e) {

                Log.e(TAG, "Error setting up VPN: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Starts the VPN service, initializing the packet queues and executor service.
     * It also starts separate threads for handling the input and output of TCP and UDP packets.
     */
    private void startVPN() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                deviceToNetworkUDPQueue = new ConcurrentLinkedQueue<>();
                deviceToNetworkTCPQueue = new ConcurrentLinkedQueue<>();
                networkToDeviceQueue = new ConcurrentLinkedQueue<>();

                vpnRunnable = new VPNRunnable(
                        vpnFileDescriptor,
                        deviceToNetworkUDPQueue,
                        deviceToNetworkTCPQueue,
                        networkToDeviceQueue,
                        VPNNetworkService.this,
                        VPNNetworkService.this
                );

                vpnRunnableThread = new Thread(vpnRunnable);

                executorService = Executors.newFixedThreadPool(2);
                executorService.submit(new UDPInput(networkToDeviceQueue, udpSelector));
                //executorService.submit(new UDPOutput(deviceToNetworkUDPQueue, vpnFileDescriptor));
                executorService.submit(new TCPInput(networkToDeviceQueue, vpnFileDescriptor));
                //executorService.submit(new TCPOutput(deviceToNetworkTCPQueue, networkToDeviceQueue, tcpSelector, VPNNetworkService.this));

                // Start the VPN Runnable thread
                vpnRunnableThread.start();

                Log.i(TAG, "Started");
            }
        }).start();
    }

    /**
     * Handles commands to start or stop the VPN service, managing foreground service state and cleanup as needed.
     *
     * @param intent  The Intent received by the service.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's current started state.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_VPN.equals(action)) {
                // If required, prepare to start in the foreground here
                startForegroundService();
                // Additional setup or state change to start the VPN
            } else if (ACTION_STOP_VPN.equals(action)) {
                stopForeground(true); // Remove foreground status
                isRunning = false;
                cleanup(); // Perform cleanup
                stopSelf(); // Stop the service
                Log.i(TAG, "VPN Service Stopped");
                stopAndProcessPackets();
            }
        }
        return START_STICKY;
    }

    /**
     * Stops the VPN service and processes the packets that were captured during the VPN session.
     * This method may also perform cleanup and post-processing of the packets.
     */
    public void stopAndProcessPackets() {

        if (vpnRunnable != null) {
            List<PacketInfo> packetStore = vpnRunnable.getPacketStore();
            for (PacketInfo packet : packetStore) {
                Log.d(TAG, "Packet: " + packet.toString());
                // TODO remove this and place somewhere more appropriate
                getGeolocationInfo(packet.getDestinationIp(), packet);
            }
        }
    }

    /**
     * Fetches geolocation information for a given IP address and updates the packet info with the retrieved location data.
     * This method performs a network request to an external geolocation API to find the location associated with the IP address.
     *
     * @param ip         The IP address for which to retrieve geolocation info.
     * @param packetInfo The PacketInfo object to be updated with the location data.
     */

    private void getGeolocationInfo(String ip, PacketInfo packetInfo) {
        String apiUrl = "https://api.ipgeolocation.io/ipgeo?apiKey=" + ip;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    String location = jsonObject.getString("country_name");
                    String organization = jsonObject.getString("organization");

                    packetInfo.setLocation(location);
                    packetInfo.setOrganization(organization);
                } else {
                    Log.e(TAG, "Geolocation API call failed or limit reached");
                    // Set default or null values if API fails
                    packetInfo.setLocation("Unknown");
                    packetInfo.setOrganization("Unknown");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching geolocation info", e);
                // Set default or null values in case of exceptions
                packetInfo.setLocation("Error");
                packetInfo.setOrganization("Error");
            } finally {
                // Always upload packet to Firebase regardless of geolocation success
                uploadPacketToFirebase(packetInfo);
            }
        });
        executor.shutdown();
    }


    /**
     * Uploads packet information to Firebase, allowing for persistent storage and analysis of the captured packets.
     *
     * @param packet The packet information to be uploaded.
     */
    private void uploadPacketToFirebase(PacketInfo packet) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users/" + userId + "/packets");

            databaseReference.push().setValue(packet)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Packet data saved successfully for user: " + userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save packet data for user: " + userId, e));
        } else {
            Log.e(TAG, "No authenticated user found. Cannot save packet data.");
        }
    }

    private String extractLocationFromResponse(String response) {
        // Implement this method to parse the response and extract location data
        // This is just a placeholder
        return "Location data extracted from API response";
    }

    /**
     * Starts the service in the foreground, showing a persistent notification to the user.
     * This is required for services that run in the foreground for an extended period.
     */
    private void startForegroundService() {
        // Create a notification channel (Oreo and higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.packet_channel_name); // The user-visible name of the channel.
            String description = getString(R.string.packet_channel_description); // The user-visible description of the channel.
            String channelId = "vpn_service_channel_id"; // The internal ID of the channel.
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Intent to launch the app when the user taps on the notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        Notification.Builder builder = new Notification.Builder(this, "vpn_service_channel_id") // Don't forget to set the channel ID for Oreo and above
                .setContentTitle(getString(R.string.notification_title)) // Title for the notification
                .setContentText(getString(R.string.notification_message)) // Message for the notification
                .setSmallIcon(R.drawable.ic_vpn) // Icon for the notification
                .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
                .setTicker(getString(R.string.ticker_text)) // Set the ticker text
                .setPriority(Notification.PRIORITY_LOW); // Set the priority of this notification

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_message))
                    .setSmallIcon(R.drawable.ic_vpn)
                    .setContentIntent(pendingIntent)
                    .setTicker(getString(R.string.ticker_text))
                    .setPriority(Notification.PRIORITY_LOW);
        }

        // Start service in the foreground
        startForeground(NOTIFICATION_ID, builder.build());
    }

    public static boolean isRunning() {
        return isRunning;
    }

    /**
     * Cleans up resources upon destruction of the service, including clearing packet queues and shutting down the executor service.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    private synchronized void cleanup() {
        if (deviceToNetworkTCPQueue != null) {
            deviceToNetworkTCPQueue.clear();
            deviceToNetworkTCPQueue = null;
        }
        if (deviceToNetworkUDPQueue != null) {
            deviceToNetworkUDPQueue.clear();
            deviceToNetworkUDPQueue = null;
        }
        if (networkToDeviceQueue != null) {
            networkToDeviceQueue.clear();
            networkToDeviceQueue = null;
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }

        // Clear any custom buffer pools or caches
        ByteBufferPool.clear();

        // Close and nullify the VPN interface
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing vpnInterface", e);
            } finally {
                vpnInterface = null; // Ensure it's nullified even if an exception occurs
            }
        }

        // Update any state variables or flags
        isRunning = false;
    }
}
