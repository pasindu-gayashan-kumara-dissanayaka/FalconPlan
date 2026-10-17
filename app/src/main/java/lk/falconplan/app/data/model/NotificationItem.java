package lk.falconplan.app.data.model;

import org.json.JSONObject;

public class NotificationItem {

    public static final String TYPE_BOOKING_CREATED = "booking_created";
    public static final String TYPE_BOOKING_DECISION = "booking_decision";
    public static final String TYPE_CHAT_MESSAGE = "chat_message";
    public static final String TYPE_PAYMENT_REVIEW = "payment_review";

    private String notificationId;
    private String recipientUid;
    private String type;
    private String sourceEventId;
    private String entityType;
    private String entityId;
    private String title;
    private String body;
    private boolean isRead;
    private long createdAt;

    public NotificationItem() {}

    public NotificationItem(String notificationId, String recipientUid, String type, String sourceEventId,
                            String entityType, String entityId, String title, String body,
                            boolean isRead, long createdAt) {
        this.notificationId = notificationId;
        this.recipientUid = recipientUid;
        this.type = type != null ? type : "system_event";
        this.sourceEventId = sourceEventId != null ? sourceEventId : "";
        this.entityType = entityType != null ? entityType : "";
        this.entityId = entityId != null ? entityId : "";
        this.title = title != null ? title : "FalconPlan Update";
        this.body = body != null ? body : "";
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public static NotificationItem fromJson(JSONObject json) {
        if (json == null) return null;
        return new NotificationItem(
                json.optString("notificationId", ""),
                json.optString("recipientUid", ""),
                json.optString("type", "system_event"),
                json.optString("sourceEventId", ""),
                json.optString("entityType", ""),
                json.optString("entityId", ""),
                json.optString("title", "FalconPlan Update"),
                json.optString("body", ""),
                json.optBoolean("isRead", false),
                json.optLong("createdAt", System.currentTimeMillis())
        );
    }

    public String getNotificationId() { return notificationId; }
    public String getRecipientUid() { return recipientUid; }
    public String getType() { return type; }
    public String getSourceEventId() { return sourceEventId; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public boolean isRead() { return isRead; }
    public long getCreatedAt() { return createdAt; }

    public void setRead(boolean read) { isRead = read; }
}