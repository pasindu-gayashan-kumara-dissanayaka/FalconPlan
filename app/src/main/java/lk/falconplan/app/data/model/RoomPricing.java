package lk.falconplan.app.data.model;

import java.util.HashMap;
import java.util.Map;

public class RoomPricing {

    public static final String CURRENCY_LKR = "LKR";

    private String currency;
    private long pricePerNightLkr;
    private int includedGuests;
    private long extraGuestFeeLkr;
    private boolean breakfastOffered;
    private long breakfastFeePerGuestLkr;
    private long createdAt;
    private long updatedAt;

    public RoomPricing() {
        // Required empty constructor for Firestore
    }

    public RoomPricing(String currency, long pricePerNightLkr, int includedGuests,
                       long extraGuestFeeLkr, boolean breakfastOffered,
                       long breakfastFeePerGuestLkr, long createdAt, long updatedAt) {
        this.currency = currency != null ? currency : CURRENCY_LKR;
        this.pricePerNightLkr = pricePerNightLkr;
        this.includedGuests = includedGuests;
        this.extraGuestFeeLkr = extraGuestFeeLkr;
        this.breakfastOffered = breakfastOffered;
        this.breakfastFeePerGuestLkr = breakfastFeePerGuestLkr;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency != null ? currency : CURRENCY_LKR; }

    public long getPricePerNightLkr() { return pricePerNightLkr; }
    public void setPricePerNightLkr(long pricePerNightLkr) { this.pricePerNightLkr = pricePerNightLkr; }

    public int getIncludedGuests() { return includedGuests; }
    public void setIncludedGuests(int includedGuests) { this.includedGuests = includedGuests; }

    public long getExtraGuestFeeLkr() { return extraGuestFeeLkr; }
    public void setExtraGuestFeeLkr(long extraGuestFeeLkr) { this.extraGuestFeeLkr = extraGuestFeeLkr; }

    public boolean isBreakfastOffered() { return breakfastOffered; }
    public void setBreakfastOffered(boolean breakfastOffered) { this.breakfastOffered = breakfastOffered; }

    public long getBreakfastFeePerGuestLkr() { return breakfastFeePerGuestLkr; }
    public void setBreakfastFeePerGuestLkr(long breakfastFeePerGuestLkr) { this.breakfastFeePerGuestLkr = breakfastFeePerGuestLkr; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isValid(int maxGuestsPerUnit) {
        return CURRENCY_LKR.equals(currency)
                && pricePerNightLkr > 0
                && includedGuests > 0
                && includedGuests <= maxGuestsPerUnit
                && extraGuestFeeLkr >= 0
                && breakfastFeePerGuestLkr >= 0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("currency", currency);
        map.put("pricePerNightLkr", pricePerNightLkr);
        map.put("includedGuests", includedGuests);
        map.put("extraGuestFeeLkr", extraGuestFeeLkr);
        map.put("breakfastOffered", breakfastOffered);
        map.put("breakfastFeePerGuestLkr", breakfastFeePerGuestLkr);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}