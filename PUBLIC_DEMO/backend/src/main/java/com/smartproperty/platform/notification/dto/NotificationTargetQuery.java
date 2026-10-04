package com.smartproperty.platform.notification.dto;

/** Optional browse/search parameters for the existing notification target endpoint. */
public class NotificationTargetQuery {
    private String keyword;
    private String targetType;
    private String parentType;
    private Long parentId;
    private Integer limit;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getParentType() { return parentType; }
    public void setParentType(String parentType) { this.parentType = parentType; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
