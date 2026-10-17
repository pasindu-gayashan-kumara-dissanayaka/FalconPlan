package lk.falconplan.app.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.NotificationItem;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationItem> items = new ArrayList<>();
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item);
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<NotificationItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgNotifIcon;
        private final TextView txtNotifTitle;
        private final TextView txtNotifBody;
        private final View viewUnreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            imgNotifIcon = itemView.findViewById(R.id.imgNotifIcon);
            txtNotifTitle = itemView.findViewById(R.id.txtNotifTitle);
            txtNotifBody = itemView.findViewById(R.id.txtNotifBody);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }

        public void bind(NotificationItem item, OnNotificationClickListener listener) {
            txtNotifTitle.setText(item.getTitle());
            txtNotifBody.setText(item.getBody());

            if (item.isRead()) {
                viewUnreadDot.setVisibility(View.GONE);
            } else {
                viewUnreadDot.setVisibility(View.VISIBLE);
            }

            if (NotificationItem.TYPE_CHAT_MESSAGE.equals(item.getType())) {
                imgNotifIcon.setImageResource(R.drawable.ic_close);
            } else if (NotificationItem.TYPE_BOOKING_CREATED.equals(item.getType()) || NotificationItem.TYPE_BOOKING_DECISION.equals(item.getType())) {
                imgNotifIcon.setImageResource(R.drawable.ic_story);
            } else {
                imgNotifIcon.setImageResource(R.drawable.ic_star);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(item);
            });
        }
    }
}