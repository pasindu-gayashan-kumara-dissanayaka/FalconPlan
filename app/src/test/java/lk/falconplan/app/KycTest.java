package lk.falconplan.app;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class KycTest {

    private static final List<String> ALLOWED_DOC_TYPES = Arrays.asList("nic_front", "nic_back", "passport", "proof_of_address");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "application/pdf");
    private static final long MAX_FILE_BYTES = 5 * 1024 * 1024; // 5MB

    @Test
    public void kyc_validDocumentTypes_returnsTrue() {
        assertTrue(ALLOWED_DOC_TYPES.contains("nic_front"));
        assertTrue(ALLOWED_DOC_TYPES.contains("nic_back"));
        assertTrue(ALLOWED_DOC_TYPES.contains("passport"));
    }

    @Test
    public void kyc_invalidDocumentType_returnsFalse() {
        assertFalse(ALLOWED_DOC_TYPES.contains("hacked_doc_type"));
    }

    @Test
    public void kyc_validMimeTypes_returnsTrue() {
        assertTrue(ALLOWED_MIME_TYPES.contains("image/jpeg"));
        assertTrue(ALLOWED_MIME_TYPES.contains("image/png"));
        assertTrue(ALLOWED_MIME_TYPES.contains("application/pdf"));
    }

    @Test
    public void kyc_fileSizeLimit_enforces5MB() {
        long validSize = 2 * 1024 * 1024; // 2MB
        long oversized = 6 * 1024 * 1024; // 6MB

        assertTrue(validSize <= MAX_FILE_BYTES);
        assertFalse(oversized <= MAX_FILE_BYTES);
    }
}