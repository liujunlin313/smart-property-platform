package com.smartproperty.platform.file.mapper;

import com.smartproperty.platform.file.model.FileObjectRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface FloorPlanVariantMapper {
    FileObjectRow findVariantFloorPlanForProperty(@Param("propertyUnitId") Long propertyUnitId);

    FileObjectRow findVariantByLayoutAndGrossArea(@Param("layoutTemplateId") Long layoutTemplateId,
                                                   @Param("grossArea") BigDecimal grossArea);

    int insertVariant(@Param("layoutTemplateId") Long layoutTemplateId,
                      @Param("grossArea") BigDecimal grossArea,
                      @Param("fileObjectId") Long fileObjectId);
}
