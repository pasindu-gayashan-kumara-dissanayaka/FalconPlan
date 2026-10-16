package lk.falconplan.app.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityProfile;

public class UserSummaryAdapter extends RecyclerView.Adapter<UserSummaryAdapter.UserViewHolder> {

    private final List<CommunityProfile> items = new ArrayList<>();
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(CommunityProfile profile);
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CommunityProfile> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_summary, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        CommunityProfile item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtDisplayName;
        private final Button btnViewProfile;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDisplayName = itemView.findViewById(R.id.txtDisplayName);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
        }

        public void bind(CommunityProfile item, OnUserClickListener listener) {
            txtDisplayName.setText(item.getDisplayName());

            View.OnClickListener clickListener = v -> {
                if (listener != null) listener.onUserClick(item);
            };

            itemView.setOnClickListener(clickListener);
            btnViewProfile.setOnClickListener(clickListener);
        }
    }
}