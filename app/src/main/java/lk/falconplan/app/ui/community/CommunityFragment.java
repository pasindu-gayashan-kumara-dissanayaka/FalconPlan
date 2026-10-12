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
        postAdapter.setOnPostClickListener(this::showPostDetailDialog);

        recyclerPublicPlans.setLayoutManager(new LinearLayoutManager(requireContext()));
        publicPlanAdapter = new PublicPlanAdapter();
        recyclerPublicPlans.setAdapter(publicPlanAdapter);
        publicPlanAdapter.setOnPublicPlanActionListener(this);

        viewModel.getPostsLiveData().observe(getViewLifecycleOwner(), posts -> postAdapter.setPosts(posts));

        loadPublicPlans();
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