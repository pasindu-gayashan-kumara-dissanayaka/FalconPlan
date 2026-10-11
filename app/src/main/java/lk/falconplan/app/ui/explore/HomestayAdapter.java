package lk.falconplan.app.ui.explore;

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
import lk.falconplan.app.data.model.HomestayListing;

public class HomestayAdapter extends RecyclerView.Adapter<HomestayAdapter.ListingViewHolder> {

    private final List<HomestayListing> listings = new ArrayList<>();
    private OnListingClickListener listener;

    public interface OnListingClickListener {
        void onListingClick(HomestayListing listing);
    }

    public void setOnListingClickListener(OnListingClickListener listener) {
        this.listener = listener;
    }

    public void setListings(List<HomestayListing> newListings) {
        this.listings.clear();
        if (newListings != null) {
            this.listings.addAll(newListings);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_homestay_card, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        HomestayListing item = listings.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    static class ListingViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtCategory;
        private final TextView txtPrice;
        private final TextView txtTitle;
        private final TextView txtRating;
        private final TextView txtLocation;
        private final TextView txtHost;
        private final TextView txtDescription;
        private final Button btnViewDetails;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtRating = itemView.findViewById(R.id.txtRating);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            txtHost = itemView.findViewById(R.id.txtHost);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }

        public void bind(HomestayListing item, OnListingClickListener listener) {
            txtCategory.setText(item.getRegionCategory());
            txtPrice.setText(String.format(Locale.getDefault(), "LKR %,.0f / night", item.getPriceLkrPerNight()));
            txtTitle.setText(item.getTitle());
            txtRating.setText(String.format(Locale.getDefault(), "%.1f (%d)", item.getRating(), item.getReviewCount()));
            txtLocation.setText(item.getLocationName());
            txtHost.setText(String.format("Host: %s • %s", item.getHostName(), item.getHostRole()));
            txtDescription.setText(item.getDescription());

            View.OnClickListener clickListener = v -> {
                if (listener != null) {
                    listener.onListingClick(item);
                }
            };

            itemView.setOnClickListener(clickListener);
            btnViewDetails.setOnClickListener(clickListener);
        }
    }
}