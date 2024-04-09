package com.fyp.mydataismine.packetcapture;

import android.content.Context;
import android.net.VpnService;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages the VPN service's network traffic, capturing, processing, and routing packets.
 * This class reads packets from the VPN interface, processes them based on protocol type,
 * and handles their forwarding or analysis.
 */
public class VPNRunnable extends VpnService implements Runnable  {
    private static final String TAG = "VPNRunnable";
    private static final int MAX_PACKET_SIZE = 1500;
    public static final String ACTION_NEW_PACKET = "com.fyp.mydataismine.NEW_PACKET";
    private FileDescriptor vpnFileDescriptor;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private PacketDbHelper dbHelper;
    private Context context;
    private VPNNetworkService vpnService;
    private DatabaseReference mDatabase;
    private List<PacketInfo> packetStore = new ArrayList<>();

    /**
     * Constructs a VPNRunnable with the necessary network queues and file descriptor for the VPN interface.
     * @param vpnFileDescriptor The file descriptor of the VPN interface.
     * @param deviceToNetworkUDPQueue Queue for UDP packets from the device to the network.
     * @param deviceToNetworkTCPQueue Queue for TCP packets from the device to the network.
     * @param networkToDeviceQueue Queue for packets from the network to the device.
     * @param vpnService The VPN service instance that created this runnable.
     * @param context The application context.
     */
    public VPNRunnable(FileDescriptor vpnFileDescriptor,
                       ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue,
                       ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue,
                       ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue,
                       VPNNetworkService vpnService,
                       Context context
    ) {
        this.vpnFileDescriptor = vpnFileDescriptor;
        this.deviceToNetworkUDPQueue = deviceToNetworkUDPQueue;
        this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
        this.networkToDeviceQueue = networkToDeviceQueue;
        this.dbHelper = new PacketDbHelper(context); // Initialize dbHelper with the context
        this.vpnService = vpnService;
        this.context = context;
    }

    public List<PacketInfo> getPacketStore() {
        return new ArrayList<>(packetStore); // Return a copy of the packetStore to avoid concurrency issues
    }

    /**
     * The main run method that performs packet capture and processing.
     */
    @Override
    public void run() {
        Log.i(TAG, "VPN Runnable Started");

        FileChannel vpnInput = new FileInputStream(vpnFileDescriptor).getChannel();
        FileChannel vpnOutput = new FileOutputStream(vpnFileDescriptor).getChannel();

        try {
            ByteBuffer bufferToNetwork = ByteBuffer.allocate(MAX_PACKET_SIZE);
            boolean dataSent;
            boolean dataReceived;

            while (!Thread.interrupted()) {
                bufferToNetwork.clear();
                int readBytes = vpnInput.read(bufferToNetwork);

                if (readBytes > 0) {
                    dataSent = true;
                    bufferToNetwork.flip();

                    // Log raw packet data
                    byte[] raw = new byte[bufferToNetwork.remaining()];
                    bufferToNetwork.mark();  // Mark the current position
                    bufferToNetwork.get(raw);  // Read the data into the raw array
                    bufferToNetwork.reset(); // Reset to the marked position
                    Log.d(TAG, "Raw packet data: " + Arrays.toString(raw));

                    Packet packet = new Packet(bufferToNetwork);

                    if (packet.getIp4Header() != null) {
                        // IPv4 processing logic
                        handleIPv4Packet(packet);
                        queuePacket(packet);
                    } else if (packet.isIPv6()) {
                        // IPv6 processing logic
                        handleIPv6Packet(packet);
                    } else {
                        Log.w(TAG, "Unsupported IP version or unknown packet type.");
                        dataSent = false;
                    }
                } else {
                    dataSent = false;
                }

                bufferFromNetworkOperations(vpnOutput);
            }
        } catch (IOException e) {
            Log.w(TAG, e.toString(), e);
        } finally {
            closeResources(vpnInput, vpnOutput);
        }
    }

    /**
     * Processes IPv4 packets, extracting and logging information, and handling different protocols.
     *
     * @param packet The packet to be processed.
     */
    private void handleIPv4Packet(Packet packet) {

        IP4Header ip4Header = packet.getIp4Header();
        String sourceIp = ip4Header.sourceAddress.getHostAddress();
        String destinationIp = ip4Header.destinationAddress.getHostAddress();
        int payloadSize = packet.calculatePayloadSize();

        // Log basic packet information
        Log.d(TAG, "Received IPv4 Packet: " + packet);
        Log.d(TAG, "Source IP: " + sourceIp + ", Destination IP: " + destinationIp + ", Payload Size: " + payloadSize);

        // Determine the protocol (TCP, UDP, etc.) and process accordingly
        switch (ip4Header.protocol) {
            case TCP:
                handleTCPPacket(packet, sourceIp, destinationIp, payloadSize);
                break;
            case UDP:
                handleUDPPacket(packet, sourceIp, destinationIp, payloadSize);
                break;
            default:
                Log.w(TAG, "Unsupported IPv4 protocol: " + ip4Header.protocol);
                break;
        }
    }

    /**
     * Handles TCP packets, logging details and potentially modifying or analyzing the packet.
     *
     * @param packet The packet to handle.
     * @param sourceIp The source IP address of the packet.
     * @param destinationIp The destination IP address of the packet.
     * @param payloadSize The size of the packet's payload.
     */
    private void handleTCPPacket(Packet packet, String sourceIp, String destinationIp, int payloadSize) {
        TCPHeader tcpHeader = packet.getTcpHeader();
        if (tcpHeader != null) {
            int sourcePort = tcpHeader.sourcePort;
            int destinationPort = tcpHeader.destinationPort;
            Log.d(TAG, "TCP Packet: Source Port: " + sourcePort + ", Destination Port: " + destinationPort + ", Payload Size: " + payloadSize);

            // TODO Additional TCP packet processing logic
            broadcastPacketData(sourceIp, destinationIp, payloadSize, "TCP" );
            if (payloadSize > 0) {
                PacketInfo packetInfo = new PacketInfo(sourceIp, destinationIp, payloadSize, "TCP" );
                packetStore.add(packetInfo);
            }
        }
    }

    /**
     * Handles UDP packets, logging details and performing additional processing as needed.
     *
     * @param packet The packet to handle.
     * @param sourceIp The source IP address of the packet.
     * @param destinationIp The destination IP address of the packet.
     * @param payloadSize The size of the packet's payload.
     */
    private void handleUDPPacket(Packet packet, String sourceIp, String destinationIp, int payloadSize) {
        UDPHeader udpHeader = packet.getUdpHeader();
        if (udpHeader != null) {
            int sourcePort = udpHeader.sourcePort;
            int destinationPort = udpHeader.destinationPort;
            Log.d(TAG, "UDP Packet: Source Port: " + sourcePort + ", Destination Port: " + destinationPort + ", Payload Size: " + payloadSize);

            // TODO Additional UDP packet processing logic here
            broadcastPacketData(sourceIp, destinationIp, payloadSize, "UDP" );
            if (payloadSize > 0) {
                PacketInfo packetInfo = new PacketInfo(sourceIp, destinationIp, payloadSize, "UDP");
                packetStore.add(packetInfo);
            }
        }
    }

    /**
     * Processes IPv6 packets, handling different protocols and extracting relevant information.
     *
     * @param packet The packet to be processed.
     */
    private void handleIPv6Packet(Packet packet) {
        IP6Header ip6Header = packet.getIp6Header();
        if (ip6Header != null) {
            String sourceIp = ip6Header.getSourceAddress().getHostAddress();
            String destinationIp = ip6Header.getDestinationAddress().getHostAddress();
            int payloadLength = ip6Header.getPayloadLength();
            int nextHeader = ip6Header.getNextHeader();
            int hopLimit = ip6Header.getHopLimit();

            Log.d(TAG, "IPv6 Packet: Source IP: " + sourceIp + ", Destination IP: " + destinationIp +
                    ", Payload Length: " + payloadLength + ", Next Header: " + nextHeader +
                    ", Hop Limit: " + hopLimit);

            switch (nextHeader) {
                case 6: // TCP
                    handleTCPPacket(packet, sourceIp, destinationIp, payloadLength);
                    break;
                case 17: // UDP
                    handleUDPPacket(packet, sourceIp, destinationIp, payloadLength);
                    break;
                case 58: // ICMPv6
                    handleICMPv6Packet(packet, sourceIp, destinationIp, payloadLength);
                    break;
                default:
                    Log.d(TAG, "Unsupported IPv6 Next Header: " + nextHeader);
                    break;
            }

            // TODO: IPv6 specific processing
        }
    }

    /**
     * Processes ICMPv6 packets, extracting type, code, and other details for logging or analysis.
     *
     * @param packet The packet to be processed.
     * @param sourceIp The source IP address of the packet.
     * @param destinationIp The destination IP address of the packet.
     * @param payloadSize The size of the packet's payload.
     */
    private void handleICMPv6Packet(Packet packet, String sourceIp, String destinationIp, int payloadSize) {
        ICMPv6Header icmpv6Header = packet.getIcmpv6Header();
        if (icmpv6Header != null) {
            int type = icmpv6Header.getType();
            int code = icmpv6Header.getCode();
            Log.d(TAG, "ICMPv6 Packet: Type: " + type + ", Code: " + code + ", Payload Size: " + payloadSize);

            // TODO: Additional ICMPv6 packet processing logic here
            broadcastPacketData(sourceIp, destinationIp, payloadSize, "ICMP");
        }
    }

    /**
     * Adds a packet to the appropriate queue for network transmission based on its protocol.
     *
     * @param packet The packet to queue.
     */
    private void queuePacket(Packet packet) {
        if (packet.isUDP()) {
            deviceToNetworkUDPQueue.offer(packet);
        } else if (packet.isTCP()) {
            deviceToNetworkTCPQueue.offer(packet);
        }
    }

    /**
     * Manages the transfer of packets from the network to the device, writing to the VPN interface.
     *
     * @param vpnOutput The channel to write packets to the VPN interface.
     * @throws IOException If an I/O error occurs.
     */
    private void bufferFromNetworkOperations(FileChannel vpnOutput) throws IOException {
        boolean dataReceived;
        ByteBuffer bufferFromNetwork = networkToDeviceQueue.poll();
        if (bufferFromNetwork != null) {
            bufferFromNetwork.flip();
            while (bufferFromNetwork.hasRemaining()) {
                vpnOutput.write(bufferFromNetwork);
            }
            dataReceived = true;
            ByteBufferPool.release(bufferFromNetwork);
        } else {
            dataReceived = false;
        }

        if (!dataReceived) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Closes all provided Closeable resources, handling any IOExceptions that occur.
     *
     * @param resources The resources to close.
     */
    private void closeResources(Closeable... resources) {
        for (Closeable resource : resources) {
            try {
                if (resource != null) {
                    resource.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing resource", e);
            }
        }
    }

    /**
     * Handles DNS packets, forwarding them to a specified DNS server and relaying responses.
     *
     * @param packet The DNS packet to handle.
     * @param vpnOutput The channel to write responses to the VPN interface.
     * @throws IOException If an I/O error occurs.
     */
    private void handleDnsPacket(Packet packet, FileChannel vpnOutput) throws IOException {
        DatagramChannel dnsChannel = DatagramChannel.open();
        try {
            // Assuming DNS server address and port are already defined
            InetAddress dnsServerAddress = InetAddress.getByName("8.8.8.8");
            int dnsServerPort = 53;

            // Extract the DNS query from the packet
            ByteBuffer dnsQuery = packet.getPayload();

            // Send the DNS query to the DNS server
            dnsChannel.connect(new InetSocketAddress(dnsServerAddress, dnsServerPort));
            dnsChannel.write(dnsQuery);

            // Prepare to receive the DNS response
            ByteBuffer dnsResponse = ByteBuffer.allocate(1024); // Allocate more if needed
            dnsChannel.read(dnsResponse);

            // Forward the DNS response back to the client through the VPN
            dnsResponse.flip(); // Switch from reading to writing mode

            // Writing the DNS response back to the VPN output, not directly to the client
            // Need to ensure it is routed correctly, e.g., wrapped in the correct IP/UDP headers
            vpnOutput.write(dnsResponse);
        } finally {
            dnsChannel.close();
        }
    }

    /**
     * Forwards a packet directly to the VPN interface.
     *
     * @param packet The packet to forward.
     * @param vpnOutput The channel to write the packet to the VPN interface.
     * @throws IOException If an I/O error occurs.
     */
    private void forwardPacket(Packet packet, FileChannel vpnOutput) throws IOException {
        // Forward the packet to the appropriate destination
        ByteBuffer packetBuffer = packet.getBackingBuffer();
        vpnOutput.write(packetBuffer);
    }

    /**
     * Logs or stores packet data for analysis or debugging purposes.
     *
     * @param sourceIp The source IP address of the packet.
     * @param destinationIp The destination IP address of the packet.
     * @param payloadSize The size of the packet's payload.
     * @param protocol The protocol of the packet.
     */
    private void logOrStorePacketData(String sourceIp, String destinationIp, int payloadSize, String protocol) {
        // Example logging implementation
        Log.d(TAG, "Packet [Protocol: " + protocol + ", Source: " + sourceIp + ", Destination: " + destinationIp + ", Size: " + payloadSize + " bytes]");
        // Extend this method to store the data as needed for your analysis

        dbHelper.insertPacketData(sourceIp, destinationIp, payloadSize, protocol);
    }

    /**
     * Sets the context for this runnable, allowing access to application-specific resources.
     *
     * @param context The new context to set.
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Broadcasts packet data using a simple event bus, notifying listeners of new packets.
     *
     * @param sourceIp The source IP address of the packet.
     * @param destinationIp The destination IP address of the packet.
     * @param payloadSize The size of the packet's payload.
     * @param protocol The protocol of the packet.
     */
    private void broadcastPacketData(String sourceIp, String destinationIp, int payloadSize, String protocol) {
        PacketInfo packetInfo = new PacketInfo(sourceIp, destinationIp, payloadSize, protocol);
        //Log.d(TAG, "DESTINATION: " + destinationIp);
        SimpleEventBus.postPacket(packetInfo);

        // Call the method to get geolocation information
        //getGeolocationInfo(destinationIp);
    }

    /**
     * Fetches geolocation information for a given IP address using an external API.
     *
     * @param ip The IP address to fetch geolocation information for.
     */
    private void getGeolocationInfo(String ip) {
        // Define the API endpoint URL with the IP address
        String apiUrl = "https://api.ipgeolocation.io/ipgeo?apiKey=cb8e51e6c0f1403891496151485de117&ip=" + ip;

        // Create a new thread or use an AsyncTask to make the network request
        new Thread(() -> {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Log the response or process it as needed
                Log.d(TAG, "Geolocation info: " + response.toString());

                // Here you can parse the response and extract the geolocation data
                // and then use it as needed in your application

            } catch (Exception e) {
                Log.e(TAG, "Error fetching geolocation info", e);
            }
        }).start();
    }
}

