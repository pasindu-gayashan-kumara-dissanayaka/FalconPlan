package lk.falconplan.app.data.repository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.data.model.ProviderRoom;

public class ProviderRoomRepositoryImpl implements ProviderRoomRepository {

    public static final String PROVIDER_PROFILES_COLLECTION = "providerProfiles";
    public static final String LISTINGS_COLLECTION = "listings";
    public static final String ROOMS_COLLECTION = "rooms";

    private final FirebaseFirestore db;

    public ProviderRoomRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    public ProviderRoomRepositoryImpl(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void createRoom(String providerUid, String listingId, ProviderRoom room, OperationCallback callback) {
        if (providerUid == null || providerUid.isEmpty() || listingId == null || listingId.isEmpty() || room == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        if (!room.isValid()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Please complete required room inventory fields"));
            return;
        }

        DocumentReference docRef = db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document(listingId)
                .collection(ROOMS_COLLECTION).document();

        String roomId = docRef.getId();
        room.setId(roomId);
        room.setProviderUid(providerUid);
        room.setListingId(listingId);

        docRef.set(room.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void getListingRooms(String providerUid, String listingId, RoomsCallback callback) {
        if (providerUid == null || providerUid.isEmpty() || listingId == null || listingId.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document(listingId)
                .collection(ROOMS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    List<ProviderRoom> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshots) {
                        ProviderRoom item = doc.toObject(ProviderRoom.class);
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
    public void updateRoom(String providerUid, String listingId, ProviderRoom room, OperationCallback callback) {
        if (providerUid == null || listingId == null || room == null || room.getId() == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        room.setUpdatedAt(System.currentTimeMillis());

        db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document(listingId)
                .collection(ROOMS_COLLECTION).document(room.getId())
                .set(room.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void deleteRoom(String providerUid, String listingId, String roomId, OperationCallback callback) {
        if (providerUid == null || listingId == null || roomId == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        DocumentReference roomRef = db.collection(PROVIDER_PROFILES_COLLECTION).document(providerUid)
                .collection(LISTINGS_COLLECTION).document(listingId)
                .collection(ROOMS_COLLECTION).document(roomId);

        DocumentReference pricingRef = roomRef.collection("pricing").document("current");

        roomRef.collection("availabilityDays").get()
                .addOnSuccessListener(querySnapshots -> {
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : querySnapshots) {
                        batch.delete(doc.getReference());
                    }

                    batch.delete(pricingRef);
                    batch.delete(roomRef);

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }
}