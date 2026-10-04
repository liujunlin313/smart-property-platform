package com.smartproperty.platform.property.mapper;

import com.smartproperty.platform.property.query.PropertyQuery;
import com.smartproperty.platform.property.vo.MeterVO;
import com.smartproperty.platform.property.vo.PropertyDetailVO;
import com.smartproperty.platform.property.vo.PropertyDetailBaseVO;
import com.smartproperty.platform.property.vo.PropertyListVO;
import com.smartproperty.platform.property.vo.PropertyPartyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PropertyMapper {
    long countProperties(@Param("query") PropertyQuery query);
    List<PropertyListVO> selectPropertyPage(@Param("query") PropertyQuery query, @Param("offset") long offset, @Param("limit") int limit);
    PropertyDetailBaseVO selectPropertyDetail(@Param("propertyUnitId") Long propertyUnitId);
    List<PropertyPartyVO> selectCurrentOwners(@Param("propertyUnitId") Long propertyUnitId);
    List<PropertyPartyVO> selectCurrentResidents(@Param("propertyUnitId") Long propertyUnitId);
    List<MeterVO> selectActiveMeters(@Param("propertyUnitId") Long propertyUnitId);
}
