package lk.falconplan.app.data.model;

import java.util.HashMap;
import java.util.Map;

public class TravelPlanStop {

    private String id;
    private String destinationId;
    private String destinationName;
    private String district;
    private double latitude;
    private double longitude;
    private long visitDate;
    private String notes;
    private int orderIndex;

    public TravelPlanStop() {
        // Required empty constructor for Firestore
    }

    public TravelPlanStop(String id, String destinationId, String destinationName, String district,
                          double latitude, double longitude, long visitDate, String notes, int orderIndex) {
        this.id = id;
        this.destinationId = destinationId;
        this.destinationName = destinationName;
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
        this.visitDate = visitDate;
        this.notes = notes != null ? notes : "";
        this.orderIndex = orderIndex;
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

    public long getVisitDate() { return visitDate; }
    public void setVisitDate(long visitDate) { this.visitDate = visitDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes != null ? notes : ""; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

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
        map.put("visitDate", visitDate);
        map.put("notes", notes);
        map.put("orderIndex", orderIndex);
        return map;
    }
}