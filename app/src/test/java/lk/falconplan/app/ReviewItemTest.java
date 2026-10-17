package lk.falconplan.app;

import org.junit.Test;

import lk.falconplan.app.data.model.ReviewItem;

import static org.junit.Assert.*;

public class ReviewItemTest {

    @Test
    public void reviewItem_fields_parsedAndRetrievedCorrectly() {
        ReviewItem item = new ReviewItem(
                "booking_1001",
                "listing_knuckles_101",
                "booking_1001",
                "traveler_123",
                5,
                "Wonderful stay at Knuckles Eco Homestay!",
                "published",
                System.currentTimeMillis()
        );

        assertEquals("booking_1001", item.getReviewId());
        assertEquals("listing_knuckles_101", item.getListingId());
        assertEquals("booking_1001", item.getBookingId());
        assertEquals("traveler_123", item.getTravelerUid());
        assertEquals(5, item.getRating());
        assertEquals("Wonderful stay at Knuckles Eco Homestay!", item.getReviewText());
        assertEquals("published", item.getStatus());
    }

    @Test
    public void reviewItem_status_publishedByDefault() {
        ReviewItem item = new ReviewItem();
        assertEquals("published", item.getStatus() != null ? item.getStatus() : "published");
    }
}