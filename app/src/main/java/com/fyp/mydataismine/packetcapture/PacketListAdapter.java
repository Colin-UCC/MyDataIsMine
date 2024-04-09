package com.fyp.mydataismine.packetcapture;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.fyp.mydataismine.R;

import java.util.List;

/**
 * Adapter for displaying packet information in a RecyclerView.
 * Manages the data model and binds packet data to the view.
 */
public class PacketListAdapter extends RecyclerView.Adapter<PacketListAdapter.PacketViewHolder> {
    private Context context;
    private List<PacketInfo> packetList;

    /**
     * Constructor for PacketListAdapter.
     *
     * @param context    The current context.
     * @param packetList The list of packets to be displayed.
     */
    public PacketListAdapter(Context context, List<PacketInfo> packetList) {
        this.context = context;
        this.packetList = packetList;
    }

    /**
     * Returns the size of the packet list.
     *
     * @return The number of packets in the list.
     */
    @Override
    public int getItemCount() {
        return packetList.size();
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @Override
    public PacketViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item, parent, false);
        return new PacketViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(PacketViewHolder holder, int position) {
        PacketInfo packetInfo = packetList.get(position);
        holder.bind(packetInfo);
    }

    /**
     * ViewHolder class for packet items in the RecyclerView.
     */
    public static class PacketViewHolder extends RecyclerView.ViewHolder {
        public TextView sourceIpTextView;
        public TextView destinationIpTextView;
        public TextView protocolTextView;
        public TextView sizeTextView;
        public TextView timeStampTextView;

        public PacketViewHolder(View itemView) {
            super(itemView);
            sourceIpTextView = itemView.findViewById(R.id.sourceIpTextView);
            destinationIpTextView = itemView.findViewById(R.id.destinationIpTextView);
            protocolTextView = itemView.findViewById(R.id.protocolTextView);
            sizeTextView = itemView.findViewById(R.id.sizeTextView);
            timeStampTextView = itemView.findViewById(R.id.timeStampTextView);
        }

        /**
         * Binds a packet's information to the view.
         *
         * @param packet The packet info to bind to the view.
         */
        public void bind(PacketInfo packet) {
            sourceIpTextView.setText("Source IP: " + packet.getSourceIp());
            destinationIpTextView.setText("Destination IP: " + packet.getDestinationIp());
            protocolTextView.setText("Protocol: " + packet.getProtocol());
            sizeTextView.setText("Packet Size: " + packet.getPayloadSize() + " bytes");
            timeStampTextView.setText("Timestamp: " + packet.getTimestamp());
            //Log.d("Bind", "packet binding here");
        }
    }
}

