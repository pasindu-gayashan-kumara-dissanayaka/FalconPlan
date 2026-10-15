package lk.falconplan.app;

import org.junit.Test;

import lk.falconplan.app.data.model.RoomPricing;

import static org.junit.Assert.*;

public class RoomPricingTest {

    @Test
    public void pricing_validLkrPricing_returnsTrue() {
        RoomPricing pricing = new RoomPricing(
                RoomPricing.CURRENCY_LKR,
                9500,
                2,
                1500,
                true,
                800,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertTrue(pricing.isValid(4)); // maxGuestsPerUnit = 4
    }

    @Test
    public void pricing_invalidCurrency_returnsFalse() {
        RoomPricing pricing = new RoomPricing(
                "USD",
                9500,
                2,
                1500,
                false,
                0,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertFalse(pricing.isValid(4));
    }

    @Test
    public void pricing_zeroBasePrice_returnsFalse() {
        RoomPricing pricing = new RoomPricing(
                RoomPricing.CURRENCY_LKR,
                0, // Invalid zero price
                2,
                1500,
                false,
                0,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertFalse(pricing.isValid(4));
    }

    @Test
    public void pricing_includedGuestsExceedsCapacity_returnsFalse() {
        RoomPricing pricing = new RoomPricing(
                RoomPricing.CURRENCY_LKR,
                9500,
                5, // Exceeds max 2 guests
                1500,
                false,
                0,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertFalse(pricing.isValid(2)); // maxGuestsPerUnit = 2
    }
}