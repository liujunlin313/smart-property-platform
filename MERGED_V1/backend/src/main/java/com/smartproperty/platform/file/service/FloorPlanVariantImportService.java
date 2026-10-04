package com.smartproperty.platform.file.service;

import java.util.Map;

public interface FloorPlanVariantImportService {
    ImportResult importStandardVariants();

    record ImportResult(Map<String, String> variants) {
    }
}
