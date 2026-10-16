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

import lk.falconplan.app.data.model.CommunityComment;

public class CommunityCommentRepositoryImpl implements CommunityCommentRepository {

    private static final String BASE_URL = "http://10.0.2.2:3000/api/";
    private final ExecutorService executor;
    private final Handler mainHandler;

    public CommunityCommentRepositoryImpl() {
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void createComment(String idToken, String postId, String text, CommentCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/comments");
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
                body.put("text", text);

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
                    CommunityComment comment = CommunityComment.fromJson(data);

                    mainHandler.post(() -> callback.onSuccess(comment));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Create Comment Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void getComments(String postId, int page, int limit, CommentsListCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/comments?page=" + page + "&limit=" + limit);
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
                    List<CommunityComment> list = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.optJSONObject(i);
                            if (item != null) {
                                list.add(CommunityComment.fromJson(item));
                            }
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Get Comments Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void editComment(String idToken, String postId, String commentId, String text, CommentCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/comments/" + commentId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                if (idToken != null && !idToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                }
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("text", text);

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
                    CommunityComment comment = CommunityComment.fromJson(data);

                    mainHandler.post(() -> callback.onSuccess(comment));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Edit Comment Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void deleteComment(String idToken, String postId, String commentId, OperationCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/comments/" + commentId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                if (idToken != null && !idToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + idToken);
                }
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Delete Comment Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void reportComment(String idToken, String postId, String commentId, String reason, OperationCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/comments/" + commentId + "/report");
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
                body.put("reason", reason);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Report Comment Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
}