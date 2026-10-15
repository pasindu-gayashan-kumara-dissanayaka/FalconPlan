package lk.falconplan.app.ui.provider;

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
import lk.falconplan.app.data.model.Booking;

public class ProviderBookingAdapter extends RecyclerView.Adapter<ProviderBookingAdapter.BookingViewHolder> {

    private final List<Booking> items = new ArrayList<>();
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onAcceptClick(Booking booking);
        void onDeclineClick(Booking booking);
        void onItemClick(Booking booking);
    }

    public void setOnBookingActionListener(OnBookingActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Booking> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtBookingTitle;
        private final TextView txtStatusBadge;
        private final TextView txtBookingDates;
        private final TextView txtBookingTotal;
        private final View layoutDecisionActions;
        private final Button btnAcceptBooking;
        private final Button btnDeclineBooking;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            txtBookingTitle = itemView.findViewById(R.id.txtBookingTitle);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtBookingDates = itemView.findViewById(R.id.txtBookingDates);
            txtBookingTotal = itemView.findViewById(R.id.txtBookingTotal);
            layoutDecisionActions = itemView.findViewById(R.id.layoutDecisionActions);
            btnAcceptBooking = itemView.findViewById(R.id.btnAcceptBooking);
            btnDeclineBooking = itemView.findViewById(R.id.btnDeclineBooking);
        }

        public void bind(Booking item, OnBookingActionListener listener) {
            txtBookingTitle.setText(String.format("Booking ID: %s", item.getBookingId()));
            txtStatusBadge.setText(item.getStatus().toUpperCase(Locale.US));

            txtBookingDates.setText(String.format(Locale.US, "%s to %s (%d Nights)",
                    item.getCheckInDate(), item.getCheckOutDate(), item.getNights()));

            txtBookingTotal.setText(String.format("Quoted Amount: %s", item.getFormattedPrice()));

            if (Booking.STATUS_PENDING.equals(item.getStatus())) {
                layoutDecisionActions.setVisibility(View.VISIBLE);
                btnAcceptBooking.setOnClickListener(v -> {
                    if (listener != null) listener.onAcceptClick(item);
                });
                btnDeclineBooking.setOnClickListener(v -> {
                    if (listener != null) listener.onDeclineClick(item);
                });
            } else {
                layoutDecisionActions.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}