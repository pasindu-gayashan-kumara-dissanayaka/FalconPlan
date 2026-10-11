package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.SavedPlace;

public interface SavedPlacesRepository {

    interface SavedPlacesCallback {
        void onSuccess(List<SavedPlace> places);
        void onError(Exception exception);
    }

    interface CheckCallback {
        void onResult(boolean isSaved);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void savePlace(String uid, SavedPlace place, OperationCallback callback);

    void getSavedPlaces(String uid, SavedPlacesCallback callback);

    void updateNote(String uid, String savedPlaceId, String newNote, OperationCallback callback);

    void deleteSavedPlace(String uid, String savedPlaceId, OperationCallback callback);

    void isPlaceSaved(String uid, String destinationId, CheckCallback callback);
}