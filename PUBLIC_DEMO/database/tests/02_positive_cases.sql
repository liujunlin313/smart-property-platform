-- Positive write cases run inside a transaction and are rolled back.

START TRANSACTION;

INSERT INTO community (
    community_code, community_name, address_line, longitude, latitude,
    completion_year, status
) VALUES (
    'TEST_POSITIVE', '正向测试小区', '北京市海淀区测试路1号',
    116.3000000, 39.9000000, 2026, 'ACTIVE'
);
SET @test_community_id = LAST_INSERT_ID();

INSERT INTO building (community_id, building_no, building_name, total_floors, status)
VALUES (@test_community_id, '1', '1号测试楼', 6, 'ACTIVE');
SET @test_building_id = LAST_INSERT_ID();

INSERT INTO building_unit (building_id, unit_no, unit_name, status)
VALUES (@test_building_id, '1', '1单元', 'ACTIVE');
SET @test_building_unit_id = LAST_INSERT_ID();

INSERT INTO property_unit (
    building_unit_id, layout_template_id, property_no, floor_no,
    gross_area, usable_area, property_use, status
) VALUES (
    @test_building_unit_id, 5, '102', 1, 89.00, 72.00,
    'RESIDENTIAL', 'ACTIVE'
);
SET @test_property_id = LAST_INSERT_ID();

INSERT INTO person (person_code, full_name, gender, phone, status)
VALUES ('P-TEST-OWNER', '测试产权人', 'UNKNOWN', '13900009999', 'ACTIVE');
SET @test_person_id = LAST_INSERT_ID();

INSERT INTO ownership (
    person_id, property_unit_id, ownership_share, start_date, end_date
) VALUES (
    @test_person_id, @test_property_id, 1.000000, '2026-01-01', NULL
);

INSERT INTO residence (
    person_id, property_unit_id, residence_role, start_date, end_date
) VALUES (
    @test_person_id, 14, 'TENANT', '2026-08-01', NULL
);

SELECT
    'POSITIVE_ASSET_CHAIN' AS test_case,
    c.community_name,
    b.building_name,
    bu.unit_name,
    pu.property_no,
    pu.gross_area
FROM property_unit pu
JOIN building_unit bu ON bu.building_unit_id = pu.building_unit_id
JOIN building b ON b.building_id = bu.building_id
JOIN community c ON c.community_id = b.community_id
WHERE pu.property_unit_id = @test_property_id;

SELECT
    'POSITIVE_OWNERSHIP_RESIDENCE_SEPARATION' AS test_case,
    p.full_name,
    own.property_unit_id AS owned_property_id,
    res.property_unit_id AS residence_property_id,
    CASE WHEN own.property_unit_id <> res.property_unit_id THEN 'PASS' ELSE 'FAIL' END AS result
FROM person p
JOIN ownership own ON own.person_id = p.person_id AND own.end_date IS NULL
JOIN residence res ON res.person_id = p.person_id AND res.end_date IS NULL
WHERE p.person_id = @test_person_id;

ROLLBACK;

