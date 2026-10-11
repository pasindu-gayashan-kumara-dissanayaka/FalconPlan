package lk.falconplan.app.ui.plans;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.PublicTravelPlan;
import lk.falconplan.app.data.model.TravelPlanStop;
import lk.falconplan.app.data.repository.TravelPlanRepository;
import lk.falconplan.app.data.repository.TravelPlanRepositoryImpl;

public class PublicPlanDetailFragment extends Fragment implements PlanStopAdapter.OnStopActionListener {

    private static final String ARG_PUBLIC_PLAN_ID = "arg_public_plan_id";

    private TravelPlanRepository travelPlanRepository;
    private PublicTravelPlan publicPlan;
    private PlanStopAdapter adapter;

    private TextView txtPublicPlanTitle;
    private TextView txtOwnerName;
    private TextView txtPlanDates;
    private TextView txtPlanDescription;
    private ProgressBar progressBar;

    public static PublicPlanDetailFragment newInstance(String publicPlanId) {
        PublicPlanDetailFragment fragment = new PublicPlanDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PUBLIC_PLAN_ID, publicPlanId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_plan_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        travelPlanRepository = new TravelPlanRepositoryImpl();

        String planId = getArguments() != null ? getArguments().getString(ARG_PUBLIC_PLAN_ID) : null;

        txtPublicPlanTitle = view.findViewById(R.id.txtPublicPlanTitle);
        txtOwnerName = view.findViewById(R.id.txtOwnerName);
        txtPlanDates = view.findViewById(R.id.txtPlanDates);
        txtPlanDescription = view.findViewById(R.id.txtPlanDescription);
        RecyclerView recyclerStops = view.findViewById(R.id.recyclerStops);
        progressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnSharePlan).setOnClickListener(v -> shareItinerary());
        view.findViewById(R.id.btnCopyPlan).setOnClickListener(v -> copyPlanToPrivate());

        recyclerStops.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PlanStopAdapter();
        recyclerStops.setAdapter(adapter);
        adapter.setOnStopActionListener(this);

        loadPublicPlan(planId);
    }

    private void loadPublicPlan(String planId) {
        progressBar.setVisibility(View.VISIBLE);
        travelPlanRepository.getPublicPlans(new TravelPlanRepository.PublicPlansCallback() {
            @Override
            public void onSuccess(List<PublicTravelPlan> plans) {
                progressBar.setVisibility(View.GONE);
                if (plans != null) {
                    for (PublicTravelPlan item : plans) {
                        if (item.getPublicPlanId().equals(planId)) {
                            publicPlan = item;
                            displayPublicPlan();
                            return;
                        }
                    }
                }
                Toast.makeText(requireContext(), "Public plan not found.", Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load public plan: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPublicPlan() {
        if (publicPlan == null) return;
        txtPublicPlanTitle.setText(publicPlan.getTitle());
        txtOwnerName.setText(String.format("Published by %s", publicPlan.getOwnerDisplayName()));

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        txtPlanDates.setText(String.format("%s – %s", sdf.format(new Date(publicPlan.getStartDate())), sdf.format(new Date(publicPlan.getEndDate()))));
        txtPlanDescription.setText(publicPlan.getDescription() != null && !publicPlan.getDescription().isEmpty()
                ? publicPlan.getDescription() : "No description provided.");

        adapter.setStops(publicPlan.getStops());
    }

    private void copyPlanToPrivate() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to copy travel plans.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (publicPlan == null) return;

        progressBar.setVisibility(View.VISIBLE);
        travelPlanRepository.copyPublicPlanToPrivate(user.getUid(), publicPlan, new TravelPlanRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Copied to your private Travel Plans!", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_plans);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to copy plan: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareItinerary() {
        if (publicPlan == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Explore this Sri Lankan village travel itinerary on FalconPlan!\n\n");
        sb.append("📌 ").append(publicPlan.getTitle()).append("\n");
        sb.append("👤 Published by ").append(publicPlan.getOwnerDisplayName()).append("\n");
        if (publicPlan.getDescription() != null && !publicPlan.getDescription().isEmpty()) {
            sb.append("📝 ").append(publicPlan.getDescription()).append("\n");
        }
        sb.append("\nItinerary Destinations:\n");
        if (publicPlan.getStops() != null) {
            for (int i = 0; i < publicPlan.getStops().size(); i++) {
                TravelPlanStop stop = publicPlan.getStops().get(i);
                sb.append(i + 1).append(". ").append(stop.getDestinationName()).append(" (").append(stop.getDistrict()).append(")\n");
            }
        }
        sb.append("\nhttps://falconplan.app/plans/").append(publicPlan.getPublicPlanId());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, publicPlan.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Itinerary via"));
    }

    @Override public void onEditNoteClick(TravelPlanStop stop) {}
    @Override public void onRemoveClick(TravelPlanStop stop) {}
    @Override public void onMoveUpClick(TravelPlanStop stop, int position) {}
    @Override public void onMoveDownClick(TravelPlanStop stop, int position) {}

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