package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.CommunityComment;

public interface CommunityCommentRepository {

    interface CommentCallback {
        void onSuccess(CommunityComment comment);
        void onError(Exception exception);
    }

    interface CommentsListCallback {
        void onSuccess(List<CommunityComment> comments);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void createComment(String idToken, String postId, String text, CommentCallback callback);

    void getComments(String postId, int page, int limit, CommentsListCallback callback);

    void editComment(String idToken, String postId, String commentId, String text, CommentCallback callback);

    void deleteComment(String idToken, String postId, String commentId, OperationCallback callback);

    void reportComment(String idToken, String postId, String commentId, String reason, OperationCallback callback);
}