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
import lk.falconplan.app.data.model.ProviderListing;

public class ProviderListingAdapter extends RecyclerView.Adapter<ProviderListingAdapter.ListingViewHolder> {

    private final List<ProviderListing> items = new ArrayList<>();
    private OnListingActionListener listener;

    public interface OnListingActionListener {
        void onEditClick(ProviderListing listing);
        void onDeleteClick(ProviderListing listing);
    }

    public void setOnListingActionListener(OnListingActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ProviderListing> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_listing_card, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        ProviderListing item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ListingViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtListingTitle;
        private final TextView txtStatusBadge;
        private final TextView txtTypeLocation;
        private final TextView txtDescription;
        private final Button btnEditDraft;
        private final Button btnDeleteDraft;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            txtListingTitle = itemView.findViewById(R.id.txtListingTitle);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtTypeLocation = itemView.findViewById(R.id.txtTypeLocation);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            btnEditDraft = itemView.findViewById(R.id.btnEditDraft);
            btnDeleteDraft = itemView.findViewById(R.id.btnDeleteDraft);
        }

        public void bind(ProviderListing item, OnListingActionListener listener) {
            txtListingTitle.setText(item.getTitle() != null ? item.getTitle() : "Untitled Listing");
            txtTypeLocation.setText(String.format(Locale.US, "%s • %s, %s",
                    item.getListingType() != null ? item.getListingType().toUpperCase(Locale.US) : "HOMESTAY",
                    item.getDistrict() != null ? item.getDistrict() : "",
                    item.getVillage() != null ? item.getVillage() : ""));

            txtDescription.setText(item.getDescription() != null && !item.getDescription().isEmpty()
                    ? item.getDescription() : "No description provided.");

            txtStatusBadge.setText("Draft");

            btnEditDraft.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(item);
            });

            btnDeleteDraft.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(item);
            });
        }
    }
}