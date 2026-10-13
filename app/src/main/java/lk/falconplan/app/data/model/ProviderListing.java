package lk.falconplan.app.data.model;

import java.util.HashMap;
import java.util.Map;

public class ProviderListing {

    public static final String TYPE_HOMESTAY = "homestay";
    public static final String TYPE_EXPERIENCE = "experience";
    public static final String STATUS_DRAFT = "draft";

    private String id;
    private String providerUid;
    private String listingType;
    private String title;
    private String description;
    private String district;
    private String village;
    private String status;
    private long createdAt;
    private long updatedAt;

    // Homestay specific fields
    private int maxGuests;
    private int bedrooms;
    private String amenities;

    // Experience specific fields
    private int durationMinutes;
    private int maxParticipants;
    private String experienceCategory;

    public ProviderListing() {
        // Required empty constructor for Firestore
    }

    public ProviderListing(String id, String providerUid, String listingType, String title,
                           String description, String district, String village, String status,
                           long createdAt, long updatedAt, int maxGuests, int bedrooms, String amenities,
                           int durationMinutes, int maxParticipants, String experienceCategory) {
        this.id = id;
        this.providerUid = providerUid;
        this.listingType = listingType != null ? listingType : TYPE_HOMESTAY;
        this.title = title;
        this.description = description != null ? description : "";
        this.district = district;
        this.village = village;
        this.status = status != null ? status : STATUS_DRAFT;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.maxGuests = maxGuests;
        this.bedrooms = bedrooms;
        this.amenities = amenities != null ? amenities : "";
        this.durationMinutes = durationMinutes;
        this.maxParticipants = maxParticipants;
        this.experienceCategory = experienceCategory != null ? experienceCategory : "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProviderUid() { return providerUid; }
    public void setProviderUid(String providerUid) { this.providerUid = providerUid; }

    public String getListingType() { return listingType; }
    public void setListingType(String listingType) { this.listingType = listingType != null ? listingType : TYPE_HOMESTAY; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status != null ? status : STATUS_DRAFT; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public int getMaxGuests() { return maxGuests; }
    public void setMaxGuests(int maxGuests) { this.maxGuests = maxGuests; }

    public int getBedrooms() { return bedrooms; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities != null ? amenities : ""; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public String getExperienceCategory() { return experienceCategory; }
    public void setExperienceCategory(String experienceCategory) { this.experienceCategory = experienceCategory != null ? experienceCategory : ""; }

    public boolean isValid() {
        return providerUid != null && !providerUid.isEmpty()
                && title != null && !title.trim().isEmpty()
                && district != null && !district.trim().isEmpty()
                && village != null && !village.trim().isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("providerUid", providerUid);
        map.put("listingType", listingType);
        map.put("title", title);
        map.put("description", description);
        map.put("district", district);
        map.put("village", village);
        map.put("status", status);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("maxGuests", maxGuests);
        map.put("bedrooms", bedrooms);
        map.put("amenities", amenities);
        map.put("durationMinutes", durationMinutes);
        map.put("maxParticipants", maxParticipants);
        map.put("experienceCategory", experienceCategory);
        return map;
    }
}