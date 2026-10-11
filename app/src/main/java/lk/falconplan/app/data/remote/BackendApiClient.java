package lk.falconplan.app.data.remote;

import android.os.Handler;
import android.os.Looper;

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

import lk.falconplan.app.data.model.PublicTravelPlan;
import lk.falconplan.app.data.model.TravelPlan;
import lk.falconplan.app.data.model.TravelPlanStop;

public class BackendApiClient {

    private static final String BASE_URL = "http://10.0.2.2:3000/api/";
    private static BackendApiClient instance;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(Exception exception);
    }

    private BackendApiClient() {
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized BackendApiClient getInstance() {
        if (instance == null) {
            instance = new BackendApiClient();
        }
        return instance;
    }

    public void publishPlan(String idToken, TravelPlan plan, List<TravelPlanStop> stops, String ownerDisplayName, ApiCallback<PublicTravelPlan> callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "plans/" + plan.getId() + "/publish");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + idToken);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("title", plan.getTitle());
                jsonBody.put("description", plan.getDescription());
                jsonBody.put("startDate", plan.getStartDate());
                jsonBody.put("endDate", plan.getEndDate());
                jsonBody.put("ownerDisplayName", ownerDisplayName != null ? ownerDisplayName : "FalconPlan Member");

                JSONArray stopsArray = new JSONArray();
                if (stops != null) {
                    for (TravelPlanStop stop : stops) {
                        JSONObject stopJson = new JSONObject();
                        stopJson.put("destinationId", stop.getDestinationId());
                        stopJson.put("destinationName", stop.getDestinationName());
                        stopJson.put("district", stop.getDistrict());
                        stopJson.put("latitude", stop.getLatitude());
                        stopJson.put("longitude", stop.getLongitude());
                        stopsArray.put(stopJson);
                    }
                }
                jsonBody.put("stops", stopsArray);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    JSONObject resJson = new JSONObject(response.toString());
                    JSONObject dataJson = resJson.getJSONObject("data");

                    PublicTravelPlan publicPlan = parsePublicPlanJson(dataJson);
                    mainHandler.post(() -> callback.onSuccess(publicPlan));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("HTTP Error: " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void unpublishPlan(String idToken, String planId, ApiCallback<Void> callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "plans/" + planId + "/unpublish");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + idToken);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("HTTP Error: " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getPublicPlans(int page, int limit, ApiCallback<List<PublicTravelPlan>> callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "public-plans?page=" + page + "&limit=" + limit);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    JSONObject resJson = new JSONObject(response.toString());
                    JSONArray dataArray = resJson.getJSONArray("data");
                    List<PublicTravelPlan> list = new ArrayList<>();

                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject itemJson = dataArray.getJSONObject(i);
                        list.add(parsePublicPlanJson(itemJson));
                    }

                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("HTTP Error: " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private PublicTravelPlan parsePublicPlanJson(JSONObject json) throws Exception {
        String publicPlanId = json.optString("publicPlanId", "");
        String ownerUid = json.optString("ownerUid", "");
        String ownerDisplayName = json.optString("ownerDisplayName", "FalconPlan Member");
        String title = json.optString("title", "");
        String description = json.optString("description", "");
        long startDate = json.optLong("startDate", System.currentTimeMillis());
        long endDate = json.optLong("endDate", System.currentTimeMillis());
        long publishedAt = json.optLong("publishedAt", System.currentTimeMillis());
        String status = json.optString("status", "published");

        List<TravelPlanStop> stops = new ArrayList<>();
        if (json.has("stops")) {
            JSONArray stopsArray = json.getJSONArray("stops");
            for (int i = 0; i < stopsArray.length(); i++) {
                JSONObject s = stopsArray.getJSONObject(i);
                TravelPlanStop stop = new TravelPlanStop(
                        "stop_" + i,
                        s.optString("destinationId", ""),
                        s.optString("destinationName", ""),
                        s.optString("district", ""),
                        s.optDouble("latitude", 0.0),
                        s.optDouble("longitude", 0.0),
                        startDate,
                        "",
                        s.optInt("orderIndex", i + 1)
                );
                stops.add(stop);
            }
        }

        return new PublicTravelPlan(publicPlanId, ownerUid, ownerDisplayName, title, description,
                startDate, endDate, publishedAt, status, stops);
    }
}