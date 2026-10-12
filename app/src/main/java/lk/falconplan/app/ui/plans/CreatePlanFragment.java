package lk.falconplan.app.ui.plans;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.TravelPlan;
import lk.falconplan.app.data.repository.TravelPlanRepository;
import lk.falconplan.app.data.repository.TravelPlanRepositoryImpl;

public class CreatePlanFragment extends Fragment {

    private TravelPlanRepository travelPlanRepository;
    private EditText editPlanTitle;
    private EditText editPlanDescription;
    private Button btnSelectStartDate;
    private Button btnSelectEndDate;
    private ProgressBar progressBar;

    private long startDateMs = System.currentTimeMillis();
    private long endDateMs = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L); // Default 3 days

    public static CreatePlanFragment newInstance() {
        return new CreatePlanFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        travelPlanRepository = new TravelPlanRepositoryImpl();

        editPlanTitle = view.findViewById(R.id.editPlanTitle);
        editPlanDescription = view.findViewById(R.id.editPlanDescription);
        btnSelectStartDate = view.findViewById(R.id.btnSelectStartDate);
        btnSelectEndDate = view.findViewById(R.id.btnSelectEndDate);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        updateDateButtons();

        btnSelectStartDate.setOnClickListener(v -> showDatePicker(true));
        btnSelectEndDate.setOnClickListener(v -> showDatePicker(false));

        view.findViewById(R.id.btnSavePlan).setOnClickListener(v -> saveNewPlan());
    }

    private void updateDateButtons() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        btnSelectStartDate.setText("Start: " + sdf.format(new Date(startDateMs)));
        btnSelectEndDate.setText("End: " + sdf.format(new Date(endDateMs)));
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(isStart ? startDateMs : endDateMs);

        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 0, 0, 0);
            if (isStart) {
                startDateMs = selected.getTimeInMillis();
                if (endDateMs < startDateMs) endDateMs = startDateMs;
            } else {
                endDateMs = selected.getTimeInMillis();
            }
            updateDateButtons();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveNewPlan() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to create travel plans.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = editPlanTitle.getText() != null ? editPlanTitle.getText().toString().trim() : "";
        String description = editPlanDescription.getText() != null ? editPlanDescription.getText().toString().trim() : "";

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a plan title.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDateMs > endDateMs) {
            Toast.makeText(requireContext(), "Start date cannot be after end date.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        long now = System.currentTimeMillis();
        TravelPlan plan = new TravelPlan(
                "",
                user.getUid(),
                title,
                description,
                startDateMs,
                endDateMs,
                TravelPlan.STATUS_DRAFT,
                now,
                now,
                0
        );

        travelPlanRepository.createPlan(user.getUid(), plan, new TravelPlanRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Travel Plan Created!", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(PlanDetailFragment.newInstance(plan.getId()), false);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to create plan: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}