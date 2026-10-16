package lk.falconplan.app.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityPost;
import lk.falconplan.app.data.repository.CommunityPostRepository;
import lk.falconplan.app.data.repository.CommunityPostRepositoryImpl;

public class MyCommunityPostsFragment extends Fragment implements CommunityPostAdapter.OnPostActionListener {

    private CommunityPostRepository communityPostRepository;
    private CommunityPostAdapter adapter;

    private RecyclerView recyclerPosts;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static MyCommunityPostsFragment newInstance() {
        return new MyCommunityPostsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_community_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        communityPostRepository = new CommunityPostRepositoryImpl();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CommunityPostAdapter();
        recyclerPosts.setAdapter(adapter);
        adapter.setOnPostActionListener(this);

        loadTravelerPosts();
    }

    private void loadTravelerPosts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            communityPostRepository.getTravelerPosts(idToken, new CommunityPostRepository.PostsListCallback() {
                @Override
                public void onSuccess(List<CommunityPost> posts) {
                    progressBar.setVisibility(View.GONE);
                    adapter.setItems(posts);

                    if (posts == null || posts.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerPosts.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerPosts.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load community posts: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onReportClick(CommunityPost post) {
        Toast.makeText(requireContext(), "Post reported for review.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(CommunityPost post) {
        // Optional detail view or edit
    }
}