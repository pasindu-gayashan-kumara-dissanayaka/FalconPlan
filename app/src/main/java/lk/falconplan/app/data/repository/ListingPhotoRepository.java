package lk.falconplan.app.data.repository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public interface ListingPhotoRepository {

    interface PhotosCallback {
        void onSuccess(JSONArray photos);
        void onError(Exception exception);
    }

    interface UploadCallback {
        void onSuccess(JSONObject photoData);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void getListingPhotos(String idToken, String listingId, PhotosCallback callback);

    void uploadListingPhoto(String idToken, String listingId, String mimeType, String fileBase64, boolean isCover, UploadCallback callback);

    void reorderListingPhotos(String idToken, String listingId, List<String> photoOrder, OperationCallback callback);

    void setCoverPhoto(String idToken, String listingId, String photoId, OperationCallback callback);

    void deleteListingPhoto(String idToken, String listingId, String photoId, OperationCallback callback);
}