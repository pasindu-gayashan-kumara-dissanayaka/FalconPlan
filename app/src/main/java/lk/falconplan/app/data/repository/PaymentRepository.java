package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.Payment;

public interface PaymentRepository {

    interface PaymentCallback {
        void onSuccess(Payment payment);
        void onError(Exception exception);
    }

    interface PaymentsListCallback {
        void onSuccess(List<Payment> payments);
        void onError(Exception exception);
    }

    interface ReceiptCallback {
        void onSuccess(String receiptId, String paymentStatus);
        void onError(Exception exception);
    }

    void initiatePayment(String idToken, String bookingId, String method, PaymentCallback callback);

    void getTravelerPayments(String idToken, PaymentsListCallback callback);

    void getPaymentDetail(String idToken, String paymentId, PaymentCallback callback);

    void uploadReceiptProof(String idToken, String paymentId, String mimeType, String fileBase64, ReceiptCallback callback);
}