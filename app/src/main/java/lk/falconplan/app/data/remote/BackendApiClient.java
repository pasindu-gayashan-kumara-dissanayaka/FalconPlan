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

    private static final String[] BASE_URLS = {
            "http://10.0.2.2:3000/api/",
            "http://localhost:3000/api/",
            "http://127.0.0.1:3000/api/"
    };

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

    // --- ROOM AVAILABILITY ENDPOINTS (PHASE 04-C3-1/2) ---

    public void getRoomAvailability(String idToken, String listingId, String roomId, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "provider/listings/" + listingId + "/rooms/" + roomId + "/availability");
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
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        JSONObject resJson = new JSONObject(response.toString());
                        JSONObject dataJson = resJson.optJSONObject("data");
                        mainHandler.post(() -> callback.onSuccess(dataJson));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Failed to connect to backend (" + BASE_URLS[0] + "). Please ensure Express server is running on port 3000.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    public void updateRoomAvailability(String idToken, String listingId, String roomId, String dateStr, int blockedUnits, String note, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "provider/listings/" + listingId + "/rooms/" + roomId + "/availability/" + dateStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Content-Type", "application/json");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setDoOutput(true);

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("blockedUnits", blockedUnits);
                    if (note != null && !note.isEmpty()) {
                        jsonBody.put("note", note);
                    }

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
                        JSONObject dataJson = resJson.optJSONObject("data");
                        mainHandler.post(() -> callback.onSuccess(dataJson));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Failed to connect to backend (" + BASE_URLS[0] + "). Please ensure Express server is running on port 3000.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    // --- PROVIDER LISTING SUBMISSION ENDPOINT ---

    public void submitListing(String idToken, String listingId, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "provider/listings/" + listingId + "/submit");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
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
                        JSONObject dataJson = resJson.optJSONObject("data");
                        mainHandler.post(() -> callback.onSuccess(dataJson));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Failed to connect to backend (" + BASE_URLS[0] + "). Please ensure Express server is running on port 3000.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    // --- ADMIN KYC REVIEW ENDPOINTS (PHASE 04-A3-2) ---

    public void getAdminKycApplications(String idToken, String statusFilter, int page, int limit, ApiCallback<JSONArray> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    String query = "admin/kyc?page=" + page + "&limit=" + limit;
                    if (statusFilter != null && !statusFilter.isEmpty()) {
                        query += "&status=" + statusFilter;
                    }
                    URL url = new URL(baseUrl + query);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    int statusCode = conn.getResponseCode();
                    if (statusCode == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        JSONObject resJson = new JSONObject(response.toString());
                        JSONArray dataArray = resJson.optJSONArray("data");
                        mainHandler.post(() -> callback.onSuccess(dataArray != null ? dataArray : new JSONArray()));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    public void getAdminKycDetail(String idToken, String providerUid, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "admin/kyc/" + providerUid);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    int statusCode = conn.getResponseCode();
                    if (statusCode == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        JSONObject resJson = new JSONObject(response.toString());
                        JSONObject dataJson = resJson.optJSONObject("data");
                        mainHandler.post(() -> callback.onSuccess(dataJson));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    public void getAdminDocumentPreview(String idToken, String providerUid, String documentId, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "admin/kyc/" + providerUid + "/documents/" + documentId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    int statusCode = conn.getResponseCode();
                    if (statusCode == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        JSONObject resJson = new JSONObject(response.toString());
                        JSONObject dataJson = resJson.optJSONObject("data");
                        mainHandler.post(() -> callback.onSuccess(dataJson));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    public void submitAdminDecision(String idToken, String providerUid, String decision, String reason, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "admin/kyc/" + providerUid + "/decision");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    conn.setDoOutput(true);

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("decision", decision);
                    if (reason != null && !reason.isEmpty()) {
                        jsonBody.put("reason", reason);
                    }

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
                        JSONObject dataJson = resJson.optJSONObject("data");
                        mainHandler.post(() -> callback.onSuccess(dataJson));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    // --- KYC PROVIDER METHODS ---

    public void getKycStatus(String idToken, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "provider/kyc/status");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    int statusCode = conn.getResponseCode();
                    if (statusCode == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        JSONObject resJson = new JSONObject(response.toString());
                        JSONObject dataJson = resJson.optJSONObject("data");
                        mainHandler.post(() -> callback.onSuccess(dataJson));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    public void uploadKycDocument(String idToken, String documentType, String mimeType, String fileBase64, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "provider/kyc/documents");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setDoOutput(true);

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("documentType", documentType);
                    jsonBody.put("mimeType", mimeType);
                    jsonBody.put("fileBase64", fileBase64);

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
                        JSONObject dataJson = resJson.optJSONObject("data");
                        mainHandler.post(() -> callback.onSuccess(dataJson));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    public void deleteKycDocument(String idToken, String documentId, ApiCallback<Void> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "provider/kyc/documents/" + documentId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("DELETE");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    int statusCode = conn.getResponseCode();
                    if (statusCode == 200) {
                        mainHandler.post(() -> callback.onSuccess(null));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    public void submitKycApplication(String idToken, ApiCallback<JSONObject> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "provider/kyc/submit");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    int statusCode = conn.getResponseCode();
                    if (statusCode == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        JSONObject resJson = new JSONObject(response.toString());
                        JSONObject dataJson = resJson.optJSONObject("data");
                        mainHandler.post(() -> callback.onSuccess(dataJson));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    // --- TRAVEL PLAN METHODS ---

    public void publishPlan(String idToken, TravelPlan plan, List<TravelPlanStop> stops, String ownerDisplayName, ApiCallback<PublicTravelPlan> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "plans/" + plan.getId() + "/publish");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
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
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    public void unpublishPlan(String idToken, String planId, ApiCallback<Void> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "plans/" + planId + "/unpublish");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    if (idToken != null && !idToken.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + idToken);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    int statusCode = conn.getResponseCode();
                    if (statusCode == 200) {
                        mainHandler.post(() -> callback.onSuccess(null));
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
        });
    }

    public void getPublicPlans(int page, int limit, ApiCallback<List<PublicTravelPlan>> callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (String baseUrl : BASE_URLS) {
                try {
                    URL url = new URL(baseUrl + "public-plans?page=" + page + "&limit=" + limit);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

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
                        return;
                    } else {
                        lastException = new Exception("Backend Error (" + statusCode + ")");
                    }
                } catch (Exception e) {
                    lastException = e;
                }
            }

            Exception finalErr = lastException != null ? lastException : new Exception("Backend connection failed.");
            mainHandler.post(() -> callback.onError(finalErr));
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