package lk.falconplan.app.ui.community;

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
import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityPost;

public class CommunityPostAdapter extends RecyclerView.Adapter<CommunityPostAdapter.PostViewHolder> {

    private final List<CommunityPost> items = new ArrayList<>();
    private OnPostActionListener listener;

    public interface OnPostActionListener {
        void onLikeClick(CommunityPost post);
        void onCommentClick(CommunityPost post);
        void onReportClick(CommunityPost post);
        void onItemClick(CommunityPost post);
    }

    public void setOnPostActionListener(OnPostActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CommunityPost> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setPosts(List<CommunityPost> posts) {
        setItems(posts);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        CommunityPost item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtAuthorName;
        private final TextView txtStatusBadge;
        private final TextView txtCaption;
        private final ImageButton btnLikePost;
        private final TextView txtLikeCount;
        private final ImageButton btnCommentPost;
        private final TextView txtCommentCount;
        private final Button btnReportPost;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAuthorName = itemView.findViewById(R.id.txtAuthorName);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtCaption = itemView.findViewById(R.id.txtCaption);
            btnLikePost = itemView.findViewById(R.id.btnLikePost);
            txtLikeCount = itemView.findViewById(R.id.txtLikeCount);
            btnCommentPost = itemView.findViewById(R.id.btnCommentPost);
            txtCommentCount = itemView.findViewById(R.id.txtCommentCount);
            btnReportPost = itemView.findViewById(R.id.btnReportPost);
        }

        public void bind(CommunityPost item, OnPostActionListener listener) {
            txtAuthorName.setText(item.getAuthorDisplayName());
            txtStatusBadge.setText(item.getStatus().toUpperCase(Locale.US));
            txtCaption.setText(item.getCaption());
            txtLikeCount.setText(String.format(Locale.US, "%d Likes", item.getLikeCount()));
            txtCommentCount.setText(String.format(Locale.US, "%d Comments", item.getCommentCount()));

            if (item.isLikedByMe()) {
                btnLikePost.setColorFilter(itemView.getContext().getColor(R.color.accent_orange));
            } else {
                btnLikePost.setColorFilter(itemView.getContext().getColor(R.color.text_secondary));
            }

            btnLikePost.setOnClickListener(v -> {
                if (listener != null) listener.onLikeClick(item);
            });

            View.OnClickListener commentClickListener = v -> {
                if (listener != null) listener.onCommentClick(item);
            };
            btnCommentPost.setOnClickListener(commentClickListener);
            txtCommentCount.setOnClickListener(commentClickListener);

            btnReportPost.setOnClickListener(v -> {
                if (listener != null) listener.onReportClick(item);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}