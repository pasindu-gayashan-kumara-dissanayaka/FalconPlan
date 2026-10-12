package lk.falconplan.app.data.repository;

import org.json.JSONObject;

import lk.falconplan.app.data.remote.BackendApiClient;

public interface KycRepository {

    interface KycStatusCallback {
        void onSuccess(JSONObject statusData);
        void onError(Exception exception);
    }

    interface UploadCallback {
        void onSuccess(JSONObject documentMetadata);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void getKycStatus(String idToken, KycStatusCallback callback);

    void uploadDocument(String idToken, String documentType, String mimeType, String fileBase64, UploadCallback callback);

    void deleteDocument(String idToken, String documentId, OperationCallback callback);

    void submitKycApplication(String idToken, KycStatusCallback callback);
}