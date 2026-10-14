package lk.falconplan.app.ui.admin;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.remote.BackendApiClient;

public class AdminListingReviewFragment extends Fragment implements AdminListingAdapter.OnListingItemClickListener {

    private BackendApiClient apiClient;
    private AdminListingAdapter adapter;
    private RecyclerView recyclerListings;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static AdminListingReviewFragment newInstance() {
        return new AdminListingReviewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_listing_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiClient = BackendApiClient.getInstance();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recyclerListings = view.findViewById(R.id.recyclerListings);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerListings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminListingAdapter();
        recyclerListings.setAdapter(adapter);
        adapter.setOnListingItemClickListener(this);

        loadAdminListings();
    }

    private void loadAdminListings() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            apiClient.getAdminListings(idToken, 1, 20, new BackendApiClient.ApiCallback<JSONArray>() {
                @Override
                public void onSuccess(JSONArray result) {
                    progressBar.setVisibility(View.GONE);
                    List<JSONObject> list = new ArrayList<>();
                    if (result != null) {
                        for (int i = 0; i < result.length(); i++) {
                            JSONObject item = result.optJSONObject(i);
                            if (item != null) list.add(item);
                        }
                    }

                    adapter.setItems(list);
                    if (list.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerListings.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerListings.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Admin access denied or network error: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onItemClick(JSONObject item) {
        String listingId = item.optString("id", item.optString("listingId", ""));
        String providerUid = item.optString("providerUid", "");
        if (listingId.isEmpty()) return;

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadMainContentFragment(AdminListingDetailFragment.newInstance(listingId, providerUid), true);
        }
    }
}