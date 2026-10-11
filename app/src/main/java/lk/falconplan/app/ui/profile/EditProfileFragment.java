package lk.falconplan.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.UserProfile;
import lk.falconplan.app.data.repository.UserRepository;
import lk.falconplan.app.data.repository.UserRepositoryImpl;
import lk.falconplan.app.ui.auth.AuthViewModel;

public class EditProfileFragment extends Fragment {

    private AuthViewModel authViewModel;
    private UserRepository userRepository;

    private EditText editDisplayName;
    private EditText editEmail;
    private ProgressBar progressBar;

    public static EditProfileFragment newInstance() {
        return new EditProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        userRepository = new UserRepositoryImpl();

        editDisplayName = view.findViewById(R.id.editDisplayName);
        editEmail = view.findViewById(R.id.editEmail);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnCancelProfile).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            editEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            editDisplayName.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
            userRepository.getUserProfile(user.getUid(), new UserRepository.ProfileCallback() {
                @Override
                public void onSuccess(UserProfile profile) {
                    if (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().isEmpty()) {
                        editDisplayName.setText(profile.getDisplayName());
                    }
                }

                @Override
                public void onError(Exception exception) {}
            });
        }

        view.findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfileChanges(user));
    }

    private void saveProfileChanges(FirebaseUser user) {
        if (user == null) return;

        String newName = editDisplayName.getText() != null ? editDisplayName.getText().toString().trim() : "";
        if (newName.isEmpty()) {
            Toast.makeText(requireContext(), "Display name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            userRepository.getUserProfile(user.getUid(), new UserRepository.ProfileCallback() {
                @Override
                public void onSuccess(UserProfile existingProfile) {
                    long now = System.currentTimeMillis();
                    String activeMode = (existingProfile != null && existingProfile.getActiveMode() != null)
                            ? existingProfile.getActiveMode() : UserProfile.MODE_TRAVELER;
                    long createdAt = (existingProfile != null && existingProfile.getCreatedAt() > 0)
                            ? existingProfile.getCreatedAt() : now;

                    UserProfile updatedProfile = new UserProfile(
                            user.getUid(),
                            newName,
                            user.getEmail(),
                            user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                            activeMode,
                            createdAt,
                            now
                    );

                    userRepository.createUserProfile(updatedProfile, new UserRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            authViewModel.loadProfile(user.getUid());
                            Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }

                        @Override
                        public void onError(Exception exception) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Failed to update Firestore profile: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to fetch profile: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}