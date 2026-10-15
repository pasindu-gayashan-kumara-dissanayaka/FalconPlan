package lk.falconplan.app;

import org.junit.Test;

import lk.falconplan.app.data.model.RoomAvailabilityDay;

import static org.junit.Assert.*;

public class RoomAvailabilityTest {

    @Test
    public void availability_validDateAndBlockedUnits_returnsTrue() {
        RoomAvailabilityDay day = new RoomAvailabilityDay("2026-11-03", 1, "Maintenance", System.currentTimeMillis());
        assertTrue(day.isValid(2)); // totalUnits = 2
    }

    @Test
    public void availability_invalidDateFormat_returnsFalse() {
        RoomAvailabilityDay day = new RoomAvailabilityDay("03-11-2026", 1, "Maintenance", System.currentTimeMillis());
        assertFalse(day.isValid(2));
    }

    @Test
    public void availability_negativeBlockedUnits_returnsFalse() {
        RoomAvailabilityDay day = new RoomAvailabilityDay("2026-11-03", -1, "Negative", System.currentTimeMillis());
        assertFalse(day.isValid(2));
    }

    @Test
    public void availability_blockedUnitsExceedsTotalUnits_returnsFalse() {
        RoomAvailabilityDay day = new RoomAvailabilityDay("2026-11-03", 3, "Exceeds total", System.currentTimeMillis());
        assertFalse(day.isValid(2)); // totalUnits = 2
    }
}