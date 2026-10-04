-- Stage F: authorization, community data scope, real manually calibrated boundaries and property notifications.
-- Historical migrations remain immutable. This migration never alters existing property, resident, billing or map point data.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS app_permission (
    permission_id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    permission_code VARCHAR(64) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (permission_id),
    CONSTRAINT uk_app_permission_code UNIQUE (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台能力权限字典；角色决定能做什么';

CREATE TABLE IF NOT EXISTS role_permission (
    role_id SMALLINT UNSIGNED NOT NULL,
    permission_id SMALLINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES app_role (role_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES app_permission (permission_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色与能力权限关系';

CREATE TABLE IF NOT EXISTS account_community_scope (
    account_id BIGINT UNSIGNED NOT NULL,
    community_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    assigned_by_account_id BIGINT UNSIGNED NULL,
    assigned_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (account_id, community_id),
    KEY idx_account_community_scope_community (community_id),
    CONSTRAINT fk_account_community_scope_account FOREIGN KEY (account_id) REFERENCES app_account (account_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_account_community_scope_community FOREIGN KEY (community_id) REFERENCES community (community_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_account_community_scope_assigner FOREIGN KEY (assigned_by_account_id) REFERENCES app_account (account_id) ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT ck_account_community_scope_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物业工作人员按小区的数据作用域；SYSTEM_ADMIN 由全局角色获得全范围';

INSERT INTO app_role (role_code, role_name, description)
VALUES ('PROPERTY_STAFF', '物业工作人员', '在被授权小区范围内管理物业业务')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

INSERT INTO app_permission (permission_code, permission_name, description) VALUES
    ('PROPERTY_VIEW', '查看房产', '查看负责范围内的房产与资产信息'),
    ('RESIDENT_VIEW', '查看居住人员', '查看负责范围内的当前居住信息'),
    ('BILLING_VIEW', '查看物业收费', '查看负责范围内的账单、缴费和欠费信息'),
    ('MAP_VIEW', '查看空间地图', '查看负责范围内的小区与楼栋地图信息'),
    ('MAP_BOUNDARY_EDIT', '校准空间边界', '保存负责范围内小区或楼栋的人工校准边界'),
    ('NOTIFICATION_VIEW', '查看通知', '查看本人已发送的物业通知'),
    ('NOTIFICATION_SEND', '发送通知', '向负责范围内的人员发送物业通知')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), description = VALUES(description);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM app_role r CROSS JOIN app_permission p
WHERE r.role_code = 'SYSTEM_ADMIN';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM app_role r
JOIN app_permission p ON p.permission_code IN (
    'PROPERTY_VIEW', 'RESIDENT_VIEW', 'BILLING_VIEW', 'MAP_VIEW',
    'MAP_BOUNDARY_EDIT', 'NOTIFICATION_VIEW', 'NOTIFICATION_SEND'
)
WHERE r.role_code = 'PROPERTY_STAFF';

CREATE TABLE IF NOT EXISTS spatial_boundary (
    spatial_boundary_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    subject_type VARCHAR(16) NOT NULL COMMENT 'COMMUNITY/BUILDING',
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '对应业务对象主键；多态关联不覆盖既有 Point',
    boundary_geojson JSON NOT NULL COMMENT '真实人工校准 GeoJSON Polygon；坐标系单独记录',
    coordinate_system VARCHAR(16) NOT NULL DEFAULT 'GCJ02',
    boundary_source VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    verified_at DATETIME(3) NULL,
    created_by_account_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (spatial_boundary_id),
    CONSTRAINT uk_spatial_boundary_subject UNIQUE (subject_type, subject_id),
    KEY idx_spatial_boundary_subject_status (subject_type, status),
    CONSTRAINT fk_spatial_boundary_creator FOREIGN KEY (created_by_account_id) REFERENCES app_account (account_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_spatial_boundary_subject_type CHECK (subject_type IN ('COMMUNITY', 'BUILDING')),
    CONSTRAINT ck_spatial_boundary_coordinate_system CHECK (coordinate_system = 'GCJ02'),
    CONSTRAINT ck_spatial_boundary_source CHECK (boundary_source IN ('MANUAL', 'IMPORT')),
    CONSTRAINT ck_spatial_boundary_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='真实小区或楼栋边界；GCJ02 Polygon 与既有中心点并存';

-- The Pre-Stage1 schema already owns `notification` for account-oriented system notices.
-- Preserve it and extend it in place; recipient_account_id stays nullable for historic compatibility,
-- while Stage F expands recipients into the dedicated Person-based table below.
ALTER TABLE notification
    MODIFY COLUMN recipient_account_id BIGINT UNSIGNED NULL,
    MODIFY COLUMN title VARCHAR(160) NOT NULL,
    ADD COLUMN sender_account_id BIGINT UNSIGNED NULL AFTER notification_id,
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'SENT' AFTER category,
    ADD COLUMN sent_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) AFTER status,
    ADD KEY idx_notification_sender_sent (sender_account_id, sent_at),
    ADD CONSTRAINT fk_notification_sender FOREIGN KEY (sender_account_id) REFERENCES app_account (account_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    DROP CHECK ck_notification_category,
    ADD CONSTRAINT ck_notification_category_stage_f CHECK (category IN ('SYSTEM', 'ASSET', 'RESIDENCE', 'AGENCY', 'RENTAL', 'CONTRACT', 'PROPERTY_NOTICE', 'MAINTENANCE', 'BILLING', 'EMERGENCY', 'OTHER')),
    ADD CONSTRAINT ck_notification_status CHECK (status IN ('DRAFT', 'SENT', 'CANCELLED'));

CREATE TABLE IF NOT EXISTS notification_recipient (
    notification_recipient_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    notification_id BIGINT UNSIGNED NOT NULL,
    person_id BIGINT UNSIGNED NOT NULL,
    recipient_role VARCHAR(4) NOT NULL COMMENT 'TO/CC；同一人员 TO 优先',
    delivery_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    read_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (notification_recipient_id),
    CONSTRAINT uk_notification_recipient_once UNIQUE (notification_id, person_id),
    KEY idx_notification_recipient_person_read (person_id, read_at),
    CONSTRAINT fk_notification_recipient_notification FOREIGN KEY (notification_id) REFERENCES notification (notification_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_notification_recipient_person FOREIGN KEY (person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_notification_recipient_role CHECK (recipient_role IN ('TO', 'CC')),
    CONSTRAINT ck_notification_delivery_status CHECK (delivery_status IN ('PENDING', 'DELIVERED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知实际接收人；允许 Person 暂无登录账号';
