package lk.falconplan.app.data.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CommunityPost {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_HIDDEN = "hidden";
    public static final String STATUS_DELETED = "deleted";

    private String postId;
    private String authorUid;
    private String authorDisplayName;
    private String authorPhotoUrl;
    private String caption;
    private String destinationId;
    private String status;
    private List<String> photos;
    private int likeCount;
    private boolean likedByMe;
    private boolean savedByMe;
    private int commentCount;
    private long createdAt;
    private long updatedAt;
    private long publishedAt;

    public CommunityPost() {
        this.photos = new ArrayList<>();
    }

    public CommunityPost(String postId, String authorUid, String authorDisplayName, String authorPhotoUrl,
                         String caption, String destinationId, String status, List<String> photos,
                         long createdAt, long updatedAt, long publishedAt) {
        this.postId = postId;
        this.authorUid = authorUid;
        this.authorDisplayName = authorDisplayName != null ? authorDisplayName : "FalconPlan Traveler";
        this.authorPhotoUrl = authorPhotoUrl != null ? authorPhotoUrl : "";
        this.caption = caption != null ? caption : "";
        this.destinationId = destinationId != null ? destinationId : "";
        this.status = status != null ? status : STATUS_DRAFT;
        this.photos = photos != null ? photos : new ArrayList<>();
        this.likeCount = 0;
        this.likedByMe = false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
    }

    // Legacy compatibility constructor
    public CommunityPost(String id, String authorName, String authorRole, String villageLocation,
                         String timeAgo, String title, String contentSnippet,
                         int likeCount, int commentCount, boolean isLikedByMe) {
        this.postId = id;
        this.authorUid = id;
        this.authorDisplayName = authorName;
        this.authorPhotoUrl = "";
        this.caption = title + "\n\n" + contentSnippet;
        this.destinationId = villageLocation;
        this.status = STATUS_PUBLISHED;
        this.photos = new ArrayList<>();
        this.likeCount = Math.max(0, likeCount);
        this.likedByMe = isLikedByMe;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.publishedAt = System.currentTimeMillis();
    }

    public static CommunityPost fromJson(JSONObject json) {
        if (json == null) return null;
        List<String> photosList = new ArrayList<>();
        JSONArray photosArray = json.optJSONArray("photos");
        if (photosArray != null) {
            for (int i = 0; i < photosArray.length(); i++) {
                JSONObject pObj = photosArray.optJSONObject(i);
                if (pObj != null && pObj.has("storagePath")) {
                    photosList.add(pObj.optString("storagePath"));
                }
            }
        }

        CommunityPost post = new CommunityPost(
                json.optString("postId", ""),
                json.optString("authorUid", ""),
                json.optString("authorDisplayName", "FalconPlan Traveler"),
                json.optString("authorPhotoUrl", ""),
                json.optString("caption", ""),
                json.optString("destinationId", ""),
                json.optString("status", STATUS_DRAFT),
                photosList,
                json.optLong("createdAt", System.currentTimeMillis()),
                json.optLong("updatedAt", System.currentTimeMillis()),
                json.optLong("publishedAt", System.currentTimeMillis())
        );
        post.setLikeCount(json.optInt("likeCount", 0));
        post.setLikedByMe(json.optBoolean("likedByMe", false));
        post.setCommentCount(json.optInt("commentCount", 0));
        post.setSavedByMe(json.optBoolean("savedByMe", false));
        return post;
    }

    public String getPostId() { return postId; }
    public String getAuthorUid() { return authorUid; }
    public String getAuthorDisplayName() { return authorDisplayName; }
    public String getAuthorPhotoUrl() { return authorPhotoUrl; }
    public String getCaption() { return caption; }
    public String getDestinationId() { return destinationId; }
    public String getStatus() { return status; }
    public List<String> getPhotos() { return photos; }
    public int getLikeCount() { return likeCount; }
    public boolean isLikedByMe() { return likedByMe; }
    public boolean isSavedByMe() { return savedByMe; }
    public int getCommentCount() { return commentCount; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getPublishedAt() { return publishedAt; }

    public void setLikeCount(int likeCount) { this.likeCount = Math.max(0, likeCount); }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }
    public void setSavedByMe(boolean savedByMe) { this.savedByMe = savedByMe; }
    public void setCommentCount(int commentCount) { this.commentCount = Math.max(0, commentCount); }

    // Legacy compatibility getters
    public String getId() { return postId; }
    public String getAuthorName() { return authorDisplayName; }
    public String getAuthorRole() { return "FalconPlan Member"; }
    public String getVillageLocation() { return destinationId; }
    public String getTimeAgo() { return "Recently"; }
    public String getTitle() { return caption.length() > 30 ? caption.substring(0, 30) + "..." : caption; }
    public String getContentSnippet() { return caption; }
}