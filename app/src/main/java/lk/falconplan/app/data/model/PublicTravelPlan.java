package lk.falconplan.app.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PublicTravelPlan {

    private String publicPlanId;
    private String ownerUid;
    private String ownerDisplayName;
    private String title;
    private String description;
    private long startDate;
    private long endDate;
    private long publishedAt;
    private String status;
    private List<TravelPlanStop> stops;

    public PublicTravelPlan() {
        this.stops = new ArrayList<>();
    }

    public PublicTravelPlan(String publicPlanId, String ownerUid, String ownerDisplayName,
                            String title, String description, long startDate, long endDate,
                            long publishedAt, String status, List<TravelPlanStop> stops) {
        this.publicPlanId = publicPlanId;
        this.ownerUid = ownerUid;
        this.ownerDisplayName = ownerDisplayName != null ? ownerDisplayName : "FalconPlan Member";
        this.title = title;
        this.description = description != null ? description : "";
        this.startDate = startDate;
        this.endDate = endDate;
        this.publishedAt = publishedAt;
        this.status = status != null ? status : "published";
        this.stops = stops != null ? stops : new ArrayList<>();
    }

    public String getPublicPlanId() { return publicPlanId; }
    public void setPublicPlanId(String publicPlanId) { this.publicPlanId = publicPlanId; }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public String getOwnerDisplayName() { return ownerDisplayName; }
    public void setOwnerDisplayName(String ownerDisplayName) { this.ownerDisplayName = ownerDisplayName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public long getPublishedAt() { return publishedAt; }
    public void setPublishedAt(long publishedAt) { this.publishedAt = publishedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<TravelPlanStop> getStops() { return stops; }
    public void setStops(List<TravelPlanStop> stops) { this.stops = stops != null ? stops : new ArrayList<>(); }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("publicPlanId", publicPlanId);
        map.put("ownerUid", ownerUid);
        map.put("ownerDisplayName", ownerDisplayName);
        map.put("title", title);
        map.put("description", description);
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        map.put("publishedAt", publishedAt);
        map.put("status", status);

        List<Map<String, Object>> stopsList = new ArrayList<>();
        if (stops != null) {
            for (TravelPlanStop stop : stops) {
                stopsList.add(stop.toMap());
            }
        }
        map.put("stops", stopsList);
        return map;
    }
}