package lk.falconplan.app.ui.provider;

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

import lk.falconplan.app.R;

public class ListingPhotoAdapter extends RecyclerView.Adapter<ListingPhotoAdapter.PhotoViewHolder> {

    private final List<JSONObject> items = new ArrayList<>();
    private OnPhotoActionListener listener;

    public interface OnPhotoActionListener {
        void onSetCoverClick(JSONObject photo);
        void onDeleteClick(JSONObject photo);
    }

    public void setOnPhotoActionListener(OnPhotoActionListener listener) {
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
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listing_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        JSONObject item = items.get(position);
        holder.bind(item, position, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtPhotoTitle;
        private final TextView txtCoverBadge;
        private final Button btnSetCover;
        private final Button btnDeletePhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPhotoTitle = itemView.findViewById(R.id.txtPhotoTitle);
            txtCoverBadge = itemView.findViewById(R.id.txtCoverBadge);
            btnSetCover = itemView.findViewById(R.id.btnSetCover);
            btnDeletePhoto = itemView.findViewById(R.id.btnDeletePhoto);
        }

        public void bind(JSONObject item, int position, OnPhotoActionListener listener) {
            boolean isCover = item.optBoolean("isCover", false);
            txtPhotoTitle.setText(String.format("Photo #%d", position + 1));

            if (isCover) {
                txtCoverBadge.setVisibility(View.VISIBLE);
                btnSetCover.setVisibility(View.GONE);
            } else {
                txtCoverBadge.setVisibility(View.GONE);
                btnSetCover.setVisibility(View.VISIBLE);
            }

            btnSetCover.setOnClickListener(v -> {
                if (listener != null) listener.onSetCoverClick(item);
            });

            btnDeletePhoto.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(item);
            });
        }
    }
}