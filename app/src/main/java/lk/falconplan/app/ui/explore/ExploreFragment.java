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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.Destination;
import lk.falconplan.app.data.model.SavedPlace;
import lk.falconplan.app.data.repository.SavedPlacesRepository;
import lk.falconplan.app.data.repository.SavedPlacesRepositoryImpl;

public class ExploreFragment extends Fragment {

    private ExploreViewModel viewModel;
    private HomestayAdapter homestayAdapter;
    private DestinationAdapter destinationAdapter;
    private SavedPlacesRepository savedPlacesRepository;
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
        savedPlacesRepository = new SavedPlacesRepositoryImpl();

        RecyclerView recyclerListings = view.findViewById(R.id.recyclerListings);
        RecyclerView recyclerDestinations = view.findViewById(R.id.recyclerDestinations);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        EditText editSearch = view.findViewById(R.id.editSearch);
        ChipGroup chipGroupCategories = view.findViewById(R.id.chipGroupCategories);

        recyclerListings.setLayoutManager(new LinearLayoutManager(requireContext()));
        homestayAdapter = new HomestayAdapter();
        recyclerListings.setAdapter(homestayAdapter);

        recyclerDestinations.setLayoutManager(new LinearLayoutManager(requireContext()));
        destinationAdapter = new DestinationAdapter();
        recyclerDestinations.setAdapter(destinationAdapter);

        destinationAdapter.setOnDestinationClickListener(new DestinationAdapter.OnDestinationClickListener() {
            @Override
            public void onDestinationClick(Destination destination) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMainContentFragment(
                            DestinationDetailFragment.newInstance(destination.getId()), true
                    );
                }
            }

            @Override
            public void onFavoriteClick(Destination destination) {
                saveDestinationToFirestore(destination);
            }
        });

        view.findViewById(R.id.btnSavedPlaces).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(new SavedPlacesFragment(), true);
            }
        });

        viewModel.getListingsLiveData().observe(getViewLifecycleOwner(), listings -> {
            homestayAdapter.setListings(listings);
            updateEmptyState();
        });

        viewModel.getDestinationsLiveData().observe(getViewLifecycleOwner(), destinations -> {
            destinationAdapter.setDestinations(destinations);
            updateEmptyState();
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
            if (checkedId == R.id.chipCultural) {
                viewModel.filterByCategory("Cultural Heritage");
            } else if (checkedId == R.id.chipVillages) {
                viewModel.filterByCategory("Villages");
            } else if (checkedId == R.id.chipMountains) {
                viewModel.filterByCategory("Mountains");
            } else if (checkedId == R.id.chipNature) {
                viewModel.filterByCategory("Nature");
            } else if (checkedId == R.id.chipBeaches) {
                viewModel.filterByCategory("Beaches");
            } else {
                viewModel.filterByCategory("All Regions");
            }
        });

        view.findViewById(R.id.btnFilter).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Search and category filters active.", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateEmptyState() {
        boolean hasHomestays = homestayAdapter.getItemCount() > 0;
        boolean hasDestinations = destinationAdapter.getItemCount() > 0;
        if (!hasHomestays && !hasDestinations) {
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void saveDestinationToFirestore(Destination destination) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to save destinations.", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();
        SavedPlace place = new SavedPlace(
                destination.getId(),
                destination.getId(),
                destination.getName(),
                destination.getDistrict(),
                destination.getLatitude(),
                destination.getLongitude(),
                "",
                now,
                now
        );

        savedPlacesRepository.savePlace(user.getUid(), place, new SavedPlacesRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Saved " + destination.getName() + " to your places!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(requireContext(), "Failed to save: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}