package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.CommunityPost;

public interface CommunityPostRepository {

    interface PostCallback {
        void onSuccess(CommunityPost post);
        void onError(Exception exception);
    }

    interface PostsListCallback {
        void onSuccess(List<CommunityPost> posts);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    interface LikeCallback {
        void onSuccess(int likeCount, boolean likedByMe);
        void onError(Exception exception);
    }

    interface SaveCallback {
        void onSuccess(boolean savedByMe);
        void onError(Exception exception);
    }

    void createPost(String idToken, String caption, String destinationId, PostCallback callback);

    void getTravelerPosts(String idToken, PostsListCallback callback);

    void uploadPostPhoto(String idToken, String postId, String mimeType, String fileBase64, OperationCallback callback);

    void publishPost(String idToken, String postId, PostCallback callback);

    void getCommunityFeed(int page, int limit, PostsListCallback callback);

    void reportPost(String idToken, String postId, String reason, OperationCallback callback);

    void deletePost(String idToken, String postId, OperationCallback callback);

    void likePost(String idToken, String postId, LikeCallback callback);

    void unlikePost(String idToken, String postId, LikeCallback callback);

    void savePost(String idToken, String postId, SaveCallback callback);

    void unsavePost(String idToken, String postId, SaveCallback callback);

    void getSavedPosts(String idToken, int page, int limit, PostsListCallback callback);
}