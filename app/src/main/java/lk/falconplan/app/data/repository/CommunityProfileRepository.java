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

    void getOwnProfile(String idToken, ProfileCallback callback);

    void updateOwnProfile(String idToken, String displayName, String bio, String publicAvatarUrl, String visibility, ProfileCallback callback);

    void getPublicProfile(String profileId, ProfileCallback callback);

    void getPublicProfilePosts(String profileId, int page, int limit, PostsListCallback callback);
}