package com.smartproperty.platform.file.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.file.mapper.FileObjectMapper;
import com.smartproperty.platform.file.mapper.FloorPlanVariantMapper;
import com.smartproperty.platform.file.mapper.TemplateFloorPlanMapper;
import com.smartproperty.platform.file.model.FileObjectEntity;
import com.smartproperty.platform.file.model.FileObjectRow;
import com.smartproperty.platform.file.service.FloorPlanVariantImportService;
import com.smartproperty.platform.infrastructure.storage.StorageAvailabilityService;
import com.smartproperty.platform.infrastructure.storage.StorageProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FloorPlanVariantImportServiceImpl implements FloorPlanVariantImportService {
    private static final List<VariantPlan> VARIANT_PLANS = List.of(
            new VariantPlan("LT-1B1L-58", "56.50", "A1_56_50.webp"),
            new VariantPlan("LT-1B1L-58", "58.00", "A1_58_00.png"),
            new VariantPlan("LT-1B1L-58", "59.50", "A1_59_50.webp"),
            new VariantPlan("LT-2B-68", "66.50", "B0_66_50.webp"),
            new VariantPlan("LT-2B-68", "68.00", "B0_68_00.png"),
            new VariantPlan("LT-2B-68", "69.50", "B0_69_50.jpg"),
            new VariantPlan("LT-2B1L-89", "87.50", "B1_87_50.webp"),
            new VariantPlan("LT-2B1L-89", "89.00", "B1_89_00.png"),
            new VariantPlan("LT-2B1L-89", "90.50", "B1_90_50.webp"),
            new VariantPlan("LT-2B2L-108", "106.50", "B2_106_50.jpg"),
            new VariantPlan("LT-2B2L-108", "108.00", "B2_108_00.png"),
            new VariantPlan("LT-2B2L-108", "109.50", "B2_109_50.webp"));

    private final AuthorizationService authorizationService;
    private final FloorPlanVariantMapper variantMapper;
    private final TemplateFloorPlanMapper templateMapper;
    private final FileObjectMapper fileMapper;
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final StorageAvailabilityService storageAvailability;

    public FloorPlanVariantImportServiceImpl(AuthorizationService authorizationService,
                                             FloorPlanVariantMapper variantMapper,
                                             TemplateFloorPlanMapper templateMapper,
                                             FileObjectMapper fileMapper,
                                             MinioClient minioClient,
                                             StorageProperties storageProperties,
                                             StorageAvailabilityService storageAvailability) {
        this.authorizationService = authorizationService;
        this.variantMapper = variantMapper;
        this.templateMapper = templateMapper;
        this.fileMapper = fileMapper;
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
        this.storageAvailability = storageAvailability;
    }

    @Override
    @Transactional
    public ImportResult importStandardVariants() {
        Long accountId = authorizationService.requireSystemAdmin().accountId();
        if (!storageAvailability.ensureReady()) {
            throw unavailable("户型图存储服务暂时不可用");
        }
        Map<String, String> results = new LinkedHashMap<>();
        for (VariantPlan plan : VARIANT_PLANS) {
            BigDecimal grossArea = new BigDecimal(plan.grossArea()).setScale(2);
            Long layoutTemplateId = templateMapper.findLayoutTemplateIdByCode(plan.layoutCode());
            if (layoutTemplateId == null) {
                throw new IllegalStateException("layout template missing: " + plan.layoutCode());
            }
            if (variantMapper.findVariantByLayoutAndGrossArea(layoutTemplateId, grossArea) != null) {
                results.put(plan.key(), "EXISTS");
                continue;
            }
            ImportSource source = loadSource(plan.resourceName());
            FileObjectRow reusable = fileMapper.findActiveBySha256(storageProperties.propertyBucket(), source.sha256());
            Long fileObjectId = reusable == null
                    ? createFileObject(plan, source, accountId)
                    : reusable.fileObjectId();
            variantMapper.insertVariant(layoutTemplateId, grossArea, fileObjectId);
            results.put(plan.key(), reusable == null ? "IMPORTED" : "REUSED");
        }
        return new ImportResult(results);
    }

    private Long createFileObject(VariantPlan plan, ImportSource source, Long accountId) {
        String objectKey = "floorplans/layout-variants/" + plan.layoutCode() + "/" + plan.resourceName();
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(storageProperties.propertyBucket()).object(objectKey)
                    .contentType(source.contentType()).stream(new ByteArrayInputStream(source.bytes()), source.bytes().length, -1).build());
        } catch (Exception exception) {
            storageAvailability.markUnavailable(exception);
            throw unavailable("标准户型图上传到存储服务失败");
        }
        FileObjectEntity file = new FileObjectEntity();
        file.setBucketName(storageProperties.propertyBucket());
        file.setObjectKey(objectKey);
        file.setOriginalName(plan.resourceName());
        file.setContentType(source.contentType());
        file.setSizeBytes(source.bytes().length);
        file.setSha256(source.sha256());
        file.setUploadedByAccountId(accountId);
        fileMapper.insertFileObject(file);
        return file.getFileObjectId();
    }

    private ImportSource loadSource(String resourceName) {
        String resourcePath = "bootstrap/floorplans/variants/" + resourceName;
        try {
            byte[] bytes = new ClassPathResource(resourcePath).getContentAsByteArray();
            if (bytes.length == 0) throw new IllegalStateException("empty standard floor-plan resource");
            return new ImportSource(bytes, sha256(bytes), contentType(resourceName));
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取面积户型图资源 " + resourcePath, exception);
        }
    }

    private String contentType(String resourceName) {
        String lower = resourceName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        throw new IllegalArgumentException("unsupported floor-plan resource type");
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "FILE_STORAGE_UNAVAILABLE", message);
    }

    private record VariantPlan(String layoutCode, String grossArea, String resourceName) {
        String key() {
            return layoutCode + "@" + grossArea;
        }
    }

    private record ImportSource(byte[] bytes, String sha256, String contentType) {
    }
}
