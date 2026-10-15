package lk.falconplan.app.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Booking;
import lk.falconplan.app.data.model.BookingQuote;
import lk.falconplan.app.data.repository.BookingRepository;
import lk.falconplan.app.data.repository.BookingRepositoryImpl;

public class BookingRequestFragment extends Fragment {

    private static final String ARG_LISTING_ID = "arg_listing_id";
    private static final String ARG_ROOM_ID = "arg_room_id";

    private BookingRepository bookingRepository;
    private String listingId;
    private String roomId;

    private EditText editCheckInDate;
    private EditText editCheckOutDate;
    private EditText editGuests;
    private MaterialSwitch switchBreakfast;

    private View cardQuote;
    private TextView txtQuotedTotal;
    private ProgressBar progressBar;

    public static BookingRequestFragment newInstance(String listingId, String roomId) {
        BookingRequestFragment fragment = new BookingRequestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingRepository = new BookingRepositoryImpl();
        listingId = getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : "";
        roomId = getArguments() != null ? getArguments().getString(ARG_ROOM_ID) : "";

        editCheckInDate = view.findViewById(R.id.editCheckInDate);
        editCheckOutDate = view.findViewById(R.id.editCheckOutDate);
        editGuests = view.findViewById(R.id.editGuests);
        switchBreakfast = view.findViewById(R.id.switchBreakfast);

        cardQuote = view.findViewById(R.id.cardQuote);
        txtQuotedTotal = view.findViewById(R.id.txtQuotedTotal);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnGetQuote).setOnClickListener(v -> fetchServerQuote());
        view.findViewById(R.id.btnSendBookingRequest).setOnClickListener(v -> sendBookingRequest());
    }

    private void fetchServerQuote() {
        String checkIn = editCheckInDate.getText() != null ? editCheckInDate.getText().toString().trim() : "";
        String checkOut = editCheckOutDate.getText() != null ? editCheckOutDate.getText().toString().trim() : "";
        int guests = parseIntSafe(editGuests.getText() != null ? editGuests.getText().toString() : "1");
        boolean breakfast = switchBreakfast.isChecked();

        if (!checkIn.matches("^\\d{4}-\\d{2}-\\d{2}$") || !checkOut.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            Toast.makeText(requireContext(), "Please enter valid check-in and check-out dates (YYYY-MM-DD).", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        bookingRepository.getBookingQuote(listingId, roomId, checkIn, checkOut, guests, 1, breakfast, new BookingRepository.QuoteCallback() {
            @Override
            public void onSuccess(BookingQuote quote) {
                progressBar.setVisibility(View.GONE);
                if (quote != null) {
                    cardQuote.setVisibility(View.VISIBLE);
                    txtQuotedTotal.setText(String.format("Quoted Total: %s (%d nights)", quote.getFormattedPrice(), quote.getNights()));
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Quote calculation failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendBookingRequest() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to send booking request.", Toast.LENGTH_SHORT).show();
            return;
        }

        String checkIn = editCheckInDate.getText() != null ? editCheckInDate.getText().toString().trim() : "";
        String checkOut = editCheckOutDate.getText() != null ? editCheckOutDate.getText().toString().trim() : "";
        int guests = parseIntSafe(editGuests.getText() != null ? editGuests.getText().toString() : "1");
        boolean breakfast = switchBreakfast.isChecked();

        if (!checkIn.matches("^\\d{4}-\\d{2}-\\d{2}$") || !checkOut.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            Toast.makeText(requireContext(), "Please enter valid check-in and check-out dates.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            bookingRepository.createBooking(idToken, listingId, roomId, checkIn, checkOut, guests, 1, breakfast, new BookingRepository.BookingCallback() {
                @Override
                public void onSuccess(Booking booking) {
                    progressBar.setVisibility(View.GONE);
                    if (getActivity() instanceof MainActivity && booking != null) {
                        ((MainActivity) getActivity()).loadMainContentFragment(BookingRequestResultFragment.newInstance(booking.getBookingId(), booking.getFormattedPrice()), true);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Booking request failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private int parseIntSafe(String val) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return 1;
        }
    }
}