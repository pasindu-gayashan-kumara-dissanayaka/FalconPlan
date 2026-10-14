package lk.falconplan.app.data.repository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import lk.falconplan.app.data.remote.BackendApiClient;

public class ListingPhotoRepositoryImpl implements ListingPhotoRepository {

    private final BackendApiClient apiClient;

    public ListingPhotoRepositoryImpl() {
        this.apiClient = BackendApiClient.getInstance();
    }

    @Override
    public void getListingPhotos(String idToken, String listingId, PhotosCallback callback) {
        apiClient.getAdminKycApplications(idToken, null, 1, 20, new BackendApiClient.ApiCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception exception) {
                if (callback != null) callback.onError(exception);
            }
        });
    }

    @Override
    public void uploadListingPhoto(String idToken, String listingId, String mimeType, String fileBase64, boolean isCover, UploadCallback callback) {
        apiClient.uploadKycDocument(idToken, "homestay_photo", mimeType, fileBase64, new BackendApiClient.ApiCallback<JSONObject>() {
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
    public void reorderListingPhotos(String idToken, String listingId, List<String> photoOrder, OperationCallback callback) {
        if (callback != null) callback.onSuccess();
    }

    @Override
    public void setCoverPhoto(String idToken, String listingId, String photoId, OperationCallback callback) {
        if (callback != null) callback.onSuccess();
    }

    @Override
    public void deleteListingPhoto(String idToken, String listingId, String photoId, OperationCallback callback) {
        apiClient.deleteKycDocument(idToken, photoId, new BackendApiClient.ApiCallback<Void>() {
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
}