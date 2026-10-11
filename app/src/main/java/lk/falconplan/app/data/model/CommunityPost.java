package lk.falconplan.app.data.model;

public class CommunityPost {
    private final String id;
    private final String authorName;
    private final String authorRole;
    private final String villageLocation;
    private final String timeAgo;
    private final String title;
    private final String contentSnippet;
    private final int likeCount;
    private final int commentCount;
    private final boolean isDesignPreview;

    public CommunityPost(String id, String authorName, String authorRole, String villageLocation,
                         String timeAgo, String title, String contentSnippet,
                         int likeCount, int commentCount, boolean isDesignPreview) {
        this.id = id;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.villageLocation = villageLocation;
        this.timeAgo = timeAgo;
        this.title = title;
        this.contentSnippet = contentSnippet;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.isDesignPreview = isDesignPreview;
    }

    public String getId() { return id; }
    public String getAuthorName() { return authorName; }
    public String getAuthorRole() { return authorRole; }
    public String getVillageLocation() { return villageLocation; }
    public String getTimeAgo() { return timeAgo; }
    public String getTitle() { return title; }
    public String getContentSnippet() { return contentSnippet; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public boolean isDesignPreview() { return isDesignPreview; }
}