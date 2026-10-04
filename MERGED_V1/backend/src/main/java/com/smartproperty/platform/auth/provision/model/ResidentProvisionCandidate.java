package com.smartproperty.platform.auth.provision.model;

public record ResidentProvisionCandidate(Long personId, String fullName, Long accountId, String username,
                                         boolean residentAccount, String residenceRole, String occupancyStatus,
                                         boolean ownerFlag, boolean ownerTenantPair) {
    public String residentKind() {
        if (ownerFlag && "TENANT_OCCUPIED".equals(occupancyStatus)) return "OWNER（出租房产权人）";
        if (ownerFlag) return "OWNER_OCCUPIED";
        if ("TENANT_OCCUPIED".equals(occupancyStatus)) return "TENANT";
        return "MEMBER".equals(residenceRole) ? "MEMBER_RESIDENT" : "RESPONSIBLE_RESIDENT";
    }
}
