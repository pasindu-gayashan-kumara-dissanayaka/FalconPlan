package lk.falconplan.app.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityPost;

public class CommunityPostAdapter extends RecyclerView.Adapter<CommunityPostAdapter.PostViewHolder> {

    private final List<CommunityPost> posts = new ArrayList<>();
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(CommunityPost post);
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }

    public void setPosts(List<CommunityPost> newPosts) {
        this.posts.clear();
        if (newPosts != null) {
            this.posts.addAll(newPosts);
        }
        notifyDataSetChanged();
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
        CommunityPost post = posts.get(position);
        holder.bind(post, listener);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtAuthor;
        private final TextView txtRoleLocation;
        private final TextView txtTimeAgo;
        private final TextView txtTitle;
        private final TextView txtContent;
        private final TextView txtLikes;
        private final TextView txtComments;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAuthor = itemView.findViewById(R.id.txtAuthor);
            txtRoleLocation = itemView.findViewById(R.id.txtRoleLocation);
            txtTimeAgo = itemView.findViewById(R.id.txtTimeAgo);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtContent = itemView.findViewById(R.id.txtContent);
            txtLikes = itemView.findViewById(R.id.txtLikes);
            txtComments = itemView.findViewById(R.id.txtComments);
        }

        public void bind(CommunityPost post, OnPostClickListener listener) {
            txtAuthor.setText(post.getAuthorName());
            txtRoleLocation.setText(post.getAuthorRole() + " • " + post.getVillageLocation());
            txtTimeAgo.setText(post.getTimeAgo());
            txtTitle.setText(post.getTitle());
            txtContent.setText(post.getContentSnippet());
            txtLikes.setText(String.valueOf(post.getLikeCount()));
            txtComments.setText(String.valueOf(post.getCommentCount()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostClick(post);
                }
            });
        }
    }
}