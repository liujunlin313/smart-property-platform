package com.smartproperty.platform.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartproperty.platform.asset.mapper.AssetAuditMapper;
import org.springframework.stereotype.Service;

@Service
public class AssetAuditService {
    private final AssetAuditMapper mapper;
    private final ObjectMapper objectMapper;

    public AssetAuditService(AssetAuditMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public void record(String entityType, Long entityId, String operation,
                       Object before, Object after, String reason, Long accountId) {
        try {
            mapper.insert(entityType, entityId, operation,
                    before == null ? null : objectMapper.writeValueAsString(before),
                    after == null ? null : objectMapper.writeValueAsString(after),
                    AssetServiceSupport.trimToNull(reason), accountId);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize asset audit record", exception);
        }
    }
}
