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
import lk.falconplan.app.data.model.ProviderRoom;
import lk.falconplan.app.data.repository.ProviderRoomRepository;
import lk.falconplan.app.data.repository.ProviderRoomRepositoryImpl;

public class ProviderRoomsFragment extends Fragment implements ProviderRoomAdapter.OnRoomActionListener {

    private static final String ARG_LISTING_ID = "arg_listing_id";

    private ProviderRoomRepository roomRepository;
    private ProviderRoomAdapter adapter;
    private String listingId;

    private RecyclerView recyclerRooms;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static ProviderRoomsFragment newInstance(String listingId) {
        ProviderRoomsFragment fragment = new ProviderRoomsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_rooms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        roomRepository = new ProviderRoomRepositoryImpl();
        listingId = getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : "";

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        View.OnClickListener addListener = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(AddEditRoomFragment.newInstance(listingId), true);
            }
        };

        view.findViewById(R.id.btnAddRoom).setOnClickListener(addListener);
        view.findViewById(R.id.btnAddFirstRoom).setOnClickListener(addListener);

        recyclerRooms = view.findViewById(R.id.recyclerRooms);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerRooms.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProviderRoomAdapter();
        recyclerRooms.setAdapter(adapter);
        adapter.setOnRoomActionListener(this);

        loadRooms();
    }

    private void loadRooms() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || listingId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        roomRepository.getListingRooms(user.getUid(), listingId, new ProviderRoomRepository.RoomsCallback() {
            @Override
            public void onSuccess(List<ProviderRoom> rooms) {
                progressBar.setVisibility(View.GONE);
                adapter.setItems(rooms);

                if (rooms == null || rooms.isEmpty()) {
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
                Toast.makeText(requireContext(), "Failed to load room inventory: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditClick(ProviderRoom room) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadMainContentFragment(AddEditRoomFragment.newInstance(listingId, room.getId()), true);
        }
    }

    @Override
    public void onDeleteClick(ProviderRoom room) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || room == null || room.getId() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Room Type")
                .setMessage("Are you sure you want to delete this room type from inventory?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    roomRepository.deleteRoom(user.getUid(), listingId, room.getId(), new ProviderRoomRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Room type deleted.", Toast.LENGTH_SHORT).show();
                            loadRooms();
                        }

                        @Override
                        public void onError(Exception exception) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Failed to delete: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}