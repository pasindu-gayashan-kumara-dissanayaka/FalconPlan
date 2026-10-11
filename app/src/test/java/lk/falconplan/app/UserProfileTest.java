package lk.falconplan.app;

import org.junit.Test;

import lk.falconplan.app.data.common.Resource;
import lk.falconplan.app.data.model.UserProfile;

import static org.junit.Assert.*;

public class UserProfileTest {

    @Test
    public void userProfile_validActiveMode_setsCorrectly() {
        UserProfile profile = new UserProfile("uid_123", "Saman Perera", "saman@example.com", "", "provider", System.currentTimeMillis(), System.currentTimeMillis());
        assertEquals("provider", profile.getActiveMode());

        profile.setActiveMode("traveler");
        assertEquals("traveler", profile.getActiveMode());
    }

    @Test
    public void userProfile_invalidActiveMode_defaultsToTraveler() {
        UserProfile profile = new UserProfile("uid_123", "Saman Perera", "saman@example.com", "", "invalid_mode", System.currentTimeMillis(), System.currentTimeMillis());
        assertEquals("traveler", profile.getActiveMode());

        profile.setActiveMode("admin");
        assertEquals("traveler", profile.getActiveMode());
    }

    @Test
    public void resource_success_containsCorrectStatusAndData() {
        Resource<String> resource = Resource.success("Test Data");
        assertEquals(Resource.Status.SUCCESS, resource.getStatus());
        assertEquals("Test Data", resource.getData());
        assertNull(resource.getMessage());
    }

    @Test
    public void resource_error_containsCorrectStatusAndMessage() {
        Resource<String> resource = Resource.error("An error occurred", null);
        assertEquals(Resource.Status.ERROR, resource.getStatus());
        assertNull(resource.getData());
        assertEquals("An error occurred", resource.getMessage());
    }
}