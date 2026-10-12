package lk.falconplan.app.data.repository;

import lk.falconplan.app.data.model.ProviderProfile;

public interface ProviderProfileRepository {

    interface ProfileCallback {
        void onSuccess(ProviderProfile profile);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void createProviderProfile(ProviderProfile profile, OperationCallback callback);

    void getProviderProfile(String uid, ProfileCallback callback);

    void updateProviderProfile(ProviderProfile profile, OperationCallback callback);
}