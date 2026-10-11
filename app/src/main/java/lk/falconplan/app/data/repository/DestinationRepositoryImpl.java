package lk.falconplan.app.data.repository;

import java.util.ArrayList;
import java.util.List;

import lk.falconplan.app.data.model.Destination;

public class DestinationRepositoryImpl implements DestinationRepository {

    private static DestinationRepositoryImpl instance;
    private final List<Destination> curatedDestinations;

    private DestinationRepositoryImpl() {
        curatedDestinations = new ArrayList<>();
        populateCuratedCatalogue();
    }

    public static synchronized DestinationRepositoryImpl getInstance() {
        if (instance == null) {
            instance = new DestinationRepositoryImpl();
        }
        return instance;
    }

    private void populateCuratedCatalogue() {
        curatedDestinations.add(new Destination(
                "dest_sigiriya",
                "Sigiriya Rock Fortress",
                "Matale District",
                "Central Province",
                "5th-century ancient palace and fortress complex with world-famous water gardens and frescoes.",
                "Cultural Heritage",
                7.9570,
                80.7603,
                "logo_falconplan",
                "Best visited in early morning or late afternoon. Wear comfortable walking shoes."
        ));

        curatedDestinations.add(new Destination(
                "dest_meemure",
                "Meemure Ancient Village",
                "Kandy District",
                "Central Province",
                "Secluded, historic mountain village surrounded by the Knuckles Range and Lakegala Peak.",
                "Villages",
                7.4333,
                80.8500,
                "logo_falconplan",
                "Cell coverage is limited. Respect local village traditions and rivers."
        ));

        curatedDestinations.add(new Destination(
                "dest_nine_arches",
                "Nine Arches Bridge & Ella",
                "Badulla District",
                "Uva Province",
                "Iconic colonial-era viaduct bridge nestled amidst misty tea gardens in the Ella highlands.",
                "Mountains",
                6.8768,
                81.0608,
                "logo_falconplan",
                "Check daily train timetable to watch trains cross the viaduct bridge."
        ));

        curatedDestinations.add(new Destination(
                "dest_sinharaja",
                "Sinharaja Rainforest Reserve",
                "Ratnapura District",
                "Sabaragamuwa Province",
                "UNESCO World Heritage tropical rainforest home to endemic birds, orchids, and biodiversity.",
                "Nature",
                6.4167,
                80.4167,
                "logo_falconplan",
                "Guide required for entry at Kudawa or Pitadeniya gates. Rain gear recommended."
        ));

        curatedDestinations.add(new Destination(
                "dest_mirissa",
                "Mirissa Coastal Village",
                "Matara District",
                "Southern Province",
                "Scenic southern bay famous for whale watching, palm-fringed beaches, and fresh seafood.",
                "Beaches",
                5.9483,
                80.4716,
                "logo_falconplan",
                "Whale watching season runs November through April."
        ));

        curatedDestinations.add(new Destination(
                "dest_knuckles",
                "Knuckles Conservation Range",
                "Matale & Kandy Districts",
                "Central Province",
                "High-altitude cloud forests, cascading waterfalls, and eco-trekking trails.",
                "Mountains",
                7.4667,
                80.8000,
                "logo_falconplan",
                "Ideal for guided multi-day village trekking and camping."
        ));

        curatedDestinations.add(new Destination(
                "dest_anuradhapura",
                "Anuradhapura Sacred City",
                "Anuradhapura District",
                "North Central Province",
                "Ancient capital boasting majestic brick stupas, sacred Bodhi tree, and monastic ruins.",
                "Cultural Heritage",
                8.3114,
                80.4037,
                "logo_falconplan",
                "Respectful attire covering shoulders and knees is required at sacred sites."
        ));
    }

    @Override
    public List<Destination> getCuratedDestinations() {
        return new ArrayList<>(curatedDestinations);
    }

    @Override
    public Destination getDestinationById(String id) {
        if (id == null) return null;
        for (Destination dest : curatedDestinations) {
            if (dest.getId().equalsIgnoreCase(id)) {
                return dest;
            }
        }
        return null;
    }

    @Override
    public List<Destination> filterDestinations(String query, String category) {
        String lowerQuery = (query != null) ? query.toLowerCase().trim() : "";
        boolean isAllCategory = (category == null || category.equalsIgnoreCase("All") || category.equalsIgnoreCase("All Regions"));

        List<Destination> filtered = new ArrayList<>();
        for (Destination dest : curatedDestinations) {
            boolean matchesCategory = isAllCategory || dest.getCategory().equalsIgnoreCase(category);
            boolean matchesQuery = lowerQuery.isEmpty()
                    || dest.getName().toLowerCase().contains(lowerQuery)
                    || dest.getDistrict().toLowerCase().contains(lowerQuery)
                    || dest.getProvince().toLowerCase().contains(lowerQuery)
                    || dest.getDescription().toLowerCase().contains(lowerQuery);

            if (matchesCategory && matchesQuery) {
                filtered.add(dest);
            }
        }
        return filtered;
    }
}