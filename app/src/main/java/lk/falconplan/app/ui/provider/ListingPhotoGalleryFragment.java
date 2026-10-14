package lk.falconplan.app.ui.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.falconplan.app.R;
import lk.falconplan.app.data.repository.ListingPhotoRepository;
import lk.falconplan.app.data.repository.ListingPhotoRepositoryImpl;

public class ListingPhotoGalleryFragment extends Fragment implements ListingPhotoAdapter.OnPhotoActionListener {

    private static final String ARG_LISTING_ID = "arg_listing_id";

    private ListingPhotoRepository photoRepository;
    private ListingPhotoAdapter adapter;
    private ExecutorService executor;

    private String listingId;
    private RecyclerView recyclerPhotos;
    private View layoutEmpty;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<String> getContentLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processAndUploadPhoto(uri);
                }
            });

    public static ListingPhotoGalleryFragment newInstance(String listingId) {
        ListingPhotoGalleryFragment fragment = new ListingPhotoGalleryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_listing_photo_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        photoRepository = new ListingPhotoRepositoryImpl();
        executor = Executors.newSingleThreadExecutor();
        listingId = getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : "";

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        View.OnClickListener addListener = v -> getContentLauncher.launch("image/*");
        view.findViewById(R.id.btnAddPhoto).setOnClickListener(addListener);
        view.findViewById(R.id.btnAddFirstPhoto).setOnClickListener(addListener);

        recyclerPhotos = view.findViewById(R.id.recyclerPhotos);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerPhotos.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ListingPhotoAdapter();
        recyclerPhotos.setAdapter(adapter);
        adapter.setOnPhotoActionListener(this);

        loadPhotos();
    }

    private void loadPhotos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || listingId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            photoRepository.getListingPhotos(idToken, listingId, new ListingPhotoRepository.PhotosCallback() {
                @Override
                public void onSuccess(JSONArray photos) {
                    progressBar.setVisibility(View.GONE);
                    List<JSONObject> list = new ArrayList<>();
                    if (photos != null) {
                        for (int i = 0; i < photos.length(); i++) {
                            JSONObject p = photos.optJSONObject(i);
                            if (p != null) list.add(p);
                        }
                    }

                    adapter.setItems(list);
                    if (list.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerPhotos.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerPhotos.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load photos: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void processAndUploadPhoto(Uri uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                ContentResolver resolver = requireContext().getContentResolver();
                String mimeType = resolver.getType(uri);
                if (mimeType == null) mimeType = "image/jpeg";

                InputStream is = resolver.openInputStream(uri);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[8192];
                int bytesRead;
                int totalBytes = 0;

                while ((bytesRead = is.read(data, 0, data.length)) != -1) {
                    totalBytes += bytesRead;
                    if (totalBytes > 5 * 1024 * 1024) {
                        mainHandlerPostToast("Selected photo exceeds 5MB limit.");
                        return;
                    }
                    buffer.write(data, 0, bytesRead);
                }

                byte[] bytes = buffer.toByteArray();
                String base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP);

                String finalMimeType = mimeType;
                user.getIdToken(true).addOnCompleteListener(task -> {
                    String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                    photoRepository.uploadListingPhoto(idToken, listingId, finalMimeType, base64Str, false, new ListingPhotoRepository.UploadCallback() {
                        @Override
                        public void onSuccess(JSONObject photoData) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Photo uploaded successfully!", Toast.LENGTH_SHORT).show();
                            loadPhotos();
                        }

                        @Override
                        public void onError(Exception exception) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Upload failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });

            } catch (Exception e) {
                mainHandlerPostToast("Failed to process photo: " + e.getLocalizedMessage());
            }
        });
    }

    @Override
    public void onSetCoverClick(JSONObject photo) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String photoId = photo != null ? photo.optString("photoId", "") : "";
        if (user == null || photoId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            photoRepository.setCoverPhoto(idToken, listingId, photoId, new ListingPhotoRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Cover photo updated!", Toast.LENGTH_SHORT).show();
                    loadPhotos();
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to set cover: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onDeleteClick(JSONObject photo) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String photoId = photo != null ? photo.optString("photoId", "") : "";
        if (user == null || photoId.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this listing photo?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        photoRepository.deleteListingPhoto(idToken, listingId, photoId, new ListingPhotoRepository.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Photo deleted.", Toast.LENGTH_SHORT).show();
                                loadPhotos();
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

    private void mainHandlerPostToast(String msg) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}