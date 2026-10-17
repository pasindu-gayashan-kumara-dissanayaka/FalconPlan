package lk.falconplan.app.ui.chat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ChatMessage;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private final List<ChatMessage> items = new ArrayList<>();

    public void setItems(List<ChatMessage> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        if (message != null) {
            this.items.add(message);
            notifyItemInserted(this.items.size() - 1);
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage item = items.get(position);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = user != null ? user.getUid() : "";
        holder.bind(item, currentUid);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout layoutContainer;
        private final MaterialCardView cardBubble;
        private final TextView txtMessageText;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutContainer = itemView.findViewById(R.id.layoutContainer);
            cardBubble = itemView.findViewById(R.id.cardBubble);
            txtMessageText = itemView.findViewById(R.id.txtMessageText);
        }

        public void bind(ChatMessage item, String currentUid) {
            txtMessageText.setText(item.getText());

            boolean isSentByMe = currentUid != null && !currentUid.isEmpty() && currentUid.equals(item.getSenderUid());
            if (isSentByMe) {
                layoutContainer.setGravity(Gravity.END);
                cardBubble.setCardBackgroundColor(itemView.getContext().getColor(R.color.primary));
                txtMessageText.setTextColor(itemView.getContext().getColor(R.color.white));
            } else {
                layoutContainer.setGravity(Gravity.START);
                cardBubble.setCardBackgroundColor(itemView.getContext().getColor(R.color.surface));
                txtMessageText.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            }
        }
    }
}