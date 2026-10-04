package com.smartproperty.platform.asset.mapper;

import com.smartproperty.platform.asset.dto.response.LayoutTemplateResponse;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface LayoutTemplateMapper {
    List<LayoutTemplateResponse> findAll();
}
