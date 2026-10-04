package com.smartproperty.platform.rental.query;

import java.math.BigDecimal;

public class RentalQuery {
    private Long communityId;
    private Long agencyId;
    private String status;
    private String listingMode;
    private String layoutType;
    private String keyword;
    private BigDecimal rentMin;
    private BigDecimal rentMax;
    private Integer page = 1;
    private Integer pageSize = 20;
    public Long getCommunityId() { return communityId; } public void setCommunityId(Long value) { communityId = value; }
    public Long getAgencyId() { return agencyId; } public void setAgencyId(Long value) { agencyId = value; }
    public String getStatus() { return status; } public void setStatus(String value) { status = value; }
    public String getListingMode() { return listingMode; } public void setListingMode(String value) { listingMode = value; }
    public String getLayoutType() { return layoutType; } public void setLayoutType(String value) { layoutType = value; }
    public String getKeyword() { return keyword; } public void setKeyword(String value) { keyword = value; }
    public BigDecimal getRentMin() { return rentMin; } public void setRentMin(BigDecimal value) { rentMin = value; }
    public BigDecimal getRentMax() { return rentMax; } public void setRentMax(BigDecimal value) { rentMax = value; }
    public Integer getPage() { return page; } public void setPage(Integer value) { page = value; }
    public Integer getPageSize() { return pageSize; } public void setPageSize(Integer value) { pageSize = value; }
}
