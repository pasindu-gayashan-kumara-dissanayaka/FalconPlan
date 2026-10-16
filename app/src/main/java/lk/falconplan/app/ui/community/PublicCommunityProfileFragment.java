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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityPost;
import lk.falconplan.app.data.model.CommunityProfile;
import lk.falconplan.app.data.repository.CommunityProfileRepository;
import lk.falconplan.app.data.repository.CommunityProfileRepositoryImpl;

public class PublicCommunityProfileFragment extends Fragment implements CommunityPostAdapter.OnPostActionListener {

    private static final String ARG_PROFILE_ID = "arg_profile_id";

    private CommunityProfileRepository repository;
    private CommunityPostAdapter adapter;
    private String profileId;

    private TextView txtDisplayName;
    private TextView txtVerifiedBadge;
    private TextView txtBio;
    private RecyclerView recyclerProfilePosts;
    private View layoutPrivateOrNotFound;
    private ProgressBar progressBar;

    public static PublicCommunityProfileFragment newInstance(String profileId) {
        PublicCommunityProfileFragment fragment = new PublicCommunityProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROFILE_ID, profileId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_community_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new CommunityProfileRepositoryImpl();
        profileId = getArguments() != null ? getArguments().getString(ARG_PROFILE_ID) : "";

        txtDisplayName = view.findViewById(R.id.txtDisplayName);
        txtVerifiedBadge = view.findViewById(R.id.txtVerifiedBadge);
        txtBio = view.findViewById(R.id.txtBio);
        recyclerProfilePosts = view.findViewById(R.id.recyclerProfilePosts);
        layoutPrivateOrNotFound = view.findViewById(R.id.layoutPrivateOrNotFound);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recyclerProfilePosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CommunityPostAdapter();
        recyclerProfilePosts.setAdapter(adapter);
        adapter.setOnPostActionListener(this);

        loadPublicProfile();
    }

    private void loadPublicProfile() {
        if (profileId == null || profileId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        repository.getPublicProfile(profileId, new CommunityProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(CommunityProfile profile) {
                progressBar.setVisibility(View.GONE);
                if (profile != null) {
                    displayProfileHeader(profile);
                    loadProfilePosts();
                } else {
                    showPrivateOrNotFoundState();
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                showPrivateOrNotFoundState();
            }
        });
    }

    private void displayProfileHeader(CommunityProfile profile) {
        txtDisplayName.setText(profile.getDisplayName());
        txtBio.setText(profile.getBio().isEmpty() ? "No bio provided." : profile.getBio());

        if (profile.isVerifiedHost()) {
            txtVerifiedBadge.setVisibility(View.VISIBLE);
        } else {
            txtVerifiedBadge.setVisibility(View.GONE);
        }
    }

    private void loadProfilePosts() {
        repository.getPublicProfilePosts(profileId, 1, 20, new CommunityProfileRepository.PostsListCallback() {
            @Override
            public void onSuccess(List<CommunityPost> posts) {
                adapter.setItems(posts);
            }

            @Override
            public void onError(Exception exception) {
                // Silently ignore or show empty state
            }
        });
    }

    private void showPrivateOrNotFoundState() {
        layoutPrivateOrNotFound.setVisibility(View.VISIBLE);
        recyclerProfilePosts.setVisibility(View.GONE);
    }

    @Override
    public void onLikeClick(CommunityPost post) {
        // Likes active on main feed
    }

    @Override
    public void onSaveClick(CommunityPost post) {
        // Save active on main feed
    }

    @Override
    public void onCommentClick(CommunityPost post) {
        if (post != null && post.getPostId() != null) {
            CommunityCommentsBottomSheetFragment.newInstance(post.getPostId())
                    .show(getChildFragmentManager(), "profile_comments_dialog");
        }
    }

    @Override
    public void onReportClick(CommunityPost post) {
        Toast.makeText(requireContext(), "Post reported for review.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(CommunityPost post) {
        // Optional detail view
    }
}