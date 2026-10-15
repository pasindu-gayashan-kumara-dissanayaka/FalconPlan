package lk.falconplan.app.data.repository;

import org.json.JSONObject;

public interface RoomAvailabilityRepository {

    interface AvailabilityCallback {
        void onSuccess(JSONObject data);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void getRoomAvailability(String idToken, String listingId, String roomId, AvailabilityCallback callback);

    void updateRoomAvailability(String idToken, String listingId, String roomId, String dateStr, int blockedUnits, String note, OperationCallback callback);
}