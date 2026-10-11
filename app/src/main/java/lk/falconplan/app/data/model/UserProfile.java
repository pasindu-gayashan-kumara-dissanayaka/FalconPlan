package lk.falconplan.app.data.model;

import java.util.HashMap;
import java.util.Map;

public class UserProfile {

    public static final String MODE_TRAVELER = "traveler";
    public static final String MODE_PROVIDER = "provider";

    private String uid;
    private String displayName;
    private String email;
    private String photoUrl;
    private String activeMode;
    private long createdAt;
    private long updatedAt;

    public UserProfile() {
        // Required empty constructor for Firestore deserialization
    }

    public UserProfile(String uid, String displayName, String email, String photoUrl,
                       String activeMode, long createdAt, long updatedAt) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = photoUrl;
        setActiveMode(activeMode);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getActiveMode() { return activeMode; }
    public void setActiveMode(String activeMode) {
        if (MODE_TRAVELER.equals(activeMode) || MODE_PROVIDER.equals(activeMode)) {
            this.activeMode = activeMode;
        } else {
            this.activeMode = MODE_TRAVELER;
        }
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("displayName", displayName);
        map.put("email", email);
        map.put("photoUrl", photoUrl);
        map.put("activeMode", activeMode);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}