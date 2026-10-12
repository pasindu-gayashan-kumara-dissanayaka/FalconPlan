package lk.falconplan.app;

import org.junit.Test;

import lk.falconplan.app.data.model.ProviderProfile;

import static org.junit.Assert.*;

public class ProviderProfileTest {

    @Test
    public void providerProfile_validFields_returnsTrue() {
        long now = System.currentTimeMillis();
        ProviderProfile profile = new ProviderProfile(
                "uid_host_123",
                "Saman Perera",
                "Knuckles Eco Homestay",
                ProviderProfile.TYPE_HOMESTAY,
                "Matale",
                "Meemure",
                "Authentic village stay",
                "+94 77 123 4567",
                ProviderProfile.KYC_PENDING,
                now,
                now
        );

        assertTrue(profile.isValid());
        assertEquals("uid_host_123", profile.getUid());
        assertEquals("pending_identity_verification", profile.getKycStatus());
        assertEquals("homestay", profile.getBusinessType());
    }

    @Test
    public void providerProfile_emptyBusinessName_returnsFalse() {
        long now = System.currentTimeMillis();
        ProviderProfile invalidProfile = new ProviderProfile(
                "uid_host_123",
                "Saman Perera",
                "", // Empty Business Name
                ProviderProfile.TYPE_HOMESTAY,
                "Matale",
                "Meemure",
                "Authentic village stay",
                "",
                ProviderProfile.KYC_PENDING,
                now,
                now
        );

        assertFalse(invalidProfile.isValid());
    }
}