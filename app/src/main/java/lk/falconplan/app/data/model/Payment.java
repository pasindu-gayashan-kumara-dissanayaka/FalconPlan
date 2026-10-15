package lk.falconplan.app.data.model;

import org.json.JSONObject;

import java.util.Locale;

public class Payment {

    public static final String METHOD_BANK_TRANSFER = "bank_transfer";
    public static final String METHOD_CASH_ON_ARRIVAL = "cash_on_arrival";

    public static final String STATUS_AWAITING_PROOF = "awaiting_proof";
    public static final String STATUS_UNDER_REVIEW = "under_review";
    public static final String STATUS_VERIFIED = "verified";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_DUE_ON_ARRIVAL = "due_on_arrival";
    public static final String STATUS_COLLECTED = "collected";

    private String paymentId;
    private String bookingId;
    private String travelerUid;
    private String providerUid;
    private String listingId;
    private long amountDueLkr;
    private String currency;
    private String method;
    private String status;
    private long createdAt;
    private long updatedAt;

    public Payment() {
        // Required empty constructor
    }

    public Payment(String paymentId, String bookingId, String travelerUid, String providerUid,
                   String listingId, long amountDueLkr, String currency, String method,
                   String status, long createdAt, long updatedAt) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.travelerUid = travelerUid;
        this.providerUid = providerUid;
        this.listingId = listingId;
        this.amountDueLkr = amountDueLkr;
        this.currency = currency != null ? currency : "LKR";
        this.method = method != null ? method : METHOD_BANK_TRANSFER;
        this.status = status != null ? status : STATUS_AWAITING_PROOF;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment fromJson(JSONObject json) {
        if (json == null) return null;
        return new Payment(
                json.optString("paymentId", ""),
                json.optString("bookingId", ""),
                json.optString("travelerUid", ""),
                json.optString("providerUid", ""),
                json.optString("listingId", ""),
                json.optLong("amountDueLkr", 0),
                json.optString("currency", "LKR"),
                json.optString("method", METHOD_BANK_TRANSFER),
                json.optString("status", STATUS_AWAITING_PROOF),
                json.optLong("createdAt", System.currentTimeMillis()),
                json.optLong("updatedAt", System.currentTimeMillis())
        );
    }

    public String getPaymentId() { return paymentId; }
    public String getBookingId() { return bookingId; }
    public String getTravelerUid() { return travelerUid; }
    public String getProviderUid() { return providerUid; }
    public String getListingId() { return listingId; }
    public long getAmountDueLkr() { return amountDueLkr; }
    public String getCurrency() { return currency; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public String getFormattedStatus() {
        if (STATUS_UNDER_REVIEW.equals(status)) {
            return "Proof Submitted – Under Review";
        } else if (STATUS_VERIFIED.equals(status)) {
            return "Verified Paid";
        } else if (STATUS_REJECTED.equals(status)) {
            return "Proof Rejected";
        } else if (STATUS_DUE_ON_ARRIVAL.equals(status)) {
            return "Due On Arrival (Cash)";
        } else if (STATUS_COLLECTED.equals(status)) {
            return "Collected (Cash)";
        } else {
            return "Awaiting Bank Receipt Proof";
        }
    }

    public String getFormattedAmount() {
        return String.format(Locale.US, "Rs. %,d LKR", amountDueLkr);
    }
}