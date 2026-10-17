package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.ReviewItem;

public interface ReviewRepository {

    interface EligibilityCallback {
        void onSuccess(boolean eligible, boolean reviewed, String listingId);
        void onError(Exception exception);
    }

    interface ReviewsListCallback {
        void onSuccess(List<ReviewItem> reviews);
        void onError(Exception exception);
    }

    interface ReviewSubmitCallback {
        void onSuccess(ReviewItem review);
        void onError(Exception exception);
    }

    void checkEligibility(String idToken, String bookingId, EligibilityCallback callback);

    void submitReview(String idToken, String bookingId, int rating, String reviewText, ReviewSubmitCallback callback);

    void getListingReviews(String listingId, int page, int limit, ReviewsListCallback callback);
}