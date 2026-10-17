package lk.falconplan.app.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.NotificationItem;
import lk.falconplan.app.data.repository.NotificationRepository;
import lk.falconplan.app.data.repository.NotificationRepositoryImpl;

public class MyNotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private NotificationRepository repository;
    private NotificationAdapter adapter;

    private RecyclerView recyclerNotifications;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static MyNotificationsFragment newInstance() {
        return new MyNotificationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new NotificationRepositoryImpl();

        recyclerNotifications = view.findViewById(R.id.recyclerNotifications);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> markAllAsRead());

        recyclerNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationAdapter();
        recyclerNotifications.setAdapter(adapter);
        adapter.setOnNotificationClickListener(this);

        loadNotifications();
    }

    private void loadNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            repository.getMyNotifications(idToken, 1, 20, new NotificationRepository.NotificationsListCallback() {
                @Override
                public void onSuccess(List<NotificationItem> notifications, int unreadCount) {
                    progressBar.setVisibility(View.GONE);
                    adapter.setItems(notifications);

                    if (notifications == null || notifications.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerNotifications.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerNotifications.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load notifications: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void markAllAsRead() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            repository.markAllAsRead(idToken, new NotificationRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "All notifications marked as read.", Toast.LENGTH_SHORT).show();
                    loadNotifications();
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                }
            });
        });
    }

    @Override
    public void onNotificationClick(NotificationItem item) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || item == null) return;

        if (!item.isRead()) {
            user.getIdToken(true).addOnCompleteListener(task -> {
                String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                repository.markAsRead(idToken, item.getNotificationId(), new NotificationRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        item.setRead(true);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Exception exception) {}
                });
            });
        }

        // Navigate based on entity type
        if ("booking".equals(item.getEntityType()) && item.getEntityId() != null && !item.getEntityId().isEmpty()) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(
                        lk.falconplan.app.ui.booking.BookingDetailFragment.newInstance(item.getEntityId()), true
                );
            }
        } else if ("chat".equals(item.getEntityType()) && item.getEntityId() != null && !item.getEntityId().isEmpty()) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(
                        lk.falconplan.app.ui.chat.BookingChatFragment.newInstanceForConversation(item.getEntityId()), true
                );
            }
        }
    }
}