package lk.falconplan.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.data.model.CommunityPost;

import static org.junit.Assert.*;

public class CommunityPostTest {

    @Test
    public void communityPost_draftStatus_returnsCorrectStatus() {
        List<String> photos = new ArrayList<>();
        photos.add("community_posts_media/user_123/post_101/photo_1");

        CommunityPost post = new CommunityPost(
                "post_101",
                "user_123",
                "Saman Perera",
                "https://falconplan.app/avatar.jpg",
                "Exploring Knuckles Cloud Forest!",
                "dest_knuckles",
                CommunityPost.STATUS_DRAFT,
                photos,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertEquals("post_101", post.getPostId());
        assertEquals("user_123", post.getAuthorUid());
        assertEquals("Saman Perera", post.getAuthorDisplayName());
        assertEquals("draft", post.getStatus());
        assertEquals(1, post.getPhotos().size());
    }

    @Test
    public void communityPost_publishedStatus_returnsCorrectStatus() {
        CommunityPost post = new CommunityPost(
                "post_102",
                "user_123",
                "Champa Ranasinghe",
                "",
                "Cooking heirloom jackfruit curry in Sigiriya",
                "dest_sigiriya",
                CommunityPost.STATUS_PUBLISHED,
                new ArrayList<>(),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertEquals("published", post.getStatus());
        assertEquals("Champa Ranasinghe", post.getAuthorDisplayName());
    }

    @Test
    public void communityPost_legacyCompatibilityGetters_returnFormattedValues() {
        CommunityPost post = new CommunityPost(
                "post_01",
                "Saman Perera",
                "Traveler",
                "Meemure",
                "2 hours ago",
                "Misty Peak Hike",
                "Trekking through Meemure trail with local guide.",
                10,
                2,
                true
        );

        assertEquals("Saman Perera", post.getAuthorName());
        assertEquals("Meemure", post.getVillageLocation());
        assertEquals("Misty Peak Hike\n\nTrekking through Meemure trail with local guide.", post.getContentSnippet());
    }

    @Test
    public void communityPost_likeCountIncrement_updatesCountAndLikedState() {
        CommunityPost post = new CommunityPost(
                "post_103",
                "user_456",
                "Kasun Silva",
                "",
                "Hiking Lakegala Mountain",
                "dest_meemure",
                CommunityPost.STATUS_PUBLISHED,
                new ArrayList<>(),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        post.setLikeCount(12);
        post.setLikedByMe(false);
        assertEquals(12, post.getLikeCount());
        assertFalse(post.isLikedByMe());

        post.setLikeCount(post.getLikeCount() + 1);
        post.setLikedByMe(true);
        assertEquals(13, post.getLikeCount());
        assertTrue(post.isLikedByMe());
    }

    @Test
    public void communityPost_unlikeCountDecrement_preventsNegativeCount() {
        CommunityPost post = new CommunityPost(
                "post_104",
                "user_456",
                "Kasun Silva",
                "",
                "Sunset over Ella Gap",
                "dest_ella",
                CommunityPost.STATUS_PUBLISHED,
                new ArrayList<>(),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        post.setLikeCount(0);
        post.setLikeCount(post.getLikeCount() - 1);
        assertEquals(0, post.getLikeCount());
    }

    @Test
    public void communityPost_savedByMeState_updatesCorrectly() {
        CommunityPost post = new CommunityPost(
                "post_105",
                "user_789",
                "Nimali Fernando",
                "",
                "Whale watching in Mirissa",
                "dest_mirissa",
                CommunityPost.STATUS_PUBLISHED,
                new ArrayList<>(),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        post.setSavedByMe(false);
        assertFalse(post.isSavedByMe());

        post.setSavedByMe(true);
        assertTrue(post.isSavedByMe());
    }

    @Test
    public void communityPost_unsaveState_updatesCorrectly() {
        CommunityPost post = new CommunityPost(
                "post_106",
                "user_789",
                "Nimali Fernando",
                "",
                "Camping in Horton Plains",
                "dest_horton",
                CommunityPost.STATUS_PUBLISHED,
                new ArrayList<>(),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        post.setSavedByMe(true);
        assertTrue(post.isSavedByMe());

        post.setSavedByMe(false);
        assertFalse(post.isSavedByMe());
    }
}