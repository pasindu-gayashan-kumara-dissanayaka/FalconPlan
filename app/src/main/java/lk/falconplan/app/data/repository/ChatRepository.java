package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.ChatConversation;
import lk.falconplan.app.data.model.ChatMessage;

public interface ChatRepository {

    interface ConversationCallback {
        void onSuccess(ChatConversation conversation);
        void onError(Exception exception);
    }

    interface ConversationsListCallback {
        void onSuccess(List<ChatConversation> conversations);
        void onError(Exception exception);
    }

    interface MessageCallback {
        void onSuccess(ChatMessage message);
        void onError(Exception exception);
    }

    interface MessagesListCallback {
        void onSuccess(List<ChatMessage> messages);
        void onError(Exception exception);
    }

    void initiateConversation(String idToken, String bookingId, ConversationCallback callback);

    void getMyConversations(String idToken, ConversationsListCallback callback);

    void sendMessage(String idToken, String conversationId, String text, String clientRequestId, MessageCallback callback);

    void getMessages(String idToken, String conversationId, int page, int limit, MessagesListCallback callback);
}