package lk.falconplan.app.ui.booking;

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
import lk.falconplan.app.data.repository.BookingRepository;
import lk.falconplan.app.data.repository.BookingRepositoryImpl;

public class MyBookingsFragment extends Fragment implements BookingAdapter.OnBookingActionListener {

    private BookingRepository bookingRepository;
    private BookingAdapter adapter;

    private RecyclerView recyclerBookings;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static MyBookingsFragment newInstance() {
        return new MyBookingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingRepository = new BookingRepositoryImpl();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recyclerBookings = view.findViewById(R.id.recyclerBookings);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BookingAdapter();
        recyclerBookings.setAdapter(adapter);
        adapter.setOnBookingActionListener(this);

        loadTravelerBookings();
    }

    private void loadTravelerBookings() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            bookingRepository.getTravelerBookings(idToken, new BookingRepository.BookingsListCallback() {
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
                    Toast.makeText(requireContext(), "Failed to load bookings: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onViewDetailClick(Booking booking) {
        if (getActivity() instanceof MainActivity && booking != null && booking.getBookingId() != null) {
            ((MainActivity) getActivity()).loadMainContentFragment(BookingDetailFragment.newInstance(booking.getBookingId()), true);
        }
    }

    @Override
    public void onChatClick(Booking booking) {
        if (getActivity() instanceof MainActivity && booking != null && booking.getBookingId() != null) {
            ((MainActivity) getActivity()).loadMainContentFragment(
                    lk.falconplan.app.ui.chat.BookingChatFragment.newInstance(booking.getBookingId()), true
            );
        }
    }

    @Override
    public void onCancelClick(Booking booking) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || booking == null || booking.getBookingId() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Booking Request")
                .setMessage("Are you sure you want to cancel this booking request?")
                .setPositiveButton("Cancel Request", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        bookingRepository.cancelBooking(idToken, booking.getBookingId(), new BookingRepository.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Booking request cancelled.", Toast.LENGTH_SHORT).show();
                                loadTravelerBookings();
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