package com.smartproperty.platform.file.model;

public record FileObjectRow(
        Long fileObjectId,
        Long propertyUnitId,
        String bucketName,
        String objectKey,
        String originalName,
        String contentType,
        long sizeBytes,
        String sha256,
        String status
) {
}
