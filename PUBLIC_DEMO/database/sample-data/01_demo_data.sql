-- Database V1 demonstration data.
-- Run once against a schema migrated through V2.
-- IDs are explicit so the reusable test queries have stable evidence.

SET NAMES utf8mb4;

START TRANSACTION;

INSERT INTO community (
    community_id, community_code, community_name, address_line,
    longitude, latitude, completion_year, status
) VALUES
    (1, 'ZGC_ACADEMY', '中关村学苑小区', '北京市海淀区中关村南大街12号', 116.3267200, 39.9572100, 2012, 'ACTIVE'),
    (2, 'HAIDIAN_SPRING', '海淀春景家园', '北京市海淀区学院路28号', 116.3528100, 39.9912600, 2018, 'ACTIVE');

INSERT INTO building (
    building_id, community_id, building_no, building_name, total_floors, status
) VALUES
    (1, 1, '1', '1号楼', 18, 'ACTIVE'),
    (2, 1, '2', '2号楼', 12, 'ACTIVE'),
    (3, 2, '1', '1号楼', 20, 'ACTIVE'),
    (4, 2, '2', '2号楼', 16, 'ACTIVE');

INSERT INTO building_unit (
    building_unit_id, building_id, unit_no, unit_name, status
) VALUES
    (1, 1, '1', '1单元', 'ACTIVE'),
    (2, 1, '2', '2单元', 'ACTIVE'),
    (3, 2, '1', '1单元', 'ACTIVE'),
    (4, 2, '2', '2单元', 'ACTIVE'),
    (5, 3, '1', '1单元', 'ACTIVE'),
    (6, 3, '2', '2单元', 'ACTIVE'),
    (7, 4, '1', '1单元', 'ACTIVE'),
    (8, 4, '2', '2单元', 'ACTIVE');

INSERT INTO property_unit (
    property_unit_id, building_unit_id, layout_template_id, property_no,
    floor_no, gross_area, usable_area, property_use, floor_plan_object_key, status
) VALUES
    (1, 1, 5, '101', 1, 89.50, 72.20, 'RESIDENTIAL', 'property-units/zgca-1-1-101-full-plan.svg', 'ACTIVE'),
    (2, 1, 3, '102', 1, 58.00, 46.50, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (3, 1, 7, '201', 2, 118.00, 96.00, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (4, 2, 5, '101', 1, 89.00, 71.80, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (5, 2, 6, '102', 1, 108.00, 88.00, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (6, 2, 8, '201', 2, 138.00, 112.00, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (7, 3, 2, '101', 1, 50.00, 39.50, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (8, 3, 5, '102', 1, 89.00, 71.50, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (9, 3, 7, '201', 2, 118.00, 95.50, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (10, 4, 1, '101', 1, 35.00, 27.00, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (11, 4, 4, '102', 1, 68.00, 54.00, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (12, 4, 8, '201', 2, 138.00, 111.80, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (13, 5, 3, '101', 1, 58.00, 46.30, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (14, 5, 6, '202', 2, 108.00, 87.20, 'RESIDENTIAL', 'property-units/hds-1-1-202-full-plan.svg', 'ACTIVE'),
    (15, 5, 8, '301', 3, 138.00, 112.10, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (16, 6, 5, '101', 1, 89.00, 71.30, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (17, 6, 7, '202', 2, 118.00, 95.20, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (18, 6, 9, '301', 3, 168.00, 137.00, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (19, 7, 2, '101', 1, 50.00, 39.80, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (20, 7, 5, '202', 2, 89.00, 71.40, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (21, 7, 8, '301', 3, 138.00, 111.60, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (22, 8, 1, '101', 1, 35.00, 27.20, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (23, 8, 6, '202', 2, 108.00, 87.00, 'RESIDENTIAL', NULL, 'ACTIVE'),
    (24, 8, 9, '301', 3, 168.00, 136.50, 'RESIDENTIAL', NULL, 'ACTIVE');

INSERT INTO person (
    person_id, person_code, full_name, gender, phone, id_number, status
) VALUES
    (1, 'P-ZHANGSAN', '张三', 'MALE', '13800000001', 'DEMO-ID-110001', 'ACTIVE'),
    (2, 'P-LISI', '李四', 'MALE', '13800000002', 'DEMO-ID-110002', 'ACTIVE'),
    (3, 'P-WANGFANG', '王芳', 'FEMALE', '13800000003', 'DEMO-ID-110003', 'ACTIVE'),
    (4, 'P-CHENCHEN', '陈晨', 'OTHER', '13800000004', 'DEMO-ID-110004', 'ACTIVE'),
    (5, 'P-ZHAOMIN', '赵敏', 'FEMALE', '13800000005', 'DEMO-ID-110005', 'ACTIVE'),
    (6, 'P-ZHOUQIANG', '周强', 'MALE', '13800000006', 'DEMO-ID-110006', 'ACTIVE');

INSERT INTO ownership (
    ownership_id, person_id, property_unit_id, ownership_share,
    start_date, end_date, certificate_no
) VALUES
    (1, 1, 1, 1.000000, '2020-01-01', NULL, 'CERT-ZGCA-1-1-101'),
    (2, 1, 13, 1.000000, '2023-03-01', NULL, 'CERT-HDS-1-1-101'),
    (3, 2, 14, 1.000000, '2021-06-01', NULL, 'CERT-HDS-1-1-202'),
    (5, 4, 2, 1.000000, '2022-08-01', NULL, 'CERT-ZGCA-1-1-102'),
    (6, 5, 3, 1.000000, '2020-10-01', NULL, 'CERT-ZGCA-1-1-201'),
    (7, 6, 15, 1.000000, '2024-01-15', NULL, 'CERT-HDS-1-1-301');

INSERT INTO residence (
    residence_id, person_id, property_unit_id, residence_role, start_date, end_date
) VALUES
    (1, 1, 14, 'TENANT', '2026-01-01', NULL),
    (2, 4, 14, 'TENANT', '2026-01-01', NULL),
    (3, 2, 1, 'PRIMARY_RESIDENT', '2025-05-01', NULL),
    (4, 3, 1, 'FAMILY_MEMBER', '2025-05-01', NULL),
    (5, 5, 3, 'PRIMARY_RESIDENT', '2020-10-01', NULL),
    (6, 6, 15, 'PRIMARY_RESIDENT', '2024-01-15', NULL);

INSERT INTO agency (
    agency_id, agency_code, agency_name, license_no, contact_phone, address_line, status
) VALUES
    (1, 'AG-HOME', '安居中介有限公司', 'LIC-AG-20260001', '010-60000001', '北京市海淀区中关村大街18号', 'ACTIVE'),
    (2, 'AG-LINK', '链家园住房服务有限公司', 'LIC-AG-20260002', '010-60000002', '北京市海淀区学院路30号', 'ACTIVE');

INSERT INTO authorization (
    authorization_id, person_id, property_unit_id, agency_id,
    start_date, end_date, status, can_view, can_publish,
    can_edit_listing, can_manage_contract
) VALUES
    (1, 1, 13, 1, '2026-07-01', '2026-12-31', 'ACTIVE', 1, 1, 1, 1),
    (2, 2, 14, 2, '2025-12-01', '2026-12-31', 'ACTIVE', 1, 1, 1, 1),
    (3, 4, 2, 2, '2026-08-01', '2026-12-31', 'ACTIVE', 1, 1, 0, 0);

INSERT INTO rental_listing (
    rental_listing_id, property_unit_id, listing_mode, publisher_person_id,
    authorization_id, listing_title, description, monthly_rent,
    deposit_amount, available_from, published_at, closed_at, status
) VALUES
    (1, 1, 'SELF', 1, NULL, '中关村学苑两室一厅整租', '完整房产出租，不拆分房间。', 6800.00, 6800.00, '2026-09-01', '2026-07-01 09:00:00', '2026-07-20 18:00:00', 'CLOSED'),
    (2, 13, 'AGENCY', NULL, 1, '海淀春景一室一厅出租', '业主授权安居中介发布。', 5200.00, 5200.00, '2026-09-01', '2026-08-01 10:00:00', NULL, 'PUBLISHED'),
    (3, 14, 'AGENCY', NULL, 2, '海淀春景两室两厅已成交', '单一产权人出租、多人共同承租示例。', 7600.00, 7600.00, '2026-01-01', '2025-12-05 10:00:00', '2025-12-20 16:00:00', 'RENTED'),
    (4, 2, 'SELF', 4, NULL, '中关村学苑一室一厅直租', '产权人直接发布。', 5600.00, 5600.00, '2026-10-01', '2026-08-10 14:00:00', NULL, 'PUBLISHED');

INSERT INTO lease_contract (
    lease_contract_id, rental_listing_id, contract_no, signed_at,
    lease_start_date, lease_end_date, monthly_rent, deposit_amount, status
) VALUES
    (1, 3, 'LEASE-2026-0001', '2025-12-20', '2026-01-01', '2026-12-31', 7600.00, 7600.00, 'ACTIVE');

INSERT INTO contract_participation (
    lease_contract_id, person_id, participant_role, is_primary, signed_at
) VALUES
    (1, 2, 'LANDLORD', 1, '2025-12-20 15:00:00'),
    (1, 1, 'TENANT', 1, '2025-12-20 15:10:00'),
    (1, 4, 'TENANT', 0, '2025-12-20 15:12:00');

INSERT INTO fee_standard (
    fee_standard_id, community_id, fee_type_id, standard_name,
    unit_price, effective_from, effective_to, status
) VALUES
    (1, 1, 1, '学苑小区2026物业费标准', 2.8000, '2026-01-01', NULL, 'ACTIVE'),
    (2, 1, 2, '学苑小区2026水费标准', 4.6000, '2026-01-01', NULL, 'ACTIVE'),
    (3, 1, 3, '学苑小区2026电费标准', 0.6000, '2026-01-01', NULL, 'ACTIVE'),
    (4, 1, 4, '学苑小区2026车位服务费', 180.0000, '2026-01-01', NULL, 'ACTIVE'),
    (5, 1, 5, '学苑小区2026垃圾处理费', 30.0000, '2026-01-01', NULL, 'ACTIVE'),
    (6, 2, 1, '春景家园2026物业费标准', 3.0000, '2026-01-01', NULL, 'ACTIVE'),
    (7, 2, 2, '春景家园2026水费标准', 4.8000, '2026-01-01', NULL, 'ACTIVE'),
    (8, 2, 3, '春景家园2026电费标准', 0.6200, '2026-01-01', NULL, 'ACTIVE'),
    (9, 2, 4, '春景家园2026车位服务费', 220.0000, '2026-01-01', NULL, 'ACTIVE'),
    (10, 2, 5, '春景家园2026垃圾处理费', 35.0000, '2026-01-01', NULL, 'ACTIVE');

INSERT INTO meter (
    meter_id, property_unit_id, fee_type_id, meter_no,
    installed_at, decommissioned_at, initial_reading, status
) VALUES
    (1, 1, 2, 'WATER-ZGCA-0001', '2020-01-01', NULL, 0.000, 'ACTIVE'),
    (2, 1, 3, 'ELEC-ZGCA-0001', '2020-01-01', NULL, 0.000, 'ACTIVE'),
    (3, 14, 2, 'WATER-HDS-0014', '2021-06-01', NULL, 0.000, 'ACTIVE'),
    (4, 14, 3, 'ELEC-HDS-0014', '2021-06-01', NULL, 0.000, 'ACTIVE');

INSERT INTO meter_reading (
    meter_reading_id, meter_id, reading_at, reading_value,
    reading_source, recorder_person_id, note
) VALUES
    (1, 1, '2026-07-01 08:00:00', 100.000, 'MANUAL', 6, '七月初水表读数'),
    (2, 1, '2026-08-01 08:00:00', 110.000, 'MANUAL', 6, '八月初水表读数'),
    (3, 2, '2026-07-01 08:05:00', 1000.000, 'IMPORT', NULL, '七月初电表读数'),
    (4, 2, '2026-08-01 08:05:00', 1200.000, 'IMPORT', NULL, '八月初电表读数'),
    (5, 3, '2026-07-01 09:00:00', 200.000, 'MANUAL', 6, '七月初水表读数'),
    (6, 3, '2026-08-01 09:00:00', 208.000, 'MANUAL', 6, '八月初水表读数'),
    (7, 4, '2026-07-01 09:05:00', 800.000, 'DEVICE', NULL, '七月初电表读数'),
    (8, 4, '2026-08-01 09:05:00', 950.000, 'DEVICE', NULL, '八月初电表读数');

INSERT INTO bill (
    bill_id, bill_no, property_unit_id, billing_year, billing_month,
    issued_at, due_date, status
) VALUES
    (1, 'BILL-2026-08-0001', 1, 2026, 8, '2026-08-02 09:00:00', '2026-08-31', 'PARTIALLY_PAID'),
    (2, 'BILL-2026-08-0014', 14, 2026, 8, '2026-08-02 09:05:00', '2026-08-31', 'PAID'),
    (3, 'BILL-2026-07-0013', 13, 2026, 7, '2026-07-02 09:00:00', '2026-07-31', 'OVERDUE');

INSERT INTO bill_item (
    bill_id, item_no, fee_standard_id, previous_reading_id,
    current_reading_id, quantity, unit_price, note
) VALUES
    (1, 1, 1, NULL, NULL, 89.500, 2.8000, '按建筑面积计费'),
    (1, 2, 2, 1, 2, 10.000, 4.6000, '用量=110-100'),
    (1, 3, 3, 3, 4, 200.000, 0.6000, '用量=1200-1000'),
    (2, 1, 6, NULL, NULL, 108.000, 3.0000, '按建筑面积计费'),
    (2, 2, 7, 5, 6, 8.000, 4.8000, '用量=208-200'),
    (2, 3, 8, 7, 8, 150.000, 0.6200, '用量=950-800'),
    (3, 1, 6, NULL, NULL, 58.000, 3.0000, '按建筑面积计费'),
    (3, 2, 10, NULL, NULL, 1.000, 35.0000, '月固定费用');

INSERT INTO payment (
    payment_id, payment_no, bill_id, payer_person_id, amount,
    paid_at, payment_method, external_transaction_no, status
) VALUES
    (1, 'PAY-2026-08-0001', 1, 2, 200.00, '2026-08-10 10:00:00', 'BANK_TRANSFER', 'BANK-DEMO-0001', 'SUCCESS'),
    (2, 'PAY-2026-08-0002', 2, 1, 455.40, '2026-08-08 15:30:00', 'WECHAT', 'WX-DEMO-0002', 'SUCCESS');

INSERT INTO parking_space (
    parking_space_id, community_id, space_no, location_description,
    space_type, area, status
) VALUES
    (1, 1, 'B1-001', '地下一层A区', 'STANDARD', 12.50, 'ACTIVE'),
    (2, 1, 'B1-002', '地下一层A区', 'EV', 13.00, 'ACTIVE'),
    (3, 1, 'G-001', '地面访客区旁', 'ACCESSIBLE', 14.00, 'ACTIVE'),
    (4, 2, 'B1-001', '地下一层B区', 'STANDARD', 12.50, 'ACTIVE'),
    (5, 2, 'B1-002', '地下一层B区', 'COMPACT', 10.50, 'ACTIVE'),
    (6, 2, 'B2-001', '地下二层充电区', 'EV', 13.00, 'MAINTENANCE');

INSERT INTO vehicle (
    vehicle_id, person_id, plate_number, vehicle_type, brand, model, color, status
) VALUES
    (1, 1, '京A·DEMO01', 'CAR', '比亚迪', '汉', '黑色', 'ACTIVE'),
    (2, 2, '京A·DEMO02', 'CAR', '大众', '高尔夫', '白色', 'ACTIVE'),
    (3, 5, '京A·DEMO03', 'CAR', '特斯拉', 'Model 3', '蓝色', 'ACTIVE');

INSERT INTO parking_right (
    parking_right_id, person_id, parking_space_id, right_type,
    start_date, end_date, status
) VALUES
    (1, 1, 1, 'OWNED', '2022-01-01', NULL, 'ACTIVE'),
    (2, 2, 2, 'LEASED', '2026-01-01', '2026-12-31', 'ACTIVE'),
    (3, 5, 4, 'OWNED', '2020-10-01', NULL, 'ACTIVE');

INSERT INTO facility_placement (
    facility_placement_id, property_unit_id, facility_type_id, placement_label,
    x_ratio, y_ratio, width_ratio, height_ratio, rotation_degrees,
    mobility_mode, anchor_x_ratio, anchor_y_ratio, movement_radius_ratio,
    installation_note
) VALUES
    (1, 1, 1, '主卧双人床', 0.120000, 0.180000, 0.220000, 0.180000, 0.000, 'FREE', NULL, NULL, NULL, '可在整套户型图允许区域内自由调整'),
    (2, 1, 3, '客厅挂机空调', 0.720000, 0.160000, 0.120000, 0.080000, 0.000, 'LIMITED', 0.780000, 0.200000, 0.030000, '受原墙孔、冷媒管和排水坡度限制，只能在锚点附近小范围移动'),
    (3, 1, 5, '卫生间热水器', 0.780000, 0.660000, 0.100000, 0.100000, 0.000, 'FIXED', 0.830000, 0.710000, 0.000000, '固定给排水和承重安装点'),
    (4, 14, 2, '客厅沙发', 0.280000, 0.580000, 0.260000, 0.140000, 0.000, 'FREE', NULL, NULL, NULL, '按整套房平面图定位'),
    (5, 14, 7, '阳台洗衣机', 0.820000, 0.760000, 0.090000, 0.100000, 0.000, 'LIMITED', 0.865000, 0.810000, 0.020000, '受进水、排水和插座位置限制');

COMMIT;
