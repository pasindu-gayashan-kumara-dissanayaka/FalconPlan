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

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.repository.AdminKycRepository;
import lk.falconplan.app.data.repository.AdminKycRepositoryImpl;

public class AdminKycDashboardFragment extends Fragment implements AdminKycAdapter.OnKycItemClickListener {

    private AdminKycRepository adminKycRepository;
    private AdminKycAdapter adapter;
    private RecyclerView recyclerKycApplications;
    private View layoutEmpty;
    private ProgressBar progressBar;
    private String currentStatusFilter = null;

    public static AdminKycDashboardFragment newInstance() {
        return new AdminKycDashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_kyc_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminKycRepository = new AdminKycRepositoryImpl();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        recyclerKycApplications = view.findViewById(R.id.recyclerKycApplications);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);
        ChipGroup chipGroupKycFilter = view.findViewById(R.id.chipGroupKycFilter);

        recyclerKycApplications.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminKycAdapter();
        recyclerKycApplications.setAdapter(adapter);
        adapter.setOnKycItemClickListener(this);

        chipGroupKycFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipFilterSubmitted) {
                currentStatusFilter = "submitted_under_review";
            } else if (checkedId == R.id.chipFilterVerified) {
                currentStatusFilter = "verified";
            } else if (checkedId == R.id.chipFilterRejected) {
                currentStatusFilter = "rejected";
            } else {
                currentStatusFilter = null;
            }
            loadAdminKycApplications();
        });

        loadAdminKycApplications();
    }

    private void loadAdminKycApplications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in as administrator.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        user.getIdToken(true).addOnCompleteListener(task -> {
            String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
            adminKycRepository.getAdminKycApplications(idToken, currentStatusFilter, 1, 20, new AdminKycRepository.ApplicationsCallback() {
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
                        recyclerKycApplications.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerKycApplications.setVisibility(View.VISIBLE);
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
        String providerUid = item.optString("providerUid", "");
        if (providerUid.isEmpty()) return;

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadMainContentFragment(AdminKycDetailFragment.newInstance(providerUid), true);
        }
    }
}