-- Stable reference data required by Database V1.
-- Demo-specific people, assets and transactions live outside Flyway in sample-data/.

INSERT INTO layout_template (
    layout_template_id, layout_code, layout_name, layout_type, standard_area,
    template_plan_object_key, description, status
) VALUES
    (1, 'LT-STUDIO-35', 'S1开间', 'STUDIO', 35.00, 'layout-templates/s1-studio-35.svg', '整套房开间户型模板', 'ACTIVE'),
    (2, 'LT-1B-50', 'A0一居室', 'ONE_BEDROOM', 50.00, 'layout-templates/a0-one-bedroom-50.svg', '整套房一居室模板', 'ACTIVE'),
    (3, 'LT-1B1L-58', 'A1一室一厅', 'ONE_BEDROOM_ONE_LIVING', 58.00, 'layout-templates/a1-one-bedroom-one-living-58.svg', '整套房一室一厅模板', 'ACTIVE'),
    (4, 'LT-2B-68', 'B0两居室', 'TWO_BEDROOM', 68.00, 'layout-templates/b0-two-bedroom-68.svg', '整套房两居室模板', 'ACTIVE'),
    (5, 'LT-2B1L-89', 'B1两室一厅', 'TWO_BEDROOM_ONE_LIVING', 89.00, 'layout-templates/b1-two-bedroom-one-living-89.svg', '整套房两室一厅模板', 'ACTIVE'),
    (6, 'LT-2B2L-108', 'B2两室两厅', 'TWO_BEDROOM_TWO_LIVING', 108.00, 'layout-templates/b2-two-bedroom-two-living-108.svg', '整套房两室两厅模板', 'ACTIVE'),
    (7, 'LT-3B1L-118', 'C1三室一厅', 'THREE_BEDROOM_ONE_LIVING', 118.00, 'layout-templates/c1-three-bedroom-one-living-118.svg', '整套房三室一厅模板', 'ACTIVE'),
    (8, 'LT-3B2L-138', 'C2三室两厅', 'THREE_BEDROOM_TWO_LIVING', 138.00, 'layout-templates/c2-three-bedroom-two-living-138.svg', '整套房三室两厅模板', 'ACTIVE'),
    (9, 'LT-4B2L-168', 'D1四室两厅', 'FOUR_BEDROOM_TWO_LIVING', 168.00, 'layout-templates/d1-four-bedroom-two-living-168.svg', '整套房四室两厅模板', 'ACTIVE'),
    (10, 'LT-OTHER', '其他户型', 'OTHER', NULL, NULL, '无法归入标准枚举时使用', 'ACTIVE');

INSERT INTO fee_type (
    fee_type_id, fee_code, fee_name, billing_method, unit_name, status
) VALUES
    (1, 'PROPERTY_AREA', '物业服务费', 'AREA', '平方米·月', 'ACTIVE'),
    (2, 'WATER', '生活用水费', 'METERED', '吨', 'ACTIVE'),
    (3, 'ELECTRICITY', '居民用电费', 'METERED', '千瓦时', 'ACTIVE'),
    (4, 'PARKING_SERVICE', '车位服务费', 'FIXED', '月', 'ACTIVE'),
    (5, 'WASTE', '生活垃圾处理费', 'FIXED', '月', 'ACTIVE');

INSERT INTO facility_type (
    facility_type_id, facility_code, facility_name, category,
    default_mobility_mode, status
) VALUES
    (1, 'BED_DOUBLE', '双人床', 'FURNITURE', 'FREE', 'ACTIVE'),
    (2, 'SOFA', '沙发', 'FURNITURE', 'FREE', 'ACTIVE'),
    (3, 'AIR_CONDITIONER', '空调', 'APPLIANCE', 'LIMITED', 'ACTIVE'),
    (4, 'REFRIGERATOR', '冰箱', 'APPLIANCE', 'FREE', 'ACTIVE'),
    (5, 'WATER_HEATER', '热水器', 'FIXTURE', 'FIXED', 'ACTIVE'),
    (6, 'WARDROBE', '衣柜', 'FURNITURE', 'FREE', 'ACTIVE'),
    (7, 'WASHING_MACHINE', '洗衣机', 'APPLIANCE', 'LIMITED', 'ACTIVE');
