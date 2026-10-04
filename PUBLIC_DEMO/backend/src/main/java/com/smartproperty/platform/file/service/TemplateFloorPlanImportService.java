package com.smartproperty.platform.file.service;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.file.mapper.FileObjectMapper;
import com.smartproperty.platform.file.mapper.TemplateFloorPlanMapper;
import com.smartproperty.platform.file.model.FileObjectEntity;
import com.smartproperty.platform.file.model.FileObjectRow;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateFloorPlanImportService {
    private static final List<StandardPlan> STANDARD_PLANS = List.of(
            new StandardPlan("A1", "LT-1B1L-58", "layout_A1_58m2.png"),
            new StandardPlan("B0", "LT-2B-68", "layout_B0_68m2.png"),
            new StandardPlan("B1", "LT-2B1L-89", "layout_B1_89m2.png"),
            new StandardPlan("B2", "LT-2B2L-108", "layout_B2_108m2.png"));

    private final AuthorizationService authorizationService;
    private final TemplateFloorPlanMapper templateMapper;
    private final FileObjectMapper fileMapper;
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final StorageAvailabilityService storageAvailability;

    public TemplateFloorPlanImportService(AuthorizationService authorizationService,
                                          TemplateFloorPlanMapper templateMapper,
                                          FileObjectMapper fileMapper, MinioClient minioClient,
                                          StorageProperties storageProperties,
                                          StorageAvailabilityService storageAvailability) {
        this.authorizationService = authorizationService;
        this.templateMapper = templateMapper;
        this.fileMapper = fileMapper;
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
        this.storageAvailability = storageAvailability;
    }

    @Transactional
    public ImportResult importStandardPlans() {
        Long accountId = authorizationService.requireSystemAdmin().accountId();
        if (!storageAvailability.ensureReady()) {
            throw unavailable("户型图存储服务暂时不可用");
        }
        Map<String, String> results = new LinkedHashMap<>();
        for (StandardPlan plan : STANDARD_PLANS) {
            FileObjectRow existing = templateMapper.findTemplateFloorPlanByLayoutCode(plan.databaseLayoutCode());
            if (existing != null) {
                results.put(plan.label(), "EXISTS");
                continue;
            }
            ImportSource source = loadSource(plan);
            String objectKey = "floorplans/templates/" + plan.label() + "/" + plan.resourceName();
            putObject(objectKey, source.bytes());
            FileObjectEntity file = new FileObjectEntity();
            file.setBucketName(storageProperties.propertyBucket());
            file.setObjectKey(objectKey);
            file.setOriginalName(plan.resourceName());
            file.setContentType("image/png");
            file.setSizeBytes(source.bytes().length);
            file.setSha256(source.sha256());
            file.setUploadedByAccountId(accountId);
            fileMapper.insertFileObject(file);
            templateMapper.insertTemplateFloorPlan(source.layoutTemplateId(), file.getFileObjectId());
            results.put(plan.label(), "IMPORTED");
        }
        return new ImportResult(results);
    }

    private ImportSource loadSource(StandardPlan plan) {
        String resourcePath = "bootstrap/floorplans/" + plan.resourceName();
        try {
            byte[] bytes = new ClassPathResource(resourcePath).getContentAsByteArray();
            if (bytes.length == 0) throw new IllegalStateException("empty standard floor-plan resource");
            Long layoutTemplateId = templateMapper.findLayoutTemplateIdByCode(plan.databaseLayoutCode());
            if (layoutTemplateId == null) throw new IllegalStateException("layout template missing: " + plan.databaseLayoutCode());
            return new ImportSource(layoutTemplateId, bytes, sha256(bytes));
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取标准户型图资源 " + resourcePath, exception);
        }
    }

    private void putObject(String objectKey, byte[] bytes) {
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(storageProperties.propertyBucket()).object(objectKey)
                    .contentType("image/png").stream(new ByteArrayInputStream(bytes), bytes.length, -1).build());
        } catch (Exception exception) {
            storageAvailability.markUnavailable(exception);
            throw unavailable("标准户型图上传到存储服务失败");
        }
    }

    private String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "FILE_STORAGE_UNAVAILABLE", message);
    }

    private record StandardPlan(String label, String databaseLayoutCode, String resourceName) { }
    private record ImportSource(Long layoutTemplateId, byte[] bytes, String sha256) { }
    public record ImportResult(Map<String, String> layouts) { }
}
