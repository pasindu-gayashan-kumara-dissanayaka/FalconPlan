package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.Booking;

public interface ProviderBookingRepository {

    interface BookingsListCallback {
        void onSuccess(List<Booking> bookings);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess(String status);
        void onError(Exception exception);
    }

    void getProviderBookings(String idToken, BookingsListCallback callback);

    void submitBookingDecision(String idToken, String bookingId, String decision, OperationCallback callback);
}