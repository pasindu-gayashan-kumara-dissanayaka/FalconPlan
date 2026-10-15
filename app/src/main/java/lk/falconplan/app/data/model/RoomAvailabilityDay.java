package lk.falconplan.app.data.model;

import java.util.HashMap;
import java.util.Map;

public class RoomAvailabilityDay {

    private String date;
    private int blockedUnits;
    private String note;
    private long updatedAt;

    public RoomAvailabilityDay() {
        // Required empty constructor for Firestore / JSON parsing
    }

    public RoomAvailabilityDay(String date, int blockedUnits, String note, long updatedAt) {
        this.date = date;
        this.blockedUnits = blockedUnits;
        this.note = note != null ? note : "";
        this.updatedAt = updatedAt;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getBlockedUnits() { return blockedUnits; }
    public void setBlockedUnits(int blockedUnits) { this.blockedUnits = blockedUnits; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note != null ? note : ""; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isValid(int totalUnits) {
        return date != null && date.matches("^\\d{4}-\\d{2}-\\d{2}$")
                && blockedUnits >= 0
                && blockedUnits <= totalUnits;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("date", date);
        map.put("blockedUnits", blockedUnits);
        map.put("note", note);
        map.put("updatedAt", updatedAt);
        return map;
    }
}