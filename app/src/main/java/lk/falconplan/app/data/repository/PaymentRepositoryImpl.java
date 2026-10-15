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

public class PaymentRepositoryImpl implements PaymentRepository {

    private static final String BASE_URL = "http://10.0.2.2:3000/api/";
    private final ExecutorService executor;
    private final Handler mainHandler;

    public PaymentRepositoryImpl() {
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void initiatePayment(String idToken, String bookingId, String method, PaymentCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "payments/initiate");
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
                body.put("bookingId", bookingId);
                body.put("method", method);

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
                    Payment payment = Payment.fromJson(data);

                    mainHandler.post(() -> callback.onSuccess(payment));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Initiate Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void getTravelerPayments(String idToken, PaymentsListCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "payments/mine");
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
    public void getPaymentDetail(String idToken, String paymentId, PaymentCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "payments/" + paymentId);
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
                    Payment payment = Payment.fromJson(data);

                    mainHandler.post(() -> callback.onSuccess(payment));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Detail Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void uploadReceiptProof(String idToken, String paymentId, String mimeType, String fileBase64, ReceiptCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "payments/" + paymentId + "/receipts");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                if (idToken != null && !idToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                }
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("mimeType", mimeType);
                body.put("fileBase64", fileBase64);

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
                    String receiptId = data != null ? data.optString("receiptId", "") : "";
                    String paymentStatus = data != null ? data.optString("paymentStatus", "under_review") : "under_review";

                    mainHandler.post(() -> callback.onSuccess(receiptId, paymentStatus));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Upload Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
}