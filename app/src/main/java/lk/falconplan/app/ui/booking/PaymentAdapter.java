package lk.falconplan.app.ui.booking;

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

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {

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
                .inflate(R.layout.item_payment_card, parent, false);
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
        private final TextView txtPaymentMethod;
        private final TextView txtAmountDue;
        private final Button btnViewDetail;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPaymentTitle = itemView.findViewById(R.id.txtPaymentTitle);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtPaymentMethod = itemView.findViewById(R.id.txtPaymentMethod);
            txtAmountDue = itemView.findViewById(R.id.txtAmountDue);
            btnViewDetail = itemView.findViewById(R.id.btnViewDetail);
        }

        public void bind(Payment item, OnPaymentClickListener listener) {
            txtPaymentTitle.setText(String.format("Payment ID: %s", item.getPaymentId()));
            txtStatusBadge.setText(item.getStatus().toUpperCase(Locale.US));
            txtPaymentMethod.setText(String.format("Method: %s", item.getMethod()));
            txtAmountDue.setText(String.format("Amount: %s", item.getFormattedAmount()));

            View.OnClickListener clickListener = v -> {
                if (listener != null) listener.onItemClick(item);
            };

            itemView.setOnClickListener(clickListener);
            btnViewDetail.setOnClickListener(clickListener);
        }
    }
}