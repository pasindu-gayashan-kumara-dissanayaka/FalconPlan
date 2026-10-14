package lk.falconplan.app.ui.provider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.util.List;

import lk.falconplan.app.MainActivity;
import lk.falconplan.app.R;
import lk.falconplan.app.data.model.ProviderListing;
import lk.falconplan.app.data.remote.BackendApiClient;
import lk.falconplan.app.data.repository.ProviderListingRepository;
import lk.falconplan.app.data.repository.ProviderListingRepositoryImpl;

public class ProviderListingsFragment extends Fragment implements ProviderListingAdapter.OnListingActionListener {

    private ProviderListingRepository listingRepository;
    private ProviderListingAdapter adapter;
    private RecyclerView recyclerProviderListings;
    private View layoutEmpty;
    private ProgressBar progressBar;

    public static ProviderListingsFragment newInstance() {
        return new ProviderListingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_listings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listingRepository = new ProviderListingRepositoryImpl();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        View.OnClickListener addListener = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMainContentFragment(AddListingFragment.newInstance(), true);
            }
        };

        view.findViewById(R.id.btnAddListingTop).setOnClickListener(addListener);
        view.findViewById(R.id.btnAddFirstListing).setOnClickListener(addListener);

        recyclerProviderListings = view.findViewById(R.id.recyclerProviderListings);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerProviderListings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProviderListingAdapter();
        recyclerProviderListings.setAdapter(adapter);
        adapter.setOnListingActionListener(this);

        loadListings();
    }

    private void loadListings() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        listingRepository.getProviderListings(user.getUid(), new ProviderListingRepository.ListingsCallback() {
            @Override
            public void onSuccess(List<ProviderListing> listings) {
                progressBar.setVisibility(View.GONE);
                adapter.setItems(listings);

                if (listings == null || listings.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerProviderListings.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerProviderListings.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load listings: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPhotosClick(ProviderListing listing) {
        if (getActivity() instanceof MainActivity && listing != null && listing.getId() != null) {
            ((MainActivity) getActivity()).loadMainContentFragment(ListingPhotoGalleryFragment.newInstance(listing.getId()), true);
        }
    }

    @Override
    public void onSubmitClick(ProviderListing listing) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || listing == null || listing.getId() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Submit for Admin Review")
                .setMessage("Submit this listing draft for admin moderation review?")
                .setPositiveButton("Submit", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    user.getIdToken(true).addOnCompleteListener(task -> {
                        String idToken = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                        BackendApiClient.getInstance().submitListing(idToken, listing.getId(), new BackendApiClient.ApiCallback<JSONObject>() {
                            @Override
                            public void onSuccess(JSONObject result) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Listing submitted for review!", Toast.LENGTH_SHORT).show();
                                loadListings();
                            }

                            @Override
                            public void onError(Exception exception) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Submission failed: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onEditClick(ProviderListing listing) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadMainContentFragment(AddListingFragment.newInstance(listing.getId()), true);
        }
    }

    @Override
    public void onDeleteClick(ProviderListing listing) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || listing == null || listing.getId() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Private Draft")
                .setMessage("Are you sure you want to delete this private listing draft?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    listingRepository.deleteListing(user.getUid(), listing.getId(), new ProviderListingRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Draft deleted.", Toast.LENGTH_SHORT).show();
                            loadListings();
                        }

                        @Override
                        public void onError(Exception exception) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Failed to delete: " + exception.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}