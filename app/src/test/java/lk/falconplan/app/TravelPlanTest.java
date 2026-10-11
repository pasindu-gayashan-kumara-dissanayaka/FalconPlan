package lk.falconplan.app;

import org.junit.Test;

import lk.falconplan.app.data.model.TravelPlan;
import lk.falconplan.app.data.model.TravelPlanStop;

import static org.junit.Assert.*;

public class TravelPlanTest {

    @Test
    public void travelPlan_validDateRange_returnsTrue() {
        long start = System.currentTimeMillis();
        long end = start + (86400000L * 3); // 3 days later
        TravelPlan plan = new TravelPlan("plan_01", "uid_alice", "Ella Trek", "Desc", start, end, "draft", start, start, 0);
        assertTrue(plan.isValidDateRange());
    }

    @Test
    public void travelPlan_invalidDateRange_returnsFalse() {
        long start = System.currentTimeMillis();
        long end = start - 86400000L; // 1 day earlier
        TravelPlan plan = new TravelPlan("plan_01", "uid_alice", "Invalid Trip", "Desc", start, end, "draft", start, start, 0);
        assertFalse(plan.isValidDateRange());
    }

    @Test
    public void travelPlanStop_validCoordinatesAndOrder_setsCorrectly() {
        long visitDate = System.currentTimeMillis();
        TravelPlanStop stop = new TravelPlanStop("stop_01", "dest_sigiriya", "Sigiriya Rock Fortress", "Matale", 7.9570, 80.7603, visitDate, "Early visit", 1);
        assertEquals("stop_01", stop.getId());
        assertEquals("dest_sigiriya", stop.getDestinationId());
        assertEquals("Sigiriya Rock Fortress", stop.getDestinationName());
        assertEquals(1, stop.getOrderIndex());
        assertTrue(stop.isValidCoordinates());
    }

    @Test
    public void travelPlanStop_invalidCoordinates_returnsFalse() {
        TravelPlanStop invalidStop = new TravelPlanStop("stop_02", "dest_invalid", "Invalid Spot", "District", 100.0, 200.0, System.currentTimeMillis(), "", 2);
        assertFalse(invalidStop.isValidCoordinates());
    }
}