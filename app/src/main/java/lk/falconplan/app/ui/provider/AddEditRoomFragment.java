package lk.falconplan.app.ui.provider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ProviderRoom;
import lk.falconplan.app.data.repository.ProviderRoomRepository;
import lk.falconplan.app.data.repository.ProviderRoomRepositoryImpl;

public class AddEditRoomFragment extends Fragment {

    private static final String ARG_LISTING_ID = "arg_listing_id";
    private static final String ARG_EDIT_ROOM_ID = "arg_edit_room_id";

    private ProviderRoomRepository roomRepository;
    private String listingId;
    private String editRoomId;

    private EditText editRoomName;
    private RadioGroup radioGroupRoomType;
    private EditText editRoomDescription;
    private EditText editTotalUnits;
    private EditText editMaxGuestsPerUnit;
    private EditText editBedCount;
    private EditText editBedType;
    private RadioGroup radioGroupBathroom;
    private EditText editAmenities;

    private ProgressBar progressBar;

    public static AddEditRoomFragment newInstance(String listingId) {
        return newInstance(listingId, null);
    }

    public static AddEditRoomFragment newInstance(String listingId, String roomId) {
        AddEditRoomFragment fragment = new AddEditRoomFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        args.putString(ARG_EDIT_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_edit_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        roomRepository = new ProviderRoomRepositoryImpl();
        listingId = getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : "";
        editRoomId = getArguments() != null ? getArguments().getString(ARG_EDIT_ROOM_ID) : null;

        editRoomName = view.findViewById(R.id.editRoomName);
        radioGroupRoomType = view.findViewById(R.id.radioGroupRoomType);
        editRoomDescription = view.findViewById(R.id.editRoomDescription);
        editTotalUnits = view.findViewById(R.id.editTotalUnits);
        editMaxGuestsPerUnit = view.findViewById(R.id.editMaxGuestsPerUnit);
        editBedCount = view.findViewById(R.id.editBedCount);
        editBedType = view.findViewById(R.id.editBedType);
        radioGroupBathroom = view.findViewById(R.id.radioGroupBathroom);
        editAmenities = view.findViewById(R.id.editAmenities);

        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnSaveRoom).setOnClickListener(v -> saveRoomInventory());
    }

    private void saveRoomInventory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || listingId.isEmpty()) {
            Toast.makeText(requireContext(), "Please sign in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String roomName = editRoomName.getText() != null ? editRoomName.getText().toString().trim() : "";
        String description = editRoomDescription.getText() != null ? editRoomDescription.getText().toString().trim() : "";
        int totalUnits = parseIntSafe(editTotalUnits.getText() != null ? editTotalUnits.getText().toString() : "1");
        int maxGuests = parseIntSafe(editMaxGuestsPerUnit.getText() != null ? editMaxGuestsPerUnit.getText().toString() : "1");
        int bedCount = parseIntSafe(editBedCount.getText() != null ? editBedCount.getText().toString() : "1");
        String bedType = editBedType.getText() != null ? editBedType.getText().toString().trim() : "Bed";
        String amenities = editAmenities.getText() != null ? editAmenities.getText().toString().trim() : "";

        if (roomName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter Room Name.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalUnits <= 0 || maxGuests <= 0) {
            Toast.makeText(requireContext(), "Total Units and Max Guests must be greater than 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        String roomType = ProviderRoom.TYPE_DOUBLE;
        int checkedTypeId = radioGroupRoomType.getCheckedRadioButtonId();
        if (checkedTypeId == R.id.radioSingle) {
            roomType = ProviderRoom.TYPE_SINGLE;
        } else if (checkedTypeId == R.id.radioTwin) {
            roomType = ProviderRoom.TYPE_TWIN;
        } else if (checkedTypeId == R.id.radioFamily) {
            roomType = ProviderRoom.TYPE_FAMILY;
        } else if (checkedTypeId == R.id.radioDormitory) {
            roomType = ProviderRoom.TYPE_DORMITORY;
        }

        String bathroomType = radioGroupBathroom.getCheckedRadioButtonId() == R.id.radioBathroomShared
                ? ProviderRoom.BATHROOM_SHARED : ProviderRoom.BATHROOM_PRIVATE;

        progressBar.setVisibility(View.VISIBLE);
        long now = System.currentTimeMillis();

        ProviderRoom room = new ProviderRoom(
                editRoomId,
                user.getUid(),
                listingId,
                roomName,
                roomType,
                description,
                totalUnits,
                maxGuests,
                bedCount,
                bedType,
                bathroomType,
                amenities,
                now,
                now
        );

        ProviderRoomRepository.OperationCallback callback = new ProviderRoomRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Room Inventory Saved!", Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Save failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (editRoomId != null && !editRoomId.isEmpty()) {
            roomRepository.updateRoom(user.getUid(), listingId, room, callback);
        } else {
            roomRepository.createRoom(user.getUid(), listingId, room, callback);
        }
    }

    private int parseIntSafe(String val) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return 1;
        }
    }
}