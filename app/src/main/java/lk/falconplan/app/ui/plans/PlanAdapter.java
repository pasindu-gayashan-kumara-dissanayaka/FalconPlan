package lk.falconplan.app.ui.plans;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.TravelPlan;

public class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.PlanViewHolder> {

    private final List<TravelPlan> plans = new ArrayList<>();
    private OnPlanClickListener listener;

    public interface OnPlanClickListener {
        void onPlanClick(TravelPlan plan);
    }

    public void setOnPlanClickListener(OnPlanClickListener listener) {
        this.listener = listener;
    }

    public void setPlans(List<TravelPlan> newPlans) {
        this.plans.clear();
        if (newPlans != null) {
            this.plans.addAll(newPlans);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_travel_plan_card, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        TravelPlan item = plans.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtPlanTitle;
        private final TextView txtStatusBadge;
        private final TextView txtPlanDates;
        private final TextView txtPlanDescription;
        private final TextView txtStopCount;
        private final Button btnOpenPlan;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPlanTitle = itemView.findViewById(R.id.txtPlanTitle);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtPlanDates = itemView.findViewById(R.id.txtPlanDates);
            txtPlanDescription = itemView.findViewById(R.id.txtPlanDescription);
            txtStopCount = itemView.findViewById(R.id.txtStopCount);
            btnOpenPlan = itemView.findViewById(R.id.btnOpenPlan);
        }

        public void bind(TravelPlan item, OnPlanClickListener listener) {
            txtPlanTitle.setText(item.getTitle());
            txtStatusBadge.setText(item.getStatus() != null ? item.getStatus().toUpperCase(Locale.US) : "DRAFT");

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            String dateStr = String.format("%s – %s", sdf.format(new Date(item.getStartDate())), sdf.format(new Date(item.getEndDate())));
            txtPlanDates.setText(dateStr);

            txtPlanDescription.setText(item.getDescription() != null && !item.getDescription().isEmpty()
                    ? item.getDescription() : "No description added.");

            txtStopCount.setText(String.format(Locale.US, "%d Destinations Planned", item.getStopCount()));

            View.OnClickListener clickListener = v -> {
                if (listener != null) listener.onPlanClick(item);
            };

            itemView.setOnClickListener(clickListener);
            btnOpenPlan.setOnClickListener(clickListener);
        }
    }
}