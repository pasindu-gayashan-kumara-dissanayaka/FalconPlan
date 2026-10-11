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
import lk.falconplan.app.data.model.Destination;

public class DestinationAdapter extends RecyclerView.Adapter<DestinationAdapter.DestinationViewHolder> {

    private final List<Destination> destinations = new ArrayList<>();
    private OnDestinationClickListener listener;

    public interface OnDestinationClickListener {
        void onDestinationClick(Destination destination);
        void onFavoriteClick(Destination destination);
    }

    public void setOnDestinationClickListener(OnDestinationClickListener listener) {
        this.listener = listener;
    }

    public void setDestinations(List<Destination> newDestinations) {
        this.destinations.clear();
        if (newDestinations != null) {
            this.destinations.addAll(newDestinations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DestinationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_destination_card, parent, false);
        return new DestinationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DestinationViewHolder holder, int position) {
        Destination item = destinations.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return destinations.size();
    }

    static class DestinationViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtName;
        private final TextView txtCategory;
        private final TextView txtDistrictProvince;
        private final TextView txtDescription;
        private final Button btnViewDetails;
        private final ImageButton btnFavorite;

        public DestinationViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtDistrictProvince = itemView.findViewById(R.id.txtDistrictProvince);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        public void bind(Destination item, OnDestinationClickListener listener) {
            txtName.setText(item.getName());
            txtCategory.setText(item.getCategory());
            txtDistrictProvince.setText(String.format("%s • %s", item.getDistrict(), item.getProvince()));
            txtDescription.setText(item.getDescription());

            View.OnClickListener clickListener = v -> {
                if (listener != null) {
                    listener.onDestinationClick(item);
                }
            };

            itemView.setOnClickListener(clickListener);
            btnViewDetails.setOnClickListener(clickListener);

            btnFavorite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClick(item);
                }
            });
        }
    }
}