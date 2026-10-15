package lk.falconplan.app.ui.provider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ProviderRoom;

public class ProviderRoomAdapter extends RecyclerView.Adapter<ProviderRoomAdapter.RoomViewHolder> {

    private final List<ProviderRoom> items = new ArrayList<>();
    private OnRoomActionListener listener;

    public interface OnRoomActionListener {
        void onPricingClick(ProviderRoom room);
        void onEditClick(ProviderRoom room);
        void onDeleteClick(ProviderRoom room);
    }

    public void setOnRoomActionListener(OnRoomActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ProviderRoom> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        ProviderRoom item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtRoomName;
        private final TextView txtTypeBadge;
        private final TextView txtUnitsCapacity;
        private final TextView txtBedBathroomDetails;
        private final Button btnManagePricing;
        private final Button btnEditRoom;
        private final Button btnDeleteRoom;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRoomName = itemView.findViewById(R.id.txtRoomName);
            txtTypeBadge = itemView.findViewById(R.id.txtTypeBadge);
            txtUnitsCapacity = itemView.findViewById(R.id.txtUnitsCapacity);
            txtBedBathroomDetails = itemView.findViewById(R.id.txtBedBathroomDetails);
            btnManagePricing = itemView.findViewById(R.id.btnManagePricing);
            btnEditRoom = itemView.findViewById(R.id.btnEditRoom);
            btnDeleteRoom = itemView.findViewById(R.id.btnDeleteRoom);
        }

        public void bind(ProviderRoom item, OnRoomActionListener listener) {
            txtRoomName.setText(item.getRoomName() != null ? item.getRoomName() : "Untitled Room");
            txtTypeBadge.setText(item.getRoomType() != null ? item.getRoomType().toUpperCase(Locale.US) : "DOUBLE");

            txtUnitsCapacity.setText(String.format(Locale.US, "%d Physical Units • Up to %d Guests/Unit",
                    item.getTotalUnits(), item.getMaxGuestsPerUnit()));

            txtBedBathroomDetails.setText(String.format(Locale.US, "%d %s • %s Bathroom • %s",
                    item.getBedCount(), item.getBedType(),
                    item.getBathroomType() != null ? item.getBathroomType() : "Private",
                    item.getAmenities() != null ? item.getAmenities() : ""));

            btnManagePricing.setOnClickListener(v -> {
                if (listener != null) listener.onPricingClick(item);
            });

            btnEditRoom.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(item);
            });

            btnDeleteRoom.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(item);
            });
        }
    }
}