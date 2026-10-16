package lk.falconplan.app.ui.community;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.CommunityPost;
import lk.falconplan.app.data.repository.CommunityPostRepository;
import lk.falconplan.app.data.repository.CommunityPostRepositoryImpl;

public class CreateCommunityPostFragment extends Fragment {

    private CommunityPostRepository communityPostRepository;
    private EditText editCaption;
    private ProgressBar progressBar;
    private String currentPostId = null;

    private ActivityResultLauncher<String> photoPickerLauncher;

    public static CreateCommunityPostFragment newInstance() {
        return new CreateCommunityPostFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadPhotoUri(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_community_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        communityPostRepository = new CommunityPostRepositoryImpl();

        editCaption = view.findViewById(R.id.editCaption);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnAddPhoto).setOnClickListener(v -> {
            if (currentPostId == null) {
                createDraftAndPickPhoto();
            } else {
                photoPickerLauncher.launch("image/*");
            }
        });

        view.findViewById(R.id.btnPublishPost).setOnClickListener(v -> publishPost());
    }

    private void createDraftAndPickPhoto() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to share a story.", Toast.LENGTH_SHORT).show();
            return;
        }

        String caption = editCaption.getText() != null ? editCaption.getText().toString().trim() : "Travel Story";
        if (caption.isEmpty()) caption = "Travel Story";

        progressBar.setVisibility(View.VISIBLE);
        String finalCaption = caption;
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            communityPostRepository.createPost(idToken, finalCaption, "", new CommunityPostRepository.PostCallback() {
                @Override
                public void onSuccess(CommunityPost post) {
                    progressBar.setVisibility(View.GONE);
                    if (post != null) {
                        currentPostId = post.getPostId();
                        photoPickerLauncher.launch("image/*");
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to create draft: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void uploadPhotoUri(Uri uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentPostId == null || uri == null) return;

        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(requireContext(), "Invalid image file.", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageBytes = baos.toByteArray();

            if (imageBytes.length > 5 * 1024 * 1024) {
                Toast.makeText(requireContext(), "Image file size exceeds 5MB limit.", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            progressBar.setVisibility(View.VISIBLE);
            user.getIdToken(true).addOnCompleteListener(task -> {
                String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                communityPostRepository.uploadPostPhoto(idToken, currentPostId, "image/jpeg", fileBase64, new CommunityPostRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Photo added to story draft!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception exception) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Photo upload failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to read photo: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void publishPost() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to publish.", Toast.LENGTH_SHORT).show();
            return;
        }

        String caption = editCaption.getText() != null ? editCaption.getText().toString().trim() : "";
        if (caption.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a caption for your travel story.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        if (currentPostId == null) {
            // Create draft and immediately publish
            user.getIdToken(true).addOnCompleteListener(task -> {
                String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                communityPostRepository.createPost(idToken, caption, "", new CommunityPostRepository.PostCallback() {
                    @Override
                    public void onSuccess(CommunityPost post) {
                        if (post != null) {
                            publishPostById(idToken, post.getPostId());
                        } else {
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Publish failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            user.getIdToken(true).addOnCompleteListener(task -> {
                String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                publishPostById(idToken, currentPostId);
            });
        }
    }

    private void publishPostById(String idToken, String postId) {
        communityPostRepository.publishPost(idToken, postId, new CommunityPostRepository.PostCallback() {
            @Override
            public void onSuccess(CommunityPost post) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Travel Story Published to Community!", Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Publish failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}