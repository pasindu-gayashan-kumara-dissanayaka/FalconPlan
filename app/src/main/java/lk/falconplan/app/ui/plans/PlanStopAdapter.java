package lk.falconplan.app.ui.plans;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.TravelPlanStop;

public class PlanStopAdapter extends RecyclerView.Adapter<PlanStopAdapter.StopViewHolder> {

    private final List<TravelPlanStop> stops = new ArrayList<>();
    private OnStopActionListener listener;

    public interface OnStopActionListener {
        void onEditNoteClick(TravelPlanStop stop);
        void onRemoveClick(TravelPlanStop stop);
        void onMoveUpClick(TravelPlanStop stop, int position);
        void onMoveDownClick(TravelPlanStop stop, int position);
        void onViewMapClick(TravelPlanStop stop);
        void onDirectionsClick(TravelPlanStop stop);
    }

    public void setOnStopActionListener(OnStopActionListener listener) {
        this.listener = listener;
    }

    public void setStops(List<TravelPlanStop> newStops) {
        this.stops.clear();
        if (newStops != null) {
            this.stops.addAll(newStops);
        }
        notifyDataSetChanged();
    }

    public List<TravelPlanStop> getStops() {
        return stops;
    }

    @NonNull
    @Override
    public StopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plan_stop_card, parent, false);
        return new StopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StopViewHolder holder, int position) {
        TravelPlanStop item = stops.get(position);
        holder.bind(item, position, stops.size(), listener);
    }

    @Override
    public int getItemCount() {
        return stops.size();
    }

    static class StopViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtOrderIndex;
        private final TextView txtDestinationName;
        private final TextView txtDistrict;
        private final TextView txtVisitDate;
        private final TextView txtNotes;
        private final TextView btnEditNote;
        private final ImageButton btnMoveUp;
        private final ImageButton btnMoveDown;
        private final ImageButton btnRemoveStop;
        private final Button btnViewMap;
        private final Button btnGetDirections;

        public StopViewHolder(@NonNull View itemView) {
            super(itemView);
            txtOrderIndex = itemView.findViewById(R.id.txtOrderIndex);
            txtDestinationName = itemView.findViewById(R.id.txtDestinationName);
            txtDistrict = itemView.findViewById(R.id.txtDistrict);
            txtVisitDate = itemView.findViewById(R.id.txtVisitDate);
            txtNotes = itemView.findViewById(R.id.txtNotes);
            btnEditNote = itemView.findViewById(R.id.btnEditNote);
            btnMoveUp = itemView.findViewById(R.id.btnMoveUp);
            btnMoveDown = itemView.findViewById(R.id.btnMoveDown);
            btnRemoveStop = itemView.findViewById(R.id.btnRemoveStop);
            btnViewMap = itemView.findViewById(R.id.btnViewMap);
            btnGetDirections = itemView.findViewById(R.id.btnGetDirections);
        }

        public void bind(TravelPlanStop item, int position, int totalCount, OnStopActionListener listener) {
            txtOrderIndex.setText(String.valueOf(position + 1));
            txtDestinationName.setText(item.getDestinationName());
            txtDistrict.setText(item.getDistrict());

            if (item.getVisitDate() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                txtVisitDate.setText("Visit Date: " + sdf.format(new Date(item.getVisitDate())));
                txtVisitDate.setVisibility(View.VISIBLE);
            } else {
                txtVisitDate.setVisibility(View.GONE);
            }

            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                txtNotes.setText("Note: " + item.getNotes());
            } else {
                txtNotes.setText("No notes added yet.");
            }

            btnMoveUp.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE);
            btnMoveDown.setVisibility(position < totalCount - 1 ? View.VISIBLE : View.INVISIBLE);

            btnEditNote.setOnClickListener(v -> { if (listener != null) listener.onEditNoteClick(item); });
            btnRemoveStop.setOnClickListener(v -> { if (listener != null) listener.onRemoveClick(item); });
            btnMoveUp.setOnClickListener(v -> { if (listener != null) listener.onMoveUpClick(item, position); });
            btnMoveDown.setOnClickListener(v -> { if (listener != null) listener.onMoveDownClick(item, position); });
            btnViewMap.setOnClickListener(v -> { if (listener != null) listener.onViewMapClick(item); });
            btnGetDirections.setOnClickListener(v -> { if (listener != null) listener.onDirectionsClick(item); });
        }
    }
}