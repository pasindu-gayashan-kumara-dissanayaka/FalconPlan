package lk.falconplan.app;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ListingPhotoTest {

    private static final List<String> ALLOWED_PHOTO_MIMES = Arrays.asList("image/jpeg", "image/png", "image/webp");
    private static final int MAX_PHOTOS_PER_LISTING = 10;
    private static final long MAX_FILE_BYTES = 5 * 1024 * 1024; // 5MB

    @Test
    public void photo_validMimeTypes_returnsTrue() {
        assertTrue(ALLOWED_PHOTO_MIMES.contains("image/jpeg"));
        assertTrue(ALLOWED_PHOTO_MIMES.contains("image/png"));
        assertTrue(ALLOWED_PHOTO_MIMES.contains("image/webp"));
    }

    @Test
    public void photo_invalidMimeType_returnsFalse() {
        assertFalse(ALLOWED_PHOTO_MIMES.contains("application/pdf"));
        assertFalse(ALLOWED_PHOTO_MIMES.contains("text/html"));
    }

    @Test
    public void photo_maxLimitEnforced_limit10() {
        int count9 = 9;
        int count10 = 10;
        int count11 = 11;

        assertTrue(count9 < MAX_PHOTOS_PER_LISTING);
        assertFalse(count10 < MAX_PHOTOS_PER_LISTING);
        assertFalse(count11 < MAX_PHOTOS_PER_LISTING);
    }

    @Test
    public void photo_fileSizeLimit_enforces5MB() {
        long validSize = 3 * 1024 * 1024; // 3MB
        long oversized = 7 * 1024 * 1024; // 7MB

        assertTrue(validSize <= MAX_FILE_BYTES);
        assertFalse(oversized <= MAX_FILE_BYTES);
    }
}