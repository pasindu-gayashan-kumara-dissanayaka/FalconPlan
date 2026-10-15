package lk.falconplan.app.data.repository;

import org.json.JSONObject;

import lk.falconplan.app.data.remote.BackendApiClient;

public class RoomAvailabilityRepositoryImpl implements RoomAvailabilityRepository {

    private final BackendApiClient apiClient;

    public RoomAvailabilityRepositoryImpl() {
        this.apiClient = BackendApiClient.getInstance();
    }

    @Override
    public void getRoomAvailability(String idToken, String listingId, String roomId, AvailabilityCallback callback) {
        apiClient.getRoomAvailability(idToken, listingId, roomId, new BackendApiClient.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception exception) {
                if (callback != null) callback.onError(exception);
            }
        });
    }

    @Override
    public void updateRoomAvailability(String idToken, String listingId, String roomId, String dateStr, int blockedUnits, String note, OperationCallback callback) {
        apiClient.updateRoomAvailability(idToken, listingId, roomId, dateStr, blockedUnits, note, new BackendApiClient.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(Exception exception) {
                if (callback != null) callback.onError(exception);
            }
        });
    }
}