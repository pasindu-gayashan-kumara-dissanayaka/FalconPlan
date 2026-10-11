package lk.falconplan.app.ui.explore;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.SavedPlace;
import lk.falconplan.app.data.repository.SavedPlacesRepository;
import lk.falconplan.app.data.repository.SavedPlacesRepositoryImpl;

public class SavedPlacesFragment extends Fragment implements SavedPlacesAdapter.OnSavedPlaceActionListener {

    private SavedPlacesRepository savedPlacesRepository;
    private SavedPlacesAdapter adapter;
    private RecyclerView recyclerSavedPlaces;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static SavedPlacesFragment newInstance() {
        return new SavedPlacesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_places, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        savedPlacesRepository = new SavedPlacesRepositoryImpl();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recyclerSavedPlaces = view.findViewById(R.id.recyclerSavedPlaces);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerSavedPlaces.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SavedPlacesAdapter();
        recyclerSavedPlaces.setAdapter(adapter);
        adapter.setOnSavedPlaceActionListener(this);

        loadSavedPlaces();
    }

    private void loadSavedPlaces() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerSavedPlaces.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        savedPlacesRepository.getSavedPlaces(user.getUid(), new SavedPlacesRepository.SavedPlacesCallback() {
            @Override
            public void onSuccess(List<SavedPlace> places) {
                progressBar.setVisibility(View.GONE);
                adapter.setPlaces(places);
                if (places == null || places.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerSavedPlaces.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerSavedPlaces.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load saved places: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditNoteClick(SavedPlace place) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        EditText input = new EditText(requireContext());
        input.setText(place.getNote() != null ? place.getNote() : "");
        input.setHint("Add personal travel notes...");

        new AlertDialog.Builder(requireContext())
                .setTitle("Personal Travel Note")
                .setView(input)
                .setPositiveButton("Save Note", (dialog, which) -> {
                    String newNote = input.getText() != null ? input.getText().toString().trim() : "";
                    savedPlacesRepository.updateNote(user.getUid(), place.getId(), newNote, new SavedPlacesRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(requireContext(), "Note updated!", Toast.LENGTH_SHORT).show();
                            loadSavedPlaces();
                        }

                        @Override
                        public void onError(Exception exception) {
                            Toast.makeText(requireContext(), "Failed to update note: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDeleteClick(SavedPlace place) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Saved Place")
                .setMessage("Remove " + place.getDestinationName() + " from your saved places?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    savedPlacesRepository.deleteSavedPlace(user.getUid(), place.getId(), new SavedPlacesRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(requireContext(), "Removed!", Toast.LENGTH_SHORT).show();
                            loadSavedPlaces();
                        }

                        @Override
                        public void onError(Exception exception) {
                            Toast.makeText(requireContext(), "Failed to remove: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onViewMapClick(SavedPlace place) {
        if (!place.isValidCoordinates()) {
            Toast.makeText(requireContext(), "Invalid coordinates.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                place.getLatitude(), place.getLongitude(),
                place.getLatitude(), place.getLongitude(),
                Uri.encode(place.getDestinationName()));

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            String webUri = String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%f,%f",
                    place.getLatitude(), place.getLongitude());
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            startActivity(webIntent);
        }
    }

    @Override
    public void onDirectionsClick(SavedPlace place) {
        if (!place.isValidCoordinates()) {
            Toast.makeText(requireContext(), "Invalid coordinates.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uri = String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                place.getLatitude(), place.getLongitude());

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(webIntent);
        }
    }
}