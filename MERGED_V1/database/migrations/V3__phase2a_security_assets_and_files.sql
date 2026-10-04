-- Phase 2A: authentication, asset concurrency, draft properties, building coordinates and file metadata.
-- V1/V2 remain immutable; all changes are forward-only.

SET NAMES utf8mb4;

CREATE TABLE app_account (
    account_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '登录账号主键',
    person_id BIGINT UNSIGNED NULL COMMENT '可选关联现实人员；游客和系统账号可以为空',
    username VARCHAR(64) NOT NULL COMMENT '登录名；当前排序规则下不区分大小写',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码散列，永不保存明文',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/LOCKED/INACTIVE',
    failed_login_attempts SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    locked_until DATETIME(3) NULL,
    token_version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '强制旧令牌失效的账号版本',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    last_login_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (account_id),
    CONSTRAINT uk_app_account_username UNIQUE (username),
    CONSTRAINT uk_app_account_person UNIQUE (person_id),
    CONSTRAINT fk_app_account_person FOREIGN KEY (person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_app_account_status CHECK (status IN ('ACTIVE', 'LOCKED', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统登录账号；与现实人员和业务身份分离';

CREATE TABLE app_role (
    role_id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(32) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (role_id),
    CONSTRAINT uk_app_role_code UNIQUE (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色字典';

INSERT INTO app_role (role_code, role_name, description) VALUES
    ('SYSTEM_ADMIN', '系统管理员', '管理平台和全部小区资产'),
    ('PERSON_USER', '个人用户', '通过 Person 与产权、居住等业务关系获得对象权限'),
    ('AGENCY_ADMIN', '中介公司管理员', '在绑定的中介公司范围内管理授权、房源和合同'),
    ('DEMO_VIEWER', '演示游客', '只读访问演示数据');

CREATE TABLE account_global_role (
    account_id BIGINT UNSIGNED NOT NULL,
    role_id SMALLINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (account_id, role_id),
    CONSTRAINT fk_account_global_role_account FOREIGN KEY (account_id) REFERENCES app_account (account_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_account_global_role_role FOREIGN KEY (role_id) REFERENCES app_role (role_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号全局角色';

CREATE TABLE account_agency_role (
    account_id BIGINT UNSIGNED NOT NULL,
    agency_id BIGINT UNSIGNED NOT NULL,
    role_id SMALLINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (account_id, agency_id, role_id),
    CONSTRAINT fk_account_agency_role_account FOREIGN KEY (account_id) REFERENCES app_account (account_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_account_agency_role_agency FOREIGN KEY (agency_id) REFERENCES agency (agency_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_account_agency_role_role FOREIGN KEY (role_id) REFERENCES app_role (role_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号在指定中介公司的作用域角色';

CREATE TABLE refresh_token_session (
    refresh_token_session_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '随机 Refresh Token 的 SHA-256 十六进制散列',
    expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    replaced_by_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_used_at DATETIME(3) NULL,
    PRIMARY KEY (refresh_token_session_id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    KEY idx_refresh_token_account_expiry (account_id, expires_at),
    CONSTRAINT fk_refresh_token_account FOREIGN KEY (account_id) REFERENCES app_account (account_id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT ck_refresh_token_expiry CHECK (expires_at > created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='可轮换、可撤销的 Refresh Token 会话';

ALTER TABLE community
    ADD COLUMN location_source VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/AMAP' AFTER latitude,
    ADD COLUMN coordinate_system VARCHAR(16) NOT NULL DEFAULT 'GCJ02' COMMENT '高德接入统一使用 GCJ02' AFTER location_source,
    ADD COLUMN amap_poi_id VARCHAR(64) NULL AFTER coordinate_system,
    ADD COLUMN location_verified_at DATETIME(3) NULL AFTER amap_poi_id,
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER status,
    ADD CONSTRAINT ck_community_location_source CHECK (location_source IN ('MANUAL', 'AMAP')),
    ADD CONSTRAINT ck_community_coordinate_system CHECK (coordinate_system = 'GCJ02');

ALTER TABLE building
    ADD COLUMN longitude DECIMAL(10, 7) NULL COMMENT '楼栋 GCJ02 经度' AFTER total_floors,
    ADD COLUMN latitude DECIMAL(10, 7) NULL COMMENT '楼栋 GCJ02 纬度' AFTER longitude,
    ADD COLUMN location_source VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/AMAP' AFTER latitude,
    ADD COLUMN amap_poi_id VARCHAR(64) NULL AFTER location_source,
    ADD COLUMN location_verified_at DATETIME(3) NULL AFTER amap_poi_id,
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER status,
    ADD CONSTRAINT ck_building_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    ADD CONSTRAINT ck_building_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT ck_building_location_source CHECK (location_source IN ('MANUAL', 'AMAP'));

ALTER TABLE building_unit
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER status;

ALTER TABLE layout_template
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER status;

ALTER TABLE property_unit
    DROP CHECK ck_property_unit_gross_area,
    DROP CHECK ck_property_unit_usable_area,
    DROP CHECK ck_property_unit_status,
    MODIFY COLUMN gross_area DECIMAL(10, 2) NULL COMMENT '建筑面积；DRAFT 草稿允许未填',
    MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/INACTIVE/ARCHIVED',
    ADD COLUMN occupancy_status VARCHAR(24) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/VACANT/OWNER_OCCUPIED/AVAILABLE_FOR_RENT/TENANT_OCCUPIED' AFTER status,
    ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER occupancy_status,
    ADD CONSTRAINT ck_property_unit_gross_area CHECK (
        (status = 'DRAFT' AND (gross_area IS NULL OR gross_area > 0))
        OR (status <> 'DRAFT' AND gross_area > 0)
    ),
    ADD CONSTRAINT ck_property_unit_usable_area CHECK (
        usable_area IS NULL OR (usable_area > 0 AND gross_area IS NOT NULL AND usable_area <= gross_area)
    ),
    ADD CONSTRAINT ck_property_unit_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')),
    ADD CONSTRAINT ck_property_unit_occupancy CHECK (
        occupancy_status IN ('UNKNOWN', 'VACANT', 'OWNER_OCCUPIED', 'AVAILABLE_FOR_RENT', 'TENANT_OCCUPIED')
    );

ALTER TABLE ownership
    DROP CHECK ck_ownership_share,
    ADD CONSTRAINT ck_ownership_single_owner_share CHECK (ownership_share = 1.000000),
    ADD COLUMN current_property_unit_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN end_date IS NULL THEN property_unit_id ELSE NULL END) STORED COMMENT '用于保证每套房同一时刻只有一个当前产权人',
    ADD CONSTRAINT uk_ownership_one_current_owner UNIQUE (current_property_unit_id);

CREATE TABLE file_object (
    file_object_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    storage_provider VARCHAR(16) NOT NULL DEFAULT 'MINIO',
    bucket_name VARCHAR(63) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT UNSIGNED NOT NULL,
    sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    uploaded_by_account_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DELETED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (file_object_id),
    CONSTRAINT uk_file_object_storage_key UNIQUE (bucket_name, object_key),
    KEY idx_file_object_uploader (uploaded_by_account_id),
    CONSTRAINT fk_file_object_uploader FOREIGN KEY (uploaded_by_account_id) REFERENCES app_account (account_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_file_object_size CHECK (size_bytes > 0),
    CONSTRAINT ck_file_object_status CHECK (status IN ('ACTIVE', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MinIO 对象元数据；不保存文件二进制';

CREATE TABLE property_floor_plan (
    property_unit_id BIGINT UNSIGNED NOT NULL,
    file_object_id BIGINT UNSIGNED NOT NULL,
    uploaded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (property_unit_id),
    CONSTRAINT uk_property_floor_plan_file UNIQUE (file_object_id),
    CONSTRAINT fk_property_floor_plan_property FOREIGN KEY (property_unit_id) REFERENCES property_unit (property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_property_floor_plan_file FOREIGN KEY (file_object_id) REFERENCES file_object (file_object_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一套具体房产当前采用的一张整套户型图';

CREATE TABLE asset_change_audit (
    audit_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entity_type VARCHAR(32) NOT NULL,
    entity_id BIGINT UNSIGNED NOT NULL,
    operation VARCHAR(24) NOT NULL,
    before_data JSON NULL,
    after_data JSON NULL,
    reason VARCHAR(255) NULL,
    changed_by_account_id BIGINT UNSIGNED NOT NULL,
    changed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (audit_id),
    KEY idx_asset_audit_entity (entity_type, entity_id, changed_at),
    CONSTRAINT fk_asset_audit_account FOREIGN KEY (changed_by_account_id) REFERENCES app_account (account_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产关键字段修改与位置纠错审计';
