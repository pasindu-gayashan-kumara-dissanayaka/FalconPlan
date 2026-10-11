package lk.falconplan.app.ui.plans;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Destination;
import lk.falconplan.app.data.model.TravelPlan;
import lk.falconplan.app.data.model.TravelPlanStop;
import lk.falconplan.app.data.repository.DestinationRepositoryImpl;
import lk.falconplan.app.data.repository.TravelPlanRepository;
import lk.falconplan.app.data.repository.TravelPlanRepositoryImpl;

public class PlanDetailFragment extends Fragment implements PlanStopAdapter.OnStopActionListener {

    private static final String ARG_PLAN_ID = "arg_plan_id";

    private TravelPlanRepository travelPlanRepository;
    private TravelPlan currentPlan;
    private PlanStopAdapter adapter;

    private TextView txtPlanTitle;
    private TextView txtPlanDates;
    private TextView txtPlanDescription;
    private RecyclerView recyclerStops;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static PlanDetailFragment newInstance(String planId) {
        PlanDetailFragment fragment = new PlanDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLAN_ID, planId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        travelPlanRepository = new TravelPlanRepositoryImpl();

        String planId = getArguments() != null ? getArguments().getString(ARG_PLAN_ID) : null;

        txtPlanTitle = view.findViewById(R.id.txtPlanTitle);
        txtPlanDates = view.findViewById(R.id.txtPlanDates);
        txtPlanDescription = view.findViewById(R.id.txtPlanDescription);
        recyclerStops = view.findViewById(R.id.recyclerStops);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnDeletePlan).setOnClickListener(v -> confirmDeletePlan());
        view.findViewById(R.id.btnAddStop).setOnClickListener(v -> showAddDestinationPicker());

        recyclerStops.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PlanStopAdapter();
        recyclerStops.setAdapter(adapter);
        adapter.setOnStopActionListener(this);

        loadPlanAndStops(planId);
    }

    private void loadPlanAndStops(String planId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || planId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        travelPlanRepository.getPlanDetails(user.getUid(), planId, new TravelPlanRepository.PlanCallback() {
            @Override
            public void onSuccess(TravelPlan plan) {
                if (plan == null) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Plan not found.", Toast.LENGTH_SHORT).show();
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    return;
                }

                currentPlan = plan;
                txtPlanTitle.setText(plan.getTitle());
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                txtPlanDates.setText(String.format("%s – %s", sdf.format(new Date(plan.getStartDate())), sdf.format(new Date(plan.getEndDate()))));
                txtPlanDescription.setText(plan.getDescription() != null && !plan.getDescription().isEmpty() ? plan.getDescription() : "No description.");

                loadStops(user.getUid(), plan.getId());
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load plan: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStops(String uid, String planId) {
        travelPlanRepository.getStopsForPlan(uid, planId, new TravelPlanRepository.StopsCallback() {
            @Override
            public void onSuccess(List<TravelPlanStop> stops) {
                progressBar.setVisibility(View.GONE);
                adapter.setStops(stops);
                if (stops == null || stops.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerStops.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerStops.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load stops: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDestinationPicker() {
        List<Destination> curated = DestinationRepositoryImpl.getInstance().getCuratedDestinations();
        String[] names = new String[curated.size()];
        for (int i = 0; i < curated.size(); i++) {
            names[i] = curated.get(i).getName() + " (" + curated.get(i).getDistrict() + ")";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Destination to Add")
                .setItems(names, (dialog, which) -> {
                    Destination selected = curated.get(which);
                    addDestinationToCurrentPlan(selected);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void addDestinationToCurrentPlan(Destination destination) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentPlan == null || destination == null) return;

        progressBar.setVisibility(View.VISIBLE);
        TravelPlanStop stop = new TravelPlanStop(
                "",
                destination.getId(),
                destination.getName(),
                destination.getDistrict(),
                destination.getLatitude(),
                destination.getLongitude(),
                currentPlan.getStartDate(),
                "",
                0
        );

        travelPlanRepository.addStopToPlan(user.getUid(), currentPlan.getId(), stop, new TravelPlanRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Added " + destination.getName() + " to plan!", Toast.LENGTH_SHORT).show();
                loadStops(user.getUid(), currentPlan.getId());
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to add stop: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeletePlan() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentPlan == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Travel Plan")
                .setMessage("Are you sure you want to delete '" + currentPlan.getTitle() + "' and its itinerary stops?")
                .setPositiveButton("Delete Plan", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    travelPlanRepository.deletePlan(user.getUid(), currentPlan.getId(), new TravelPlanRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Plan deleted.", Toast.LENGTH_SHORT).show();
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }

                        @Override
                        public void onError(Exception exception) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Failed to delete plan: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onEditNoteClick(TravelPlanStop stop) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentPlan == null) return;

        EditText input = new EditText(requireContext());
        input.setText(stop.getNotes() != null ? stop.getNotes() : "");
        input.setHint("Add notes or visit details...");

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Note for " + stop.getDestinationName())
                .setView(input)
                .setPositiveButton("Save Note", (dialog, which) -> {
                    String newNote = input.getText() != null ? input.getText().toString().trim() : "";
                    stop.setNotes(newNote);
                    travelPlanRepository.updateStop(user.getUid(), currentPlan.getId(), stop, new TravelPlanRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(requireContext(), "Note updated!", Toast.LENGTH_SHORT).show();
                            loadStops(user.getUid(), currentPlan.getId());
                        }

                        @Override
                        public void onError(Exception exception) {
                            Toast.makeText(requireContext(), "Failed to update note: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onRemoveClick(TravelPlanStop stop) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentPlan == null) return;

        travelPlanRepository.removeStopFromPlan(user.getUid(), currentPlan.getId(), stop.getId(), new TravelPlanRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Removed destination.", Toast.LENGTH_SHORT).show();
                loadStops(user.getUid(), currentPlan.getId());
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(requireContext(), "Failed to remove: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMoveUpClick(TravelPlanStop stop, int position) {
        if (position <= 0) return;
        swapStopsAndSave(position, position - 1);
    }

    @Override
    public void onMoveDownClick(TravelPlanStop stop, int position) {
        List<TravelPlanStop> currentStops = adapter.getStops();
        if (position >= currentStops.size() - 1) return;
        swapStopsAndSave(position, position + 1);
    }

    private void swapStopsAndSave(int fromPos, int toPos) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentPlan == null) return;

        List<TravelPlanStop> stops = adapter.getStops();
        TravelPlanStop temp = stops.get(fromPos);
        stops.set(fromPos, stops.get(toPos));
        stops.set(toPos, temp);

        progressBar.setVisibility(View.VISIBLE);
        travelPlanRepository.reorderStops(user.getUid(), currentPlan.getId(), stops, new TravelPlanRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                loadStops(user.getUid(), currentPlan.getId());
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to reorder: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewMapClick(TravelPlanStop stop) {
        if (!stop.isValidCoordinates()) return;
        String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                stop.getLatitude(), stop.getLongitude(),
                stop.getLatitude(), stop.getLongitude(),
                Uri.encode(stop.getDestinationName()));
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(webIntent);
        }
    }

    @Override
    public void onDirectionsClick(TravelPlanStop stop) {
        if (!stop.isValidCoordinates()) return;
        String uri = String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                stop.getLatitude(), stop.getLongitude());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(webIntent);
        }
    }
}