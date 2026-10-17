package lk.falconplan.app.ui.chat;

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
import lk.falconplan.app.data.model.ChatConversation;
import lk.falconplan.app.data.repository.ChatRepository;
import lk.falconplan.app.data.repository.ChatRepositoryImpl;

public class MyChatConversationsFragment extends Fragment implements ChatConversationAdapter.OnConversationClickListener {

    private ChatRepository chatRepository;
    private ChatConversationAdapter adapter;

    private RecyclerView recyclerConversations;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static MyChatConversationsFragment newInstance() {
        return new MyChatConversationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_chat_conversations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatRepository = new ChatRepositoryImpl();

        recyclerConversations = view.findViewById(R.id.recyclerConversations);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recyclerConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChatConversationAdapter();
        recyclerConversations.setAdapter(adapter);
        adapter.setOnConversationClickListener(this);

        loadConversations();
    }

    private void loadConversations() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            chatRepository.getMyConversations(idToken, new ChatRepository.ConversationsListCallback() {
                @Override
                public void onSuccess(List<ChatConversation> conversations) {
                    progressBar.setVisibility(View.GONE);
                    adapter.setItems(conversations);

                    if (conversations == null || conversations.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerConversations.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerConversations.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load chats: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onConversationClick(ChatConversation conversation) {
        if (getActivity() instanceof MainActivity && conversation != null && conversation.getBookingId() != null) {
            ((MainActivity) getActivity()).loadMainContentFragment(
                    BookingChatFragment.newInstance(conversation.getBookingId()), true
            );
        }
    }
}