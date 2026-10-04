package com.smartproperty.platform.asset.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AssetAuditMapper {
    int insert(@Param("entityType") String entityType, @Param("entityId") Long entityId,
            @Param("operation") String operation, @Param("beforeData") String beforeData,
            @Param("afterData") String afterData, @Param("reason") String reason,
            @Param("accountId") Long accountId);
}
