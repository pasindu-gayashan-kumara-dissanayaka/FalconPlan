package lk.falconplan.app.data.model;

import org.json.JSONObject;

public class ChatConversation {

    private String conversationId;
    private String bookingId;
    private String travelerUid;
    private String providerUid;
    private String lastMessageText;
    private long lastMessageAt;

    public ChatConversation() {}

    public ChatConversation(String conversationId, String bookingId, String travelerUid,
                            String providerUid, String lastMessageText, long lastMessageAt) {
        this.conversationId = conversationId;
        this.bookingId = bookingId;
        this.travelerUid = travelerUid;
        this.providerUid = providerUid;
        this.lastMessageText = lastMessageText != null ? lastMessageText : "";
        this.lastMessageAt = lastMessageAt;
    }

    public static ChatConversation fromJson(JSONObject json) {
        if (json == null) return null;
        return new ChatConversation(
                json.optString("conversationId", ""),
                json.optString("bookingId", ""),
                json.optString("travelerUid", ""),
                json.optString("providerUid", ""),
                json.optString("lastMessageText", ""),
                json.optLong("lastMessageAt", System.currentTimeMillis())
        );
    }

    public String getConversationId() { return conversationId; }
    public String getBookingId() { return bookingId; }
    public String getTravelerUid() { return travelerUid; }
    public String getProviderUid() { return providerUid; }
    public String getLastMessageText() { return lastMessageText; }
    public long getLastMessageAt() { return lastMessageAt; }
}