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

import lk.falconplan.app.data.model.CommunityPost;

public class CommunityPostRepositoryImpl implements CommunityPostRepository {

    private static final String BASE_URL = "http://10.0.2.2:3000/api/";
    private final ExecutorService executor;
    private final Handler mainHandler;

    public CommunityPostRepositoryImpl() {
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void createPost(String idToken, String caption, String destinationId, PostCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts");
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
                body.put("caption", caption);
                body.put("destinationId", destinationId != null ? destinationId : "");

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
                    CommunityPost post = CommunityPost.fromJson(data);

                    mainHandler.post(() -> callback.onSuccess(post));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Create Post Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void getTravelerPosts(String idToken, PostsListCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/mine");
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
                    List<CommunityPost> list = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.optJSONObject(i);
                            if (item != null) {
                                list.add(CommunityPost.fromJson(item));
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
    public void uploadPostPhoto(String idToken, String postId, String mimeType, String fileBase64, OperationCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/photos");
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
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Upload Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void publishPost(String idToken, String postId, PostCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/publish");
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
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line.trim());
                    }

                    JSONObject res = new JSONObject(sb.toString());
                    JSONObject data = res.optJSONObject("data");
                    CommunityPost post = CommunityPost.fromJson(data);

                    mainHandler.post(() -> callback.onSuccess(post));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Publish Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void getCommunityFeed(int page, int limit, PostsListCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/feed?page=" + page + "&limit=" + limit);
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
                    List<CommunityPost> list = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.optJSONObject(i);
                            if (item != null) {
                                list.add(CommunityPost.fromJson(item));
                            }
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Feed Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void reportPost(String idToken, String postId, String reason, OperationCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/report");
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
                    mainHandler.post(() -> callback.onError(new Exception("Report Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void deletePost(String idToken, String postId, OperationCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId);
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
                    mainHandler.post(() -> callback.onError(new Exception("Delete Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void likePost(String idToken, String postId, LikeCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/like");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
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
                    int count = data != null ? data.optInt("likeCount", 0) : 0;
                    boolean liked = data != null && data.optBoolean("likedByMe", true);

                    mainHandler.post(() -> callback.onSuccess(count, liked));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Like Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void unlikePost(String idToken, String postId, LikeCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/like");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
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
                    int count = data != null ? data.optInt("likeCount", 0) : 0;
                    boolean liked = data != null && data.optBoolean("likedByMe", false);

                    mainHandler.post(() -> callback.onSuccess(count, liked));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Unlike Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void savePost(String idToken, String postId, SaveCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/save");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
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
                    boolean saved = data != null && data.optBoolean("savedByMe", true);

                    mainHandler.post(() -> callback.onSuccess(saved));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Save Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void unsavePost(String idToken, String postId, SaveCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/posts/" + postId + "/save");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
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
                    boolean saved = data != null && data.optBoolean("savedByMe", false);

                    mainHandler.post(() -> callback.onSuccess(saved));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Unsave Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void getSavedPosts(String idToken, int page, int limit, PostsListCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(BASE_URL + "community/saved-posts?page=" + page + "&limit=" + limit);
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
                    List<CommunityPost> list = new ArrayList<>();

                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.optJSONObject(i);
                            if (item != null) {
                                CommunityPost p = CommunityPost.fromJson(item);
                                p.setSavedByMe(true);
                                list.add(p);
                            }
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Saved Posts Error " + statusCode)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
}