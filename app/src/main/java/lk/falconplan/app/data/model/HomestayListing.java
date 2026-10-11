package lk.falconplan.app.data.model;

public class HomestayListing {
    private final String id;
    private final String title;
    private final String locationName;
    private final String regionCategory;
    private final double priceLkrPerNight;
    private final float rating;
    private final int reviewCount;
    private final String hostName;
    private final String hostRole;
    private final String description;
    private final boolean isDesignPreview;

    public HomestayListing(String id, String title, String locationName, String regionCategory,
                           double priceLkrPerNight, float rating, int reviewCount,
                           String hostName, String hostRole, String description, boolean isDesignPreview) {
        this.id = id;
        this.title = title;
        this.locationName = locationName;
        this.regionCategory = regionCategory;
        this.priceLkrPerNight = priceLkrPerNight;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.hostName = hostName;
        this.hostRole = hostRole;
        this.description = description;
        this.isDesignPreview = isDesignPreview;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getLocationName() { return locationName; }
    public String getRegionCategory() { return regionCategory; }
    public double getPriceLkrPerNight() { return priceLkrPerNight; }
    public float getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public String getHostName() { return hostName; }
    public String getHostRole() { return hostRole; }
    public String getDescription() { return description; }
    public boolean isDesignPreview() { return isDesignPreview; }
}