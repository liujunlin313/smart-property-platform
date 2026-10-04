package com.smartproperty.platform.file.service;

import com.smartproperty.platform.asset.service.PropertyUnitService;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.file.mapper.FileObjectMapper;
import com.smartproperty.platform.file.mapper.FloorPlanVariantMapper;
import com.smartproperty.platform.file.mapper.TemplateFloorPlanMapper;
import com.smartproperty.platform.file.model.FileObjectEntity;
import com.smartproperty.platform.file.model.FileObjectRow;
import com.smartproperty.platform.infrastructure.storage.StorageAvailabilityService;
import com.smartproperty.platform.infrastructure.storage.StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class FloorPlanService {
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(FloorPlanService.class);

    private final FileObjectMapper mapper;
    private final TemplateFloorPlanMapper templateMapper;
    private final FloorPlanVariantMapper variantMapper;
    private final PropertyUnitService propertyUnitService;
    private final MinioClient minioClient;
    private final StorageProperties properties;
    private final StorageAvailabilityService storageAvailability;

    public FloorPlanService(FileObjectMapper mapper,
                            TemplateFloorPlanMapper templateMapper,
                            FloorPlanVariantMapper variantMapper,
                            PropertyUnitService propertyUnitService,
                            MinioClient minioClient,
                            StorageProperties properties,
                            StorageAvailabilityService storageAvailability) {
        this.mapper = mapper;
        this.templateMapper = templateMapper;
        this.variantMapper = variantMapper;
        this.propertyUnitService = propertyUnitService;
        this.minioClient = minioClient;
        this.properties = properties;
        this.storageAvailability = storageAvailability;
    }

    @Transactional
    public FileView upload(Long propertyUnitId, MultipartFile multipartFile, Long accountId) {
        propertyUnitService.get(propertyUnitId);
        requireStorage();
        byte[] bytes = readAndValidate(multipartFile);
        ImageType type = detectImageType(bytes);
        String objectKey = "property-units/" + propertyUnitId + "/floor-plan-"
                + UUID.randomUUID() + "." + type.extension();
        String originalName = safeOriginalName(multipartFile.getOriginalFilename(), type.extension());

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.propertyBucket())
                    .object(objectKey)
                    .contentType(type.contentType())
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .build());
        } catch (Exception exception) {
            storageAvailability.markUnavailable(exception);
            log.warn("MinIO rejected a floor-plan upload", exception);
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "FILE_STORAGE_UNAVAILABLE",
                    "户型图存储服务暂时不可用");
        }

        try {
            FileObjectRow previous = mapper.findPropertyFloorPlan(propertyUnitId);
            FileObjectEntity entity = new FileObjectEntity();
            entity.setBucketName(properties.propertyBucket());
            entity.setObjectKey(objectKey);
            entity.setOriginalName(originalName);
            entity.setContentType(type.contentType());
            entity.setSizeBytes(bytes.length);
            entity.setSha256(sha256(bytes));
            entity.setUploadedByAccountId(accountId);
            mapper.insertFileObject(entity);
            mapper.upsertPropertyFloorPlan(propertyUnitId, entity.getFileObjectId());
            if (previous != null) {
                mapper.markFileDeleted(previous.fileObjectId());
                deleteAfterCommit(previous.bucketName(), previous.objectKey());
            }
            return new FileView(String.valueOf(entity.getFileObjectId()), originalName, type.contentType(), bytes.length);
        } catch (RuntimeException exception) {
            removeQuietly(properties.propertyBucket(), objectKey);
            throw exception;
        }
    }

    public DownloadPayload download(Long propertyUnitId) {
        propertyUnitService.get(propertyUnitId);
        requireStorage();
        FileObjectRow file = mapper.findPropertyFloorPlan(propertyUnitId);
        if (file == null) {
            file = variantMapper.findVariantFloorPlanForProperty(propertyUnitId);
        }
        if (file == null) {
            file = templateMapper.findTemplateFloorPlanForProperty(propertyUnitId);
        }
        if (file == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "FLOOR_PLAN_NOT_FOUND", "该房产尚未上传户型图");
        }
        try {
            GetObjectResponse stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(file.bucketName())
                    .object(file.objectKey())
                    .build());
            return new DownloadPayload(stream, file.originalName(), file.contentType(), file.sizeBytes());
        } catch (Exception exception) {
            storageAvailability.markUnavailable(exception);
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "FILE_STORAGE_UNAVAILABLE",
                    "户型图暂时无法读取");
        }
    }

    private void requireStorage() {
        if (!storageAvailability.ensureReady()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "FILE_STORAGE_UNAVAILABLE",
                    "户型图存储服务暂时不可用，其他房产功能仍可使用");
        }
    }

    private byte[] readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidFile("请选择要上传的户型图片");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw invalidFile("户型图片不能超过 10MB");
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw invalidFile("无法读取上传的图片");
        }
    }

    private ImageType detectImageType(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return new ImageType("png", "image/png");
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return new ImageType("jpg", "image/jpeg");
        }
        if (bytes.length >= 12
                && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return new ImageType("webp", "image/webp");
        }
        throw invalidFile("只允许上传真实的 JPEG、PNG 或 WebP 图片，不接受 SVG");
    }

    private String safeOriginalName(String original, String extension) {
        if (original == null || original.isBlank()) {
            return "floor-plan." + extension;
        }
        String leaf = original.replace('\\', '/');
        leaf = leaf.substring(leaf.lastIndexOf('/') + 1).trim();
        if (leaf.length() > 200) {
            leaf = leaf.substring(leaf.length() - 200);
        }
        return leaf.isBlank() ? "floor-plan." + extension : leaf;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void deleteAfterCommit(String bucket, String objectKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                removeQuietly(bucket, objectKey);
            }
        });
    }

    private void removeQuietly(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception ignored) {
            // Orphan cleanup is intentionally deferred; a database record is never hidden by this failure.
        }
    }

    private BusinessException invalidFile(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_FLOOR_PLAN_FILE", message);
    }

    private record ImageType(String extension, String contentType) {
    }

    public record FileView(String fileObjectId, String originalName, String contentType, long sizeBytes) {
    }

    public record DownloadPayload(InputStream stream, String originalName, String contentType, long sizeBytes) {
    }
}
