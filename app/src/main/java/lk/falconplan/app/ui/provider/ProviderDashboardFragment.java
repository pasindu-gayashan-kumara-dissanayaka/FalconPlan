package lk.falconplan.app.ui.provider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.util.List;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ProviderListing;
import lk.falconplan.app.data.model.ProviderProfile;
import lk.falconplan.app.data.remote.BackendApiClient;
import lk.falconplan.app.data.repository.ProviderListingRepository;
import lk.falconplan.app.data.repository.ProviderListingRepositoryImpl;
import lk.falconplan.app.data.repository.ProviderProfileRepository;
import lk.falconplan.app.data.repository.ProviderProfileRepositoryImpl;
import lk.falconplan.app.ui.auth.AuthViewModel;

public class ProviderDashboardFragment extends Fragment {

    private ProviderProfileRepository providerProfileRepository;
    private ProviderListingRepository listingRepository;
    private AuthViewModel authViewModel;

    private TextView txtBusinessName;
    private TextView txtHostName;
    private TextView txtLocation;
    private TextView txtKycStatusBadge;
    private TextView txtDraftCount;
    private ProgressBar progressBar;

    public static ProviderDashboardFragment newInstance() {
        return new ProviderDashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        providerProfileRepository = new ProviderProfileRepositoryImpl();
        listingRepository = new ProviderListingRepositoryImpl();
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        txtBusinessName = view.findViewById(R.id.txtBusinessName);
        txtHostName = view.findViewById(R.id.txtHostName);
        txtLocation = view.findViewById(R.id.txtLocation);
        txtKycStatusBadge = view.findViewById(R.id.txtKycStatusBadge);
        txtDraftCount = view.findViewById(R.id.txtDraftCount);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        view.findViewById(R.id.btnAddListing).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(AddListingFragment.newInstance(), true);
            }
        });

        view.findViewById(R.id.btnManageListings).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(ProviderListingsFragment.newInstance(), true);
            }
        });

        view.findViewById(R.id.btnKycShortcut).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(ProviderKycFragment.newInstance(), true);
            }
        });

        view.findViewById(R.id.btnEditBusinessProfile).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(ProviderRegistrationFragment.newInstance(), true);
            }
        });

        view.findViewById(R.id.btnSwitchTraveler).setOnClickListener(v -> {
            authViewModel.updateActiveMode("traveler");
            Toast.makeText(requireContext(), "Switched to Traveler Mode.", Toast.LENGTH_SHORT).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        loadDashboardData();
    }

    private void loadDashboardData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        providerProfileRepository.getProviderProfile(user.getUid(), new ProviderProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(ProviderProfile profile) {
                progressBar.setVisibility(View.GONE);
                if (profile != null) {
                    displayProfile(profile);
                    fetchKycStatus(user);
                    fetchListingsCount(user.getUid());
                } else {
                    Toast.makeText(requireContext(), "Please complete provider registration.", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).loadMainContentFragment(ProviderRegistrationFragment.newInstance(), false);
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load provider profile: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchKycStatus(FirebaseUser user) {
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            BackendApiClient.getInstance().getKycStatus(idToken, new BackendApiClient.ApiCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (result != null && result.has("status") && isAdded()) {
                        String kycStat = result.optString("status", "pending_identity_verification");
                        if ("submitted_under_review".equals(kycStat)) {
                            txtKycStatusBadge.setText("Under Review");
                        } else if ("verified".equals(kycStat)) {
                            txtKycStatusBadge.setText("Verified Host");
                        } else {
                            txtKycStatusBadge.setText("KYC Pending");
                        }
                    }
                }

                @Override
                public void onError(Exception exception) {}
            });
        });
    }

    private void fetchListingsCount(String providerUid) {
        listingRepository.getProviderListings(providerUid, new ProviderListingRepository.ListingsCallback() {
            @Override
            public void onSuccess(List<ProviderListing> listings) {
                if (isAdded()) {
                    int count = listings != null ? listings.size() : 0;
                    txtDraftCount.setText(String.format("%d Private Draft Listings Prepared", count));
                }
            }

            @Override
            public void onError(Exception exception) {
                if (isAdded()) {
                    txtDraftCount.setText("0 Private Draft Listings Prepared");
                }
            }
        });
    }

    private void displayProfile(ProviderProfile profile) {
        txtBusinessName.setText(profile.getBusinessName());
        txtHostName.setText(String.format("Host: %s", profile.getHostDisplayName()));
        txtLocation.setText(String.format("%s, %s", profile.getDistrict(), profile.getVillage()));
    }
}