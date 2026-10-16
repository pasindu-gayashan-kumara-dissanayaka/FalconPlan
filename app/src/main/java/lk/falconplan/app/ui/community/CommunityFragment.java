package lk.falconplan.app.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityPost;
import lk.falconplan.app.data.model.PublicTravelPlan;
import lk.falconplan.app.data.repository.CommunityPostRepository;
import lk.falconplan.app.data.repository.CommunityPostRepositoryImpl;
import lk.falconplan.app.data.repository.TravelPlanRepository;
import lk.falconplan.app.data.repository.TravelPlanRepositoryImpl;
import lk.falconplan.app.ui.plans.PublicPlanAdapter;
import lk.falconplan.app.ui.plans.PublicPlanDetailFragment;

public class CommunityFragment extends Fragment implements PublicPlanAdapter.OnPublicPlanActionListener {

    private CommunityPostAdapter postAdapter;
    private PublicPlanAdapter publicPlanAdapter;
    private TravelPlanRepository travelPlanRepository;

    public static CommunityFragment newInstance() {
        return new CommunityFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        travelPlanRepository = new TravelPlanRepositoryImpl();

        CommunityViewModel viewModel = new ViewModelProvider(this).get(CommunityViewModel.class);

        RecyclerView recyclerPosts = view.findViewById(R.id.recyclerPosts);
        RecyclerView recyclerPublicPlans = view.findViewById(R.id.recyclerPublicPlans);

        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        postAdapter = new CommunityPostAdapter();
        recyclerPosts.setAdapter(postAdapter);
        postAdapter.setOnPostActionListener(new CommunityPostAdapter.OnPostActionListener() {
            @Override
            public void onLikeClick(CommunityPost post) {
                togglePostLike(post);
            }

            @Override
            public void onCommentClick(CommunityPost post) {
                if (post != null && post.getPostId() != null) {
                    CommunityCommentsBottomSheetFragment.newInstance(post.getPostId())
                            .show(getChildFragmentManager(), "comments_dialog");
                }
            }

            @Override
            public void onSaveClick(CommunityPost post) {
                togglePostSave(post);
            }

            @Override
            public void onReportClick(CommunityPost post) {
                Toast.makeText(requireContext(), "Post reported for review.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemClick(CommunityPost post) {
                showPostDetailDialog(post);
            }
        });

        View btnCreatePost = view.findViewById(R.id.btnCreatePost);
        if (btnCreatePost != null) {
            btnCreatePost.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(CreateCommunityPostFragment.newInstance(), true);
                }
            });
        }

        recyclerPublicPlans.setLayoutManager(new LinearLayoutManager(requireContext()));
        publicPlanAdapter = new PublicPlanAdapter();
        recyclerPublicPlans.setAdapter(publicPlanAdapter);
        publicPlanAdapter.setOnPublicPlanActionListener(this);

        loadCommunityFeed();
        loadPublicPlans();
    }

    private void loadCommunityFeed() {
        CommunityPostRepository communityPostRepository = new CommunityPostRepositoryImpl();
        communityPostRepository.getCommunityFeed(1, 20, new CommunityPostRepository.PostsListCallback() {
            @Override
            public void onSuccess(List<CommunityPost> posts) {
                if (postAdapter != null) {
                    postAdapter.setPosts(posts);
                }
            }

            @Override
            public void onError(Exception exception) {
                // Fallback to mock data if network unavailable
                CommunityViewModel viewModel = new ViewModelProvider(CommunityFragment.this).get(CommunityViewModel.class);
                viewModel.getPostsLiveData().observe(getViewLifecycleOwner(), posts -> postAdapter.setPosts(posts));
            }
        });
    }

    private void loadPublicPlans() {
        travelPlanRepository.getPublicPlans(new TravelPlanRepository.PublicPlansCallback() {
            @Override
            public void onSuccess(List<PublicTravelPlan> plans) {
                publicPlanAdapter.setPlans(plans);
            }

            @Override
            public void onError(Exception exception) {
                // Silently ignore or show empty state if network fails
            }
        });
    }

    @Override
    public void onViewClick(PublicTravelPlan plan) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadMainContentFragment(
                    PublicPlanDetailFragment.newInstance(plan.getPublicPlanId()), true
            );
        }
    }

    @Override
    public void onCopyClick(PublicTravelPlan plan) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to copy travel plans.", Toast.LENGTH_SHORT).show();
            return;
        }

        travelPlanRepository.copyPublicPlanToPrivate(user.getUid(), plan, new TravelPlanRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Copied to your private Travel Plans!", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_plans);
                }
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(requireContext(), "Failed to copy plan: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void togglePostSave(CommunityPost post) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to save stories.", Toast.LENGTH_SHORT).show();
            return;
        }

        CommunityPostRepository repo = new CommunityPostRepositoryImpl();
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            CommunityPostRepository.SaveCallback callback = new CommunityPostRepository.SaveCallback() {
                @Override
                public void onSuccess(boolean savedByMe) {
                    post.setSavedByMe(savedByMe);
                    if (postAdapter != null) {
                        postAdapter.notifyDataSetChanged();
                    }
                    Toast.makeText(requireContext(), savedByMe ? "Story Saved!" : "Story Unsaved.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception exception) {
                    Toast.makeText(requireContext(), "Save failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            };

            if (post.isSavedByMe()) {
                repo.unsavePost(idToken, post.getPostId(), callback);
            } else {
                repo.savePost(idToken, post.getPostId(), callback);
            }
        });
    }

    private void togglePostLike(CommunityPost post) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to like travel stories.", Toast.LENGTH_SHORT).show();
            return;
        }

        CommunityPostRepository repo = new CommunityPostRepositoryImpl();
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            CommunityPostRepository.LikeCallback callback = new CommunityPostRepository.LikeCallback() {
                @Override
                public void onSuccess(int likeCount, boolean likedByMe) {
                    post.setLikeCount(likeCount);
                    post.setLikedByMe(likedByMe);
                    if (postAdapter != null) {
                        postAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onError(Exception exception) {
                    Toast.makeText(requireContext(), "Failed to update like: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            };

            if (post.isLikedByMe()) {
                repo.unlikePost(idToken, post.getPostId(), callback);
            } else {
                repo.likePost(idToken, post.getPostId(), callback);
            }
        });
    }

    private void showPostDetailDialog(CommunityPost post) {
        String msg = String.format("%s\n\nAuthor: %s (%s)\nVillage: %s • %s\n\nContent:\n%s\n\n[Design Preview — Community engagement active in Phase 3]",
                post.getTitle(),
                post.getAuthorName(),
                post.getAuthorRole(),
                post.getVillageLocation(),
                post.getTimeAgo(),
                post.getContentSnippet()
        );

        new AlertDialog.Builder(requireContext())
                .setTitle("Design Preview: Community Post")
                .setMessage(msg)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }
}