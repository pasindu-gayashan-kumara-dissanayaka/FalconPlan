package lk.falconplan.app;

import org.junit.Test;

import lk.falconplan.app.data.model.Booking;

import static org.junit.Assert.*;

public class BookingTest {

    @Test
    public void booking_pendingStatusFormatting_returnsAwaitingConfirmation() {
        Booking booking = new Booking(
                "booking_101",
                "traveler_123",
                "provider_123",
                "listing_123",
                "room_101",
                "2026-11-10",
                "2026-11-12",
                2,
                2,
                1,
                false,
                "LKR",
                9500,
                0,
                0,
                19000,
                Booking.STATUS_PENDING,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertEquals("Booking Request Sent – Awaiting Host Confirmation", booking.getFormattedStatus());
    }

    @Test
    public void booking_confirmedStatusFormatting_returnsConfirmedReservation() {
        Booking booking = new Booking(
                "booking_101",
                "traveler_123",
                "provider_123",
                "listing_123",
                "room_101",
                "2026-11-10",
                "2026-11-12",
                2,
                2,
                1,
                false,
                "LKR",
                9500,
                0,
                0,
                19000,
                Booking.STATUS_CONFIRMED,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertEquals("Confirmed Reservation", booking.getFormattedStatus());
    }

    @Test
    public void booking_declinedStatusFormatting_returnsDeclined() {
        Booking booking = new Booking(
                "booking_101",
                "traveler_123",
                "provider_123",
                "listing_123",
                "room_101",
                "2026-11-10",
                "2026-11-12",
                2,
                2,
                1,
                false,
                "LKR",
                9500,
                0,
                0,
                19000,
                Booking.STATUS_DECLINED,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertEquals("Declined", booking.getFormattedStatus());
    }

    @Test
    public void booking_priceFormatting_returnsLkrFormatted() {
        Booking booking = new Booking(
                "booking_101",
                "traveler_123",
                "provider_123",
                "listing_123",
                "room_101",
                "2026-11-10",
                "2026-11-12",
                2,
                2,
                1,
                false,
                "LKR",
                9500,
                0,
                0,
                19000,
                Booking.STATUS_PENDING,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertEquals("Rs. 19,000 LKR", booking.getFormattedPrice());
    }
}