package com.smartproperty.platform.map.vo;

public record ManagedCommunityDistanceVO(Long communityId, String communityCode, String communityName,
                                         String address, long straightLineDistanceMeters) { }
