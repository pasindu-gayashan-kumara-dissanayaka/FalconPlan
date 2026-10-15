package lk.falconplan.app.ui.provider;

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
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import lk.falconplan.app.R;
import lk.falconplan.app.data.repository.RoomAvailabilityRepository;
import lk.falconplan.app.data.repository.RoomAvailabilityRepositoryImpl;

public class RoomAvailabilityCalendarFragment extends Fragment {

    private static final String ARG_LISTING_ID = "arg_listing_id";
    private static final String ARG_ROOM_ID = "arg_room_id";

    private RoomAvailabilityRepository availabilityRepository;
    private String listingId;
    private String roomId;

    private TextView txtRoomName;
    private TextView txtTotalUnits;

    private EditText editDate;
    private EditText editBlockedUnits;
    private EditText editNote;

    private ProgressBar progressBar;

    public static RoomAvailabilityCalendarFragment newInstance(String listingId, String roomId) {
        RoomAvailabilityCalendarFragment fragment = new RoomAvailabilityCalendarFragment();
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
        return inflater.inflate(R.layout.fragment_room_availability_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        availabilityRepository = new RoomAvailabilityRepositoryImpl();
        listingId = getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : "";
        roomId = getArguments() != null ? getArguments().getString(ARG_ROOM_ID) : "";

        txtRoomName = view.findViewById(R.id.txtRoomName);
        txtTotalUnits = view.findViewById(R.id.txtTotalUnits);

        editDate = view.findViewById(R.id.editDate);
        editBlockedUnits = view.findViewById(R.id.editBlockedUnits);
        editNote = view.findViewById(R.id.editNote);

        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnSaveAvailability).setOnClickListener(v -> saveAvailabilityBlock());

        loadRoomAvailability();
    }

    private void loadRoomAvailability() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || listingId.isEmpty() || roomId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            availabilityRepository.getRoomAvailability(idToken, listingId, roomId, new RoomAvailabilityRepository.AvailabilityCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    progressBar.setVisibility(View.GONE);
                    if (data != null) {
                        int units = data.optInt("totalUnits", 1);
                        txtTotalUnits.setText(String.format("Total Physical Units: %d", units));
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                }
            });
        });
    }

    private void saveAvailabilityBlock() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || listingId.isEmpty() || roomId.isEmpty()) {
            Toast.makeText(requireContext(), "Please sign in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateStr = editDate.getText() != null ? editDate.getText().toString().trim() : "";
        String note = editNote.getText() != null ? editNote.getText().toString().trim() : "";
        int blockedUnits = parseIntSafe(editBlockedUnits.getText() != null ? editBlockedUnits.getText().toString() : "0");

        if (!dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            Toast.makeText(requireContext(), "Please enter a valid date in YYYY-MM-DD format.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (blockedUnits < 0) {
            Toast.makeText(requireContext(), "Blocked Units cannot be negative.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            availabilityRepository.updateRoomAvailability(idToken, listingId, roomId, dateStr, blockedUnits, note, new RoomAvailabilityRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Room date availability updated!", Toast.LENGTH_SHORT).show();
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Save failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private int parseIntSafe(String val) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}