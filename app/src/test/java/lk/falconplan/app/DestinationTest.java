package lk.falconplan.app;

import org.junit.Test;

import java.util.List;

import lk.falconplan.app.data.model.Destination;
import lk.falconplan.app.data.model.SavedPlace;
import lk.falconplan.app.data.repository.DestinationRepositoryImpl;

import static org.junit.Assert.*;

public class DestinationTest {

    @Test
    public void destination_validCoordinates_returnsTrue() {
        Destination sigiriya = new Destination("dest_01", "Sigiriya Rock", "Matale", "Central", "Desc", "Cultural", 7.9570, 80.7603, "logo", "Tips");
        assertTrue(sigiriya.isValidCoordinates());
    }

    @Test
    public void destination_invalidCoordinates_returnsFalse() {
        Destination invalid = new Destination("dest_02", "Invalid Location", "District", "Province", "Desc", "Category", 105.0, 200.0, "logo", "Tips");
        assertFalse(invalid.isValidCoordinates());
    }

    @Test
    public void savedPlace_idEqualsDestinationId_preventsDuplicates() {
        long now = System.currentTimeMillis();
        SavedPlace place = new SavedPlace("dest_sigiriya", "dest_sigiriya", "Sigiriya Rock", "Matale", 7.9570, 80.7603, "My travel note", now, now);
        assertEquals("dest_sigiriya", place.getId());
        assertEquals("dest_sigiriya", place.getDestinationId());
        assertTrue(place.isValidCoordinates());
    }

    @Test
    public void destinationRepository_curatedDestinations_returnsNonEmptyList() {
        DestinationRepositoryImpl repo = DestinationRepositoryImpl.getInstance();
        List<Destination> list = repo.getCuratedDestinations();
        assertNotNull(list);
        assertTrue(list.size() >= 5);
    }

    @Test
    public void destinationRepository_filterByQuery_returnsMatchingResults() {
        DestinationRepositoryImpl repo = DestinationRepositoryImpl.getInstance();
        List<Destination> filtered = repo.filterDestinations("Sigiriya", "All");
        assertNotNull(filtered);
        assertFalse(filtered.isEmpty());
        assertEquals("Sigiriya Rock Fortress", filtered.get(0).getName());
    }
}