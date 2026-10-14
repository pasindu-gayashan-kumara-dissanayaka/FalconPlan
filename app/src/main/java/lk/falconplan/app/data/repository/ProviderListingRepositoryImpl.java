package lk.falconplan.app.data.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.data.model.ProviderListing;

public class ProviderListingRepositoryImpl implements ProviderListingRepository {

    public static final String PROVIDER_PROFILES_COLLECTION = "providerProfiles";
    public static final String LISTINGS_COLLECTION = "listings";

    private final FirebaseFirestore db;

    public ProviderListingRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    public ProviderListingRepositoryImpl(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void createListing(String providerUid, ProviderListing listing, OperationCallback callback) {
        if (providerUid == null || providerUid.isEmpty() || listing == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid provider or listing data"));
            return;
        }

        if (!listing.isValid()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Please complete all required listing fields"));
            return;
        }

        DocumentReference docRef = db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document();

        String listingId = docRef.getId();
        listing.setId(listingId);
        listing.setProviderUid(providerUid);

        docRef.set(listing.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void getProviderListings(String providerUid, ListingsCallback callback) {
        if (providerUid == null || providerUid.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Provider UID cannot be empty"));
            return;
        }

        db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ProviderListing> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ProviderListing item = doc.toObject(ProviderListing.class);
                        if (item != null) {
                            list.add(item);
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void getListingDetails(String providerUid, String listingId, ListingCallback callback) {
        if (providerUid == null || listingId == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document(listingId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        ProviderListing item = doc.toObject(ProviderListing.class);
                        if (callback != null) callback.onSuccess(item);
                    } else {
                        if (callback != null) callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void updateListing(String providerUid, ProviderListing listing, OperationCallback callback) {
        if (providerUid == null || listing == null || listing.getId() == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        listing.setUpdatedAt(System.currentTimeMillis());

        db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document(listing.getId())
                .set(listing.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void deleteListing(String providerUid, String listingId, OperationCallback callback) {
        if (providerUid == null || listingId == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document(listingId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }
}