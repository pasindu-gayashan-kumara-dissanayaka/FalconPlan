package lk.falconplan.app.data.model;

import java.util.HashMap;
import java.util.Map;

public class SavedPlace {

    private String id;
    private String destinationId;
    private String destinationName;
    private String district;
    private double latitude;
    private double longitude;
    private String note;
    private long createdAt;
    private long updatedAt;

    public SavedPlace() {
        // Required empty constructor for Firestore deserialization
    }

    public SavedPlace(String id, String destinationId, String destinationName, String district,
                      double latitude, double longitude, String note, long createdAt, long updatedAt) {
        this.id = id;
        this.destinationId = destinationId;
        this.destinationName = destinationName;
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
        this.note = note != null ? note : "";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDestinationId() { return destinationId; }
    public void setDestinationId(String destinationId) { this.destinationId = destinationId; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note != null ? note : ""; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isValidCoordinates() {
        return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("destinationId", destinationId);
        map.put("destinationName", destinationName);
        map.put("district", district);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("note", note);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}