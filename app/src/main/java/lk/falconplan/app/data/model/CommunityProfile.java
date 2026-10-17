package lk.falconplan.app.data.model;

import org.json.JSONObject;

public class CommunityProfile {

    public static final String VISIBILITY_PUBLIC = "public";
    public static final String VISIBILITY_PRIVATE = "private";

    private String ownerUid;
    private String displayName;
    private String bio;
    private String publicAvatarUrl;
    private String visibility;
    private boolean isVerifiedHost;
    private int followersCount;
    private int followingCount;
    private boolean isFollowingByMe;
    private long createdAt;

    public CommunityProfile() {}

    public CommunityProfile(String ownerUid, String displayName, String bio, String publicAvatarUrl,
                            String visibility, boolean isVerifiedHost, int followersCount,
                            int followingCount, boolean isFollowingByMe, long createdAt) {
        this.ownerUid = ownerUid;
        this.displayName = displayName != null ? displayName : "FalconPlan Member";
        this.bio = bio != null ? bio : "";
        this.publicAvatarUrl = publicAvatarUrl != null ? publicAvatarUrl : "";
        this.visibility = visibility != null ? visibility : VISIBILITY_PRIVATE;
        this.isVerifiedHost = isVerifiedHost;
        this.followersCount = Math.max(0, followersCount);
        this.followingCount = Math.max(0, followingCount);
        this.isFollowingByMe = isFollowingByMe;
        this.createdAt = createdAt;
    }

    public static CommunityProfile fromJson(JSONObject json) {
        if (json == null) return null;
        return new CommunityProfile(
                json.optString("ownerUid", json.optString("profileId", "")),
                json.optString("displayName", "FalconPlan Member"),
                json.optString("bio", ""),
                json.optString("publicAvatarUrl", ""),
                json.optString("visibility", VISIBILITY_PRIVATE),
                json.optBoolean("isVerifiedHost", false),
                json.optInt("followersCount", 0),
                json.optInt("followingCount", 0),
                json.optBoolean("isFollowingByMe", false),
                json.optLong("createdAt", System.currentTimeMillis())
        );
    }

    public String getOwnerUid() { return ownerUid; }
    public String getDisplayName() { return displayName; }
    public String getBio() { return bio; }
    public String getPublicAvatarUrl() { return publicAvatarUrl; }
    public String getVisibility() { return visibility; }
    public boolean isVerifiedHost() { return isVerifiedHost; }
    public int getFollowersCount() { return followersCount; }
    public int getFollowingCount() { return followingCount; }
    public boolean isFollowingByMe() { return isFollowingByMe; }
    public long getCreatedAt() { return createdAt; }

    public boolean isPublic() { return VISIBILITY_PUBLIC.equals(visibility); }
    public void setFollowersCount(int count) { this.followersCount = Math.max(0, count); }
    public void setFollowingByMe(boolean isFollowing) { this.isFollowingByMe = isFollowing; }
}