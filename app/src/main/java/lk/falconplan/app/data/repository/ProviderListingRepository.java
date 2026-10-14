package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.ProviderListing;

public interface ProviderListingRepository {

    interface ListingsCallback {
        void onSuccess(List<ProviderListing> listings);
        void onError(Exception exception);
    }

    interface ListingCallback {
        void onSuccess(ProviderListing listing);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void createListing(String providerUid, ProviderListing listing, OperationCallback callback);

    void getProviderListings(String providerUid, ListingsCallback callback);

    void getListingDetails(String providerUid, String listingId, ListingCallback callback);

    void updateListing(String providerUid, ProviderListing listing, OperationCallback callback);

    void deleteListing(String providerUid, String listingId, OperationCallback callback);
}