package lk.falconplan.app.ui.explore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.SavedPlace;

public class SavedPlacesAdapter extends RecyclerView.Adapter<SavedPlacesAdapter.SavedPlaceViewHolder> {

    private final List<SavedPlace> places = new ArrayList<>();
    private OnSavedPlaceActionListener listener;

    public interface OnSavedPlaceActionListener {
        void onEditNoteClick(SavedPlace place);
        void onDeleteClick(SavedPlace place);
        void onViewMapClick(SavedPlace place);
        void onDirectionsClick(SavedPlace place);
    }

    public void setOnSavedPlaceActionListener(OnSavedPlaceActionListener listener) {
        this.listener = listener;
    }

    public void setPlaces(List<SavedPlace> newPlaces) {
        this.places.clear();
        if (newPlaces != null) {
            this.places.addAll(newPlaces);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SavedPlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_place, parent, false);
        return new SavedPlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedPlaceViewHolder holder, int position) {
        SavedPlace item = places.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class SavedPlaceViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtDestinationName;
        private final TextView txtDistrict;
        private final TextView txtPersonalNote;
        private final TextView btnEditNote;
        private final ImageButton btnDelete;
        private final Button btnViewMap;
        private final Button btnGetDirections;

        public SavedPlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDestinationName = itemView.findViewById(R.id.txtDestinationName);
            txtDistrict = itemView.findViewById(R.id.txtDistrict);
            txtPersonalNote = itemView.findViewById(R.id.txtPersonalNote);
            btnEditNote = itemView.findViewById(R.id.btnEditNote);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnViewMap = itemView.findViewById(R.id.btnViewMap);
            btnGetDirections = itemView.findViewById(R.id.btnGetDirections);
        }

        public void bind(SavedPlace item, OnSavedPlaceActionListener listener) {
            txtDestinationName.setText(item.getDestinationName());
            txtDistrict.setText(item.getDistrict());

            if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
                txtPersonalNote.setText("Note: " + item.getNote());
            } else {
                txtPersonalNote.setText("Tap 'Edit Note' to add personal travel notes.");
            }

            btnEditNote.setOnClickListener(v -> {
                if (listener != null) listener.onEditNoteClick(item);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(item);
            });

            btnViewMap.setOnClickListener(v -> {
                if (listener != null) listener.onViewMapClick(item);
            });

            btnGetDirections.setOnClickListener(v -> {
                if (listener != null) listener.onDirectionsClick(item);
            });
        }
    }
}