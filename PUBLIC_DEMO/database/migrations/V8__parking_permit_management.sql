-- Stage UI-R2: append-only parking permit management.
-- Legacy vehicle is reused. Legacy parking_right remains untouched because it models a fixed parking_space,
-- while this module deliberately grants community access without allocating a parking space.
SET NAMES utf8mb4;

CREATE TABLE residence_parking_eligibility (
    residence_id BIGINT UNSIGNED NOT NULL,
    eligible TINYINT(1) NOT NULL,
    reason_code VARCHAR(32) NULL,
    remark VARCHAR(300) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (residence_id),
    KEY idx_residence_parking_eligibility_eligible (eligible),
    CONSTRAINT fk_residence_parking_eligibility_residence FOREIGN KEY (residence_id) REFERENCES residence(residence_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_residence_parking_eligibility_eligible CHECK (eligible IN (0, 1)),
    CONSTRAINT ck_residence_parking_eligibility_reason CHECK (reason_code IS NULL OR reason_code IN ('OWNER_NOT_GRANTED', 'NO_AVAILABLE_QUOTA', 'LEASE_EXCLUDES_PARKING', 'OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租赁入住记录的车证申请资格；产权资格不写入本表';

CREATE TABLE parking_permit_application (
    parking_permit_application_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    application_no VARCHAR(32) NOT NULL,
    holder_person_id BIGINT UNSIGNED NOT NULL,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    property_unit_id BIGINT UNSIGNED NOT NULL,
    residence_id BIGINT UNSIGNED NULL,
    eligibility_source VARCHAR(16) NOT NULL COMMENT 'OWNERSHIP/RESIDENCE',
    requested_months TINYINT UNSIGNED NOT NULL,
    requested_valid_from DATE NOT NULL,
    quoted_permit_sequence TINYINT UNSIGNED NOT NULL,
    quoted_monthly_rate DECIMAL(10,2) NOT NULL,
    quoted_total_amount DECIMAL(12,2) NOT NULL,
    application_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    rejection_reason VARCHAR(300) NULL,
    reviewed_by_account_id BIGINT UNSIGNED NULL,
    reviewed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (parking_permit_application_id),
    CONSTRAINT uk_parking_permit_application_no UNIQUE (application_no),
    KEY idx_parking_permit_application_property_status_created (property_unit_id, application_status, created_at),
    KEY idx_parking_permit_application_holder_created (holder_person_id, created_at),
    KEY idx_parking_permit_application_vehicle (vehicle_id),
    CONSTRAINT fk_parking_permit_application_holder FOREIGN KEY (holder_person_id) REFERENCES person(person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_parking_permit_application_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_parking_permit_application_property FOREIGN KEY (property_unit_id) REFERENCES property_unit(property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_parking_permit_application_residence FOREIGN KEY (residence_id) REFERENCES residence(residence_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_parking_permit_application_reviewer FOREIGN KEY (reviewed_by_account_id) REFERENCES app_account(account_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_parking_permit_application_source CHECK (eligibility_source IN ('OWNERSHIP', 'RESIDENCE')),
    CONSTRAINT ck_parking_permit_application_months CHECK (requested_months BETWEEN 1 AND 12),
    CONSTRAINT ck_parking_permit_application_sequence CHECK (quoted_permit_sequence IN (1, 2)),
    CONSTRAINT ck_parking_permit_application_rate CHECK (quoted_monthly_rate IN (200.00, 1200.00)),
    CONSTRAINT ck_parking_permit_application_total CHECK (quoted_total_amount = quoted_monthly_rate * requested_months),
    CONSTRAINT ck_parking_permit_application_status CHECK (application_status IN ('PENDING', 'APPROVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='居民提交、物业审核的车证申请记录';

CREATE TABLE parking_permit (
    parking_permit_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    permit_no VARCHAR(32) NOT NULL,
    parking_permit_application_id BIGINT UNSIGNED NOT NULL,
    holder_person_id BIGINT UNSIGNED NOT NULL,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    property_unit_id BIGINT UNSIGNED NOT NULL,
    residence_id BIGINT UNSIGNED NULL,
    eligibility_source VARCHAR(16) NOT NULL,
    permit_sequence TINYINT UNSIGNED NOT NULL,
    monthly_rate DECIMAL(10,2) NOT NULL,
    charged_months TINYINT UNSIGNED NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    permit_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REVOKED；过期由有效期推导',
    issued_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (parking_permit_id),
    CONSTRAINT uk_parking_permit_no UNIQUE (permit_no),
    CONSTRAINT uk_parking_permit_application UNIQUE (parking_permit_application_id),
    KEY idx_parking_permit_property_validity (property_unit_id, valid_to),
    KEY idx_parking_permit_holder_validity (holder_person_id, valid_to),
    KEY idx_parking_permit_vehicle (vehicle_id),
    CONSTRAINT fk_parking_permit_application FOREIGN KEY (parking_permit_application_id) REFERENCES parking_permit_application(parking_permit_application_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_parking_permit_holder FOREIGN KEY (holder_person_id) REFERENCES person(person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_parking_permit_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_parking_permit_property FOREIGN KEY (property_unit_id) REFERENCES property_unit(property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_parking_permit_residence FOREIGN KEY (residence_id) REFERENCES residence(residence_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_parking_permit_source CHECK (eligibility_source IN ('OWNERSHIP', 'RESIDENCE')),
    CONSTRAINT ck_parking_permit_sequence CHECK (permit_sequence IN (1, 2)),
    CONSTRAINT ck_parking_permit_rate CHECK (monthly_rate IN (200.00, 1200.00)),
    CONSTRAINT ck_parking_permit_months CHECK (charged_months BETWEEN 1 AND 12),
    CONSTRAINT ck_parking_permit_total CHECK (total_amount = monthly_rate * charged_months),
    CONSTRAINT ck_parking_permit_dates CHECK (valid_to >= valid_from),
    CONSTRAINT ck_parking_permit_status CHECK (permit_status IN ('ACTIVE', 'REVOKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已签发车证；不绑定任何固定停车位';

INSERT INTO app_permission(permission_code, permission_name, description) VALUES
    ('PARKING_PERMIT_VIEW', '查看车证管理', '查看负责范围内的车证申请和有效车证'),
    ('PARKING_PERMIT_MANAGE', '审核车证申请', '审核负责范围内的车证申请并签发车证')
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name), description=VALUES(description);

INSERT IGNORE INTO role_permission(role_id, permission_id)
SELECT r.role_id, p.permission_id FROM app_role r CROSS JOIN app_permission p
WHERE r.role_code='SYSTEM_ADMIN' AND p.permission_code IN ('PARKING_PERMIT_VIEW','PARKING_PERMIT_MANAGE');

INSERT IGNORE INTO role_permission(role_id, permission_id)
SELECT r.role_id, p.permission_id FROM app_role r JOIN app_permission p
  ON p.permission_code IN ('PARKING_PERMIT_VIEW','PARKING_PERMIT_MANAGE')
WHERE r.role_code='PROPERTY_STAFF';

-- Sparse, deterministic demonstration data. Existing person/ownership/residence/property rows are only referenced.
INSERT IGNORE INTO residence_parking_eligibility(residence_id, eligible, reason_code, remark)
SELECT r.residence_id, 1, NULL, '演示：租赁入住时已授予停车权益'
FROM residence r
WHERE r.end_date IS NULL
  AND NOT EXISTS (SELECT 1 FROM ownership o WHERE o.person_id=r.person_id AND o.property_unit_id=r.property_unit_id AND o.end_date IS NULL)
ORDER BY r.residence_id LIMIT 1;

INSERT IGNORE INTO residence_parking_eligibility(residence_id, eligible, reason_code, remark)
SELECT r.residence_id, 0, 'LEASE_EXCLUDES_PARKING', '演示：当前租赁约定不包含停车权益'
FROM residence r
WHERE r.end_date IS NULL
  AND NOT EXISTS (SELECT 1 FROM ownership o WHERE o.person_id=r.person_id AND o.property_unit_id=r.property_unit_id AND o.end_date IS NULL)
  AND NOT EXISTS (SELECT 1 FROM residence_parking_eligibility e WHERE e.residence_id=r.residence_id)
ORDER BY r.residence_id LIMIT 1;

INSERT IGNORE INTO vehicle(person_id, plate_number, vehicle_type, brand, color, status)
SELECT o.person_id, CONCAT('京A', LPAD(o.person_id, 5, '0')), 'CAR', '演示车辆', '白色', 'ACTIVE'
FROM ownership o WHERE o.end_date IS NULL ORDER BY o.person_id, o.property_unit_id LIMIT 1;
INSERT IGNORE INTO vehicle(person_id, plate_number, vehicle_type, brand, color, status)
SELECT o.person_id, CONCAT('京B', LPAD(o.person_id, 5, '0')), 'CAR', '演示车辆', '银色', 'ACTIVE'
FROM ownership o WHERE o.end_date IS NULL ORDER BY o.person_id, o.property_unit_id LIMIT 1;
INSERT IGNORE INTO vehicle(person_id, plate_number, vehicle_type, brand, color, status)
SELECT o.person_id, CONCAT('京C', LPAD(o.person_id, 5, '0')), 'CAR', '演示车辆', '黑色', 'ACTIVE'
FROM ownership o WHERE o.end_date IS NULL ORDER BY o.person_id, o.property_unit_id LIMIT 1;
INSERT IGNORE INTO vehicle(person_id, plate_number, vehicle_type, brand, color, status)
SELECT r.person_id, CONCAT('京T', LPAD(r.person_id, 5, '0')), 'CAR', '演示车辆', '灰色', 'ACTIVE'
FROM residence_parking_eligibility e JOIN residence r ON r.residence_id=e.residence_id WHERE e.eligible=1 LIMIT 1;
INSERT IGNORE INTO vehicle(person_id, plate_number, vehicle_type, brand, color, status)
SELECT r.person_id, CONCAT('京R', LPAD(r.person_id, 5, '0')), 'CAR', '演示车辆', '蓝色', 'ACTIVE'
FROM residence_parking_eligibility e JOIN residence r ON r.residence_id=e.residence_id WHERE e.eligible=0 LIMIT 1;

INSERT INTO parking_permit_application(application_no, holder_person_id, vehicle_id, property_unit_id, residence_id, eligibility_source, requested_months, requested_valid_from, quoted_permit_sequence, quoted_monthly_rate, quoted_total_amount, application_status, reviewed_at)
SELECT CONCAT('PPA-OWN-', LPAD(v.vehicle_id, 8, '0')), v.person_id, v.vehicle_id, target.property_unit_id, NULL, 'OWNERSHIP', 12,
       CASE WHEN v.plate_number=CONCAT('京C', LPAD(target.person_id, 5, '0')) THEN DATE_SUB(CURRENT_DATE, INTERVAL 13 MONTH) ELSE CURRENT_DATE END,
       CASE WHEN v.plate_number=CONCAT('京B', LPAD(target.person_id, 5, '0')) THEN 2 ELSE 1 END,
       CASE WHEN v.plate_number=CONCAT('京B', LPAD(target.person_id, 5, '0')) THEN 1200.00 ELSE 200.00 END,
       CASE WHEN v.plate_number=CONCAT('京B', LPAD(target.person_id, 5, '0')) THEN 14400.00 ELSE 2400.00 END,
       'APPROVED', CURRENT_TIMESTAMP(3)
FROM vehicle v JOIN (SELECT person_id, property_unit_id FROM ownership WHERE end_date IS NULL ORDER BY person_id, property_unit_id LIMIT 1) target ON target.person_id=v.person_id
WHERE v.plate_number IN (CONCAT('京A', LPAD(target.person_id, 5, '0')), CONCAT('京B', LPAD(target.person_id, 5, '0')), CONCAT('京C', LPAD(target.person_id, 5, '0')));

INSERT INTO parking_permit(parking_permit_id, permit_no, parking_permit_application_id, holder_person_id, vehicle_id, property_unit_id, residence_id, eligibility_source, permit_sequence, monthly_rate, charged_months, total_amount, valid_from, valid_to, permit_status)
SELECT NULL, CONCAT('PP-', LPAD(a.parking_permit_application_id, 8, '0')), a.parking_permit_application_id, a.holder_person_id, a.vehicle_id, a.property_unit_id, NULL, a.eligibility_source, a.quoted_permit_sequence, a.quoted_monthly_rate, a.requested_months, a.quoted_total_amount, a.requested_valid_from, DATE_SUB(DATE_ADD(a.requested_valid_from, INTERVAL a.requested_months MONTH), INTERVAL 1 DAY), 'ACTIVE'
FROM parking_permit_application a WHERE a.application_no LIKE 'PPA-OWN-%';

INSERT INTO parking_permit_application(application_no, holder_person_id, vehicle_id, property_unit_id, residence_id, eligibility_source, requested_months, requested_valid_from, quoted_permit_sequence, quoted_monthly_rate, quoted_total_amount, application_status)
SELECT CONCAT('PPA-RES-', LPAD(v.vehicle_id, 8, '0')), r.person_id, v.vehicle_id, r.property_unit_id, r.residence_id, 'RESIDENCE', 6, CURRENT_DATE, 1, 200.00, 1200.00, 'PENDING'
FROM residence_parking_eligibility e JOIN residence r ON r.residence_id=e.residence_id JOIN vehicle v ON v.person_id=r.person_id AND v.plate_number=CONCAT('京T', LPAD(r.person_id, 5, '0'))
WHERE e.eligible=1;

INSERT INTO parking_permit_application(application_no, holder_person_id, vehicle_id, property_unit_id, residence_id, eligibility_source, requested_months, requested_valid_from, quoted_permit_sequence, quoted_monthly_rate, quoted_total_amount, application_status, rejection_reason, reviewed_at)
SELECT CONCAT('PPA-REJ-', LPAD(v.vehicle_id, 8, '0')), r.person_id, v.vehicle_id, r.property_unit_id, r.residence_id, 'RESIDENCE', 6, CURRENT_DATE, 1, 200.00, 1200.00, 'REJECTED', '租赁约定不包含停车权益', CURRENT_TIMESTAMP(3)
FROM residence_parking_eligibility e JOIN residence r ON r.residence_id=e.residence_id JOIN vehicle v ON v.person_id=r.person_id AND v.plate_number=CONCAT('京R', LPAD(r.person_id, 5, '0'))
WHERE e.eligible=0;
