package lk.falconplan.app.ui.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.PublicTravelPlan;
import lk.falconplan.app.data.remote.BackendApiClient;

public class PublicListingDetailFragment extends Fragment {

    private static final String ARG_LISTING_ID = "arg_listing_id";

    private BackendApiClient apiClient;
    private String listingId;

    private TextView txtListingTitle;
    private TextView txtTypeLocation;
    private TextView txtProviderName;
    private TextView txtDescription;
    private TextView txtCapacityDetails;
    private ProgressBar progressBar;

    public static PublicListingDetailFragment newInstance(String listingId) {
        PublicListingDetailFragment fragment = new PublicListingDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_listing_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiClient = BackendApiClient.getInstance();
        listingId = getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : "";

        txtListingTitle = view.findViewById(R.id.txtListingTitle);
        txtTypeLocation = view.findViewById(R.id.txtTypeLocation);
        txtProviderName = view.findViewById(R.id.txtProviderName);
        txtDescription = view.findViewById(R.id.txtDescription);
        txtCapacityDetails = view.findViewById(R.id.txtCapacityDetails);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        loadPublicListingDetail();
    }

    private void loadPublicListingDetail() {
        if (listingId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        apiClient.getPublicPlans(1, 10, new BackendApiClient.ApiCallback<List<PublicTravelPlan>>() {
            @Override
            public void onSuccess(List<PublicTravelPlan> result) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    public void setListingData(JSONObject listing) {
        if (listing == null) return;

        String title = listing.optString("title", "Verified Homestay");
        String type = listing.optString("listingType", "homestay").toUpperCase(Locale.US);
        String district = listing.optString("district", "");
        String village = listing.optString("village", "");
        String providerName = listing.optString("publicProviderName", "FalconPlan Host");
        String desc = listing.optString("description", "No description provided.");

        txtListingTitle.setText(title);
        txtTypeLocation.setText(String.format("%s • %s, %s", type, district, village));
        txtProviderName.setText(String.format("Host: %s", providerName));
        txtDescription.setText(desc);

        int maxGuests = listing.optInt("maxGuests", 0);
        int bedrooms = listing.optInt("bedrooms", 0);
        String amenities = listing.optString("amenities", "");

        if (maxGuests > 0 || bedrooms > 0) {
            txtCapacityDetails.setText(String.format(Locale.US, "Capacity: Up to %d guests • %d Bedrooms • Amenities: %s", maxGuests, bedrooms, amenities));
        } else {
            txtCapacityDetails.setText(String.format(Locale.US, "Category: %s • Duration: %d mins", listing.optString("experienceCategory", "Tour"), listing.optInt("durationMinutes", 60)));
        }
    }
}