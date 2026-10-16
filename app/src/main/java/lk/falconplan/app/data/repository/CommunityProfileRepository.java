package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.CommunityPost;
import lk.falconplan.app.data.model.CommunityProfile;

public interface CommunityProfileRepository {

    interface ProfileCallback {
        void onSuccess(CommunityProfile profile);
        void onError(Exception exception);
    }

    interface PostsListCallback {
        void onSuccess(List<CommunityPost> posts);
        void onError(Exception exception);
    }

    interface FollowCallback {
        void onSuccess(int followersCount, boolean isFollowingByMe);
        void onError(Exception exception);
    }

    interface UserSummariesCallback {
        void onSuccess(List<CommunityProfile> profiles);
        void onError(Exception exception);
    }

    void getOwnProfile(String idToken, ProfileCallback callback);

    void updateOwnProfile(String idToken, String displayName, String bio, String publicAvatarUrl, String visibility, ProfileCallback callback);

    void getPublicProfile(String profileId, ProfileCallback callback);

    void getPublicProfilePosts(String profileId, int page, int limit, PostsListCallback callback);

    void followProfile(String idToken, String targetUid, FollowCallback callback);

    void unfollowProfile(String idToken, String targetUid, FollowCallback callback);

    void getFollowers(String targetUid, int page, int limit, UserSummariesCallback callback);

    void getFollowing(String targetUid, int page, int limit, UserSummariesCallback callback);
}