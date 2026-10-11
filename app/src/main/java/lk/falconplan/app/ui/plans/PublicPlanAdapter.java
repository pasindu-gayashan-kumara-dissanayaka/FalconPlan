package lk.falconplan.app.ui.plans;

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
import lk.falconplan.app.data.model.PublicTravelPlan;

public class PublicPlanAdapter extends RecyclerView.Adapter<PublicPlanAdapter.PublicPlanViewHolder> {

    private final List<PublicTravelPlan> plans = new ArrayList<>();
    private OnPublicPlanActionListener listener;

    public interface OnPublicPlanActionListener {
        void onViewClick(PublicTravelPlan plan);
        void onCopyClick(PublicTravelPlan plan);
    }

    public void setOnPublicPlanActionListener(OnPublicPlanActionListener listener) {
        this.listener = listener;
    }

    public void setPlans(List<PublicTravelPlan> newPlans) {
        this.plans.clear();
        if (newPlans != null) {
            this.plans.addAll(newPlans);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PublicPlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_public_plan_card, parent, false);
        return new PublicPlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PublicPlanViewHolder holder, int position) {
        PublicTravelPlan item = plans.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class PublicPlanViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtPublicTitle;
        private final TextView txtOwnerName;
        private final TextView txtDescription;
        private final Button btnViewPublicPlan;
        private final Button btnCopyPublicPlan;

        public PublicPlanViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPublicTitle = itemView.findViewById(R.id.txtPublicTitle);
            txtOwnerName = itemView.findViewById(R.id.txtOwnerName);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            btnViewPublicPlan = itemView.findViewById(R.id.btnViewPublicPlan);
            btnCopyPublicPlan = itemView.findViewById(R.id.btnCopyPublicPlan);
        }

        public void bind(PublicTravelPlan item, OnPublicPlanActionListener listener) {
            txtPublicTitle.setText(item.getTitle());
            txtOwnerName.setText(String.format("Published by %s", item.getOwnerDisplayName()));
            txtDescription.setText(item.getDescription() != null && !item.getDescription().isEmpty()
                    ? item.getDescription() : "No description provided.");

            btnViewPublicPlan.setOnClickListener(v -> {
                if (listener != null) listener.onViewClick(item);
            });

            btnCopyPublicPlan.setOnClickListener(v -> {
                if (listener != null) listener.onCopyClick(item);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onViewClick(item);
            });
        }
    }
}