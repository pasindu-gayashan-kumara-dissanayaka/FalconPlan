package lk.falconplan.app.data.repository;

import lk.falconplan.app.data.model.RoomPricing;

public interface RoomPricingRepository {

    interface PricingCallback {
        void onSuccess(RoomPricing pricing);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void savePricing(String providerUid, String listingId, String roomId, RoomPricing pricing, OperationCallback callback);

    void getRoomPricing(String providerUid, String listingId, String roomId, PricingCallback callback);

    void deletePricing(String providerUid, String listingId, String roomId, OperationCallback callback);
}