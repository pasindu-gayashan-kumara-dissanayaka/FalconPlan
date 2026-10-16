package lk.falconplan.app.ui.community;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityProfile;
import lk.falconplan.app.data.repository.CommunityProfileRepository;
import lk.falconplan.app.data.repository.CommunityProfileRepositoryImpl;

public class MyCommunityProfileFragment extends Fragment {

    private CommunityProfileRepository repository;

    private TextView txtDisplayName;
    private TextView txtVisibilityBadge;
    private TextView txtBio;
    private ProgressBar progressBar;

    public static MyCommunityProfileFragment newInstance() {
        return new MyCommunityProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_community_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new CommunityProfileRepositoryImpl();

        txtDisplayName = view.findViewById(R.id.txtDisplayName);
        txtVisibilityBadge = view.findViewById(R.id.txtVisibilityBadge);
        txtBio = view.findViewById(R.id.txtBio);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(EditCommunityProfileFragment.newInstance(), true);
            }
        });

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
                        displayProfile(profile);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load profile: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void displayProfile(CommunityProfile profile) {
        txtDisplayName.setText(profile.getDisplayName());
        txtVisibilityBadge.setText(profile.getVisibility().toUpperCase(Locale.US));
        txtBio.setText(profile.getBio().isEmpty() ? "No bio provided." : profile.getBio());
    }
}