package lk.falconplan.app.data.model;

import java.util.HashMap;
import java.util.Map;

public class ProviderRoom {

    public static final String TYPE_SINGLE = "single";
    public static final String TYPE_DOUBLE = "double";
    public static final String TYPE_TWIN = "twin";
    public static final String TYPE_FAMILY = "family";
    public static final String TYPE_DORMITORY = "dormitory";
    public static final String TYPE_OTHER = "other";

    public static final String BATHROOM_PRIVATE = "private";
    public static final String BATHROOM_SHARED = "shared";

    private String id;
    private String providerUid;
    private String listingId;
    private String roomName;
    private String roomType;
    private String description;
    private int totalUnits;
    private int maxGuestsPerUnit;
    private int bedCount;
    private String bedType;
    private String bathroomType;
    private String amenities;
    private long createdAt;
    private long updatedAt;

    public ProviderRoom() {
        // Required empty constructor for Firestore
    }

    public ProviderRoom(String id, String providerUid, String listingId, String roomName,
                        String roomType, String description, int totalUnits, int maxGuestsPerUnit,
                        int bedCount, String bedType, String bathroomType, String amenities,
                        long createdAt, long updatedAt) {
        this.id = id;
        this.providerUid = providerUid;
        this.listingId = listingId;
        this.roomName = roomName;
        this.roomType = roomType != null ? roomType : TYPE_DOUBLE;
        this.description = description != null ? description : "";
        this.totalUnits = totalUnits;
        this.maxGuestsPerUnit = maxGuestsPerUnit;
        this.bedCount = bedCount;
        this.bedType = bedType != null ? bedType : "Bed";
        this.bathroomType = bathroomType != null ? bathroomType : BATHROOM_PRIVATE;
        this.amenities = amenities != null ? amenities : "";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProviderUid() { return providerUid; }
    public void setProviderUid(String providerUid) { this.providerUid = providerUid; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType != null ? roomType : TYPE_DOUBLE; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }

    public int getTotalUnits() { return totalUnits; }
    public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }

    public int getMaxGuestsPerUnit() { return maxGuestsPerUnit; }
    public void setMaxGuestsPerUnit(int maxGuestsPerUnit) { this.maxGuestsPerUnit = maxGuestsPerUnit; }

    public int getBedCount() { return bedCount; }
    public void setBedCount(int bedCount) { this.bedCount = bedCount; }

    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType != null ? bedType : "Bed"; }

    public String getBathroomType() { return bathroomType; }
    public void setBathroomType(String bathroomType) { this.bathroomType = bathroomType != null ? bathroomType : BATHROOM_PRIVATE; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities != null ? amenities : ""; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isValid() {
        return providerUid != null && !providerUid.isEmpty()
                && listingId != null && !listingId.isEmpty()
                && roomName != null && !roomName.trim().isEmpty()
                && totalUnits > 0
                && maxGuestsPerUnit > 0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("providerUid", providerUid);
        map.put("listingId", listingId);
        map.put("roomName", roomName);
        map.put("roomType", roomType);
        map.put("description", description);
        map.put("totalUnits", totalUnits);
        map.put("maxGuestsPerUnit", maxGuestsPerUnit);
        map.put("bedCount", bedCount);
        map.put("bedType", bedType);
        map.put("bathroomType", bathroomType);
        map.put("amenities", amenities);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}