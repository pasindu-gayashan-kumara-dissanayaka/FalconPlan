package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.ProviderRoom;

public interface ProviderRoomRepository {

    interface RoomsCallback {
        void onSuccess(List<ProviderRoom> rooms);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void createRoom(String providerUid, String listingId, ProviderRoom room, OperationCallback callback);

    void getListingRooms(String providerUid, String listingId, RoomsCallback callback);

    void updateRoom(String providerUid, String listingId, ProviderRoom room, OperationCallback callback);

    void deleteRoom(String providerUid, String listingId, String roomId, OperationCallback callback);
}