package lk.falconplan.app.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ChatConversation;

public class ChatConversationAdapter extends RecyclerView.Adapter<ChatConversationAdapter.ConversationViewHolder> {

    private final List<ChatConversation> items = new ArrayList<>();
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(ChatConversation conversation);
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ChatConversation> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ChatConversation item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtBookingTitle;
        private final TextView txtLastMessage;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            txtBookingTitle = itemView.findViewById(R.id.txtBookingTitle);
            txtLastMessage = itemView.findViewById(R.id.txtLastMessage);
        }

        public void bind(ChatConversation item, OnConversationClickListener listener) {
            txtBookingTitle.setText("Booking Chat #" + item.getBookingId());
            txtLastMessage.setText(item.getLastMessageText());

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onConversationClick(item);
            });
        }
    }
}