package com.smartproperty.platform.notification.charge;

import com.smartproperty.platform.access.CommunityDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChargeNoticeMapper {
    boolean existsActiveFeeType(@Param("feeTypeId") Long feeTypeId);
    List<ChargeNoticeRow> selectDueRows(@Param("personIds") List<Long> personIds, @Param("scope") CommunityDataScope scope);
}
