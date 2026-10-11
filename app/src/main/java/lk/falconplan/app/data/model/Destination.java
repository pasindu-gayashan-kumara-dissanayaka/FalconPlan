package lk.falconplan.app.data.model;

public class Destination {

    private final String id;
    private final String name;
    private final String district;
    private final String province;
    private final String description;
    private final String category;
    private final double latitude;
    private final double longitude;
    private final String imageResName;
    private final String travelTips;

    public Destination(String id, String name, String district, String province,
                       String description, String category, double latitude, double longitude,
                       String imageResName, String travelTips) {
        this.id = id;
        this.name = name;
        this.district = district;
        this.province = province;
        this.description = description;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageResName = imageResName;
        this.travelTips = travelTips;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDistrict() { return district; }
    public String getProvince() { return province; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getImageResName() { return imageResName; }
    public String getTravelTips() { return travelTips; }

    public boolean isValidCoordinates() {
        return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0;
    }
}