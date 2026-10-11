package lk.falconplan.app.data.repository;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.data.model.SavedPlace;

public class SavedPlacesRepositoryImpl implements SavedPlacesRepository {

    public static final String USERS_COLLECTION = "users";
    public static final String SAVED_PLACES_COLLECTION = "savedPlaces";

    private final FirebaseFirestore db;

    public SavedPlacesRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    public SavedPlacesRepositoryImpl(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void savePlace(String uid, SavedPlace place, OperationCallback callback) {
        if (uid == null || uid.isEmpty() || place == null || place.getDestinationId() == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid user or destination data"));
            return;
        }

        if (!place.isValidCoordinates()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Coordinates out of valid geographical bounds (-90 to 90, -180 to 180)"));
            return;
        }

        // Use destinationId as the document ID in savedPlaces collection to enforce unique saves per destination
        String docId = place.getDestinationId();
        place.setId(docId);

        db.collection(USERS_COLLECTION).document(uid)
                .collection(SAVED_PLACES_COLLECTION).document(docId)
                .set(place.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void getSavedPlaces(String uid, SavedPlacesCallback callback) {
        if (uid == null || uid.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("UID cannot be empty"));
            return;
        }

        db.collection(USERS_COLLECTION).document(uid)
                .collection(SAVED_PLACES_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<SavedPlace> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        SavedPlace place = doc.toObject(SavedPlace.class);
                        if (place != null) {
                            list.add(place);
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void updateNote(String uid, String savedPlaceId, String newNote, OperationCallback callback) {
        if (uid == null || uid.isEmpty() || savedPlaceId == null || savedPlaceId.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid UID or Saved Place ID"));
            return;
        }

        db.collection(USERS_COLLECTION).document(uid)
                .collection(SAVED_PLACES_COLLECTION).document(savedPlaceId)
                .update("note", newNote != null ? newNote : "", "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void deleteSavedPlace(String uid, String savedPlaceId, OperationCallback callback) {
        if (uid == null || uid.isEmpty() || savedPlaceId == null || savedPlaceId.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid UID or Saved Place ID"));
            return;
        }

        db.collection(USERS_COLLECTION).document(uid)
                .collection(SAVED_PLACES_COLLECTION).document(savedPlaceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void isPlaceSaved(String uid, String destinationId, CheckCallback callback) {
        if (uid == null || uid.isEmpty() || destinationId == null || destinationId.isEmpty()) {
            if (callback != null) callback.onResult(false);
            return;
        }

        db.collection(USERS_COLLECTION).document(uid)
                .collection(SAVED_PLACES_COLLECTION).document(destinationId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (callback != null) callback.onResult(doc.exists());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }
}