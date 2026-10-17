package lk.falconplan.app.ui.reviews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ReviewItem;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final List<ReviewItem> items = new ArrayList<>();

    public void setItems(List<ReviewItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review_card, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ReviewItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtReviewerName;
        private final TextView txtRatingStars;
        private final TextView txtReviewText;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            txtReviewerName = itemView.findViewById(R.id.txtReviewerName);
            txtRatingStars = itemView.findViewById(R.id.txtRatingStars);
            txtReviewText = itemView.findViewById(R.id.txtReviewText);
        }

        public void bind(ReviewItem item) {
            txtReviewerName.setText("Verified Traveler");
            txtRatingStars.setText("★ " + item.getRating() + ".0");
            txtReviewText.setText(item.getReviewText());
        }
    }
}