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

import org.json.JSONArray;

import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Payment;
import lk.falconplan.app.data.repository.AdminPaymentRepository;
import lk.falconplan.app.data.repository.AdminPaymentRepositoryImpl;

public class AdminPaymentDetailFragment extends Fragment {

    private static final String ARG_PAYMENT_ID = "arg_payment_id";

    private AdminPaymentRepository adminPaymentRepository;
    private String paymentId;

    private TextView txtPaymentStatus;
    private TextView txtPaymentId;
    private TextView txtMethod;
    private TextView txtAmountDue;
    private View layoutDecisionActions;
    private ProgressBar progressBar;

    public static AdminPaymentDetailFragment newInstance(String paymentId) {
        AdminPaymentDetailFragment fragment = new AdminPaymentDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAYMENT_ID, paymentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_payment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminPaymentRepository = new AdminPaymentRepositoryImpl();
        paymentId = getArguments() != null ? getArguments().getString(ARG_PAYMENT_ID) : "";

        txtPaymentStatus = view.findViewById(R.id.txtPaymentStatus);
        txtPaymentId = view.findViewById(R.id.txtPaymentId);
        txtMethod = view.findViewById(R.id.txtMethod);
        txtAmountDue = view.findViewById(R.id.txtAmountDue);
        layoutDecisionActions = view.findViewById(R.id.layoutDecisionActions);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnVerifyPayment).setOnClickListener(v -> verifyPayment());
        view.findViewById(R.id.btnRejectPayment).setOnClickListener(v -> rejectPayment());

        loadAdminPaymentDetail();
    }

    private void loadAdminPaymentDetail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || paymentId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            adminPaymentRepository.getAdminPaymentDetail(idToken, paymentId, new AdminPaymentRepository.PaymentDetailCallback() {
                @Override
                public void onSuccess(Payment payment, JSONArray receipts) {
                    progressBar.setVisibility(View.GONE);
                    if (payment != null) {
                        displayPayment(payment);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load payment: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void displayPayment(Payment payment) {
        txtPaymentStatus.setText(String.format("STATUS: %s", payment.getStatus().toUpperCase(Locale.US)));
        txtPaymentId.setText(String.format("Payment ID: %s", payment.getPaymentId()));
        txtMethod.setText(String.format("Method: %s", payment.getMethod()));
        txtAmountDue.setText(String.format("Amount Due: %s", payment.getFormattedAmount()));

        if (Payment.STATUS_UNDER_REVIEW.equals(payment.getStatus())) {
            layoutDecisionActions.setVisibility(View.VISIBLE);
        } else {
            layoutDecisionActions.setVisibility(View.GONE);
        }
    }

    private void verifyPayment() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || paymentId.isEmpty()) return;

        final EditText editBankRef = new EditText(requireContext());
        editBankRef.setHint("Enter Bank Transaction Reference Code");

        new AlertDialog.Builder(requireContext())
                .setTitle("Verify Financial Reconciliation")
                .setMessage("Certify that actual funds were reconciled in the operator bank account for this payment.")
                .setView(editBankRef)
                .setPositiveButton("Verify Payment", (dialog, which) -> {
                    String bankRef = editBankRef.getText().toString().trim();
                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        adminPaymentRepository.submitPaymentDecision(idToken, paymentId, "verify", "", bankRef, new AdminPaymentRepository.DecisionCallback() {
                            @Override
                            public void onSuccess(String status) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Payment Verified! Status: " + status.toUpperCase(Locale.US), Toast.LENGTH_SHORT).show();
                                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            }

                            @Override
                            public void onError(Exception exception) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Verification restricted: " + exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void rejectPayment() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || paymentId.isEmpty()) return;

        final EditText editReason = new EditText(requireContext());
        editReason.setHint("Enter Rejection Reason");

        new AlertDialog.Builder(requireContext())
                .setTitle("Reject Payment Evidence")
                .setMessage("Enter reason for rejecting the submitted receipt proof:")
                .setView(editReason)
                .setPositiveButton("Reject Evidence", (dialog, which) -> {
                    String reason = editReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(requireContext(), "Rejection reason is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        adminPaymentRepository.submitPaymentDecision(idToken, paymentId, "reject", reason, "", new AdminPaymentRepository.DecisionCallback() {
                            @Override
                            public void onSuccess(String status) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Payment Evidence Rejected.", Toast.LENGTH_SHORT).show();
                                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            }

                            @Override
                            public void onError(Exception exception) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Rejection failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Back", (dialog, which) -> dialog.dismiss())
                .show();
    }
}