-- Stage UI-R3: reusable standard floor-plan images belong to layout templates.
-- property_floor_plan remains the per-property override and is intentionally unchanged.
CREATE TABLE layout_template_floor_plan (
    layout_template_id BIGINT UNSIGNED NOT NULL,
    file_object_id BIGINT UNSIGNED NOT NULL,
    imported_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (layout_template_id),
    CONSTRAINT uk_layout_template_floor_plan_file UNIQUE (file_object_id),
    CONSTRAINT fk_layout_template_floor_plan_template FOREIGN KEY (layout_template_id)
        REFERENCES layout_template(layout_template_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_layout_template_floor_plan_file FOREIGN KEY (file_object_id)
        REFERENCES file_object(file_object_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='户型模板的标准户型图；property_floor_plan 仍为单套房产专属覆盖图';
