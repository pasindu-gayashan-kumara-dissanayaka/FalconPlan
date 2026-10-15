package lk.falconplan.app.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Booking;
import lk.falconplan.app.data.repository.BookingRepository;
import lk.falconplan.app.data.repository.BookingRepositoryImpl;

public class BookingDetailFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "arg_booking_id";

    private BookingRepository bookingRepository;
    private String bookingId;

    private TextView txtStatusTitle;
    private TextView txtBookingId;
    private TextView txtDatesNights;
    private TextView txtTotalAmount;
    private ProgressBar progressBar;

    public static BookingDetailFragment newInstance(String bookingId) {
        BookingDetailFragment fragment = new BookingDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOKING_ID, bookingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingRepository = new BookingRepositoryImpl();
        bookingId = getArguments() != null ? getArguments().getString(ARG_BOOKING_ID) : "";

        txtStatusTitle = view.findViewById(R.id.txtStatusTitle);
        txtBookingId = view.findViewById(R.id.txtBookingId);
        txtDatesNights = view.findViewById(R.id.txtDatesNights);
        txtTotalAmount = view.findViewById(R.id.txtTotalAmount);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnCancelRequest).setOnClickListener(v -> cancelCurrentBooking());

        loadBookingDetail();
    }

    private void loadBookingDetail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bookingId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            bookingRepository.getTravelerBookings(idToken, new BookingRepository.BookingsListCallback() {
                @Override
                public void onSuccess(java.util.List<Booking> bookings) {
                    progressBar.setVisibility(View.GONE);
                    if (bookings != null) {
                        for (Booking b : bookings) {
                            if (bookingId.equals(b.getBookingId())) {
                                displayBooking(b);
                                return;
                            }
                        }
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load booking: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void displayBooking(Booking booking) {
        txtStatusTitle.setText(booking.getFormattedStatus());
        txtBookingId.setText(String.format("Booking ID: %s", booking.getBookingId()));
        txtDatesNights.setText(String.format("Check-in: %s • Check-out: %s (%d Nights)",
                booking.getCheckInDate(), booking.getCheckOutDate(), booking.getNights()));
        txtTotalAmount.setText(String.format("Quoted Total: %s", booking.getFormattedPrice()));
    }

    private void cancelCurrentBooking() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bookingId.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Booking Request")
                .setMessage("Are you sure you want to cancel this booking request?")
                .setPositiveButton("Cancel Request", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        bookingRepository.cancelBooking(idToken, bookingId, new BookingRepository.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Booking request cancelled.", Toast.LENGTH_SHORT).show();
                                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            }

                            @Override
                            public void onError(Exception exception) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Failed to cancel: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Back", (dialog, which) -> dialog.dismiss())
                .show();
    }
}