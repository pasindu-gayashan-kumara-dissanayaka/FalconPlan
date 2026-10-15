package lk.falconplan.app.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ProviderRoom;
import lk.falconplan.app.data.repository.BookingRepository;
import lk.falconplan.app.data.repository.BookingRepositoryImpl;
import lk.falconplan.app.ui.provider.ProviderRoomAdapter;

public class BookingRoomSelectionFragment extends Fragment implements ProviderRoomAdapter.OnRoomActionListener {

    private static final String ARG_LISTING_ID = "arg_listing_id";

    private BookingRepository bookingRepository;
    private ProviderRoomAdapter adapter;
    private String listingId;

    private RecyclerView recyclerRooms;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static BookingRoomSelectionFragment newInstance(String listingId) {
        BookingRoomSelectionFragment fragment = new BookingRoomSelectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking_room_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bookingRepository = new BookingRepositoryImpl();
        listingId = getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : "";

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recyclerRooms = view.findViewById(R.id.recyclerRooms);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerRooms.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProviderRoomAdapter();
        recyclerRooms.setAdapter(adapter);
        adapter.setOnRoomActionListener(this);

        loadPublicRooms();
    }

    private void loadPublicRooms() {
        if (listingId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        bookingRepository.getPublicRooms(listingId, new BookingRepository.PublicRoomsCallback() {
            @Override
            public void onSuccess(JSONArray roomsJson) {
                progressBar.setVisibility(View.GONE);
                List<ProviderRoom> rooms = new ArrayList<>();
                if (roomsJson != null) {
                    for (int i = 0; i < roomsJson.length(); i++) {
                        JSONObject rObj = roomsJson.optJSONObject(i);
                        if (rObj != null) {
                            ProviderRoom room = new ProviderRoom(
                                    rObj.optString("roomId", ""),
                                    "",
                                    listingId,
                                    rObj.optString("roomName", "Standard Room"),
                                    rObj.optString("roomType", "double"),
                                    "",
                                    rObj.optInt("totalUnits", 1),
                                    rObj.optInt("maxGuestsPerUnit", 2),
                                    rObj.optInt("bedCount", 1),
                                    rObj.optString("bedType", "Bed"),
                                    rObj.optString("bathroomType", "private"),
                                    rObj.optString("amenities", ""),
                                    System.currentTimeMillis(),
                                    System.currentTimeMillis()
                            );
                            rooms.add(room);
                        }
                    }
                }

                adapter.setItems(rooms);
                if (rooms.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerRooms.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerRooms.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Booking not available for this listing: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCalendarClick(ProviderRoom room) {
        // Open Booking Request Fragment
        if (getActivity() instanceof MainActivity && room != null && room.getId() != null) {
            ((MainActivity) getActivity()).loadMainContentFragment(BookingRequestFragment.newInstance(listingId, room.getId()), true);
        }
    }

    @Override
    public void onPricingClick(ProviderRoom room) {
        onCalendarClick(room);
    }

    @Override
    public void onEditClick(ProviderRoom room) {
        onCalendarClick(room);
    }

    @Override
    public void onDeleteClick(ProviderRoom room) {
        onCalendarClick(room);
    }
}