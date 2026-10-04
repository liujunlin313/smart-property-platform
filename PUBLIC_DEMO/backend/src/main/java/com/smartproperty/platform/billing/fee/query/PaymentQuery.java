package com.smartproperty.platform.billing.fee.query;
import com.smartproperty.platform.access.CommunityScopeQuery;
import java.util.List;

public class PaymentQuery implements CommunityScopeQuery {
    private Long communityId; private String status; private String paymentMethod; private Integer year; private Integer month; private Integer page = 1; private Integer pageSize = 20;
    private boolean scopeRestricted; private List<Long> scopedCommunityIds = List.of();
    public Long getCommunityId() { return communityId; } public void setCommunityId(Long v) { communityId = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String v) { paymentMethod = v; }
    public Integer getYear() { return year; } public void setYear(Integer v) { year = v; }
    public Integer getMonth() { return month; } public void setMonth(Integer v) { month = v; }
    public Integer getPage() { return page; } public void setPage(Integer v) { page = v; }
    public Integer getPageSize() { return pageSize; } public void setPageSize(Integer v) { pageSize = v; }
    public boolean isScopeRestricted() { return scopeRestricted; } @Override public void setScopeRestricted(boolean v) { scopeRestricted = v; }
    public List<Long> getScopedCommunityIds() { return scopedCommunityIds; } @Override public void setScopedCommunityIds(List<Long> v) { scopedCommunityIds = v == null ? List.of() : List.copyOf(v); }
}
