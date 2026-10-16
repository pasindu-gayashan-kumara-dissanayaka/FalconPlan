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

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityProfile;
import lk.falconplan.app.data.repository.CommunityProfileRepository;
import lk.falconplan.app.data.repository.CommunityProfileRepositoryImpl;

public class FollowersListFragment extends Fragment implements UserSummaryAdapter.OnUserClickListener {

    public static final String MODE_FOLLOWERS = "mode_followers";
    public static final String MODE_FOLLOWING = "mode_following";

    private static final String ARG_TARGET_UID = "arg_target_uid";
    private static final String ARG_MODE = "arg_mode";

    private CommunityProfileRepository repository;
    private UserSummaryAdapter adapter;
    private String targetUid;
    private String mode;

    private TextView txtHeaderTitle;
    private RecyclerView recyclerUsers;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static FollowersListFragment newInstance(String targetUid, String mode) {
        FollowersListFragment fragment = new FollowersListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TARGET_UID, targetUid);
        args.putString(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_followers_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new CommunityProfileRepositoryImpl();
        targetUid = getArguments() != null ? getArguments().getString(ARG_TARGET_UID) : "";
        mode = getArguments() != null ? getArguments().getString(ARG_MODE, MODE_FOLLOWERS) : MODE_FOLLOWERS;

        txtHeaderTitle = view.findViewById(R.id.txtHeaderTitle);
        recyclerUsers = view.findViewById(R.id.recyclerUsers);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        txtHeaderTitle.setText(MODE_FOLLOWERS.equals(mode) ? "Followers" : "Following");

        recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UserSummaryAdapter();
        recyclerUsers.setAdapter(adapter);
        adapter.setOnUserClickListener(this);

        loadList();
    }

    private void loadList() {
        if (targetUid == null || targetUid.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        CommunityProfileRepository.UserSummariesCallback callback = new CommunityProfileRepository.UserSummariesCallback() {
            @Override
            public void onSuccess(List<CommunityProfile> profiles) {
                progressBar.setVisibility(View.GONE);
                adapter.setItems(profiles);

                if (profiles == null || profiles.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerUsers.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerUsers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load profiles: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (MODE_FOLLOWERS.equals(mode)) {
            repository.getFollowers(targetUid, 1, 20, callback);
        } else {
            repository.getFollowing(targetUid, 1, 20, callback);
        }
    }

    @Override
    public void onUserClick(CommunityProfile profile) {
        if (getActivity() instanceof MainActivity && profile != null && profile.getOwnerUid() != null) {
            ((MainActivity) getActivity()).loadMainContentFragment(
                    PublicCommunityProfileFragment.newInstance(profile.getOwnerUid()), true
            );
        }
    }
}