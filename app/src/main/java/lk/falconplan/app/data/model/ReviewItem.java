package lk.falconplan.app.data.model;

import org.json.JSONObject;

public class ReviewItem {

    private String reviewId;
    private String listingId;
    private String bookingId;
    private String travelerUid;
    private int rating;
    private String reviewText;
    private String status;
    private long createdAt;

    public ReviewItem() {}

    public ReviewItem(String reviewId, String listingId, String bookingId, String travelerUid,
                      int rating, String reviewText, String status, long createdAt) {
        this.reviewId = reviewId;
        this.listingId = listingId;
        this.bookingId = bookingId;
        this.travelerUid = travelerUid;
        this.rating = rating;
        this.reviewText = reviewText != null ? reviewText : "";
        this.status = status != null ? status : "published";
        this.createdAt = createdAt;
    }

    public static ReviewItem fromJson(JSONObject json) {
        if (json == null) return null;
        return new ReviewItem(
                json.optString("reviewId", ""),
                json.optString("listingId", ""),
                json.optString("bookingId", ""),
                json.optString("travelerUid", ""),
                json.optInt("rating", 5),
                json.optString("reviewText", ""),
                json.optString("status", "published"),
                json.optLong("createdAt", System.currentTimeMillis())
        );
    }

    public String getReviewId() { return reviewId; }
    public String getListingId() { return listingId; }
    public String getBookingId() { return bookingId; }
    public String getTravelerUid() { return travelerUid; }
    public int getRating() { return rating; }
    public String getReviewText() { return reviewText; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
}