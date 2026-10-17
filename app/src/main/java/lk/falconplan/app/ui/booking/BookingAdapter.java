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
import lk.falconplan.app.data.model.Booking;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private final List<Booking> items = new ArrayList<>();
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onViewDetailClick(Booking booking);
        void onCancelClick(Booking booking);
        void onChatClick(Booking booking);
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
                .inflate(R.layout.item_booking_card, parent, false);
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
        private final Button btnViewDetail;
        private final Button btnCancelBooking;
        private final Button btnChatHost;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            txtBookingTitle = itemView.findViewById(R.id.txtBookingTitle);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtBookingDates = itemView.findViewById(R.id.txtBookingDates);
            txtBookingTotal = itemView.findViewById(R.id.txtBookingTotal);
            btnViewDetail = itemView.findViewById(R.id.btnViewDetail);
            btnCancelBooking = itemView.findViewById(R.id.btnCancelBooking);
            btnChatHost = itemView.findViewById(R.id.btnChatHost);
        }

        public void bind(Booking item, OnBookingActionListener listener) {
            txtBookingTitle.setText(String.format("Booking ID: %s", item.getBookingId()));
            txtStatusBadge.setText(item.getStatus().toUpperCase(Locale.US));

            txtBookingDates.setText(String.format("%s to %s (%d Nights)",
                    item.getCheckInDate(), item.getCheckOutDate(), item.getNights()));

            txtBookingTotal.setText(String.format("Quoted Total: %s", item.getFormattedPrice()));

            if (Booking.STATUS_CANCELLED.equals(item.getStatus()) || Booking.STATUS_DECLINED.equals(item.getStatus())) {
                btnCancelBooking.setVisibility(View.GONE);
            } else {
                btnCancelBooking.setVisibility(View.VISIBLE);
            }

            btnViewDetail.setOnClickListener(v -> {
                if (listener != null) listener.onViewDetailClick(item);
            });

            btnCancelBooking.setOnClickListener(v -> {
                if (listener != null) listener.onCancelClick(item);
            });

            if (btnChatHost != null) {
                btnChatHost.setOnClickListener(v -> {
                    if (listener != null) listener.onChatClick(item);
                });
            }
        }
    }
}