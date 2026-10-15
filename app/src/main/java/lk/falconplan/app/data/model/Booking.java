package lk.falconplan.app.data.model;

import org.json.JSONObject;

import java.util.Locale;

public class Booking {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_DECLINED = "declined";
    public static final String STATUS_CANCELLED = "cancelled";

    private String bookingId;
    private String travelerUid;
    private String providerUid;
    private String listingId;
    private String roomId;
    private String checkInDate;
    private String checkOutDate;
    private int nights;
    private int guests;
    private int roomUnitsRequested;
    private boolean selectedBreakfast;
    private String currency;
    private long pricePerNightLkr;
    private long extraGuestFeeLkr;
    private long breakfastFeePerGuestLkr;
    private long quotedTotalLkr;
    private String status;
    private long createdAt;
    private long updatedAt;

    public Booking() {
        // Required empty constructor
    }

    public Booking(String bookingId, String travelerUid, String providerUid, String listingId,
                   String roomId, String checkInDate, String checkOutDate, int nights, int guests,
                   int roomUnitsRequested, boolean selectedBreakfast, String currency,
                   long pricePerNightLkr, long extraGuestFeeLkr, long breakfastFeePerGuestLkr,
                   long quotedTotalLkr, String status, long createdAt, long updatedAt) {
        this.bookingId = bookingId;
        this.travelerUid = travelerUid;
        this.providerUid = providerUid;
        this.listingId = listingId;
        this.roomId = roomId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.nights = nights;
        this.guests = guests;
        this.roomUnitsRequested = roomUnitsRequested;
        this.selectedBreakfast = selectedBreakfast;
        this.currency = currency != null ? currency : "LKR";
        this.pricePerNightLkr = pricePerNightLkr;
        this.extraGuestFeeLkr = extraGuestFeeLkr;
        this.breakfastFeePerGuestLkr = breakfastFeePerGuestLkr;
        this.quotedTotalLkr = quotedTotalLkr;
        this.status = status != null ? status : STATUS_PENDING;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Booking fromJson(JSONObject json) {
        if (json == null) return null;
        return new Booking(
                json.optString("bookingId", ""),
                json.optString("travelerUid", ""),
                json.optString("providerUid", ""),
                json.optString("listingId", ""),
                json.optString("roomId", ""),
                json.optString("checkInDate", ""),
                json.optString("checkOutDate", ""),
                json.optInt("nights", 1),
                json.optInt("guests", 1),
                json.optInt("roomUnitsRequested", 1),
                json.optBoolean("selectedBreakfast", false),
                json.optString("currency", "LKR"),
                json.optLong("pricePerNightLkr", 0),
                json.optLong("extraGuestFeeLkr", 0),
                json.optLong("breakfastFeePerGuestLkr", 0),
                json.optLong("quotedTotalLkr", 0),
                json.optString("status", STATUS_PENDING),
                json.optLong("createdAt", System.currentTimeMillis()),
                json.optLong("updatedAt", System.currentTimeMillis())
        );
    }

    public String getBookingId() { return bookingId; }
    public String getTravelerUid() { return travelerUid; }
    public String getProviderUid() { return providerUid; }
    public String getListingId() { return listingId; }
    public String getRoomId() { return roomId; }
    public String getCheckInDate() { return checkInDate; }
    public String getCheckOutDate() { return checkOutDate; }
    public int getNights() { return nights; }
    public int getGuests() { return guests; }
    public int getRoomUnitsRequested() { return roomUnitsRequested; }
    public boolean isSelectedBreakfast() { return selectedBreakfast; }
    public String getCurrency() { return currency; }
    public long getPricePerNightLkr() { return pricePerNightLkr; }
    public long getExtraGuestFeeLkr() { return extraGuestFeeLkr; }
    public long getBreakfastFeePerGuestLkr() { return breakfastFeePerGuestLkr; }
    public long getQuotedTotalLkr() { return quotedTotalLkr; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public String getFormattedStatus() {
        if (STATUS_CONFIRMED.equals(status)) {
            return "Confirmed Reservation";
        } else if (STATUS_DECLINED.equals(status)) {
            return "Declined";
        } else if (STATUS_CANCELLED.equals(status)) {
            return "Cancelled";
        } else {
            return "Booking Request Sent – Awaiting Host Confirmation";
        }
    }

    public String getFormattedPrice() {
        return String.format(Locale.US, "Rs. %,d LKR", quotedTotalLkr);
    }
}