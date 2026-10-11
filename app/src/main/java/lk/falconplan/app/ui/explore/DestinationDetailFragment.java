package lk.falconplan.app.ui.explore;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Destination;
import lk.falconplan.app.data.model.SavedPlace;
import lk.falconplan.app.data.repository.DestinationRepositoryImpl;
import lk.falconplan.app.data.repository.SavedPlacesRepository;
import lk.falconplan.app.data.repository.SavedPlacesRepositoryImpl;

public class DestinationDetailFragment extends Fragment {

    private static final String ARG_DESTINATION_ID = "arg_destination_id";

    private Destination destination;
    private SavedPlacesRepository savedPlacesRepository;
    private ImageButton btnToggleSave;
    private boolean isSavedInFirestore = false;

    public static DestinationDetailFragment newInstance(String destinationId) {
        DestinationDetailFragment fragment = new DestinationDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DESTINATION_ID, destinationId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_destination_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        savedPlacesRepository = new SavedPlacesRepositoryImpl();

        String destinationId = getArguments() != null ? getArguments().getString(ARG_DESTINATION_ID) : null;
        destination = DestinationRepositoryImpl.getInstance().getDestinationById(destinationId);

        if (destination == null) {
            Toast.makeText(requireContext(), "Destination not found.", Toast.LENGTH_SHORT).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        btnToggleSave = view.findViewById(R.id.btnToggleSave);

        TextView txtDetailName = view.findViewById(R.id.txtDetailName);
        TextView txtDetailDistrictProvince = view.findViewById(R.id.txtDetailDistrictProvince);
        TextView txtDetailCategory = view.findViewById(R.id.txtDetailCategory);
        TextView txtDetailCoordinates = view.findViewById(R.id.txtDetailCoordinates);
        TextView txtDetailDescription = view.findViewById(R.id.txtDetailDescription);
        TextView txtDetailTravelTips = view.findViewById(R.id.txtDetailTravelTips);

        txtDetailName.setText(destination.getName());
        txtDetailDistrictProvince.setText(String.format("%s • %s", destination.getDistrict(), destination.getProvince()));
        txtDetailCategory.setText(destination.getCategory());
        txtDetailCoordinates.setText(String.format(Locale.US, "%.4f° N, %.4f° E", destination.getLatitude(), destination.getLongitude()));
        txtDetailDescription.setText(destination.getDescription());
        txtDetailTravelTips.setText(destination.getTravelTips());

        checkSavedStatus();

        btnToggleSave.setOnClickListener(v -> toggleSaveDestination());
        view.findViewById(R.id.btnViewMap).setOnClickListener(v -> launchGoogleMapLocation());
        view.findViewById(R.id.btnGetDirections).setOnClickListener(v -> launchGoogleMapDirections());
        view.findViewById(R.id.btnAddTravelPlan).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Available in the Travel Plans phase (Phase 3-C).", Toast.LENGTH_SHORT).show()
        );
    }

    private void checkSavedStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || destination == null) return;

        savedPlacesRepository.isPlaceSaved(user.getUid(), destination.getId(), new SavedPlacesRepository.CheckCallback() {
            @Override
            public void onResult(boolean isSaved) {
                isSavedInFirestore = isSaved;
                updateHeartIconState();
            }

            @Override
            public void onError(Exception exception) {}
        });
    }

    private void updateHeartIconState() {
        if (btnToggleSave == null || !isAdded()) return;
        if (isSavedInFirestore) {
            btnToggleSave.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_orange));
        } else {
            btnToggleSave.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_tertiary));
        }
    }

    private void toggleSaveDestination() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to save destinations.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSavedInFirestore) {
            savedPlacesRepository.deleteSavedPlace(user.getUid(), destination.getId(), new SavedPlacesRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    isSavedInFirestore = false;
                    updateHeartIconState();
                    Toast.makeText(requireContext(), "Removed from Saved Places.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception exception) {
                    Toast.makeText(requireContext(), "Failed to remove: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            long now = System.currentTimeMillis();
            SavedPlace place = new SavedPlace(
                    destination.getId(),
                    destination.getId(),
                    destination.getName(),
                    destination.getDistrict(),
                    destination.getLatitude(),
                    destination.getLongitude(),
                    "",
                    now,
                    now
            );

            savedPlacesRepository.savePlace(user.getUid(), place, new SavedPlacesRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    isSavedInFirestore = true;
                    updateHeartIconState();
                    Toast.makeText(requireContext(), "Saved to your Places!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception exception) {
                    Toast.makeText(requireContext(), "Failed to save place: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void launchGoogleMapLocation() {
        if (destination == null || !destination.isValidCoordinates()) {
            Toast.makeText(requireContext(), "Invalid destination coordinates.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                destination.getLatitude(), destination.getLongitude(),
                destination.getLatitude(), destination.getLongitude(),
                Uri.encode(destination.getName()));

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            String webUri = String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%f,%f",
                    destination.getLatitude(), destination.getLongitude());
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            startActivity(webIntent);
        }
    }

    private void launchGoogleMapDirections() {
        if (destination == null || !destination.isValidCoordinates()) {
            Toast.makeText(requireContext(), "Invalid destination coordinates.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uri = String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                destination.getLatitude(), destination.getLongitude());

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