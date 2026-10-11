package lk.falconplan.app.data.repository;

import lk.falconplan.app.data.model.UserProfile;

public interface UserRepository {

    interface ProfileCallback {
        void onSuccess(UserProfile profile);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void getUserProfile(String uid, ProfileCallback callback);

    void createUserProfile(UserProfile profile, OperationCallback callback);

    void updateActiveMode(String uid, String activeMode, OperationCallback callback);
}