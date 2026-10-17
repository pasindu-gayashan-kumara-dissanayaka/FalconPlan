package lk.falconplan.app.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.UUID;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ChatConversation;
import lk.falconplan.app.data.model.ChatMessage;
import lk.falconplan.app.data.repository.ChatRepository;
import lk.falconplan.app.data.repository.ChatRepositoryImpl;

public class BookingChatFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "arg_booking_id";
    private static final String ARG_CONVERSATION_ID = "arg_conversation_id";

    private ChatRepository chatRepository;
    private ChatMessageAdapter adapter;

    private String bookingId;
    private String conversationId;

    private TextView txtHeaderTitle;
    private RecyclerView recyclerMessages;
    private EditText editMessageInput;
    private ImageButton btnSendMessage;
    private ProgressBar progressBar;

    public static BookingChatFragment newInstance(String bookingId) {
        BookingChatFragment fragment = new BookingChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOKING_ID, bookingId);
        fragment.setArguments(args);
        return fragment;
    }

    public static BookingChatFragment newInstanceForConversation(String conversationId) {
        BookingChatFragment fragment = new BookingChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONVERSATION_ID, conversationId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatRepository = new ChatRepositoryImpl();

        bookingId = getArguments() != null ? getArguments().getString(ARG_BOOKING_ID, "") : "";
        conversationId = getArguments() != null ? getArguments().getString(ARG_CONVERSATION_ID, "") : "";

        txtHeaderTitle = view.findViewById(R.id.txtHeaderTitle);
        recyclerMessages = view.findViewById(R.id.recyclerMessages);
        editMessageInput = view.findViewById(R.id.editMessageInput);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        btnSendMessage.setOnClickListener(v -> sendMessage());

        recyclerMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChatMessageAdapter();
        recyclerMessages.setAdapter(adapter);

        if (!conversationId.isEmpty()) {
            txtHeaderTitle.setText("Booking Chat");
            loadMessages();
        } else if (!bookingId.isEmpty()) {
            txtHeaderTitle.setText("Chat #" + bookingId);
            initiateConversation();
        }
    }

    private void initiateConversation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            chatRepository.initiateConversation(idToken, bookingId, new ChatRepository.ConversationCallback() {
                @Override
                public void onSuccess(ChatConversation conversation) {
                    progressBar.setVisibility(View.GONE);
                    if (conversation != null) {
                        conversationId = conversation.getConversationId();
                        loadMessages();
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to initiate chat: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadMessages() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || conversationId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            chatRepository.getMessages(idToken, conversationId, 1, 50, new ChatRepository.MessagesListCallback() {
                @Override
                public void onSuccess(List<ChatMessage> messages) {
                    progressBar.setVisibility(View.GONE);
                    adapter.setItems(messages);
                    if (messages != null && !messages.isEmpty()) {
                        recyclerMessages.scrollToPosition(messages.size() - 1);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load messages: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void sendMessage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || conversationId.isEmpty()) return;

        String text = editMessageInput.getText() != null ? editMessageInput.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        String clientRequestId = UUID.randomUUID().toString();
        btnSendMessage.setEnabled(false);

        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            chatRepository.sendMessage(idToken, conversationId, text, clientRequestId, new ChatRepository.MessageCallback() {
                @Override
                public void onSuccess(ChatMessage message) {
                    btnSendMessage.setEnabled(true);
                    editMessageInput.setText("");
                    adapter.addMessage(message);
                    recyclerMessages.scrollToPosition(adapter.getItemCount() - 1);
                }

                @Override
                public void onError(Exception exception) {
                    btnSendMessage.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed to send message: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}