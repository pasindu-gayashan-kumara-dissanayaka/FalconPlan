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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityComment;
import lk.falconplan.app.data.repository.CommunityCommentRepository;
import lk.falconplan.app.data.repository.CommunityCommentRepositoryImpl;

public class CommunityCommentsBottomSheetFragment extends BottomSheetDialogFragment implements CommunityCommentAdapter.OnCommentActionListener {

    private static final String ARG_POST_ID = "arg_post_id";

    private CommunityCommentRepository commentRepository;
    private CommunityCommentAdapter adapter;
    private String postId;

    private RecyclerView recyclerComments;
    private EditText editCommentText;
    private ProgressBar progressBar;

    public static CommunityCommentsBottomSheetFragment newInstance(String postId) {
        CommunityCommentsBottomSheetFragment fragment = new CommunityCommentsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        commentRepository = new CommunityCommentRepositoryImpl();
        postId = getArguments() != null ? getArguments().getString(ARG_POST_ID) : "";

        recyclerComments = view.findViewById(R.id.recyclerComments);
        editCommentText = view.findViewById(R.id.editCommentText);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnSendComment).setOnClickListener(v -> sendComment());

        recyclerComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CommunityCommentAdapter();
        recyclerComments.setAdapter(adapter);
        adapter.setOnCommentActionListener(this);

        loadComments();
    }

    private void loadComments() {
        if (postId == null || postId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        commentRepository.getComments(postId, 1, 20, new CommunityCommentRepository.CommentsListCallback() {
            @Override
            public void onSuccess(List<CommunityComment> comments) {
                progressBar.setVisibility(View.GONE);
                adapter.setItems(comments);
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load comments: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendComment() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to comment.", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = editCommentText.getText() != null ? editCommentText.getText().toString().trim() : "";
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "Comment text cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            commentRepository.createComment(idToken, postId, text, new CommunityCommentRepository.CommentCallback() {
                @Override
                public void onSuccess(CommunityComment comment) {
                    progressBar.setVisibility(View.GONE);
                    editCommentText.setText("");
                    Toast.makeText(requireContext(), "Comment added!", Toast.LENGTH_SHORT).show();
                    loadComments();
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to add comment: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onEditClick(CommunityComment comment) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || comment == null) return;

        final EditText editInput = new EditText(requireContext());
        editInput.setText(comment.getText());

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Edit Comment")
                .setView(editInput)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newText = editInput.getText().toString().trim();
                    if (newText.isEmpty()) return;

                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        commentRepository.editComment(idToken, postId, comment.getCommentId(), newText, new CommunityCommentRepository.CommentCallback() {
                            @Override
                            public void onSuccess(CommunityComment updatedComment) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Comment updated!", Toast.LENGTH_SHORT).show();
                                loadComments();
                            }

                            @Override
                            public void onError(Exception exception) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Edit failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDeleteClick(CommunityComment comment) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || comment == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        commentRepository.deleteComment(idToken, postId, comment.getCommentId(), new CommunityCommentRepository.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Comment deleted.", Toast.LENGTH_SHORT).show();
                                loadComments();
                            }

                            @Override
                            public void onError(Exception exception) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Delete failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onReportClick(CommunityComment comment) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || comment == null) return;

        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            commentRepository.reportComment(idToken, postId, comment.getCommentId(), "Inappropriate content", new CommunityCommentRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(requireContext(), "Comment reported for review.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception exception) {
                    Toast.makeText(requireContext(), "Report failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}