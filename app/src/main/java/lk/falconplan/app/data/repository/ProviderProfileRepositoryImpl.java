package lk.falconplan.app.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;

import lk.falconplan.app.data.model.ProviderProfile;

public class ProviderProfileRepositoryImpl implements ProviderProfileRepository {

    public static final String PROVIDER_PROFILES_COLLECTION = "providerProfiles";

    private final FirebaseFirestore db;

    public ProviderProfileRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    public ProviderProfileRepositoryImpl(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public void createProviderProfile(ProviderProfile profile, OperationCallback callback) {
        if (profile == null || profile.getUid() == null || profile.getUid().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid profile or UID"));
            return;
        }

        if (!profile.isValid()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Please complete all required fields"));
            return;
        }

        db.collection(PROVIDER_PROFILES_COLLECTION).document(profile.getUid())
                .set(profile.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    @Override
    public void getProviderProfile(String uid, ProfileCallback callback) {
        if (uid == null || uid.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("UID cannot be empty"));
            return;
        }

        db.collection(PROVIDER_PROFILES_COLLECTION).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        ProviderProfile profile = doc.toObject(ProviderProfile.class);
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
    public void updateProviderProfile(ProviderProfile profile, OperationCallback callback) {
        if (profile == null || profile.getUid() == null || profile.getUid().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Invalid profile or UID"));
            return;
        }

        profile.setUpdatedAt(System.currentTimeMillis());

        db.collection(PROVIDER_PROFILES_COLLECTION).document(profile.getUid())
                .update(profile.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }
}