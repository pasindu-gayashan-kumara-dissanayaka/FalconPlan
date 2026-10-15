package lk.falconplan.app.data.repository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Looper;

import lk.falconplan.app.data.model.Payment;

public class AdminPaymentRepositoryImpl implements AdminPaymentRepository {

    private static final String BASE_URL = "http://10.0.2.2:3000/api/";
    private final ExecutorService executor;
    private final Handler mainHandler;

    public AdminPaymentRepositoryImpl() {
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void getAdminPayments(String idToken, String statusFilter, int page, int limit, PaymentsListCallback callback) {
        executor.execute(() -> {
            try {
                String filter = statusFilter != null ? statusFilter : "under_review";
                URL url = new URL(BASE_URL + "admin/payments?status=" + filter + "&page=" + page + "&limit=" + limit);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (idToken != null && !idToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                }
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONObject res = new JSONObject(sb.toString());
                    JSONArray data = res.optJSONArray("data");
                    List<Payment> list = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.optJSONObject(i);
                            if (item != null) {
                                list.add(Payment.fromJson(item));
                            }
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void getAdminPaymentDetail(String idToken, String paymentId, PaymentDetailCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "admin/payments/" + paymentId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (idToken != null && !idToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                }
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONObject res = new JSONObject(sb.toString());
                    JSONObject data = res.optJSONObject("data");
                    JSONObject paymentObj = data != null ? data.optJSONObject("payment") : null;
                    JSONArray receipts = data != null ? data.optJSONArray("receipts") : new JSONArray();

                    Payment payment = Payment.fromJson(paymentObj);
                    mainHandler.post(() -> callback.onSuccess(payment, receipts != null ? receipts : new JSONArray()));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Detail Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void submitPaymentDecision(String idToken, String paymentId, String decision, String reason, String bankReference, DecisionCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "admin/payments/" + paymentId + "/decision");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                if (idToken != null && !idToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                }
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("decision", decision);
                if (reason != null && !reason.trim().isEmpty()) {
                    body.put("reason", reason);
                }
                if (bankReference != null && !bankReference.trim().isEmpty()) {
                    body.put("bankReference", bankReference);
                }

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONObject res = new JSONObject(sb.toString());
                    JSONObject data = res.optJSONObject("data");
                    String resultStatus = data != null ? data.optString("status", decision) : decision;

                    mainHandler.post(() -> callback.onSuccess(resultStatus));
                } else if (statusCode == 400) {
                    mainHandler.post(() -> callback.onError(new Exception("Reconciliation required: Live bank verification restricted in production without live bank API integration.")));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Decision Failed (" + statusCode + ")")));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
}