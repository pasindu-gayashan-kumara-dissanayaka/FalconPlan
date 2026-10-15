package lk.falconplan.app.data.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.falconplan.app.data.model.RoomPricing;

public class RoomPricingRepositoryImpl implements RoomPricingRepository {

    public static final String PROVIDER_PROFILES_COLLECTION = "providerProfiles";
    public static final String LISTINGS_COLLECTION = "listings";
    public static final String ROOMS_COLLECTION = "rooms";
    public static final String PRICING_COLLECTION = "pricing";
    public static final String CURRENT_PRICING_DOC = "current";

    private final FirebaseFirestore db;

    public RoomPricingRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    public RoomPricingRepositoryImpl(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void savePricing(String providerUid, String listingId, String roomId, RoomPricing pricing, OperationCallback callback) {
        if (providerUid == null || providerUid.isEmpty() || listingId == null || listingId.isEmpty() || roomId == null || roomId.isEmpty() || pricing == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        DocumentReference docRef = db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document(listingId)
                .collection(ROOMS_COLLECTION).document(roomId)
                .collection(PRICING_COLLECTION).document(CURRENT_PRICING_DOC);

        pricing.setUpdatedAt(System.currentTimeMillis());

        docRef.set(pricing.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void getRoomPricing(String providerUid, String listingId, String roomId, PricingCallback callback) {
        if (providerUid == null || providerUid.isEmpty() || listingId == null || listingId.isEmpty() || roomId == null || roomId.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document(listingId)
                .collection(ROOMS_COLLECTION).document(roomId)
                .collection(PRICING_COLLECTION).document(CURRENT_PRICING_DOC)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        RoomPricing pricing = doc.toObject(RoomPricing.class);
                        if (callback != null) callback.onSuccess(pricing);
                    } else {
                        if (callback != null) callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void deletePricing(String providerUid, String listingId, String roomId, OperationCallback callback) {
        if (providerUid == null || listingId == null || roomId == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document(listingId)
                .collection(ROOMS_COLLECTION).document(roomId)
                .collection(PRICING_COLLECTION).document(CURRENT_PRICING_DOC)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }
}