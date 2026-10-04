-- Stage UI-R4: reusable standard floor-plan images by layout template and exact gross area.
-- V9 remains the default template fallback; property_floor_plan remains the per-property override.
CREATE TABLE layout_template_floor_plan_variant (
    layout_template_floor_plan_variant_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    layout_template_id BIGINT UNSIGNED NOT NULL,
    gross_area DECIMAL(10,2) NOT NULL,
    file_object_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (layout_template_floor_plan_variant_id),
    CONSTRAINT uk_layout_template_floor_plan_variant_area UNIQUE (layout_template_id, gross_area),
    CONSTRAINT fk_layout_template_floor_plan_variant_template FOREIGN KEY (layout_template_id)
        REFERENCES layout_template(layout_template_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_layout_template_floor_plan_variant_file FOREIGN KEY (file_object_id)
        REFERENCES file_object(file_object_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_layout_template_floor_plan_variant_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='户型模板按建筑面积复用的标准户型图；V9 保留模板默认回退图';
