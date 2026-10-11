package lk.falconplan.app.ui.plans;

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
import lk.falconplan.app.data.model.TravelPlan;
import lk.falconplan.app.data.repository.TravelPlanRepository;
import lk.falconplan.app.data.repository.TravelPlanRepositoryImpl;

public class PlansFragment extends Fragment implements PlanAdapter.OnPlanClickListener {

    private TravelPlanRepository travelPlanRepository;
    private PlanAdapter adapter;
    private RecyclerView recyclerPlans;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static PlansFragment newInstance() {
        return new PlansFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plans, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        travelPlanRepository = new TravelPlanRepositoryImpl();

        recyclerPlans = view.findViewById(R.id.recyclerPlans);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerPlans.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PlanAdapter();
        recyclerPlans.setAdapter(adapter);
        adapter.setOnPlanClickListener(this);

        View.OnClickListener createListener = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadAuthFragment(new CreatePlanFragment(), true);
            }
        };

        view.findViewById(R.id.btnCreatePlan).setOnClickListener(createListener);
        view.findViewById(R.id.btnEmptyCreatePlan).setOnClickListener(createListener);

        loadPlans();
    }

    private void loadPlans() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerPlans.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        travelPlanRepository.getUserPlans(user.getUid(), new TravelPlanRepository.PlansCallback() {
            @Override
            public void onSuccess(List<TravelPlan> plans) {
                progressBar.setVisibility(View.GONE);
                adapter.setPlans(plans);
                if (plans == null || plans.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerPlans.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerPlans.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load travel plans: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPlanClick(TravelPlan plan) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadAuthFragment(PlanDetailFragment.newInstance(plan.getId()), true);
        }
    }
}