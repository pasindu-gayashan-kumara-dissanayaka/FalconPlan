package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.NotificationItem;

public interface NotificationRepository {

    interface NotificationsListCallback {
        void onSuccess(List<NotificationItem> notifications, int unreadCount);
        void onError(Exception exception);
    }

    interface UnreadCountCallback {
        void onSuccess(int unreadCount);
        void onError(Exception exception);
    }

    interface OperationCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    void getMyNotifications(String idToken, int page, int limit, NotificationsListCallback callback);

    void getUnreadCount(String idToken, UnreadCountCallback callback);

    void markAsRead(String idToken, String notificationId, OperationCallback callback);

    void markAllAsRead(String idToken, OperationCallback callback);
}