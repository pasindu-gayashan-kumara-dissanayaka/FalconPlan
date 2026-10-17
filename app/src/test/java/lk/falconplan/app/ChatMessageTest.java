package lk.falconplan.app;

import org.junit.Test;

import lk.falconplan.app.data.model.ChatConversation;
import lk.falconplan.app.data.model.ChatMessage;

import static org.junit.Assert.*;

public class ChatMessageTest {

    @Test
    public void chatMessage_fields_parsedAndRetrievedCorrectly() {
        ChatMessage msg = new ChatMessage(
                "msg_101",
                "chat_booking_1001",
                "booking_1001",
                "traveler_123",
                "Hello! Excited for our stay.",
                "req_abc_123",
                System.currentTimeMillis()
        );

        assertEquals("msg_101", msg.getMessageId());
        assertEquals("chat_booking_1001", msg.getConversationId());
        assertEquals("booking_1001", msg.getBookingId());
        assertEquals("traveler_123", msg.getSenderUid());
        assertEquals("Hello! Excited for our stay.", msg.getText());
        assertEquals("req_abc_123", msg.getClientRequestId());
    }

    @Test
    public void chatConversation_fields_parsedAndRetrievedCorrectly() {
        ChatConversation conv = new ChatConversation(
                "chat_booking_1001",
                "booking_1001",
                "traveler_123",
                "provider_456",
                "Looking forward to welcoming you!",
                System.currentTimeMillis()
        );

        assertEquals("chat_booking_1001", conv.getConversationId());
        assertEquals("booking_1001", conv.getBookingId());
        assertEquals("traveler_123", conv.getTravelerUid());
        assertEquals("provider_456", conv.getProviderUid());
        assertEquals("Looking forward to welcoming you!", conv.getLastMessageText());
    }
}