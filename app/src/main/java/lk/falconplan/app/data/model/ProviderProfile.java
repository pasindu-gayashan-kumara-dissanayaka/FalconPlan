package lk.falconplan.app.data.model;

import java.util.HashMap;
import java.util.Map;

public class ProviderProfile {

    public static final String TYPE_HOMESTAY = "homestay";
    public static final String TYPE_EXPERIENCE = "experience";
    public static final String TYPE_BOTH = "both";

    public static final String KYC_PENDING = "pending_identity_verification";
    public static final String KYC_VERIFIED = "verified";

    private String uid;
    private String hostDisplayName;
    private String businessName;
    private String businessType;
    private String district;
    private String village;
    private String shortDescription;
    private String contactPhone;
    private String kycStatus;
    private long createdAt;
    private long updatedAt;

    public ProviderProfile() {
        // Required empty constructor for Firestore
    }

    public ProviderProfile(String uid, String hostDisplayName, String businessName, String businessType,
                           String district, String village, String shortDescription, String contactPhone,
                           String kycStatus, long createdAt, long updatedAt) {
        this.uid = uid;
        this.hostDisplayName = hostDisplayName;
        this.businessName = businessName;
        this.businessType = businessType != null ? businessType : TYPE_HOMESTAY;
        this.district = district;
        this.village = village;
        this.shortDescription = shortDescription != null ? shortDescription : "";
        this.contactPhone = contactPhone != null ? contactPhone : "";
        this.kycStatus = kycStatus != null ? kycStatus : KYC_PENDING;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getHostDisplayName() { return hostDisplayName; }
    public void setHostDisplayName(String hostDisplayName) { this.hostDisplayName = hostDisplayName; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType != null ? businessType : TYPE_HOMESTAY; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription != null ? shortDescription : ""; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone != null ? contactPhone : ""; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus != null ? kycStatus : KYC_PENDING; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isValid() {
        return uid != null && !uid.isEmpty()
                && hostDisplayName != null && !hostDisplayName.trim().isEmpty()
                && businessName != null && !businessName.trim().isEmpty()
                && district != null && !district.trim().isEmpty()
                && village != null && !village.trim().isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("hostDisplayName", hostDisplayName);
        map.put("businessName", businessName);
        map.put("businessType", businessType);
        map.put("district", district);
        map.put("village", village);
        map.put("shortDescription", shortDescription);
        map.put("contactPhone", contactPhone);
        map.put("kycStatus", kycStatus);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}