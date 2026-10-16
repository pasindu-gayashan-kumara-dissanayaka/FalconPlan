package lk.falconplan.app.ui.community;

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

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityProfile;
import lk.falconplan.app.data.repository.CommunityProfileRepository;
import lk.falconplan.app.data.repository.CommunityProfileRepositoryImpl;

public class EditCommunityProfileFragment extends Fragment {

    private CommunityProfileRepository repository;

    private EditText editDisplayName;
    private EditText editBio;
    private MaterialSwitch switchVisibility;
    private ProgressBar progressBar;

    public static EditCommunityProfileFragment newInstance() {
        return new EditCommunityProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_community_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new CommunityProfileRepositoryImpl();

        editDisplayName = view.findViewById(R.id.editDisplayName);
        editBio = view.findViewById(R.id.editBio);
        switchVisibility = view.findViewById(R.id.switchVisibility);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());

        loadOwnProfile();
    }

    private void loadOwnProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            repository.getOwnProfile(idToken, new CommunityProfileRepository.ProfileCallback() {
                @Override
                public void onSuccess(CommunityProfile profile) {
                    progressBar.setVisibility(View.GONE);
                    if (profile != null) {
                        editDisplayName.setText(profile.getDisplayName());
                        editBio.setText(profile.getBio());
                        switchVisibility.setChecked(profile.isPublic());
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                }
            });
        });
    }

    private void saveProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String name = editDisplayName.getText() != null ? editDisplayName.getText().toString().trim() : "";
        String bio = editBio.getText() != null ? editBio.getText().toString().trim() : "";
        String visibility = switchVisibility.isChecked() ? CommunityProfile.VISIBILITY_PUBLIC : CommunityProfile.VISIBILITY_PRIVATE;

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Display name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            repository.updateOwnProfile(idToken, name, bio, "", visibility, new CommunityProfileRepository.ProfileCallback() {
                @Override
                public void onSuccess(CommunityProfile profile) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Community Profile Updated!", Toast.LENGTH_SHORT).show();
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to update profile: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}