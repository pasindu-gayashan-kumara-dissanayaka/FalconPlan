package lk.falconplan.app.ui.booking;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Payment;
import lk.falconplan.app.data.repository.PaymentRepository;
import lk.falconplan.app.data.repository.PaymentRepositoryImpl;

public class BookingPaymentFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "arg_booking_id";

    private PaymentRepository paymentRepository;
    private String bookingId;

    private TextView txtBookingTitle;
    private TextView txtAmountDue;
    private TextView txtPaymentStatus;
    private RadioGroup radioGroupPaymentMethod;
    private View layoutBankInstructions;
    private ProgressBar progressBar;

    private ActivityResultLauncher<String> photoPickerLauncher;

    public static BookingPaymentFragment newInstance(String bookingId) {
        BookingPaymentFragment fragment = new BookingPaymentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOKING_ID, bookingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadReceiptUri(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        paymentRepository = new PaymentRepositoryImpl();
        bookingId = getArguments() != null ? getArguments().getString(ARG_BOOKING_ID) : "";

        txtBookingTitle = view.findViewById(R.id.txtBookingTitle);
        txtAmountDue = view.findViewById(R.id.txtAmountDue);
        txtPaymentStatus = view.findViewById(R.id.txtPaymentStatus);
        radioGroupPaymentMethod = view.findViewById(R.id.radioGroupPaymentMethod);
        layoutBankInstructions = view.findViewById(R.id.layoutBankInstructions);
        progressBar = view.findViewById(R.id.progressBar);

        txtBookingTitle.setText(String.format("Booking ID: %s", bookingId));

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnConfirmPaymentMethod).setOnClickListener(v -> confirmPaymentMethod());
        view.findViewById(R.id.btnUploadReceipt).setOnClickListener(v -> photoPickerLauncher.launch("image/*"));

        radioGroupPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBankTransfer) {
                layoutBankInstructions.setVisibility(View.VISIBLE);
            } else {
                layoutBankInstructions.setVisibility(View.GONE);
            }
        });

        loadPaymentDetail();
    }

    private void loadPaymentDetail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bookingId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            paymentRepository.getPaymentDetail(idToken, bookingId, new PaymentRepository.PaymentCallback() {
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
                }
            });
        });
    }

    private void displayPayment(Payment payment) {
        txtAmountDue.setText(String.format("Amount Due: %s", payment.getFormattedAmount()));
        txtPaymentStatus.setText(String.format("Status: %s", payment.getFormattedStatus()));

        if (Payment.METHOD_BANK_TRANSFER.equals(payment.getMethod())) {
            radioGroupPaymentMethod.check(R.id.radioBankTransfer);
            layoutBankInstructions.setVisibility(View.VISIBLE);
        } else if (Payment.METHOD_CASH_ON_ARRIVAL.equals(payment.getMethod())) {
            radioGroupPaymentMethod.check(R.id.radioCashOnArrival);
            layoutBankInstructions.setVisibility(View.GONE);
        }
    }

    private void confirmPaymentMethod() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bookingId.isEmpty()) return;

        int selectedId = radioGroupPaymentMethod.getCheckedRadioButtonId();
        String method = selectedId == R.id.radioBankTransfer ? Payment.METHOD_BANK_TRANSFER : Payment.METHOD_CASH_ON_ARRIVAL;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            paymentRepository.initiatePayment(idToken, bookingId, method, new PaymentRepository.PaymentCallback() {
                @Override
                public void onSuccess(Payment payment) {
                    progressBar.setVisibility(View.GONE);
                    if (payment != null) {
                        displayPayment(payment);
                        Toast.makeText(requireContext(), "Payment Method Configured: " + payment.getFormattedStatus(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to configure payment: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void uploadReceiptUri(Uri uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || bookingId.isEmpty() || uri == null) return;

        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(requireContext(), "Invalid image file selected.", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageBytes = baos.toByteArray();

            if (imageBytes.length > 5 * 1024 * 1024) {
                Toast.makeText(requireContext(), "Receipt file size exceeds 5MB limit.", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            progressBar.setVisibility(View.VISIBLE);
            user.getIdToken(true).addOnCompleteListener(task -> {
                String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                paymentRepository.uploadReceiptProof(idToken, bookingId, "image/jpeg", fileBase64, new PaymentRepository.ReceiptCallback() {
                    @Override
                    public void onSuccess(String receiptId, String paymentStatus) {
                        progressBar.setVisibility(View.GONE);
                        txtPaymentStatus.setText("Status: Proof Submitted – Under Review");
                        Toast.makeText(requireContext(), "Receipt proof uploaded! Status: Under Review", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(Exception exception) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Receipt upload failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to read image: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}