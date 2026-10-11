package lk.falconplan.app.data.model;

import java.util.HashMap;
import java.util.Map;

public class TravelPlan {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PLANNED = "planned";

    private String id;
    private String ownerUid;
    private String title;
    private String description;
    private long startDate;
    private long endDate;
    private String status;
    private long createdAt;
    private long updatedAt;
    private int stopCount;

    public TravelPlan() {
        // Required empty constructor for Firestore
    }

    public TravelPlan(String id, String ownerUid, String title, String description,
                      long startDate, long endDate, String status, long createdAt, long updatedAt, int stopCount) {
        this.id = id;
        this.ownerUid = ownerUid;
        this.title = title;
        this.description = description != null ? description : "";
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status != null ? status : STATUS_DRAFT;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.stopCount = stopCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status != null ? status : STATUS_DRAFT; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public int getStopCount() { return stopCount; }
    public void setStopCount(int stopCount) { this.stopCount = stopCount; }

    public boolean isValidDateRange() {
        return startDate <= endDate;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("ownerUid", ownerUid);
        map.put("title", title);
        map.put("description", description);
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        map.put("status", status);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("stopCount", stopCount);
        return map;
    }
}