package lk.falconplan.app.ui.explore;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.data.model.Destination;
import lk.falconplan.app.data.model.HomestayListing;
import lk.falconplan.app.data.repository.DestinationRepository;
import lk.falconplan.app.data.repository.DestinationRepositoryImpl;
import lk.falconplan.app.data.repository.MockDataRepository;

public class ExploreViewModel extends ViewModel {

    private final MutableLiveData<List<HomestayListing>> listingsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Destination>> destinationsLiveData = new MutableLiveData<>();

    private final MockDataRepository homestayRepository;
    private final DestinationRepository destinationRepository;

    private List<HomestayListing> allHomestays = new ArrayList<>();
    private String currentCategory = "All Regions";
    private String currentQuery = "";

    public ExploreViewModel() {
        this.homestayRepository = MockDataRepository.getInstance();
        this.destinationRepository = DestinationRepositoryImpl.getInstance();
        loadData();
    }

    public LiveData<List<HomestayListing>> getListingsLiveData() {
        return listingsLiveData;
    }

    public LiveData<List<Destination>> getDestinationsLiveData() {
        return destinationsLiveData;
    }

    public void loadData() {
        allHomestays = homestayRepository.getFeaturedHomestays();
        applyFilters(currentCategory, currentQuery);
    }

    public void filterByCategory(String category) {
        this.currentCategory = (category != null) ? category : "All Regions";
        applyFilters(currentCategory, currentQuery);
    }

    public void search(String query) {
        this.currentQuery = (query != null) ? query : "";
        applyFilters(currentCategory, currentQuery);
    }

    private void applyFilters(String category, String query) {
        String lowerQuery = query.toLowerCase().trim();
        boolean isAllCategories = category.equalsIgnoreCase("All Regions") || category.equalsIgnoreCase("All");

        // Filter Homestays
        List<HomestayListing> filteredHomestays = new ArrayList<>();
        for (HomestayListing item : allHomestays) {
            boolean matchesCategory = isAllCategories || item.getRegionCategory().equalsIgnoreCase(category);
            boolean matchesQuery = lowerQuery.isEmpty()
                    || item.getTitle().toLowerCase().contains(lowerQuery)
                    || item.getLocationName().toLowerCase().contains(lowerQuery)
                    || item.getRegionCategory().toLowerCase().contains(lowerQuery)
                    || item.getDescription().toLowerCase().contains(lowerQuery);

            if (matchesCategory && matchesQuery) {
                filteredHomestays.add(item);
            }
        }
        listingsLiveData.setValue(filteredHomestays);

        // Filter Curated Geographical Destinations
        List<Destination> filteredDestinations = destinationRepository.filterDestinations(lowerQuery, category);
        destinationsLiveData.setValue(filteredDestinations);
    }
}