package lk.falconplan.app;

import org.junit.Test;

import lk.falconplan.app.data.model.NotificationItem;

import static org.junit.Assert.*;

public class NotificationItemTest {

    @Test
    public void notificationItem_fields_parsedAndRetrievedCorrectly() {
        NotificationItem item = new NotificationItem(
                "notif_101",
                "user_123",
                NotificationItem.TYPE_BOOKING_CREATED,
                "event_b101",
                "booking",
                "booking_1001",
                "New Booking Request",
                "A traveler requested a homestay booking.",
                false,
                System.currentTimeMillis()
        );

        assertEquals("notif_101", item.getNotificationId());
        assertEquals("user_123", item.getRecipientUid());
        assertEquals("booking_created", item.getType());
        assertEquals("booking", item.getEntityType());
        assertEquals("booking_1001", item.getEntityId());
        assertEquals("New Booking Request", item.getTitle());
        assertEquals("A traveler requested a homestay booking.", item.getBody());
        assertFalse(item.isRead());
    }

    @Test
    public void notificationItem_readState_updatesCorrectly() {
        NotificationItem item = new NotificationItem(
                "notif_102",
                "user_123",
                NotificationItem.TYPE_CHAT_MESSAGE,
                "event_c101",
                "chat",
                "chat_booking_1001",
                "New Message",
                "You have a new private message.",
                false,
                System.currentTimeMillis()
        );

        assertFalse(item.isRead());
        item.setRead(true);
        assertTrue(item.isRead());
    }
}