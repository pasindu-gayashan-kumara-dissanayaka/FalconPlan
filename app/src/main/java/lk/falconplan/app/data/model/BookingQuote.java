package lk.falconplan.app.data.model;

import org.json.JSONObject;

import java.util.Locale;

public class BookingQuote {

    private String listingId;
    private String roomId;
    private String checkInDate;
    private String checkOutDate;
    private int nights;
    private int guests;
    private int roomUnitsRequested;
    private String currency;
    private long pricePerNightLkr;
    private long extraGuestFeeLkr;
    private long breakfastFeePerGuestLkr;
    private long quotedTotalLkr;

    public BookingQuote(String listingId, String roomId, String checkInDate, String checkOutDate,
                        int nights, int guests, int roomUnitsRequested, String currency,
                        long pricePerNightLkr, long extraGuestFeeLkr, long breakfastFeePerGuestLkr,
                        long quotedTotalLkr) {
        this.listingId = listingId;
        this.roomId = roomId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.nights = nights;
        this.guests = guests;
        this.roomUnitsRequested = roomUnitsRequested;
        this.currency = currency != null ? currency : "LKR";
        this.pricePerNightLkr = pricePerNightLkr;
        this.extraGuestFeeLkr = extraGuestFeeLkr;
        this.breakfastFeePerGuestLkr = breakfastFeePerGuestLkr;
        this.quotedTotalLkr = quotedTotalLkr;
    }

    public static BookingQuote fromJson(JSONObject json) {
        if (json == null) return null;
        return new BookingQuote(
                json.optString("listingId", ""),
                json.optString("roomId", ""),
                json.optString("checkInDate", ""),
                json.optString("checkOutDate", ""),
                json.optInt("nights", 1),
                json.optInt("guests", 1),
                json.optInt("roomUnitsRequested", 1),
                json.optString("currency", "LKR"),
                json.optLong("pricePerNightLkr", 0),
                json.optLong("extraGuestFeeLkr", 0),
                json.optLong("breakfastFeePerGuestLkr", 0),
                json.optLong("quotedTotalLkr", 0)
        );
    }

    public String getListingId() { return listingId; }
    public String getRoomId() { return roomId; }
    public String getCheckInDate() { return checkInDate; }
    public String getCheckOutDate() { return checkOutDate; }
    public int getNights() { return nights; }
    public int getGuests() { return guests; }
    public int getRoomUnitsRequested() { return roomUnitsRequested; }
    public String getCurrency() { return currency; }
    public long getPricePerNightLkr() { return pricePerNightLkr; }
    public long getExtraGuestFeeLkr() { return extraGuestFeeLkr; }
    public long getBreakfastFeePerGuestLkr() { return breakfastFeePerGuestLkr; }
    public long getQuotedTotalLkr() { return quotedTotalLkr; }

    public String getFormattedPrice() {
        return String.format(Locale.US, "Rs. %,d LKR", quotedTotalLkr);
    }
}