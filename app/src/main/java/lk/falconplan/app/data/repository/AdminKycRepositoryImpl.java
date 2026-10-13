package lk.falconplan.app.data.repository;

import org.json.JSONArray;
import org.json.JSONObject;

import lk.falconplan.app.data.remote.BackendApiClient;

public class AdminKycRepositoryImpl implements AdminKycRepository {

    private final BackendApiClient apiClient;

    public AdminKycRepositoryImpl() {
        this.apiClient = BackendApiClient.getInstance();
    }

    @Override
    public void getAdminKycApplications(String idToken, String statusFilter, int page, int limit, ApplicationsCallback callback) {
        apiClient.getAdminKycApplications(idToken, statusFilter, page, limit, new BackendApiClient.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray result) {
                if (callback != null) callback.onSuccess(result != null ? result : new JSONArray());
            }

            @Override
            public void onError(Exception exception) {
                if (callback != null) callback.onError(exception);
            }
        });
    }

    @Override
    public void getAdminKycDetail(String idToken, String providerUid, ApplicationDetailCallback callback) {
        apiClient.getAdminKycDetail(idToken, providerUid, new BackendApiClient.ApiCallback<JSONObject>() {
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
    public void getAdminDocumentPreview(String idToken, String providerUid, String documentId, PreviewCallback callback) {
        apiClient.getAdminDocumentPreview(idToken, providerUid, documentId, new BackendApiClient.ApiCallback<JSONObject>() {
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
    public void submitAdminDecision(String idToken, String providerUid, String decision, String reason, OperationCallback callback) {
        apiClient.submitAdminDecision(idToken, providerUid, decision, reason, new BackendApiClient.ApiCallback<JSONObject>() {
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