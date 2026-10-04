package com.smartproperty.platform.property.query;

import com.smartproperty.platform.access.CommunityScopeQuery;
import java.util.List;

public class PropertyQuery implements CommunityScopeQuery {
    private Long communityId;
    private Long buildingId;
    private String layoutType;
    private String occupancyStatus;
    private String keyword;
    private Integer page = 1;
    private Integer pageSize = 20;
    private boolean scopeRestricted;
    private List<Long> scopedCommunityIds = List.of();

    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public Long getBuildingId() { return buildingId; }
    public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }
    public String getLayoutType() { return layoutType; }
    public void setLayoutType(String layoutType) { this.layoutType = layoutType; }
    public String getOccupancyStatus() { return occupancyStatus; }
    public void setOccupancyStatus(String occupancyStatus) { this.occupancyStatus = occupancyStatus; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public boolean isScopeRestricted() { return scopeRestricted; }
    @Override public void setScopeRestricted(boolean scopeRestricted) { this.scopeRestricted = scopeRestricted; }
    public List<Long> getScopedCommunityIds() { return scopedCommunityIds; }
    @Override public void setScopedCommunityIds(List<Long> scopedCommunityIds) { this.scopedCommunityIds = scopedCommunityIds == null ? List.of() : List.copyOf(scopedCommunityIds); }
}
