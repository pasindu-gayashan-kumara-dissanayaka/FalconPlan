package lk.falconplan.app.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityPost;

public class CommunityFragment extends Fragment {

    private CommunityViewModel viewModel;
    private CommunityPostAdapter adapter;

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

        viewModel = new ViewModelProvider(this).get(CommunityViewModel.class);

        RecyclerView recyclerPosts = view.findViewById(R.id.recyclerPosts);
        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CommunityPostAdapter();
        recyclerPosts.setAdapter(adapter);

        adapter.setOnPostClickListener(this::showPostDetailDialog);

        viewModel.getPostsLiveData().observe(getViewLifecycleOwner(), posts -> {
            adapter.setPosts(posts);
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