package lk.falconplan.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import lk.falconplan.app.R;
import lk.falconplan.app.data.repository.AdminKycRepository;
import lk.falconplan.app.data.repository.AdminKycRepositoryImpl;

public class AdminKycDetailFragment extends Fragment {

    private static final String ARG_PROVIDER_UID = "arg_provider_uid";

    private AdminKycRepository adminKycRepository;
    private String providerUid;

    private TextView txtBusinessName;
    private TextView txtHostName;
    private TextView txtLocation;
    private TextView txtStatusBadge;
    private LinearLayout layoutDocuments;
    private ProgressBar progressBar;

    public static AdminKycDetailFragment newInstance(String providerUid) {
        AdminKycDetailFragment fragment = new AdminKycDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROVIDER_UID, providerUid);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_kyc_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminKycRepository = new AdminKycRepositoryImpl();
        providerUid = getArguments() != null ? getArguments().getString(ARG_PROVIDER_UID) : "";

        txtBusinessName = view.findViewById(R.id.txtBusinessName);
        txtHostName = view.findViewById(R.id.txtHostName);
        txtLocation = view.findViewById(R.id.txtLocation);
        txtStatusBadge = view.findViewById(R.id.txtStatusBadge);
        layoutDocuments = view.findViewById(R.id.layoutDocuments);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnApproveKyc).setOnClickListener(v -> confirmDecision("approve", null));
        view.findViewById(R.id.btnRejectKyc).setOnClickListener(v -> showRejectDialog());

        loadKycDetail();
    }

    private void loadKycDetail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || providerUid.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            adminKycRepository.getAdminKycDetail(idToken, providerUid, new AdminKycRepository.ApplicationDetailCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    progressBar.setVisibility(View.GONE);
                    if (result != null) {
                        displayDetailData(result);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load KYC detail: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void displayDetailData(JSONObject data) {
        JSONObject profile = data.optJSONObject("providerProfile");
        JSONObject kyc = data.optJSONObject("kyc");

        if (profile != null) {
            txtBusinessName.setText(profile.optString("businessName", "Unnamed Business"));
            txtHostName.setText(String.format("Host: %s", profile.optString("hostDisplayName", "FalconPlan Host")));
            txtLocation.setText(String.format("Location: %s • %s", profile.optString("district", ""), profile.optString("village", "")));
        } else {
            txtBusinessName.setText(String.format("Provider UID: %s", providerUid));
        }

        if (kyc != null) {
            String status = kyc.optString("status", "pending_identity_verification");
            txtStatusBadge.setText(status.toUpperCase());

            layoutDocuments.removeAllViews();
            TextView header = new TextView(requireContext());
            header.setText("Uploaded Identity Documents:");
            header.setTextSize(14f);
            header.setTextColor(requireContext().getColor(R.color.text_primary));
            layoutDocuments.addView(header);

            JSONArray docs = kyc.optJSONArray("documents");
            if (docs != null) {
                for (int i = 0; i < docs.length(); i++) {
                    JSONObject doc = docs.optJSONObject(i);
                    if (doc == null) continue;

                    String docId = doc.optString("documentId", "");
                    String type = doc.optString("type", "document");

                    MaterialButton btnPreview = new MaterialButton(requireContext());
                    btnPreview.setText(String.format("Preview Document: %s", type));
                    btnPreview.setOnClickListener(v -> fetchAndShowPreview(docId));
                    layoutDocuments.addView(btnPreview);
                }
            }
        }
    }

    private void fetchAndShowPreview(String documentId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            adminKycRepository.getAdminDocumentPreview(idToken, providerUid, documentId, new AdminKycRepository.PreviewCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    progressBar.setVisibility(View.GONE);
                    String url = result != null ? result.optString("authorizedPreviewUrl", "") : "";
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Authorized Document Preview")
                            .setMessage("Short-lived secure preview token generated:\n" + url)
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .show();
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to generate preview link: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showRejectDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Reason for rejecting KYC application...");

        new AlertDialog.Builder(requireContext())
                .setTitle("Reject KYC Application")
                .setView(input)
                .setPositiveButton("Confirm Reject", (dialog, which) -> {
                    String reason = input.getText() != null ? input.getText().toString().trim() : "";
                    if (reason.isEmpty()) {
                        Toast.makeText(requireContext(), "A rejection reason is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    confirmDecision("reject", reason);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmDecision(String decision, String reason) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            adminKycRepository.submitAdminDecision(idToken, providerUid, decision, reason, new AdminKycRepository.OperationCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "KYC Decision recorded: " + decision.toUpperCase(), Toast.LENGTH_SHORT).show();
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Decision failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}