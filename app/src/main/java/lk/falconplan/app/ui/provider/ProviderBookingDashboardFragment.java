package lk.falconplan.app.ui.provider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Booking;
import lk.falconplan.app.data.repository.ProviderBookingRepository;
import lk.falconplan.app.data.repository.ProviderBookingRepositoryImpl;

public class ProviderBookingDashboardFragment extends Fragment implements ProviderBookingAdapter.OnBookingActionListener {

    private ProviderBookingRepository bookingRepository;
    private ProviderBookingAdapter adapter;

    private RecyclerView recyclerBookings;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static ProviderBookingDashboardFragment newInstance() {
        return new ProviderBookingDashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_booking_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingRepository = new ProviderBookingRepositoryImpl();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recyclerBookings = view.findViewById(R.id.recyclerBookings);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProviderBookingAdapter();
        recyclerBookings.setAdapter(adapter);
        adapter.setOnBookingActionListener(this);

        loadProviderBookings();
    }

    private void loadProviderBookings() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            bookingRepository.getProviderBookings(idToken, new ProviderBookingRepository.BookingsListCallback() {
                @Override
                public void onSuccess(List<Booking> bookings) {
                    progressBar.setVisibility(View.GONE);
                    adapter.setItems(bookings);

                    if (bookings == null || bookings.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerBookings.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerBookings.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load provider bookings: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onAcceptClick(Booking booking) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || booking == null || booking.getBookingId() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Accept Booking Request")
                .setMessage("Accepting this booking request will allocate room capacity across all check-in to check-out dates. Confirm acceptance?")
                .setPositiveButton("Accept & Reserve", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        bookingRepository.submitBookingDecision(idToken, booking.getBookingId(), "accept", new ProviderBookingRepository.OperationCallback() {
                            @Override
                            public void onSuccess(String status) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Booking Accepted! Status: " + status.toUpperCase(), Toast.LENGTH_SHORT).show();
                                loadProviderBookings();
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

    @Override
    public void onDeclineClick(Booking booking) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || booking == null || booking.getBookingId() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Decline Booking Request")
                .setMessage("Are you sure you want to decline this booking request?")
                .setPositiveButton("Decline Request", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        bookingRepository.submitBookingDecision(idToken, booking.getBookingId(), "decline", new ProviderBookingRepository.OperationCallback() {
                            @Override
                            public void onSuccess(String status) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Booking Declined.", Toast.LENGTH_SHORT).show();
                                loadProviderBookings();
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

    @Override
    public void onItemClick(Booking booking) {
        if (getActivity() instanceof MainActivity && booking != null && booking.getBookingId() != null) {
            ((MainActivity) getActivity()).loadMainContentFragment(ProviderBookingDetailFragment.newInstance(booking.getBookingId()), true);
        }
    }
}