package lk.falconplan.app.data.repository;

import org.json.JSONArray;
import org.json.JSONObject;

public interface AdminKycRepository {

    interface ApplicationsCallback {
        void onSuccess(JSONArray applications);
        void onError(Exception exception);
    }

    interface ApplicationDetailCallback {
        void onSuccess(JSONObject detailData);
        void onError(Exception exception);
    }

    interface PreviewCallback {
        void onSuccess(JSONObject previewData);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess(JSONObject resultData);
        void onError(Exception exception);
    }

    void getAdminKycApplications(String idToken, String statusFilter, int page, int limit, ApplicationsCallback callback);

    void getAdminKycDetail(String idToken, String providerUid, ApplicationDetailCallback callback);

    void getAdminDocumentPreview(String idToken, String providerUid, String documentId, PreviewCallback callback);

    void submitAdminDecision(String idToken, String providerUid, String decision, String reason, OperationCallback callback);
}