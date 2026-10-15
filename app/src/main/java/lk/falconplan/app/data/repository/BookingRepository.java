package lk.falconplan.app.data.repository;

import org.json.JSONArray;
import org.json.JSONObject;

import lk.falconplan.app.data.model.Booking;
import lk.falconplan.app.data.model.BookingQuote;

public interface BookingRepository {

    interface PublicRoomsCallback {
        void onSuccess(JSONArray rooms);
        void onError(Exception exception);
    }

    interface QuoteCallback {
        void onSuccess(BookingQuote quote);
        void onError(Exception exception);
    }

    interface BookingCallback {
        void onSuccess(Booking booking);
        void onError(Exception exception);
    }

    interface BookingsListCallback {
        void onSuccess(java.util.List<Booking> bookings);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void getPublicRooms(String listingId, PublicRoomsCallback callback);

    void getBookingQuote(String listingId, String roomId, String checkInDate, String checkOutDate,
                         int guests, int roomUnitsRequested, boolean selectedBreakfast, QuoteCallback callback);

    void createBooking(String idToken, String listingId, String roomId, String checkInDate,
                       String checkOutDate, int guests, int roomUnitsRequested, boolean selectedBreakfast, BookingCallback callback);

    void getTravelerBookings(String idToken, BookingsListCallback callback);

    void cancelBooking(String idToken, String bookingId, OperationCallback callback);
}