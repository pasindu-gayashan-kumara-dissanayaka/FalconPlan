package lk.falconplan.app.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;

public class BookingRequestResultFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "arg_booking_id";
    private static final String ARG_TOTAL_PRICE = "arg_total_price";

    public static BookingRequestResultFragment newInstance(String bookingId, String totalPrice) {
        BookingRequestResultFragment fragment = new BookingRequestResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOKING_ID, bookingId);
        args.putString(ARG_TOTAL_PRICE, totalPrice);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking_request_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String bookingId = getArguments() != null ? getArguments().getString(ARG_BOOKING_ID) : "";
        String totalPrice = getArguments() != null ? getArguments().getString(ARG_TOTAL_PRICE) : "";

        TextView txtBookingId = view.findViewById(R.id.txtBookingId);
        TextView txtQuotedTotal = view.findViewById(R.id.txtQuotedTotal);

        txtBookingId.setText(String.format("Booking ID: %s", bookingId));
        txtQuotedTotal.setText(String.format("Quoted Total: %s", totalPrice));

        view.findViewById(R.id.btnViewMyBookings).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(MyBookingsFragment.newInstance(), true);
            }
        });
    }
}