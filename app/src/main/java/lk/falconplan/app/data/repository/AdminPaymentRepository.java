package lk.falconplan.app.data.repository;

import org.json.JSONArray;

import java.util.List;

import lk.falconplan.app.data.model.Payment;

public interface AdminPaymentRepository {

    interface PaymentsListCallback {
        void onSuccess(List<Payment> payments);
        void onError(Exception exception);
    }

    interface PaymentDetailCallback {
        void onSuccess(Payment payment, JSONArray receipts);
        void onError(Exception exception);
    }

    interface DecisionCallback {
        void onSuccess(String status);
        void onError(Exception exception);
    }

    void getAdminPayments(String idToken, String statusFilter, int page, int limit, PaymentsListCallback callback);

    void getAdminPaymentDetail(String idToken, String paymentId, PaymentDetailCallback callback);

    void submitPaymentDecision(String idToken, String paymentId, String decision, String reason, String bankReference, DecisionCallback callback);
}