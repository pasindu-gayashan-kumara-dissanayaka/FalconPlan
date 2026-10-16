package lk.falconplan.app.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityComment;

public class CommunityCommentAdapter extends RecyclerView.Adapter<CommunityCommentAdapter.CommentViewHolder> {

    private final List<CommunityComment> items = new ArrayList<>();
    private OnCommentActionListener listener;

    public interface OnCommentActionListener {
        void onEditClick(CommunityComment comment);
        void onDeleteClick(CommunityComment comment);
        void onReportClick(CommunityComment comment);
    }

    public void setOnCommentActionListener(OnCommentActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CommunityComment> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommunityComment item = items.get(position);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";
        holder.bind(item, currentUid, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtAuthorName;
        private final TextView txtCommentText;
        private final ImageButton btnEditComment;
        private final ImageButton btnDeleteComment;
        private final ImageButton btnReportComment;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAuthorName = itemView.findViewById(R.id.txtAuthorName);
            txtCommentText = itemView.findViewById(R.id.txtCommentText);
            btnEditComment = itemView.findViewById(R.id.btnEditComment);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment);
            btnReportComment = itemView.findViewById(R.id.btnReportComment);
        }

        public void bind(CommunityComment item, String currentUserUid, OnCommentActionListener listener) {
            txtAuthorName.setText(item.getAuthorDisplayName());
            txtCommentText.setText(item.getText());

            boolean isOwner = currentUserUid != null && !currentUserUid.isEmpty() && currentUserUid.equals(item.getAuthorUid());
            if (isOwner) {
                btnEditComment.setVisibility(View.VISIBLE);
                btnDeleteComment.setVisibility(View.VISIBLE);
                btnReportComment.setVisibility(View.GONE);
            } else {
                btnEditComment.setVisibility(View.GONE);
                btnDeleteComment.setVisibility(View.GONE);
                btnReportComment.setVisibility(View.VISIBLE);
            }

            btnEditComment.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(item);
            });

            btnDeleteComment.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(item);
            });

            btnReportComment.setOnClickListener(v -> {
                if (listener != null) listener.onReportClick(item);
            });
        }
    }
}