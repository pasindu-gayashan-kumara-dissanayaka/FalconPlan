package lk.falconplan.app.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Payment;
import lk.falconplan.app.data.repository.PaymentRepository;
import lk.falconplan.app.data.repository.PaymentRepositoryImpl;

public class PaymentDetailFragment extends Fragment {

    private static final String ARG_PAYMENT_ID = "arg_payment_id";

    private PaymentRepository paymentRepository;
    private String paymentId;

    private TextView txtPaymentStatus;
    private TextView txtPaymentId;
    private TextView txtMethod;
    private TextView txtAmountDue;
    private ProgressBar progressBar;

    public static PaymentDetailFragment newInstance(String paymentId) {
        PaymentDetailFragment fragment = new PaymentDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAYMENT_ID, paymentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        paymentRepository = new PaymentRepositoryImpl();
        paymentId = getArguments() != null ? getArguments().getString(ARG_PAYMENT_ID) : "";

        txtPaymentStatus = view.findViewById(R.id.txtPaymentStatus);
        txtPaymentId = view.findViewById(R.id.txtPaymentId);
        txtMethod = view.findViewById(R.id.txtMethod);
        txtAmountDue = view.findViewById(R.id.txtAmountDue);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        loadPaymentDetail();
    }

    private void loadPaymentDetail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || paymentId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            paymentRepository.getPaymentDetail(idToken, paymentId, new PaymentRepository.PaymentCallback() {
                @Override
                public void onSuccess(Payment payment) {
                    progressBar.setVisibility(View.GONE);
                    if (payment != null) {
                        displayPayment(payment);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load payment detail: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void displayPayment(Payment payment) {
        txtPaymentStatus.setText(String.format("Status: %s", payment.getFormattedStatus()));
        txtPaymentId.setText(String.format("Payment ID: %s", payment.getPaymentId()));
        txtMethod.setText(String.format("Payment Method: %s", payment.getMethod()));
        txtAmountDue.setText(String.format("Amount Due: %s", payment.getFormattedAmount()));
    }
}