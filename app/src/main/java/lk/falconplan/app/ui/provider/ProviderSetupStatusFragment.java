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

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ProviderProfile;
import lk.falconplan.app.data.remote.BackendApiClient;
import lk.falconplan.app.data.repository.ProviderProfileRepository;
import lk.falconplan.app.data.repository.ProviderProfileRepositoryImpl;
import lk.falconplan.app.ui.auth.AuthViewModel;

public class ProviderSetupStatusFragment extends Fragment {

    private ProviderProfileRepository providerProfileRepository;
    private AuthViewModel authViewModel;

    private TextView txtBusinessName;
    private TextView txtHostName;
    private TextView txtTypeLocation;
    private TextView txtDescription;
    private TextView txtContactPhone;
    private ProgressBar progressBar;

    public static ProviderSetupStatusFragment newInstance() {
        return new ProviderSetupStatusFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_setup_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        providerProfileRepository = new ProviderProfileRepositoryImpl();
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        txtBusinessName = view.findViewById(R.id.txtBusinessName);
        txtHostName = view.findViewById(R.id.txtHostName);
        txtTypeLocation = view.findViewById(R.id.txtTypeLocation);
        txtDescription = view.findViewById(R.id.txtDescription);
        txtContactPhone = view.findViewById(R.id.txtContactPhone);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        View btnGoDashboard = view.findViewById(R.id.btnGoDashboard);
        if (btnGoDashboard != null) {
            btnGoDashboard.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(ProviderDashboardFragment.newInstance(), false);
                }
            });
        }

        view.findViewById(R.id.btnContinueKyc).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(ProviderKycFragment.newInstance(), true);
            }
        });

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(ProviderRegistrationFragment.newInstance(), true);
            }
        });

        view.findViewById(R.id.btnSwitchTraveler).setOnClickListener(v -> {
            authViewModel.updateActiveMode("traveler");
            Toast.makeText(requireContext(), "Switched to Traveler Mode.", Toast.LENGTH_SHORT).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        loadProviderProfile();
    }

    private void loadProviderProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        providerProfileRepository.getProviderProfile(user.getUid(), new ProviderProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(ProviderProfile profile) {
                progressBar.setVisibility(View.GONE);
                if (profile != null) {
                    displayProfileData(profile);
                    fetchKycStatus(user);
                } else {
                    Toast.makeText(requireContext(), "No provider profile found. Please register.", Toast.LENGTH_SHORT).show();
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
                        TextView txtKycStatusBadge = requireView().findViewById(R.id.txtKycStatusBadge);
                        if (txtKycStatusBadge != null) {
                            if ("submitted_under_review".equals(kycStat)) {
                                txtKycStatusBadge.setText("Under Review");
                            } else if ("verified".equals(kycStat)) {
                                txtKycStatusBadge.setText("Verified Host");
                            } else {
                                txtKycStatusBadge.setText("KYC Pending");
                            }
                        }
                    }
                }

                @Override
                public void onError(Exception exception) {}
            });
        });
    }

    private void displayProfileData(ProviderProfile profile) {
        txtBusinessName.setText(profile.getBusinessName());
        txtHostName.setText(String.format("Host: %s", profile.getHostDisplayName()));
        txtTypeLocation.setText(String.format("Type: %s • %s, %s",
                profile.getBusinessType().toUpperCase(),
                profile.getDistrict(),
                profile.getVillage()));

        txtDescription.setText(profile.getShortDescription() != null && !profile.getShortDescription().isEmpty()
                ? profile.getShortDescription() : "No description added.");

        if (profile.getContactPhone() != null && !profile.getContactPhone().isEmpty()) {
            txtContactPhone.setText(String.format("Contact Phone: %s", profile.getContactPhone()));
            txtContactPhone.setVisibility(View.VISIBLE);
        } else {
            txtContactPhone.setVisibility(View.GONE);
        }
    }
}