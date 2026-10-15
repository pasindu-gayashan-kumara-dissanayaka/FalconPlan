package lk.falconplan.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Payment;

public class AdminPaymentAdapter extends RecyclerView.Adapter<AdminPaymentAdapter.PaymentViewHolder> {

    private final List<Payment> items = new ArrayList<>();
    private OnPaymentClickListener listener;

    public interface OnPaymentClickListener {
        void onItemClick(Payment payment);
    }

    public void setOnPaymentClickListener(OnPaymentClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Payment> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_payment_card, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtPaymentTitle;
        private final TextView txtStatusBadge;
        private final TextView txtAmountDue;
        private final Button btnReviewPayment;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPaymentTitle = itemView.findViewById(R.id.txtPaymentTitle);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtAmountDue = itemView.findViewById(R.id.txtAmountDue);
            btnReviewPayment = itemView.findViewById(R.id.btnReviewPayment);
        }

        public void bind(Payment item, OnPaymentClickListener listener) {
            txtPaymentTitle.setText(String.format("Payment ID: %s", item.getPaymentId()));
            txtStatusBadge.setText(item.getStatus().toUpperCase(Locale.US));
            txtAmountDue.setText(String.format("Amount Due: %s", item.getFormattedAmount()));

            View.OnClickListener clickListener = v -> {
                if (listener != null) listener.onItemClick(item);
            };

            itemView.setOnClickListener(clickListener);
            btnReviewPayment.setOnClickListener(clickListener);
        }
    }
}