package lk.falconplan.app.data.repository;

import org.json.JSONObject;

import lk.falconplan.app.data.remote.BackendApiClient;

public class KycRepositoryImpl implements KycRepository {

    private final BackendApiClient apiClient;

    public KycRepositoryImpl() {
        this.apiClient = BackendApiClient.getInstance();
    }

    @Override
    public void getKycStatus(String idToken, KycStatusCallback callback) {
        apiClient.getKycStatus(idToken, new BackendApiClient.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception exception) {
                if (callback != null) callback.onError(exception);
            }
        });
    }

    @Override
    public void uploadDocument(String idToken, String documentType, String mimeType, String fileBase64, UploadCallback callback) {
        apiClient.uploadKycDocument(idToken, documentType, mimeType, fileBase64, new BackendApiClient.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception exception) {
                if (callback != null) callback.onError(exception);
            }
        });
    }

    @Override
    public void deleteDocument(String idToken, String documentId, OperationCallback callback) {
        apiClient.deleteKycDocument(idToken, documentId, new BackendApiClient.ApiCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(Exception exception) {
                if (callback != null) callback.onError(exception);
            }
        });
    }

    @Override
    public void submitKycApplication(String idToken, KycStatusCallback callback) {
        apiClient.submitKycApplication(idToken, new BackendApiClient.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception exception) {
                if (callback != null) callback.onError(exception);
            }
        });
    }
}