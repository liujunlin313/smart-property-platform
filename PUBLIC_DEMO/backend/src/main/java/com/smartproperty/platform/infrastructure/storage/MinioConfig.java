package com.smartproperty.platform.infrastructure.storage;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Bean
    MinioClient minioClient(StorageProperties properties) {
        if (isBlank(properties.accessKey()) || isBlank(properties.secretKey())) {
            throw new IllegalStateException("MINIO_ACCESS_KEY and MINIO_SECRET_KEY are required");
        }
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
