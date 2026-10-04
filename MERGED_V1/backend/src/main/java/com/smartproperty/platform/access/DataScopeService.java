package com.smartproperty.platform.access;

public interface DataScopeService {
    CommunityDataScope currentCommunityScope();
    <T extends CommunityScopeQuery> T apply(T query);
    void requireCommunity(Long communityId);
    void requireProperty(Long propertyUnitId);
    void requireBill(Long billId);
    void requireResidence(Long residenceId);
    void requireBuilding(Long buildingId);
    void requireCurrentResidentPerson(Long personId);
}
