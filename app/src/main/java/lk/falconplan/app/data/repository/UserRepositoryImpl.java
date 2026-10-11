package lk.falconplan.app.data.repository;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.falconplan.app.data.model.UserProfile;

public class UserRepositoryImpl implements UserRepository {

    public static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore db;

    public UserRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    public UserRepositoryImpl(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void getUserProfile(String uid, ProfileCallback callback) {
        if (uid == null || uid.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("UID cannot be empty"));
            return;
        }

        db.collection(USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                        if (callback != null) callback.onSuccess(profile);
                    } else {
                        if (callback != null) callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void createUserProfile(UserProfile profile, OperationCallback callback) {
        if (profile == null || profile.getUid() == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid profile data"));
            return;
        }

        db.collection(USERS_COLLECTION).document(profile.getUid())
                .set(profile.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void updateActiveMode(String uid, String activeMode, OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("UID cannot be empty"));
            return;
        }

        if (!UserProfile.MODE_TRAVELER.equals(activeMode) && !UserProfile.MODE_PROVIDER.equals(activeMode)) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid mode. Allowed: traveler or provider"));
            return;
        }

        db.collection(USERS_COLLECTION).document(uid)
                .update("activeMode", activeMode, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }
}