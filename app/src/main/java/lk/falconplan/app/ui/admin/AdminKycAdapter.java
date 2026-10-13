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

public class AdminKycAdapter extends RecyclerView.Adapter<AdminKycAdapter.AdminKycViewHolder> {

    private final List<JSONObject> items = new ArrayList<>();
    private OnKycItemClickListener listener;

    public interface OnKycItemClickListener {
        void onItemClick(JSONObject item);
    }

    public void setOnKycItemClickListener(OnKycItemClickListener listener) {
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
    public AdminKycViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_kyc_card, parent, false);
        return new AdminKycViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminKycViewHolder holder, int position) {
        JSONObject item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AdminKycViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtProviderUid;
        private final TextView txtStatusBadge;
        private final TextView txtDocumentCount;
        private final Button btnReviewApplication;

        public AdminKycViewHolder(@NonNull View itemView) {
            super(itemView);
            txtProviderUid = itemView.findViewById(R.id.txtProviderUid);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtDocumentCount = itemView.findViewById(R.id.txtDocumentCount);
            btnReviewApplication = itemView.findViewById(R.id.btnReviewApplication);
        }

        public void bind(JSONObject item, OnKycItemClickListener listener) {
            String uid = item.optString("providerUid", "Unknown UID");
            String status = item.optString("status", "pending_identity_verification");
            int docCount = item.optInt("documentCount", 0);

            txtProviderUid.setText(String.format("Provider: %s", uid));
            txtDocumentCount.setText(String.format(Locale.US, "%d Documents Uploaded", docCount));

            if ("submitted_under_review".equals(status)) {
                txtStatusBadge.setText("Under Review");
            } else if ("verified".equals(status)) {
                txtStatusBadge.setText("Verified");
            } else if ("rejected".equals(status)) {
                txtStatusBadge.setText("Rejected");
            } else {
                txtStatusBadge.setText("KYC Pending");
            }

            View.OnClickListener clickListener = v -> {
                if (listener != null) listener.onItemClick(item);
            };

            itemView.setOnClickListener(clickListener);
            btnReviewApplication.setOnClickListener(clickListener);
        }
    }
}