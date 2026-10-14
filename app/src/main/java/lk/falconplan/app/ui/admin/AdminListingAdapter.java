package lk.falconplan.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.falconplan.app.R;

public class AdminListingAdapter extends RecyclerView.Adapter<AdminListingAdapter.AdminListingViewHolder> {

    private final List<JSONObject> items = new ArrayList<>();
    private OnListingItemClickListener listener;

    public interface OnListingItemClickListener {
        void onItemClick(JSONObject item);
    }

    public void setOnListingItemClickListener(OnListingItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<JSONObject> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_listing_card, parent, false);
        return new AdminListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminListingViewHolder holder, int position) {
        JSONObject item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AdminListingViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtListingTitle;
        private final TextView txtStatusBadge;
        private final TextView txtTypeLocation;
        private final TextView txtProviderInfo;
        private final Button btnReviewListing;

        public AdminListingViewHolder(@NonNull View itemView) {
            super(itemView);
            txtListingTitle = itemView.findViewById(R.id.txtListingTitle);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtTypeLocation = itemView.findViewById(R.id.txtTypeLocation);
            txtProviderInfo = itemView.findViewById(R.id.txtProviderInfo);
            btnReviewListing = itemView.findViewById(R.id.btnReviewListing);
        }

        public void bind(JSONObject item, OnListingItemClickListener listener) {
            String title = item.optString("title", "Untitled Listing");
            String type = item.optString("listingType", "homestay").toUpperCase(Locale.US);
            String district = item.optString("district", "");
            String village = item.optString("village", "");
            String providerUid = item.optString("providerUid", "Unknown Provider");
            String status = item.optString("status", "submitted_under_review");

            txtListingTitle.setText(title);
            txtTypeLocation.setText(String.format("%s • %s, %s", type, district, village));
            txtProviderInfo.setText(String.format("Provider: %s", providerUid));

            if ("published".equals(status)) {
                txtStatusBadge.setText("Published");
            } else if ("rejected".equals(status)) {
                txtStatusBadge.setText("Rejected");
            } else {
                txtStatusBadge.setText("Under Review");
            }

            View.OnClickListener clickListener = v -> {
                if (listener != null) listener.onItemClick(item);
            };

            itemView.setOnClickListener(clickListener);
            btnReviewListing.setOnClickListener(clickListener);
        }
    }
}