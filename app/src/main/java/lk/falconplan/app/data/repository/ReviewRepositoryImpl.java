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

import lk.falconplan.app.data.model.ReviewItem;

public class ReviewRepositoryImpl implements ReviewRepository {

    private static final String BASE_URL = "http://10.0.2.2:3000/api/";
    private final ExecutorService executor;
    private final Handler mainHandler;

    public ReviewRepositoryImpl() {
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void checkEligibility(String idToken, String bookingId, EligibilityCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "reviews/eligibility?bookingId=" + bookingId);
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
                    boolean eligible = res.optBoolean("eligible", false);
                    boolean reviewed = res.optBoolean("reviewed", false);
                    String listingId = res.optString("listingId", "");

                    mainHandler.post(() -> callback.onSuccess(eligible, reviewed, listingId));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Eligibility Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void submitReview(String idToken, String bookingId, int rating, String reviewText, ReviewSubmitCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "reviews");
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
                body.put("rating", rating);
                body.put("reviewText", reviewText != null ? reviewText : "");

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
                    ReviewItem review = ReviewItem.fromJson(data);

                    mainHandler.post(() -> callback.onSuccess(review));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Submit Review Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void getListingReviews(String listingId, int page, int limit, ReviewsListCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "public-listings/" + listingId + "/reviews?page=" + page + "&limit=" + limit);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
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
                    List<ReviewItem> list = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.optJSONObject(i);
                            if (item != null) {
                                list.add(ReviewItem.fromJson(item));
                            }
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Reviews Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
}