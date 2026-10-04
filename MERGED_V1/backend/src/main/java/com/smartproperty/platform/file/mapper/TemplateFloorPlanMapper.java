package com.smartproperty.platform.file.mapper;

import com.smartproperty.platform.file.model.FileObjectRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TemplateFloorPlanMapper {
    FileObjectRow findTemplateFloorPlanForProperty(@Param("propertyUnitId") Long propertyUnitId);
    FileObjectRow findTemplateFloorPlanByLayoutCode(@Param("layoutCode") String layoutCode);
    Long findLayoutTemplateIdByCode(@Param("layoutCode") String layoutCode);
    int insertTemplateFloorPlan(@Param("layoutTemplateId") Long layoutTemplateId,
                                @Param("fileObjectId") Long fileObjectId);
}
