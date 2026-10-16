package lk.falconplan.app.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityComment;

public class CommunityCommentAdapter extends RecyclerView.Adapter<CommunityCommentAdapter.CommentViewHolder> {

    private final List<CommunityComment> items = new ArrayList<>();
    private OnCommentActionListener listener;

    public interface OnCommentActionListener {
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
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtAuthorName;
        private final TextView txtCommentText;
        private final ImageButton btnReportComment;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAuthorName = itemView.findViewById(R.id.txtAuthorName);
            txtCommentText = itemView.findViewById(R.id.txtCommentText);
            btnReportComment = itemView.findViewById(R.id.btnReportComment);
        }

        public void bind(CommunityComment item, OnCommentActionListener listener) {
            txtAuthorName.setText(item.getAuthorDisplayName());
            txtCommentText.setText(item.getText());

            btnReportComment.setOnClickListener(v -> {
                if (listener != null) listener.onReportClick(item);
            });
        }
    }
}