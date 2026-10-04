package com.smartproperty.platform.file.mapper;

import com.smartproperty.platform.file.model.FileObjectEntity;
import com.smartproperty.platform.file.model.FileObjectRow;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileObjectMapper {
    int insertFileObject(@Param("file") FileObjectEntity file);

    FileObjectRow findPropertyFloorPlan(@Param("propertyUnitId") Long propertyUnitId);

    FileObjectRow findActiveBySha256(@Param("bucketName") String bucketName,
                                     @Param("sha256") String sha256);

    int upsertPropertyFloorPlan(@Param("propertyUnitId") Long propertyUnitId,
                                @Param("fileObjectId") Long fileObjectId);

    int markFileDeleted(@Param("fileObjectId") Long fileObjectId);
}
