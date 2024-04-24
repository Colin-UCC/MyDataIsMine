package com.fyp.mydataismine.packetcapture;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.fyp.mydataismine.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying a stream of network packets captured by the application.
 * It uses a RecyclerView to show the packets' information in real time.
 */
public class PacketStreamActivity extends AppCompatActivity {

    private RecyclerView packetListView;
    private PacketListAdapter packetListAdapter;
    private List<PacketInfo> packetList;
    private final String ACTION_NEW_PACKET = "com.fyp.mydataismine.packetcapture.NEW_PACKET";
    private boolean isReceiverRegistered = false;

    /**
     * Initializes the activity, setting up the RecyclerView and its adapter.
     * @param savedInstanceState Contains data supplied in onSaveInstanceState(Bundle) if the activity is re-initialized.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packet_stream);

        // Initialize the packet list
        packetList = new ArrayList<>();
        packetListView = findViewById(R.id.packetListView);
        packetList = new ArrayList<>();
        packetListAdapter = new PacketListAdapter(this, packetList);
        packetListView.setLayoutManager(new LinearLayoutManager(this));
        packetListView.setAdapter(packetListAdapter);
    }

    /**
     * Handles new intents, potentially containing new packet data to display.
     * @param intent The new intent that was started for the activity.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle the intent
        if (intent.getAction() != null && intent.getAction().equals(VPNRunnable.ACTION_NEW_PACKET)) {
            String sourceIp = intent.getStringExtra("sourceIp");
            String destinationIp = intent.getStringExtra("destinationIp");
            int payloadSize = intent.getIntExtra("payloadSize", 0);
            String protocol = intent.getStringExtra("protocol");

            // Add to UI
            PacketInfo packetInfo = new PacketInfo(sourceIp, destinationIp, payloadSize, protocol);
            addPacketToUI(packetInfo);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SimpleEventBus.registerListener(this::handlePacketEvent);
    }

    @Override
    protected void onStop() {
        SimpleEventBus.unregisterListener(this::handlePacketEvent);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    /**
     * Handles the receipt of packet data events and updates the UI accordingly.
     * @param packetInfo The packet information to display in the UI.
     */
    private void handlePacketEvent(PacketInfo packetInfo) {
        runOnUiThread(() -> addPacketToUI(packetInfo));
    }


//    private BroadcastReceiver packetReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Log.d("TAG", "Received broadcast in PacketStreamActivity");
//            if (intent != null && ACTION_NEW_PACKET.equals(intent.getAction())) {
//                Log.d("TAG", "Received broadcast in PacketStreamActivity");
//                String sourceIp = intent.getStringExtra("sourceIp");
//                String destinationIp = intent.getStringExtra("destinationIp");
//                int payloadSize = intent.getIntExtra("payloadSize", 0);
//                String protocol = intent.getStringExtra("protocol");
//
//                // Create a PacketInfo object and update UI
//                PacketInfo packetInfo = new PacketInfo(sourceIp, destinationIp, payloadSize, protocol);
//                addPacketToUI(packetInfo);
//            }
//        }
//    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (isFinishing()) {
//            try {
//                unregisterReceiver(packetReceiver);
//            } catch (IllegalArgumentException e) {
//                Log.e("PacketStreamActivity", "Receiver not registered", e);
//            }
//        }
    }

    private void initRecyclerView() {
        packetListView = findViewById(R.id.packetListView);
        packetListAdapter = new PacketListAdapter(this, packetList);
        packetListView.setAdapter(packetListAdapter);
        packetListView.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Adds a new packet's information to the UI.
     * @param packetInfo The packet information to be added to the list.
     */
    public void addPacketToUI(PacketInfo packetInfo) {
        runOnUiThread(() -> {
            packetList.add(packetInfo);
            packetListAdapter.notifyDataSetChanged();
        });
    }

    private void testWithData() {
        addPacketToUI(new PacketInfo("192.168.1.1", "8.8.8.8", 60, "TCP"));
    }
}
