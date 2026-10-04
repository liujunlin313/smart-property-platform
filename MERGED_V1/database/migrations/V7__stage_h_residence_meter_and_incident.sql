-- Stage H: append-only lifecycle capabilities. No historic data is changed or removed.
SET NAMES utf8mb4;

ALTER TABLE residence
    ADD COLUMN expected_end_date DATE NULL AFTER end_date,
    ADD CONSTRAINT ck_residence_expected_end_date
        CHECK (expected_end_date IS NULL OR expected_end_date >= start_date);

CREATE TABLE resident_incident (
    resident_incident_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    reporter_person_id BIGINT UNSIGNED NOT NULL,
    property_unit_id BIGINT UNSIGNED NOT NULL,
    category VARCHAR(32) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    handled_by_account_id BIGINT UNSIGNED NULL,
    resolved_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (resident_incident_id),
    KEY idx_resident_incident_property_status_created (property_unit_id, status, created_at),
    KEY idx_resident_incident_reporter_created (reporter_person_id, created_at),
    CONSTRAINT fk_resident_incident_reporter FOREIGN KEY (reporter_person_id) REFERENCES person(person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_resident_incident_property FOREIGN KEY (property_unit_id) REFERENCES property_unit(property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_resident_incident_handler FOREIGN KEY (handled_by_account_id) REFERENCES app_account(account_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_resident_incident_category CHECK (category IN ('LEAK', 'ELECTRICITY', 'GAS', 'ELEVATOR', 'PUBLIC_FACILITY', 'NOISE', 'SECURITY', 'OTHER')),
    CONSTRAINT ck_resident_incident_status CHECK (status IN ('OPEN', 'PROCESSING', 'RESOLVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='居民主动异常/报修上报；不与表计自动告警混用';

INSERT INTO app_permission(permission_code, permission_name, description) VALUES
    ('RESIDENT_MANAGE', '办理入住退租', '在负责范围内新增人员并办理入住或退租'),
    ('INCIDENT_VIEW', '查看居民异常', '查看负责范围内居民主动上报'),
    ('INCIDENT_MANAGE', '处理居民异常', '更新负责范围内居民上报的处理状态')
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name), description=VALUES(description);

INSERT IGNORE INTO role_permission(role_id, permission_id)
SELECT r.role_id, p.permission_id FROM app_role r CROSS JOIN app_permission p WHERE r.role_code='SYSTEM_ADMIN';

INSERT IGNORE INTO role_permission(role_id, permission_id)
SELECT r.role_id, p.permission_id FROM app_role r JOIN app_permission p
  ON p.permission_code IN ('RESIDENT_MANAGE','INCIDENT_VIEW','INCIDENT_MANAGE')
WHERE r.role_code='PROPERTY_STAFF';
