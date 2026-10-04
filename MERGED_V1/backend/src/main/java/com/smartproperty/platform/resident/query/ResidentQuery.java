package com.smartproperty.platform.resident.query;

import com.smartproperty.platform.access.CommunityScopeQuery;
import java.util.List;

public class ResidentQuery implements CommunityScopeQuery {
    private String keyword;
    private Long communityId;
    private String occupancyStatus;
    private String residenceRole;
    private String relationToResponsible;
    private Integer page = 1;
    private Integer pageSize = 20;
    private boolean scopeRestricted;
    private List<Long> scopedCommunityIds = List.of();
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public String getOccupancyStatus() { return occupancyStatus; }
    public void setOccupancyStatus(String occupancyStatus) { this.occupancyStatus = occupancyStatus; }
    public String getResidenceRole() { return residenceRole; }
    public void setResidenceRole(String residenceRole) { this.residenceRole = residenceRole; }
    public String getRelationToResponsible() { return relationToResponsible; }
    public void setRelationToResponsible(String relationToResponsible) { this.relationToResponsible = relationToResponsible; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public boolean isScopeRestricted() { return scopeRestricted; }
    @Override public void setScopeRestricted(boolean value) { scopeRestricted = value; }
    public List<Long> getScopedCommunityIds() { return scopedCommunityIds; }
    @Override public void setScopedCommunityIds(List<Long> value) { scopedCommunityIds = value == null ? List.of() : List.copyOf(value); }
}
