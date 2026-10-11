package lk.falconplan.app.ui.explore;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.data.model.HomestayListing;
import lk.falconplan.app.data.repository.MockDataRepository;

public class ExploreViewModel extends ViewModel {

    private final MutableLiveData<List<HomestayListing>> listingsLiveData = new MutableLiveData<>();
    private final MockDataRepository repository;
    private List<HomestayListing> allListings = new ArrayList<>();

    private String currentCategory = "All Regions";
    private String currentQuery = "";

    public ExploreViewModel() {
        this.repository = MockDataRepository.getInstance();
        loadListings();
    }

    public LiveData<List<HomestayListing>> getListingsLiveData() {
        return listingsLiveData;
    }

    public void loadListings() {
        allListings = repository.getFeaturedHomestays();
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
        boolean isAllCategories = category.equalsIgnoreCase("All Regions");

        List<HomestayListing> filtered = new ArrayList<>();
        for (HomestayListing item : allListings) {
            boolean matchesCategory = isAllCategories || item.getRegionCategory().equalsIgnoreCase(category);
            boolean matchesQuery = lowerQuery.isEmpty()
                    || item.getTitle().toLowerCase().contains(lowerQuery)
                    || item.getLocationName().toLowerCase().contains(lowerQuery)
                    || item.getRegionCategory().toLowerCase().contains(lowerQuery)
                    || item.getDescription().toLowerCase().contains(lowerQuery);

            if (matchesCategory && matchesQuery) {
                filtered.add(item);
            }
        }
        listingsLiveData.setValue(filtered);
    }
}