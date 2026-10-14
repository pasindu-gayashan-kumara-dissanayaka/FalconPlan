package lk.falconplan.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.remote.BackendApiClient;

public class AdminListingDetailFragment extends Fragment {

    private static final String ARG_LISTING_ID = "arg_listing_id";
    private static final String ARG_PROVIDER_UID = "arg_provider_uid";

    private BackendApiClient apiClient;
    private String listingId;
    private String providerUid;

    private TextView txtListingTitle;
    private TextView txtStatusBadge;
    private TextView txtTypeLocation;
    private TextView txtHostName;
    private TextView txtDescription;
    private ProgressBar progressBar;

    public static AdminListingDetailFragment newInstance(String listingId, String providerUid) {
        AdminListingDetailFragment fragment = new AdminListingDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        args.putString(ARG_PROVIDER_UID, providerUid);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_listing_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiClient = BackendApiClient.getInstance();
        listingId = getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : "";
        providerUid = getArguments() != null ? getArguments().getString(ARG_PROVIDER_UID) : "";

        txtListingTitle = view.findViewById(R.id.txtListingTitle);
        txtStatusBadge = view.findViewById(R.id.txtStatusBadge);
        txtTypeLocation = view.findViewById(R.id.txtTypeLocation);
        txtHostName = view.findViewById(R.id.txtHostName);
        txtDescription = view.findViewById(R.id.txtDescription);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnApproveListing).setOnClickListener(v -> confirmDecision("approve", null));
        view.findViewById(R.id.btnRejectListing).setOnClickListener(v -> showRejectDialog());

        loadAdminListingDetail();
    }

    private void loadAdminListingDetail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || listingId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            apiClient.getAdminListingDetail(idToken, listingId, providerUid, new BackendApiClient.ApiCallback<JSONObject>() {
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
                    Toast.makeText(requireContext(), "Failed to load listing detail: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void displayDetailData(JSONObject data) {
        JSONObject listing = data.optJSONObject("listing");
        JSONObject profile = data.optJSONObject("providerProfile");

        if (listing != null) {
            String title = listing.optString("title", "Untitled Listing");
            String type = listing.optString("listingType", "homestay").toUpperCase(Locale.US);
            String district = listing.optString("district", "");
            String village = listing.optString("village", "");
            String status = listing.optString("status", "submitted_under_review");

            txtListingTitle.setText(title);
            txtTypeLocation.setText(String.format("%s • %s, %s", type, district, village));
            txtDescription.setText(listing.optString("description", "No description provided."));
            txtStatusBadge.setText(status.toUpperCase(Locale.US));

            if (providerUid.isEmpty()) {
                providerUid = listing.optString("providerUid", "");
            }
        }

        if (profile != null) {
            String hostName = profile.optString("hostDisplayName", profile.optString("businessName", "FalconPlan Host"));
            String kycStat = profile.optString("kycStatus", "pending");
            txtHostName.setText(String.format("Host: %s (KYC: %s)", hostName, kycStat.toUpperCase(Locale.US)));
        }
    }

    private void showRejectDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Reason for rejecting listing...");

        new AlertDialog.Builder(requireContext())
                .setTitle("Reject Listing Application")
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
        if (user == null || listingId.isEmpty() || providerUid.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid provider UID or session.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            apiClient.submitAdminListingDecision(idToken, listingId, providerUid, decision, reason, new BackendApiClient.ApiCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Listing decision recorded: " + decision.toUpperCase(Locale.US), Toast.LENGTH_SHORT).show();
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