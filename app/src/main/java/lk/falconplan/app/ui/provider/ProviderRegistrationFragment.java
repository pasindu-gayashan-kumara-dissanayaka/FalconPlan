package lk.falconplan.app.ui.provider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ProviderProfile;
import lk.falconplan.app.data.repository.ProviderProfileRepository;
import lk.falconplan.app.data.repository.ProviderProfileRepositoryImpl;
import lk.falconplan.app.ui.auth.AuthViewModel;

public class ProviderRegistrationFragment extends Fragment {

    private ProviderProfileRepository providerProfileRepository;
    private AuthViewModel authViewModel;

    private EditText editHostDisplayName;
    private EditText editBusinessName;
    private RadioGroup radioGroupType;
    private EditText editDistrict;
    private EditText editVillage;
    private EditText editDescription;
    private EditText editContactPhone;
    private CheckBox checkTerms;
    private ProgressBar progressBar;

    public static ProviderRegistrationFragment newInstance() {
        return new ProviderRegistrationFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        providerProfileRepository = new ProviderProfileRepositoryImpl();
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        editHostDisplayName = view.findViewById(R.id.editHostDisplayName);
        editBusinessName = view.findViewById(R.id.editBusinessName);
        radioGroupType = view.findViewById(R.id.radioGroupType);
        editDistrict = view.findViewById(R.id.editDistrict);
        editVillage = view.findViewById(R.id.editVillage);
        editDescription = view.findViewById(R.id.editDescription);
        editContactPhone = view.findViewById(R.id.editContactPhone);
        checkTerms = view.findViewById(R.id.checkTerms);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            editHostDisplayName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
        }

        view.findViewById(R.id.btnCompleteRegistration).setOnClickListener(v -> completeRegistration(currentUser));
    }

    private void completeRegistration(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to register as a provider.", Toast.LENGTH_SHORT).show();
            return;
        }

        String hostName = editHostDisplayName.getText() != null ? editHostDisplayName.getText().toString().trim() : "";
        String businessName = editBusinessName.getText() != null ? editBusinessName.getText().toString().trim() : "";
        String district = editDistrict.getText() != null ? editDistrict.getText().toString().trim() : "";
        String village = editVillage.getText() != null ? editVillage.getText().toString().trim() : "";
        String description = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";
        String phone = editContactPhone.getText() != null ? editContactPhone.getText().toString().trim() : "";

        String businessType = ProviderProfile.TYPE_HOMESTAY;
        int checkedTypeId = radioGroupType.getCheckedRadioButtonId();
        if (checkedTypeId == R.id.radioExperience) {
            businessType = ProviderProfile.TYPE_EXPERIENCE;
        } else if (checkedTypeId == R.id.radioBoth) {
            businessType = ProviderProfile.TYPE_BOTH;
        }

        if (hostName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter Host Display Name.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (businessName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter Business/Homestay Name.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (district.isEmpty() || village.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter District and Village location.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkTerms.isChecked()) {
            Toast.makeText(requireContext(), "Please agree to host guidelines and terms.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        long now = System.currentTimeMillis();
        ProviderProfile profile = new ProviderProfile(
                user.getUid(),
                hostName,
                businessName,
                businessType,
                district,
                village,
                description,
                phone,
                ProviderProfile.KYC_PENDING,
                now,
                now
        );

        providerProfileRepository.createProviderProfile(profile, new ProviderProfileRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                authViewModel.updateActiveMode("provider");
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Provider Registration Complete!", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(ProviderSetupStatusFragment.newInstance(), false);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Registration failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}