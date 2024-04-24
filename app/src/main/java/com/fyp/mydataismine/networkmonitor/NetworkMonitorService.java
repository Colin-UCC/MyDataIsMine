package com.fyp.mydataismine.networkmonitor;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fyp.mydataismine.packetcapture.PacketInfo;
import com.fyp.mydataismine.packetcapture.SimpleEventBus;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service class responsible for monitoring network traffic on the device.
 * This service captures network traffic in real-time using the tcpdump utility, processes each line of the captured data,
 * and handles specific network events such as ARP, DNS failures, and normal IP traffic. The service can differentiate between
 * various protocols like TCP and UDP, extract essential details, and log or respond to different types of network activities.
 * It also supports geolocation lookup for IP addresses using an external API and uploads relevant traffic data to Firebase.
 * The service manages IPv6 addresses and can broadcast sensor usage data within the application.
 *
 * The service is designed to run in the background and can be controlled by intents to start and stop monitoring.
 */
public class NetworkMonitorService extends Service {
    private static final String TAG = "NetworkMonitorService";
    private ScheduledExecutorService scheduler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startNetworkMonitoring();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("ACTION_START".equals(action)) {
                // Start or continue monitoring
            } else if ("ACTION_STOP".equals(action)) {
                // Stop monitoring and stop the service
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void startNetworkMonitoring() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        scheduler.scheduleAtFixedRate(this::captureAndProcessNetworkTraffic, 0, 2, TimeUnit.SECONDS);
        List<String> ipv6Addresses = getIPv6Addresses();
    }

    private void captureAndProcessNetworkTraffic() {
        Process process = null;
        try {
            // Execute the tcpdump command (root permissions required)
            process = Runtime.getRuntime().exec("su -c tcpdump -c 100");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                processNetworkTrafficLine(line);
                Log.w("TCP Dump Line:", line);

            }
        } catch (Exception e) {
            Log.e(TAG, "Error capturing network traffic", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Processes a single line of network traffic data, determining whether to ignore it or to parse it
     * and log relevant information or handle the data further.
     *
     * @param line the string line of network traffic data to be processed.
     */
    private void processNetworkTrafficLine(String line) {
        try {

            // Skip lines containing "MyRouter"
            if (line.contains("MyRouter")) {
                Log.d(TAG, "MyRouter packet ignored.");
                return;
            }
            String[] parts = line.split("\\s+");

            if (parts.length >= 2 && "ARP,".equals(parts[1])) {
                // Ignore ARP packets
                Log.d(TAG, "ARP packet ignored: " + line);
                return;
            }

            // Check for DNS service failure (ServFail)
            if (line.contains("ServFail")) {
                Log.d(TAG, "DNS service failure packet ignored.");
                return; // Ignore this line and exit the method
            }

            if (parts.length >= 5) {
                String fullTime = parts[0];  // The first part is the timestamp
                String type = parts[1];  // The second part indicates the type (IP or IP6)
                String source = parts[2];  // The third part is the source IP and port
                String destination = parts[4];  // The fifth part is the destination IP and port
                //String length = parts[parts.length - 1];
                String protocol = (line.contains("UDP") ? "UDP" : (line.contains("TCP") ? "TCP" : ""));
                String lengthStr = parts[parts.length - 1].replaceAll("\\D+", "");
                int length = lengthStr.isEmpty() ? 0 : Integer.parseInt(lengthStr);
                long timestamp = System.currentTimeMillis();
                Date date = new Date(timestamp);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = formatter.format(date);

                String time = fullTime.split("\\.")[0];

                String sourceIp, sourcePort = "";
                String destinationIp, destinationPort = "";

                if (source.contains(".")) {
                    sourceIp = source.substring(0, source.lastIndexOf('.'));
                    sourcePort = source.substring(source.lastIndexOf('.') + 1);
                } else {
                    sourceIp = source;
                }

                if (destination.contains(".")) {
                    destinationIp = destination.substring(0, destination.lastIndexOf('.'));
                    destinationPort = destination.substring(destination.lastIndexOf('.') + 1);
                } else {
                    destinationIp = destination;
                }

                // Trim the colon from the destination port if necessary
                destinationPort = destinationPort.replace(":", "");

                String deviceIp = "192.168.1.28";

                //Log.w(TAG, (Throwable) getIPv6Addresses());
                //List<String> ipv6Addresses = getIPv6Addresses();

                PacketInfo newPacket = new PacketInfo(sourceIp, destinationIp, length, protocol);
                SimpleEventBus.postPacket(newPacket);

                int baseHeaderSize = 14 + 20; // Ethernet + IPv4 common header sizes
                int protocolHeaderSize = protocol.equals("TCP") ? 20 : 8; // TCP or UDP header size
                int totalHeaderSize = baseHeaderSize + protocolHeaderSize;
                int minPayloadSize = 1; // Define as needed to exclude empty payload packets
                int totalSizeThreshold = totalHeaderSize + minPayloadSize;

                if (source.startsWith(deviceIp) && length > totalSizeThreshold) {

                //if (source.startsWith(deviceIp) && (length > 34) ) {//||
                      //  (ipv6Addresses.contains(source) && (length > 34))) {

                    NetworkTrafficData trafficData = new NetworkTrafficData(
                            sourceIp,
                            destinationIp,
                            protocol,
                            length,
                            formattedDate
                    );

                    if (source.contains(".")) {
                        String[] sourceParts = source.split("\\.");
                        //sourceIp = sourceParts[0];
                        sourcePort = sourceParts[1].matches("\\d+") ? sourceParts[1] : "443";  // Default to 443 for HTTPS if not numeric
                    }

                    if (destination.contains(".")) {
                        String[] destinationParts = destination.split("\\.");
                        //destinationIp = destinationParts[0];
                        destinationPort = destinationParts[1].matches("\\d+") ? destinationParts[1] : "443";  // Default to 443 for HTTPS if not numeric
                    }

                    trafficData.setSourcePort(Integer.parseInt(sourcePort));
                    trafficData.setDestinationPort(Integer.parseInt(destinationPort));
                    //PacketInfo newPacket = new PacketInfo(sourceIp, destinationIp, length, protocol);

                    processAndStoreTrafficData(trafficData, processedTrafficData -> {
                        //SimpleEventBus.postPacket(newPacket);
                        Log.d(TAG, "Processed Traffic Data: " + processedTrafficData);
                   });
                }

                // Log for verification
                Log.d(TAG, "Time: " + time + ", Type: " + type +
                        ", Protocol: " + protocol +
                        ", Source IP: " + sourceIp + ", Source Port: " + sourcePort +
                        ", Destination IP: " + destinationIp + ", Destination Port: " + destinationPort +
                        ", Length: " + length);
            } else {
                Log.w(TAG, "Line does not have enough parts to process: " + line);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing network traffic line: " + line, e);
        }
    }

    /**
     * Processes network traffic data asynchronously, performs geolocation lookup for IP addresses,
     * and uploads the results to a Firebase database.
     *
     * @param trafficData the network traffic data to be processed and stored.
     * @param processor a functional interface callback for processing the traffic data after lookup.
     */
    private void processAndStoreTrafficData(NetworkTrafficData trafficData, TrafficDataProcessor processor) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String apiUrl = "https://api.ipgeolocation.io/ipgeo?apiKey=" + trafficData.getDestinationIp();
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

                        trafficData.setLocation(location);
                        trafficData.setOrganization(organization);
                    } else {
                        // Set default or null values if API fails
                        Log.e(TAG, "Geolocation API call failed or limit reached");
                        trafficData.setLocation("Unknown");
                        trafficData.setOrganization("Unknown");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching geolocation data", e);
                    // Set default or null values in case of exceptions
                    trafficData.setLocation("Error");
                    trafficData.setOrganization("Error");
                }
            } catch (Exception e) {
                Log.e(TAG, "Network or other error", e);
            } finally {
                // Always process and upload the traffic data, even if the geolocation fails
                processor.process(trafficData);
                uploadPacketToFirebase(trafficData);
            }
        });
        executor.shutdown();
    }
    /**
     * Uploads network traffic data to Firebase under the authenticated user's unique path.
     *
     * @param trafficData the network traffic data to upload.
     */
    private void uploadPacketToFirebase(NetworkTrafficData trafficData) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users/" + userId + "/tcp_dump_packets");

            databaseReference.push().setValue(trafficData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Network traffic data saved successfully for user: " + userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save network traffic data for user: " + userId, e));
        } else {
            Log.e(TAG, "No authenticated user found. Cannot save network traffic data.");
        }
    }
    /**
     * Retrieves all IPv6 addresses associated with network interfaces on the device, excluding
     * link-local and site-local addresses.
     *
     * @return a list of global IPv6 addresses, or a single-element list containing "Not found" if none.
     */
    public List<String> getIPv6Addresses() {
        List<String> ipv6Addresses = new ArrayList<>();
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : networkInterfaces) {
                List<InetAddress> inetAddresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress inetAddress : inetAddresses) {
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet6Address) {
                        Inet6Address ipv6Address = (Inet6Address) inetAddress;
                        // Check if the address is a valid global IPv6 address and does not start with "fe80"
                        if (!ipv6Address.isLinkLocalAddress() && !ipv6Address.isSiteLocalAddress() &&
                                !ipv6Address.getHostAddress().startsWith("fe80")) {
                            ipv6Addresses.add(ipv6Address.getHostAddress());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ipv6Addresses.isEmpty() ? Collections.singletonList("Not found") : ipv6Addresses;
    }

    private void broadcastSensorUsage(String sensorName, int count) {
        Intent intent = new Intent("com.fyp.mydataismine.SENSOR_USAGE");
        intent.putExtra("sensor_name", sensorName);
        intent.putExtra("count", count);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
