package lk.falconplan.app.ui.provider;

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

import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Booking;
import lk.falconplan.app.data.repository.ProviderBookingRepository;
import lk.falconplan.app.data.repository.ProviderBookingRepositoryImpl;

public class ProviderBookingDetailFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "arg_booking_id";

    private ProviderBookingRepository bookingRepository;
    private String bookingId;

    private TextView txtStatusTitle;
    private TextView txtBookingId;
    private TextView txtDatesNights;
    private TextView txtTotalAmount;
    private View layoutDecisionActions;
    private ProgressBar progressBar;

    public static ProviderBookingDetailFragment newInstance(String bookingId) {
        ProviderBookingDetailFragment fragment = new ProviderBookingDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOKING_ID, bookingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_booking_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingRepository = new ProviderBookingRepositoryImpl();
        bookingId = getArguments() != null ? getArguments().getString(ARG_BOOKING_ID) : "";

        txtStatusTitle = view.findViewById(R.id.txtStatusTitle);
        txtBookingId = view.findViewById(R.id.txtBookingId);
        txtDatesNights = view.findViewById(R.id.txtDatesNights);
        txtTotalAmount = view.findViewById(R.id.txtTotalAmount);
        layoutDecisionActions = view.findViewById(R.id.layoutDecisionActions);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnAcceptBooking).setOnClickListener(v -> acceptBooking());
        view.findViewById(R.id.btnDeclineBooking).setOnClickListener(v -> declineBooking());

        loadBookingDetail();
    }

    private void loadBookingDetail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bookingId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            bookingRepository.getProviderBookings(idToken, new ProviderBookingRepository.BookingsListCallback() {
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
        txtStatusTitle.setText(booking.getStatus().toUpperCase(Locale.US));
        txtBookingId.setText(String.format("Booking ID: %s", booking.getBookingId()));
        txtDatesNights.setText(String.format(Locale.US, "Check-in: %s • Check-out: %s (%d Nights)",
                booking.getCheckInDate(), booking.getCheckOutDate(), booking.getNights()));
        txtTotalAmount.setText(String.format("Quoted Amount: %s", booking.getFormattedPrice()));

        if (Booking.STATUS_PENDING.equals(booking.getStatus())) {
            layoutDecisionActions.setVisibility(View.VISIBLE);
        } else {
            layoutDecisionActions.setVisibility(View.GONE);
        }
    }

    private void acceptBooking() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bookingId.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Accept Booking Request")
                .setMessage("Confirm accepting this booking request? Room capacity will be allocated across all check-in to check-out dates.")
                .setPositiveButton("Accept & Reserve", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        bookingRepository.submitBookingDecision(idToken, bookingId, "accept", new ProviderBookingRepository.OperationCallback() {
                            @Override
                            public void onSuccess(String status) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Booking Accepted! Status: " + status.toUpperCase(Locale.US), Toast.LENGTH_SHORT).show();
                                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            }

                            @Override
                            public void onError(Exception exception) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Accept failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void declineBooking() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bookingId.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Decline Booking Request")
                .setMessage("Are you sure you want to decline this booking request?")
                .setPositiveButton("Decline Request", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        bookingRepository.submitBookingDecision(idToken, bookingId, "decline", new ProviderBookingRepository.OperationCallback() {
                            @Override
                            public void onSuccess(String status) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Booking Declined.", Toast.LENGTH_SHORT).show();
                                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            }

                            @Override
                            public void onError(Exception exception) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Decline failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Back", (dialog, which) -> dialog.dismiss())
                .show();
    }
}