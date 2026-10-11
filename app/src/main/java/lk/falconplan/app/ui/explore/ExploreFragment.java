package lk.falconplan.app.ui.explore;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import java.util.Locale;

import lk.falconplan.app.R;
import lk.falconplan.app.data.model.HomestayListing;

public class ExploreFragment extends Fragment {

    private ExploreViewModel viewModel;
    private HomestayAdapter adapter;
    private View layoutEmpty;

    public static ExploreFragment newInstance() {
        return new ExploreFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ExploreViewModel.class);

        RecyclerView recyclerListings = view.findViewById(R.id.recyclerListings);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        EditText editSearch = view.findViewById(R.id.editSearch);
        ChipGroup chipGroupCategories = view.findViewById(R.id.chipGroupCategories);

        recyclerListings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HomestayAdapter();
        recyclerListings.setAdapter(adapter);

        adapter.setOnListingClickListener(this::showListingDetailDialog);

        viewModel.getListingsLiveData().observe(getViewLifecycleOwner(), listings -> {
            adapter.setListings(listings);
            if (listings == null || listings.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                recyclerListings.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                recyclerListings.setVisibility(View.VISIBLE);
            }
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.search(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipHillCountry) {
                viewModel.filterByCategory("Ella & Hill Country");
            } else if (checkedId == R.id.chipCultural) {
                viewModel.filterByCategory("Cultural Triangle");
            } else if (checkedId == R.id.chipKnuckles) {
                viewModel.filterByCategory("Knuckles Villages");
            } else if (checkedId == R.id.chipRainforest) {
                viewModel.filterByCategory("Sinharaja Edge");
            } else {
                viewModel.filterByCategory("All Regions");
            }
        });

        view.findViewById(R.id.btnFilter).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Filter options design preview", Toast.LENGTH_SHORT).show()
        );
    }

    private void showListingDetailDialog(HomestayListing listing) {
        String msg = String.format(Locale.getDefault(),
                "%s\n\nLocation: %s\nHost: %s (%s)\nPrice: LKR %,.0f / night\nRating: %.1f\n\nDescription:\n%s\n\n[Design Preview — Booking flow enabled in Phase 2]",
                listing.getTitle(),
                listing.getLocationName(),
                listing.getHostName(),
                listing.getHostRole(),
                listing.getPriceLkrPerNight(),
                listing.getRating(),
                listing.getDescription()
        );

        new AlertDialog.Builder(requireContext())
                .setTitle("Design Preview: Homestay Listing")
                .setMessage(msg)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }
}