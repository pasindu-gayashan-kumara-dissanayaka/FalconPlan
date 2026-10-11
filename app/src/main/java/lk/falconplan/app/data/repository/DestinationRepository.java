package lk.falconplan.app.data.repository;

import java.util.List;

import lk.falconplan.app.data.model.Destination;

public interface DestinationRepository {

    List<Destination> getCuratedDestinations();

    Destination getDestinationById(String id);

    List<Destination> filterDestinations(String query, String category);
}