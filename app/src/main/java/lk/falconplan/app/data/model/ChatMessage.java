package lk.falconplan.app.data.model;

import org.json.JSONObject;

public class ChatMessage {

    private String messageId;
    private String conversationId;
    private String bookingId;
    private String senderUid;
    private String text;
    private String clientRequestId;
    private long createdAt;

    public ChatMessage() {}

    public ChatMessage(String messageId, String conversationId, String bookingId, String senderUid,
                       String text, String clientRequestId, long createdAt) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.bookingId = bookingId;
        this.senderUid = senderUid;
        this.text = text != null ? text : "";
        this.clientRequestId = clientRequestId != null ? clientRequestId : "";
        this.createdAt = createdAt;
    }

    public static ChatMessage fromJson(JSONObject json) {
        if (json == null) return null;
        return new ChatMessage(
                json.optString("messageId", ""),
                json.optString("conversationId", ""),
                json.optString("bookingId", ""),
                json.optString("senderUid", ""),
                json.optString("text", ""),
                json.optString("clientRequestId", ""),
                json.optLong("createdAt", System.currentTimeMillis())
        );
    }

    public String getMessageId() { return messageId; }
    public String getConversationId() { return conversationId; }
    public String getBookingId() { return bookingId; }
    public String getSenderUid() { return senderUid; }
    public String getText() { return text; }
    public String getClientRequestId() { return clientRequestId; }
    public long getCreatedAt() { return createdAt; }
}