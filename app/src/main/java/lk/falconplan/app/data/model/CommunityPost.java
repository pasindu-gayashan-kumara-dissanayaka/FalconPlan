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

        return new CommunityPost(
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
    }

    public String getPostId() { return postId; }
    public String getAuthorUid() { return authorUid; }
    public String getAuthorDisplayName() { return authorDisplayName; }
    public String getAuthorPhotoUrl() { return authorPhotoUrl; }
    public String getCaption() { return caption; }
    public String getDestinationId() { return destinationId; }
    public String getStatus() { return status; }
    public List<String> getPhotos() { return photos; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getPublishedAt() { return publishedAt; }

    // Legacy compatibility getters
    public String getId() { return postId; }
    public String getAuthorName() { return authorDisplayName; }
    public String getAuthorRole() { return "FalconPlan Member"; }
    public String getVillageLocation() { return destinationId; }
    public String getTimeAgo() { return "Recently"; }
    public String getTitle() { return caption.length() > 30 ? caption.substring(0, 30) + "..." : caption; }
    public String getContentSnippet() { return caption; }
}