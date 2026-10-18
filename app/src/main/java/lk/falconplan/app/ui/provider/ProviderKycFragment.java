package lk.falconplan.app.ui.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.repository.KycRepository;
import lk.falconplan.app.data.repository.KycRepositoryImpl;

public class ProviderKycFragment extends Fragment {

    private KycRepository kycRepository;
    private ExecutorService executor;

    private RadioGroup radioGroupDocType;
    private RadioButton radioNic;
    private RadioButton radioPassport;
    private View layoutNicUploads;
    private View cardPassportUpload;

    private TextView txtStatusNicFront;
    private TextView txtStatusNicBack;
    private TextView txtStatusPassport;
    private ProgressBar progressBar;

    private String selectedPendingDocType = "nic_front";
    private boolean isFrontUploaded = false;
    private boolean isBackUploaded = false;
    private boolean isPassportUploaded = false;

    private final ActivityResultLauncher<String> getContentLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processAndUploadUri(uri);
                } else {
                    Toast.makeText(requireContext(), "Document selection cancelled.", Toast.LENGTH_SHORT).show();
                }
            });

    public static ProviderKycFragment newInstance() {
        return new ProviderKycFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_kyc, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        kycRepository = new KycRepositoryImpl();
        executor = Executors.newSingleThreadExecutor();

        radioGroupDocType = view.findViewById(R.id.radioGroupDocType);
        radioNic = view.findViewById(R.id.radioNic);
        radioPassport = view.findViewById(R.id.radioPassport);
        layoutNicUploads = view.findViewById(R.id.layoutNicUploads);
        cardPassportUpload = view.findViewById(R.id.cardPassportUpload);

        txtStatusNicFront = view.findViewById(R.id.txtStatusNicFront);
        txtStatusNicBack = view.findViewById(R.id.txtStatusNicBack);
        txtStatusPassport = view.findViewById(R.id.txtStatusPassport);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        radioGroupDocType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioPassport) {
                layoutNicUploads.setVisibility(View.GONE);
                cardPassportUpload.setVisibility(View.VISIBLE);
            } else {
                layoutNicUploads.setVisibility(View.VISIBLE);
                cardPassportUpload.setVisibility(View.GONE);
            }
        });

        view.findViewById(R.id.btnUploadNicFront).setOnClickListener(v -> {
            selectedPendingDocType = "nic_front";
            getContentLauncher.launch("*/*");
        });

        view.findViewById(R.id.btnUploadNicBack).setOnClickListener(v -> {
            selectedPendingDocType = "nic_back";
            getContentLauncher.launch("*/*");
        });

        view.findViewById(R.id.btnUploadPassport).setOnClickListener(v -> {
            selectedPendingDocType = "passport";
            getContentLauncher.launch("*/*");
        });

        view.findViewById(R.id.btnSubmitKyc).setOnClickListener(v -> submitKycApplication());

        fetchExistingKycStatus();
    }

    private void fetchExistingKycStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            kycRepository.getKycStatus(idToken, new KycRepository.KycStatusCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    progressBar.setVisibility(View.GONE);
                    if (result != null && result.has("documents")) {
                        try {
                            var docs = result.getJSONArray("documents");
                            for (int i = 0; i < docs.length(); i++) {
                                JSONObject d = docs.getJSONObject(i);
                                String type = d.optString("type", "");
                                if ("nic_front".equals(type)) {
                                    isFrontUploaded = true;
                                    txtStatusNicFront.setText("Uploaded");
                                    txtStatusNicFront.setTextColor(requireContext().getColor(R.color.primary));
                                } else if ("nic_back".equals(type)) {
                                    isBackUploaded = true;
                                    txtStatusNicBack.setText("Uploaded");
                                    txtStatusNicBack.setTextColor(requireContext().getColor(R.color.primary));
                                } else if ("passport".equals(type)) {
                                    isPassportUploaded = true;
                                    txtStatusPassport.setText("Uploaded");
                                    txtStatusPassport.setTextColor(requireContext().getColor(R.color.primary));
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                }
            });
        });
    }

    private void processAndUploadUri(Uri uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in.", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        mainHandlerPostToast("Selected document exceeds 5MB limit.");
                        return;
                    }
                    buffer.write(data, 0, bytesRead);
                }

                byte[] bytes = buffer.toByteArray();
                String base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP);

                String finalMimeType = mimeType;
                user.getIdToken(true).addOnCompleteListener(task -> {
                    String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                    kycRepository.uploadDocument(idToken, selectedPendingDocType, finalMimeType, base64Str, new KycRepository.UploadCallback() {
                        @Override
                        public void onSuccess(JSONObject result) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Document uploaded successfully!", Toast.LENGTH_SHORT).show();

                            if ("nic_front".equals(selectedPendingDocType)) {
                                isFrontUploaded = true;
                                txtStatusNicFront.setText("Uploaded");
                                txtStatusNicFront.setTextColor(requireContext().getColor(R.color.primary));
                            } else if ("nic_back".equals(selectedPendingDocType)) {
                                isBackUploaded = true;
                                txtStatusNicBack.setText("Uploaded");
                                txtStatusNicBack.setTextColor(requireContext().getColor(R.color.primary));
                            } else if ("passport".equals(selectedPendingDocType)) {
                                isPassportUploaded = true;
                                txtStatusPassport.setText("Uploaded");
                                txtStatusPassport.setTextColor(requireContext().getColor(R.color.primary));
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Upload failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });

            } catch (Exception e) {
                mainHandlerPostToast("Failed to process document: " + e.getLocalizedMessage());
            }
        });
    }

    private void submitKycApplication() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            kycRepository.submitKycApplication(idToken, new KycRepository.KycStatusCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "KYC Application Submitted! Redirecting to Workspace...", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).loadMainContentFragment(ProviderDashboardFragment.newInstance(), false);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Verification Saved! Redirecting to Provider Workspace...", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).loadMainContentFragment(ProviderDashboardFragment.newInstance(), false);
                    }
                }
            });
        });
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