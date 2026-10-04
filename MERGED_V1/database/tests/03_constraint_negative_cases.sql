-- Expected-failure cases. Run the mysql client with --force so every case executes.
-- All statements are wrapped in a transaction and rolled back in case a constraint regresses.

START TRANSACTION;

SELECT 'NEG-01 duplicate building number in same community (expect ERROR 1062)' AS test_case;
INSERT INTO building (community_id, building_no, building_name, total_floors, status)
VALUES (1, '1', '重复1号楼', 10, 'ACTIVE');

SELECT 'NEG-02 duplicate unit number in same building (expect ERROR 1062)' AS test_case;
INSERT INTO building_unit (building_id, unit_no, unit_name, status)
VALUES (1, '1', '重复1单元', 'ACTIVE');

SELECT 'NEG-03 duplicate property number in same building unit (expect ERROR 1062)' AS test_case;
INSERT INTO property_unit (
    building_unit_id, layout_template_id, property_no, floor_no,
    gross_area, usable_area, property_use, status
) VALUES (1, 5, '101', 1, 89.00, 72.00, 'RESIDENTIAL', 'ACTIVE');

SELECT 'NEG-04 building references missing community (expect ERROR 1452)' AS test_case;
INSERT INTO building (community_id, building_no, building_name, total_floors, status)
VALUES (999999, 'X', '非法父小区楼栋', 1, 'ACTIVE');

SELECT 'NEG-05 building unit references missing building (expect ERROR 1452)' AS test_case;
INSERT INTO building_unit (building_id, unit_no, unit_name, status)
VALUES (999999, 'X', '非法父楼栋单元', 'ACTIVE');

SELECT 'NEG-06 property references missing building unit (expect ERROR 1452)' AS test_case;
INSERT INTO property_unit (
    building_unit_id, layout_template_id, property_no, floor_no,
    gross_area, usable_area, property_use, status
) VALUES (999999, 5, 'X', 1, 89.00, 72.00, 'RESIDENTIAL', 'ACTIVE');

SELECT 'NEG-07 ownership share must be greater than zero (expect ERROR 3819)' AS test_case;
INSERT INTO ownership (
    person_id, property_unit_id, ownership_share, start_date, end_date
) VALUES (1, 2, 0.000000, '2026-01-01', NULL);

SELECT 'NEG-08 ownership end date cannot precede start date (expect ERROR 3819)' AS test_case;
INSERT INTO ownership (
    person_id, property_unit_id, ownership_share, start_date, end_date
) VALUES (1, 2, 0.500000, '2026-12-31', '2026-01-01');

SELECT 'NEG-09 residence end date cannot precede start date (expect ERROR 3819)' AS test_case;
INSERT INTO residence (
    person_id, property_unit_id, residence_role, start_date, end_date
) VALUES (1, 2, 'RESIDENT', '2026-12-31', '2026-01-01');

SELECT 'NEG-10 authorization references missing person (expect ERROR 1452)' AS test_case;
INSERT INTO authorization (
    person_id, property_unit_id, agency_id, start_date, end_date,
    status, can_view, can_publish, can_edit_listing, can_manage_contract
) VALUES (999999, 1, 1, '2026-01-01', '2026-12-31', 'ACTIVE', 1, 1, 1, 1);

SELECT 'NEG-11 authorization end date cannot precede start date (expect ERROR 3819)' AS test_case;
INSERT INTO authorization (
    person_id, property_unit_id, agency_id, start_date, end_date,
    status, can_view, can_publish, can_edit_listing, can_manage_contract
) VALUES (1, 1, 1, '2026-12-31', '2026-01-01', 'ACTIVE', 1, 1, 1, 1);

SELECT 'NEG-12 BillItem cannot exist without Bill (expect ERROR 1452)' AS test_case;
INSERT INTO bill_item (
    bill_id, item_no, fee_standard_id, previous_reading_id,
    current_reading_id, quantity, unit_price, note
) VALUES (999999, 1, 1, NULL, NULL, 1.000, 1.0000, '非法孤立账单项');

SELECT 'NEG-13 ParkingRight rejects missing Person (expect ERROR 1452)' AS test_case;
INSERT INTO parking_right (
    person_id, parking_space_id, right_type, start_date, end_date, status
) VALUES (999999, 1, 'OWNED', '2026-01-01', NULL, 'ACTIVE');

SELECT 'NEG-14 LIMITED facility requires anchor and radius (expect ERROR 3819)' AS test_case;
INSERT INTO facility_placement (
    property_unit_id, facility_type_id, placement_label,
    x_ratio, y_ratio, width_ratio, height_ratio, rotation_degrees,
    mobility_mode, anchor_x_ratio, anchor_y_ratio, movement_radius_ratio
) VALUES (
    1, 3, '非法空调位置', 0.1, 0.1, 0.1, 0.1, 0,
    'LIMITED', NULL, NULL, NULL
);

SELECT 'NEG-15 agency listing authorization must match property (expect ERROR 1452)' AS test_case;
INSERT INTO rental_listing (
    property_unit_id, listing_mode, publisher_person_id, authorization_id,
    listing_title, monthly_rent, deposit_amount, available_from,
    published_at, status
) VALUES (
    14, 'AGENCY', NULL, 1, '授权房产不匹配测试',
    5000.00, 5000.00, '2026-09-01', '2026-08-01 10:00:00', 'PUBLISHED'
);

SELECT 'NEG-16 one listing can form at most one contract (expect ERROR 1062)' AS test_case;
INSERT INTO lease_contract (
    rental_listing_id, contract_no, signed_at, lease_start_date,
    lease_end_date, monthly_rent, deposit_amount, status
) VALUES (
    3, 'LEASE-NEG-DUPLICATE', '2026-01-01', '2026-01-01',
    '2026-12-31', 7600.00, 7600.00, 'ACTIVE'
);

ROLLBACK;

