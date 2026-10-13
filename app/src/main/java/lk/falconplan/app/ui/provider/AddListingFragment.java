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
import lk.falconplan.app.data.model.ProviderListing;
import lk.falconplan.app.data.repository.ProviderListingRepository;
import lk.falconplan.app.data.repository.ProviderListingRepositoryImpl;

public class AddListingFragment extends Fragment {

    private static final String ARG_EDIT_LISTING_ID = "arg_edit_listing_id";

    private ProviderListingRepository listingRepository;
    private String editListingId;

    private RadioGroup radioGroupListingType;
    private View layoutHomestayFields;
    private View layoutExperienceFields;

    private EditText editTitle;
    private EditText editDescription;
    private EditText editDistrict;
    private EditText editVillage;

    private EditText editMaxGuests;
    private EditText editBedrooms;
    private EditText editAmenities;

    private EditText editDurationMinutes;
    private EditText editMaxParticipants;
    private EditText editExperienceCategory;

    private ProgressBar progressBar;
    private ProviderListing existingListing;

    public static AddListingFragment newInstance() {
        return new AddListingFragment();
    }

    public static AddListingFragment newInstance(String listingId) {
        AddListingFragment fragment = new AddListingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EDIT_LISTING_ID, listingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_listing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listingRepository = new ProviderListingRepositoryImpl();
        editListingId = getArguments() != null ? getArguments().getString(ARG_EDIT_LISTING_ID) : null;

        radioGroupListingType = view.findViewById(R.id.radioGroupListingType);
        layoutHomestayFields = view.findViewById(R.id.layoutHomestayFields);
        layoutExperienceFields = view.findViewById(R.id.layoutExperienceFields);

        editTitle = view.findViewById(R.id.editTitle);
        editDescription = view.findViewById(R.id.editDescription);
        editDistrict = view.findViewById(R.id.editDistrict);
        editVillage = view.findViewById(R.id.editVillage);

        editMaxGuests = view.findViewById(R.id.editMaxGuests);
        editBedrooms = view.findViewById(R.id.editBedrooms);
        editAmenities = view.findViewById(R.id.editAmenities);

        editDurationMinutes = view.findViewById(R.id.editDurationMinutes);
        editMaxParticipants = view.findViewById(R.id.editMaxParticipants);
        editExperienceCategory = view.findViewById(R.id.editExperienceCategory);

        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        radioGroupListingType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioExperience) {
                layoutHomestayFields.setVisibility(View.GONE);
                layoutExperienceFields.setVisibility(View.VISIBLE);
            } else {
                layoutHomestayFields.setVisibility(View.VISIBLE);
                layoutExperienceFields.setVisibility(View.GONE);
            }
        });

        view.findViewById(R.id.btnSaveDraft).setOnClickListener(v -> saveDraftListing());

        if (editListingId != null && !editListingId.isEmpty()) {
            loadExistingListing();
        }
    }

    private void loadExistingListing() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || editListingId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        listingRepository.getListingDetails(user.getUid(), editListingId, new ProviderListingRepository.ListingCallback() {
            @Override
            public void onSuccess(ProviderListing listing) {
                progressBar.setVisibility(View.GONE);
                if (listing != null) {
                    existingListing = listing;
                    populateFields(listing);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load listing: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFields(ProviderListing listing) {
        if (ProviderListing.TYPE_EXPERIENCE.equals(listing.getListingType())) {
            radioGroupListingType.check(R.id.radioExperience);
            layoutHomestayFields.setVisibility(View.GONE);
            layoutExperienceFields.setVisibility(View.VISIBLE);
        } else {
            radioGroupListingType.check(R.id.radioHomestay);
            layoutHomestayFields.setVisibility(View.VISIBLE);
            layoutExperienceFields.setVisibility(View.GONE);
        }

        editTitle.setText(listing.getTitle());
        editDescription.setText(listing.getDescription());
        editDistrict.setText(listing.getDistrict());
        editVillage.setText(listing.getVillage());

        editMaxGuests.setText(String.valueOf(listing.getMaxGuests()));
        editBedrooms.setText(String.valueOf(listing.getBedrooms()));
        editAmenities.setText(listing.getAmenities());

        editDurationMinutes.setText(String.valueOf(listing.getDurationMinutes()));
        editMaxParticipants.setText(String.valueOf(listing.getMaxParticipants()));
        editExperienceCategory.setText(listing.getExperienceCategory());
    }

    private void saveDraftListing() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
        String description = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";
        String district = editDistrict.getText() != null ? editDistrict.getText().toString().trim() : "";
        String village = editVillage.getText() != null ? editVillage.getText().toString().trim() : "";

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter Listing Title.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (district.isEmpty() || village.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter District and Village.", Toast.LENGTH_SHORT).show();
            return;
        }

        String listingType = radioGroupListingType.getCheckedRadioButtonId() == R.id.radioExperience
                ? ProviderListing.TYPE_EXPERIENCE : ProviderListing.TYPE_HOMESTAY;

        int maxGuests = parseIntSafe(editMaxGuests.getText() != null ? editMaxGuests.getText().toString() : "0");
        int bedrooms = parseIntSafe(editBedrooms.getText() != null ? editBedrooms.getText().toString() : "0");
        String amenities = editAmenities.getText() != null ? editAmenities.getText().toString().trim() : "";

        int durationMinutes = parseIntSafe(editDurationMinutes.getText() != null ? editDurationMinutes.getText().toString() : "0");
        int maxParticipants = parseIntSafe(editMaxParticipants.getText() != null ? editMaxParticipants.getText().toString() : "0");
        String experienceCategory = editExperienceCategory.getText() != null ? editExperienceCategory.getText().toString().trim() : "";

        progressBar.setVisibility(View.VISIBLE);
        long now = System.currentTimeMillis();

        ProviderListing listing = new ProviderListing(
                editListingId,
                user.getUid(),
                listingType,
                title,
                description,
                district,
                village,
                ProviderListing.STATUS_DRAFT,
                existingListing != null ? existingListing.getCreatedAt() : now,
                now,
                maxGuests,
                bedrooms,
                amenities,
                durationMinutes,
                maxParticipants,
                experienceCategory
        );

        ProviderListingRepository.OperationCallback callback = new ProviderListingRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Private Draft Saved Successfully!", Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to save draft: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (editListingId != null && !editListingId.isEmpty()) {
            listingRepository.updateListing(user.getUid(), listing, callback);
        } else {
            listingRepository.createListing(user.getUid(), listing, callback);
        }
    }

    private int parseIntSafe(String val) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}