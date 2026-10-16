package lk.falconplan.app.data.model;

import org.json.JSONObject;

public class CommunityComment {

    public static final String STATUS_VISIBLE = "visible";
    public static final String STATUS_HIDDEN = "hidden";
    public static final String STATUS_DELETED = "deleted";

    private String commentId;
    private String postId;
    private String authorUid;
    private String authorDisplayName;
    private String authorPhotoUrl;
    private String text;
    private String status;
    private long createdAt;
    private long updatedAt;

    public CommunityComment() {}

    public CommunityComment(String commentId, String postId, String authorUid, String authorDisplayName,
                            String authorPhotoUrl, String text, String status, long createdAt, long updatedAt) {
        this.commentId = commentId;
        this.postId = postId;
        this.authorUid = authorUid;
        this.authorDisplayName = authorDisplayName != null ? authorDisplayName : "FalconPlan Member";
        this.authorPhotoUrl = authorPhotoUrl != null ? authorPhotoUrl : "";
        this.text = text != null ? text : "";
        this.status = status != null ? status : STATUS_VISIBLE;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CommunityComment fromJson(JSONObject json) {
        if (json == null) return null;
        return new CommunityComment(
                json.optString("commentId", ""),
                json.optString("postId", ""),
                json.optString("authorUid", ""),
                json.optString("authorDisplayName", "FalconPlan Member"),
                json.optString("authorPhotoUrl", ""),
                json.optString("text", ""),
                json.optString("status", STATUS_VISIBLE),
                json.optLong("createdAt", System.currentTimeMillis()),
                json.optLong("updatedAt", System.currentTimeMillis())
        );
    }

    public String getCommentId() { return commentId; }
    public String getPostId() { return postId; }
    public String getAuthorUid() { return authorUid; }
    public String getAuthorDisplayName() { return authorDisplayName; }
    public String getAuthorPhotoUrl() { return authorPhotoUrl; }
    public String getText() { return text; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public void setText(String text) { this.text = text; }
}