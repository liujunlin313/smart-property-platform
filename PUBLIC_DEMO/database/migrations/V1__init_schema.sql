-- Multi-Community Smart Property Management System
-- Database V1 schema (MySQL 8.0+)
-- This directory is the single source of truth for versioned migrations.

SET NAMES utf8mb4;

CREATE TABLE community (
    community_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '小区主键',
    community_code VARCHAR(32) NOT NULL COMMENT '稳定业务编码',
    community_name VARCHAR(100) NOT NULL COMMENT '小区名称',
    address_line VARCHAR(255) NOT NULL COMMENT '小区详细地址',
    longitude DECIMAL(10, 7) NULL COMMENT 'WGS84/GCJ02 经度，坐标系由应用统一',
    latitude DECIMAL(10, 7) NULL COMMENT 'WGS84/GCJ02 纬度，坐标系由应用统一',
    completion_year SMALLINT UNSIGNED NULL COMMENT '建成年份',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (community_id),
    CONSTRAINT uk_community_code UNIQUE (community_code),
    CONSTRAINT ck_community_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_community_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_community_completion_year CHECK (completion_year IS NULL OR completion_year BETWEEN 1800 AND 2200),
    CONSTRAINT ck_community_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小区';

CREATE TABLE layout_template (
    layout_template_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '户型模板主键',
    layout_code VARCHAR(32) NOT NULL COMMENT '户型模板业务编码',
    layout_name VARCHAR(100) NOT NULL COMMENT '户型模板名称',
    layout_type ENUM(
        'STUDIO',
        'ONE_BEDROOM',
        'ONE_BEDROOM_ONE_LIVING',
        'TWO_BEDROOM',
        'TWO_BEDROOM_ONE_LIVING',
        'TWO_BEDROOM_TWO_LIVING',
        'THREE_BEDROOM_ONE_LIVING',
        'THREE_BEDROOM_TWO_LIVING',
        'FOUR_BEDROOM_TWO_LIVING',
        'OTHER'
    ) NOT NULL COMMENT '有限户型枚举：开间/一居室/一室一厅/两居室/两室一厅/两室两厅/三室一厅/三室两厅/四室两厅/其他',
    standard_area DECIMAL(10, 2) NULL COMMENT '模板参考建筑面积（平方米）',
    template_plan_object_key VARCHAR(255) NULL COMMENT '整套房户型图对象键；不是单个房间图片',
    description VARCHAR(500) NULL COMMENT '模板说明',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (layout_template_id),
    CONSTRAINT uk_layout_template_code UNIQUE (layout_code),
    CONSTRAINT ck_layout_template_area CHECK (standard_area IS NULL OR standard_area > 0),
    CONSTRAINT ck_layout_template_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='整套房户型模板';

CREATE TABLE building (
    building_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '楼栋主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属小区',
    building_no VARCHAR(32) NOT NULL COMMENT '小区内楼栋编号',
    building_name VARCHAR(100) NULL COMMENT '楼栋显示名称',
    total_floors SMALLINT UNSIGNED NULL COMMENT '总楼层数',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (building_id),
    CONSTRAINT uk_building_community_no UNIQUE (community_id, building_no),
    CONSTRAINT fk_building_community FOREIGN KEY (community_id) REFERENCES community (community_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_building_total_floors CHECK (total_floors IS NULL OR total_floors > 0),
    CONSTRAINT ck_building_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='楼栋';

CREATE TABLE building_unit (
    building_unit_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '楼栋单元主键',
    building_id BIGINT UNSIGNED NOT NULL COMMENT '所属楼栋',
    unit_no VARCHAR(32) NOT NULL COMMENT '楼栋内单元编号',
    unit_name VARCHAR(100) NULL COMMENT '单元显示名称',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (building_unit_id),
    CONSTRAINT uk_building_unit_building_no UNIQUE (building_id, unit_no),
    CONSTRAINT fk_building_unit_building FOREIGN KEY (building_id) REFERENCES building (building_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_building_unit_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='楼栋单元';

CREATE TABLE property_unit (
    property_unit_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '房产单元主键',
    building_unit_id BIGINT UNSIGNED NOT NULL COMMENT '所属楼栋单元',
    layout_template_id BIGINT UNSIGNED NULL COMMENT '采用的整套房户型模板',
    property_no VARCHAR(32) NOT NULL COMMENT '单元内房号',
    floor_no SMALLINT NULL COMMENT '所在楼层，可为地下层负数',
    gross_area DECIMAL(10, 2) NOT NULL COMMENT '建筑面积（平方米）',
    usable_area DECIMAL(10, 2) NULL COMMENT '套内使用面积（平方米）',
    property_use VARCHAR(16) NOT NULL DEFAULT 'RESIDENTIAL' COMMENT 'RESIDENTIAL/COMMERCIAL/OFFICE/OTHER',
    floor_plan_object_key VARCHAR(255) NULL COMMENT '该具体房产的整套房户型图覆盖；为空时采用模板图',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE/ARCHIVED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (property_unit_id),
    CONSTRAINT uk_property_unit_parent_no UNIQUE (building_unit_id, property_no),
    KEY idx_property_unit_layout_template (layout_template_id),
    CONSTRAINT fk_property_unit_building_unit FOREIGN KEY (building_unit_id) REFERENCES building_unit (building_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_property_unit_layout_template FOREIGN KEY (layout_template_id) REFERENCES layout_template (layout_template_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_property_unit_gross_area CHECK (gross_area > 0),
    CONSTRAINT ck_property_unit_usable_area CHECK (usable_area IS NULL OR (usable_area > 0 AND usable_area <= gross_area)),
    CONSTRAINT ck_property_unit_use CHECK (property_use IN ('RESIDENTIAL', 'COMMERCIAL', 'OFFICE', 'OTHER')),
    CONSTRAINT ck_property_unit_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房产单元；产权、居住和出租的最小粒度';

CREATE TABLE person (
    person_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '人员主键',
    person_code VARCHAR(32) NOT NULL COMMENT '人员业务编码',
    full_name VARCHAR(100) NOT NULL COMMENT '姓名',
    gender VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/MALE/FEMALE/OTHER',
    phone VARCHAR(32) NULL COMMENT '联系电话',
    id_number VARCHAR(64) NULL COMMENT '证件号；生产环境需加密/脱敏',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (person_id),
    CONSTRAINT uk_person_code UNIQUE (person_code),
    CONSTRAINT uk_person_id_number UNIQUE (id_number),
    CONSTRAINT ck_person_gender CHECK (gender IN ('UNKNOWN', 'MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_person_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='现实人员；不等同于登录账号或全局角色';

CREATE TABLE ownership (
    ownership_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '产权关系主键',
    person_id BIGINT UNSIGNED NOT NULL COMMENT '产权人',
    property_unit_id BIGINT UNSIGNED NOT NULL COMMENT '房产单元',
    ownership_share DECIMAL(7, 6) NOT NULL COMMENT '产权份额，0到1之间',
    start_date DATE NOT NULL COMMENT '产权生效日期',
    end_date DATE NULL COMMENT '产权失效日期；NULL表示当前有效',
    certificate_no VARCHAR(64) NULL COMMENT '产权证明编号',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (ownership_id),
    CONSTRAINT uk_ownership_business_key UNIQUE (person_id, property_unit_id, start_date),
    KEY idx_ownership_property_unit (property_unit_id),
    CONSTRAINT fk_ownership_person FOREIGN KEY (person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ownership_property_unit FOREIGN KEY (property_unit_id) REFERENCES property_unit (property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_ownership_share CHECK (ownership_share > 0 AND ownership_share <= 1),
    CONSTRAINT ck_ownership_dates CHECK (end_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人员与房产的时态产权关系';

CREATE TABLE residence (
    residence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '居住关系主键',
    person_id BIGINT UNSIGNED NOT NULL COMMENT '实际居住人',
    property_unit_id BIGINT UNSIGNED NOT NULL COMMENT '实际居住房产',
    residence_role VARCHAR(20) NOT NULL DEFAULT 'RESIDENT' COMMENT 'RESIDENT/PRIMARY_RESIDENT/FAMILY_MEMBER/TENANT/OTHER；仅描述居住，不表示产权',
    start_date DATE NOT NULL COMMENT '入住日期',
    end_date DATE NULL COMMENT '迁出日期；NULL表示当前居住',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (residence_id),
    CONSTRAINT uk_residence_business_key UNIQUE (person_id, property_unit_id, start_date),
    KEY idx_residence_property_unit (property_unit_id),
    CONSTRAINT fk_residence_person FOREIGN KEY (person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_residence_property_unit FOREIGN KEY (property_unit_id) REFERENCES property_unit (property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_residence_role CHECK (residence_role IN ('RESIDENT', 'PRIMARY_RESIDENT', 'FAMILY_MEMBER', 'TENANT', 'OTHER')),
    CONSTRAINT ck_residence_dates CHECK (end_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人员与房产的时态实际居住关系；与产权严格分离';

CREATE TABLE agency (
    agency_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '中介公司主键',
    agency_code VARCHAR(32) NOT NULL COMMENT '中介业务编码',
    agency_name VARCHAR(150) NOT NULL COMMENT '中介公司名称',
    license_no VARCHAR(64) NOT NULL COMMENT '机构证照编号',
    contact_phone VARCHAR(32) NULL COMMENT '联系电话',
    address_line VARCHAR(255) NULL COMMENT '办公地址',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (agency_id),
    CONSTRAINT uk_agency_code UNIQUE (agency_code),
    CONSTRAINT uk_agency_license UNIQUE (license_no),
    CONSTRAINT ck_agency_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中介公司业务主体';

CREATE TABLE authorization (
    authorization_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '中介授权主键',
    person_id BIGINT UNSIGNED NOT NULL COMMENT '授权业主',
    property_unit_id BIGINT UNSIGNED NOT NULL COMMENT '被授权房产',
    agency_id BIGINT UNSIGNED NOT NULL COMMENT '被授权中介公司',
    start_date DATE NOT NULL COMMENT '授权开始日期',
    end_date DATE NULL COMMENT '授权结束日期',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'DRAFT/ACTIVE/EXPIRED/REVOKED',
    can_view TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '可查看房产信息',
    can_publish TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '可发布房源',
    can_edit_listing TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '可修改房源',
    can_manage_contract TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '可管理租赁合同',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (authorization_id),
    CONSTRAINT uk_authorization_business_key UNIQUE (person_id, property_unit_id, agency_id, start_date),
    CONSTRAINT uk_authorization_id_property UNIQUE (authorization_id, property_unit_id),
    KEY idx_authorization_property_unit (property_unit_id),
    KEY idx_authorization_agency (agency_id),
    CONSTRAINT fk_authorization_person FOREIGN KEY (person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_authorization_property FOREIGN KEY (property_unit_id) REFERENCES property_unit (property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_authorization_agency FOREIGN KEY (agency_id) REFERENCES agency (agency_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_authorization_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_authorization_status CHECK (status IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'REVOKED')),
    CONSTRAINT ck_authorization_permissions CHECK (can_view IN (0,1) AND can_publish IN (0,1) AND can_edit_listing IN (0,1) AND can_manage_contract IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Person + PropertyUnit + Agency 三元授权联系';

CREATE TABLE rental_listing (
    rental_listing_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '出租房源主键',
    property_unit_id BIGINT UNSIGNED NOT NULL COMMENT '完整出租房产；不支持房间级出租',
    listing_mode VARCHAR(16) NOT NULL COMMENT 'SELF/AGENCY',
    publisher_person_id BIGINT UNSIGNED NULL COMMENT '个人直租发布人',
    authorization_id BIGINT UNSIGNED NULL COMMENT '中介发布依据的三元授权',
    listing_title VARCHAR(160) NOT NULL COMMENT '房源标题',
    description TEXT NULL COMMENT '房源描述',
    monthly_rent DECIMAL(12, 2) NOT NULL COMMENT '月租金',
    deposit_amount DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '押金',
    available_from DATE NOT NULL COMMENT '最早可入住日期',
    published_at DATETIME(3) NULL COMMENT '发布时间',
    closed_at DATETIME(3) NULL COMMENT '关闭时间',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/RENTED/CLOSED/EXPIRED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (rental_listing_id),
    KEY idx_rental_listing_property (property_unit_id),
    KEY idx_rental_listing_publisher (publisher_person_id),
    CONSTRAINT fk_rental_listing_property FOREIGN KEY (property_unit_id) REFERENCES property_unit (property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_rental_listing_publisher FOREIGN KEY (publisher_person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_listing_authorization_property FOREIGN KEY (authorization_id, property_unit_id) REFERENCES authorization (authorization_id, property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_rental_listing_mode CHECK (
        (listing_mode = 'SELF' AND publisher_person_id IS NOT NULL AND authorization_id IS NULL)
        OR (listing_mode = 'AGENCY' AND publisher_person_id IS NULL AND authorization_id IS NOT NULL)
    ),
    CONSTRAINT ck_rental_listing_money CHECK (monthly_rent > 0 AND deposit_amount >= 0),
    CONSTRAINT ck_rental_listing_dates CHECK (closed_at IS NULL OR (published_at IS NOT NULL AND closed_at >= published_at)),
    CONSTRAINT ck_rental_listing_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'RENTED', 'CLOSED', 'EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='完整房产的一次出租挂牌生命周期';

CREATE TABLE lease_contract (
    lease_contract_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '租赁合同主键',
    rental_listing_id BIGINT UNSIGNED NOT NULL COMMENT '成交来源房源；一个房源至多一个合同',
    contract_no VARCHAR(64) NOT NULL COMMENT '合同业务编号',
    signed_at DATE NOT NULL COMMENT '签署日期',
    lease_start_date DATE NOT NULL COMMENT '租期开始',
    lease_end_date DATE NOT NULL COMMENT '租期结束',
    monthly_rent DECIMAL(12, 2) NOT NULL COMMENT '合同约定月租金快照',
    deposit_amount DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '合同押金快照',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SIGNED/ACTIVE/ENDED/TERMINATED/CANCELLED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (lease_contract_id),
    CONSTRAINT uk_lease_contract_listing UNIQUE (rental_listing_id),
    CONSTRAINT uk_lease_contract_no UNIQUE (contract_no),
    CONSTRAINT fk_lease_contract_listing FOREIGN KEY (rental_listing_id) REFERENCES rental_listing (rental_listing_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_lease_contract_dates CHECK (lease_end_date >= lease_start_date AND signed_at <= lease_start_date),
    CONSTRAINT ck_lease_contract_money CHECK (monthly_rent > 0 AND deposit_amount >= 0),
    CONSTRAINT ck_lease_contract_status CHECK (status IN ('DRAFT', 'SIGNED', 'ACTIVE', 'ENDED', 'TERMINATED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='由房源成交形成的租赁合同';

CREATE TABLE contract_participation (
    lease_contract_id BIGINT UNSIGNED NOT NULL COMMENT '租赁合同',
    person_id BIGINT UNSIGNED NOT NULL COMMENT '合同参与人',
    participant_role VARCHAR(16) NOT NULL COMMENT 'LANDLORD/TENANT',
    is_primary TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否主要联系人',
    signed_at DATETIME(3) NULL COMMENT '该参与方签署时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (lease_contract_id, person_id, participant_role),
    KEY idx_contract_participation_person (person_id),
    CONSTRAINT fk_contract_participation_contract FOREIGN KEY (lease_contract_id) REFERENCES lease_contract (lease_contract_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_contract_participation_person FOREIGN KEY (person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_contract_participation_role CHECK (participant_role IN ('LANDLORD', 'TENANT')),
    CONSTRAINT ck_contract_participation_primary CHECK (is_primary IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人员与租赁合同的多对多参与联系';

CREATE TABLE fee_type (
    fee_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收费项目主键',
    fee_code VARCHAR(32) NOT NULL COMMENT '收费项目编码',
    fee_name VARCHAR(100) NOT NULL COMMENT '收费项目名称',
    billing_method VARCHAR(16) NOT NULL COMMENT 'FIXED/AREA/METERED',
    unit_name VARCHAR(32) NOT NULL COMMENT '计价单位，如月、平方米、吨、千瓦时',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (fee_type_id),
    CONSTRAINT uk_fee_type_code UNIQUE (fee_code),
    CONSTRAINT uk_fee_type_name UNIQUE (fee_name),
    CONSTRAINT ck_fee_type_method CHECK (billing_method IN ('FIXED', 'AREA', 'METERED')),
    CONSTRAINT ck_fee_type_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收费项目字典';

CREATE TABLE fee_standard (
    fee_standard_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收费标准主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '制定标准的小区',
    fee_type_id BIGINT UNSIGNED NOT NULL COMMENT '对应收费项目',
    standard_name VARCHAR(120) NOT NULL COMMENT '标准名称',
    unit_price DECIMAL(12, 4) NOT NULL COMMENT '单位价格',
    effective_from DATE NOT NULL COMMENT '生效日期',
    effective_to DATE NULL COMMENT '失效日期；NULL表示仍有效',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (fee_standard_id),
    CONSTRAINT uk_fee_standard_version UNIQUE (community_id, fee_type_id, effective_from),
    KEY idx_fee_standard_fee_type (fee_type_id),
    CONSTRAINT fk_fee_standard_community FOREIGN KEY (community_id) REFERENCES community (community_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_fee_standard_fee_type FOREIGN KEY (fee_type_id) REFERENCES fee_type (fee_type_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_fee_standard_price CHECK (unit_price >= 0),
    CONSTRAINT ck_fee_standard_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT ck_fee_standard_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小区在时段内采用的收费标准';

CREATE TABLE meter (
    meter_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '计量表主键',
    property_unit_id BIGINT UNSIGNED NOT NULL COMMENT '安装房产',
    fee_type_id BIGINT UNSIGNED NOT NULL COMMENT '计量对应收费项目',
    meter_no VARCHAR(64) NOT NULL COMMENT '表具编号',
    installed_at DATE NOT NULL COMMENT '安装日期',
    decommissioned_at DATE NULL COMMENT '停用日期',
    initial_reading DECIMAL(14, 3) NOT NULL DEFAULT 0 COMMENT '初始读数',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE/REMOVED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (meter_id),
    CONSTRAINT uk_meter_no UNIQUE (meter_no),
    KEY idx_meter_property (property_unit_id),
    KEY idx_meter_fee_type (fee_type_id),
    CONSTRAINT fk_meter_property FOREIGN KEY (property_unit_id) REFERENCES property_unit (property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_meter_fee_type FOREIGN KEY (fee_type_id) REFERENCES fee_type (fee_type_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_meter_dates CHECK (decommissioned_at IS NULL OR decommissioned_at >= installed_at),
    CONSTRAINT ck_meter_initial_reading CHECK (initial_reading >= 0),
    CONSTRAINT ck_meter_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'REMOVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房产计量表';

CREATE TABLE meter_reading (
    meter_reading_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '抄表记录主键',
    meter_id BIGINT UNSIGNED NOT NULL COMMENT '所属计量表',
    reading_at DATETIME(3) NOT NULL COMMENT '抄表时刻',
    reading_value DECIMAL(14, 3) NOT NULL COMMENT '累计读数',
    reading_source VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/IMPORT/DEVICE',
    recorder_person_id BIGINT UNSIGNED NULL COMMENT '人工抄表人；系统导入时可空',
    note VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (meter_reading_id),
    CONSTRAINT uk_meter_reading_time UNIQUE (meter_id, reading_at),
    KEY idx_meter_reading_recorder (recorder_person_id),
    CONSTRAINT fk_meter_reading_meter FOREIGN KEY (meter_id) REFERENCES meter (meter_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_meter_reading_recorder FOREIGN KEY (recorder_person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_meter_reading_value CHECK (reading_value >= 0),
    CONSTRAINT ck_meter_reading_source CHECK (reading_source IN ('MANUAL', 'IMPORT', 'DEVICE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计量表时序读数';

CREATE TABLE bill (
    bill_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '账单主键',
    bill_no VARCHAR(64) NOT NULL COMMENT '账单业务编号',
    property_unit_id BIGINT UNSIGNED NOT NULL COMMENT '应收房产',
    billing_year SMALLINT UNSIGNED NOT NULL COMMENT '账期年份',
    billing_month TINYINT UNSIGNED NOT NULL COMMENT '账期月份',
    issued_at DATETIME(3) NOT NULL COMMENT '出账时刻',
    due_date DATE NOT NULL COMMENT '缴费截止日期',
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED' COMMENT 'DRAFT/ISSUED/PARTIALLY_PAID/PAID/OVERDUE/CANCELLED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (bill_id),
    CONSTRAINT uk_bill_no UNIQUE (bill_no),
    CONSTRAINT uk_bill_property_period UNIQUE (property_unit_id, billing_year, billing_month),
    CONSTRAINT fk_bill_property FOREIGN KEY (property_unit_id) REFERENCES property_unit (property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_bill_year CHECK (billing_year BETWEEN 2000 AND 2200),
    CONSTRAINT ck_bill_month CHECK (billing_month BETWEEN 1 AND 12),
    CONSTRAINT ck_bill_status CHECK (status IN ('DRAFT', 'ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房产账期账单；总额由账单项汇总';

CREATE TABLE bill_item (
    bill_id BIGINT UNSIGNED NOT NULL COMMENT '所属账单；弱实体的强实体标识',
    item_no SMALLINT UNSIGNED NOT NULL COMMENT '账单内局部编号；弱实体局部标识',
    fee_standard_id BIGINT UNSIGNED NOT NULL COMMENT '计费依据；可由此追溯收费项目和小区标准',
    previous_reading_id BIGINT UNSIGNED NULL COMMENT '计量收费的上期读数',
    current_reading_id BIGINT UNSIGNED NULL COMMENT '计量收费的本期读数',
    quantity DECIMAL(14, 3) NOT NULL COMMENT '计费数量；面积/用量/固定项均结构化保存',
    unit_price DECIMAL(12, 4) NOT NULL COMMENT '出账时单位价格快照',
    amount DECIMAL(14, 2) GENERATED ALWAYS AS (ROUND(quantity * unit_price, 2)) STORED COMMENT '由数量乘单价生成的账单项金额',
    note VARCHAR(255) NULL COMMENT '账单项备注；不重复费种名称',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (bill_id, item_no),
    KEY idx_bill_item_fee_standard (fee_standard_id),
    KEY idx_bill_item_previous_reading (previous_reading_id),
    KEY idx_bill_item_current_reading (current_reading_id),
    CONSTRAINT fk_bill_item_bill FOREIGN KEY (bill_id) REFERENCES bill (bill_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bill_item_fee_standard FOREIGN KEY (fee_standard_id) REFERENCES fee_standard (fee_standard_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bill_item_previous_reading FOREIGN KEY (previous_reading_id) REFERENCES meter_reading (meter_reading_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bill_item_current_reading FOREIGN KEY (current_reading_id) REFERENCES meter_reading (meter_reading_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_bill_item_number CHECK (item_no > 0),
    CONSTRAINT ck_bill_item_money CHECK (quantity >= 0 AND unit_price >= 0),
    CONSTRAINT ck_bill_item_reading_pair CHECK (
        (previous_reading_id IS NULL AND current_reading_id IS NULL)
        OR (previous_reading_id IS NOT NULL AND current_reading_id IS NOT NULL AND previous_reading_id <> current_reading_id)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账单项目弱实体；主键=(bill_id,item_no)';

CREATE TABLE payment (
    payment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付主键',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付业务编号',
    bill_id BIGINT UNSIGNED NOT NULL COMMENT '被支付账单',
    payer_person_id BIGINT UNSIGNED NOT NULL COMMENT '付款人',
    amount DECIMAL(14, 2) NOT NULL COMMENT '支付金额',
    paid_at DATETIME(3) NOT NULL COMMENT '支付时刻',
    payment_method VARCHAR(20) NOT NULL COMMENT 'CASH/BANK_TRANSFER/WECHAT/ALIPAY/OTHER',
    external_transaction_no VARCHAR(100) NULL COMMENT '外部交易号；线下支付可空',
    status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT 'PENDING/SUCCESS/FAILED/CANCELLED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (payment_id),
    CONSTRAINT uk_payment_no UNIQUE (payment_no),
    CONSTRAINT uk_payment_external_no UNIQUE (external_transaction_no),
    KEY idx_payment_bill (bill_id),
    KEY idx_payment_payer (payer_person_id),
    CONSTRAINT fk_payment_bill FOREIGN KEY (bill_id) REFERENCES bill (bill_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_payer FOREIGN KEY (payer_person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_method CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'WECHAT', 'ALIPAY', 'OTHER')),
    CONSTRAINT ck_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人员对账单的支付记录';

CREATE TABLE parking_space (
    parking_space_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '车位主键',
    community_id BIGINT UNSIGNED NOT NULL COMMENT '所属小区',
    space_no VARCHAR(32) NOT NULL COMMENT '小区内车位编号',
    location_description VARCHAR(160) NULL COMMENT '位置说明',
    space_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD/COMPACT/ACCESSIBLE/EV/OTHER',
    area DECIMAL(8, 2) NULL COMMENT '面积（平方米）',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/MAINTENANCE/INACTIVE；不冗余实时占用状态',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (parking_space_id),
    CONSTRAINT uk_parking_space_community_no UNIQUE (community_id, space_no),
    CONSTRAINT fk_parking_space_community FOREIGN KEY (community_id) REFERENCES community (community_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_parking_space_type CHECK (space_type IN ('STANDARD', 'COMPACT', 'ACCESSIBLE', 'EV', 'OTHER')),
    CONSTRAINT ck_parking_space_area CHECK (area IS NULL OR area > 0),
    CONSTRAINT ck_parking_space_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小区车位';

CREATE TABLE vehicle (
    vehicle_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '车辆主键',
    person_id BIGINT UNSIGNED NOT NULL COMMENT '登记人；车位权利不依赖本表',
    plate_number VARCHAR(32) NOT NULL COMMENT '车牌号',
    vehicle_type VARCHAR(16) NOT NULL DEFAULT 'CAR' COMMENT 'CAR/MOTORCYCLE/OTHER',
    brand VARCHAR(64) NULL COMMENT '品牌',
    model VARCHAR(64) NULL COMMENT '型号',
    color VARCHAR(32) NULL COMMENT '颜色',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (vehicle_id),
    CONSTRAINT uk_vehicle_plate UNIQUE (plate_number),
    KEY idx_vehicle_person (person_id),
    CONSTRAINT fk_vehicle_person FOREIGN KEY (person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_vehicle_type CHECK (vehicle_type IN ('CAR', 'MOTORCYCLE', 'OTHER')),
    CONSTRAINT ck_vehicle_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人员登记车辆；不作为长期车位权利主体';

CREATE TABLE parking_right (
    parking_right_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '车位权利主键',
    person_id BIGINT UNSIGNED NOT NULL COMMENT '权利人',
    parking_space_id BIGINT UNSIGNED NOT NULL COMMENT '车位',
    right_type VARCHAR(16) NOT NULL COMMENT 'OWNED/LEASED',
    start_date DATE NOT NULL COMMENT '权利开始日期',
    end_date DATE NULL COMMENT '权利结束日期；OWNED可为空',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ENDED/CANCELLED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (parking_right_id),
    CONSTRAINT uk_parking_right_business_key UNIQUE (person_id, parking_space_id, start_date),
    KEY idx_parking_right_space (parking_space_id),
    CONSTRAINT fk_parking_right_person FOREIGN KEY (person_id) REFERENCES person (person_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_parking_right_space FOREIGN KEY (parking_space_id) REFERENCES parking_space (parking_space_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_parking_right_type CHECK (right_type IN ('OWNED', 'LEASED')),
    CONSTRAINT ck_parking_right_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_parking_right_status CHECK (status IN ('ACTIVE', 'ENDED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人员与车位的时态长期权利；不绑定车辆';

CREATE TABLE facility_type (
    facility_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设施类型主键',
    facility_code VARCHAR(32) NOT NULL COMMENT '设施类型编码',
    facility_name VARCHAR(100) NOT NULL COMMENT '设施类型名称',
    category VARCHAR(32) NOT NULL COMMENT 'FURNITURE/APPLIANCE/FIXTURE/OTHER',
    default_mobility_mode VARCHAR(16) NOT NULL COMMENT 'FREE/FIXED/LIMITED',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (facility_type_id),
    CONSTRAINT uk_facility_type_code UNIQUE (facility_code),
    CONSTRAINT uk_facility_type_name UNIQUE (facility_name),
    CONSTRAINT ck_facility_type_category CHECK (category IN ('FURNITURE', 'APPLIANCE', 'FIXTURE', 'OTHER')),
    CONSTRAINT ck_facility_type_mobility CHECK (default_mobility_mode IN ('FREE', 'FIXED', 'LIMITED')),
    CONSTRAINT ck_facility_type_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家具与设施类型字典';

CREATE TABLE facility_placement (
    facility_placement_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设施放置主键',
    property_unit_id BIGINT UNSIGNED NOT NULL COMMENT '整套房坐标系所属房产',
    facility_type_id BIGINT UNSIGNED NOT NULL COMMENT '设施类型',
    placement_label VARCHAR(100) NULL COMMENT '实例标签，如客厅挂机空调',
    x_ratio DECIMAL(8, 6) NOT NULL COMMENT '左上角X坐标/整套户型图宽度',
    y_ratio DECIMAL(8, 6) NOT NULL COMMENT '左上角Y坐标/整套户型图高度',
    width_ratio DECIMAL(8, 6) NOT NULL COMMENT '设施宽度/整套户型图宽度',
    height_ratio DECIMAL(8, 6) NOT NULL COMMENT '设施高度/整套户型图高度',
    rotation_degrees DECIMAL(7, 3) NOT NULL DEFAULT 0 COMMENT '旋转角度',
    mobility_mode VARCHAR(16) NOT NULL COMMENT 'FREE/FIXED/LIMITED；实例可覆盖类型默认值',
    anchor_x_ratio DECIMAL(8, 6) NULL COMMENT '固定安装点或有限移动中心X',
    anchor_y_ratio DECIMAL(8, 6) NULL COMMENT '固定安装点或有限移动中心Y',
    movement_radius_ratio DECIMAL(8, 6) NULL COMMENT '相对户型图短边的允许移动半径；FIXED为0',
    installation_note VARCHAR(500) NULL COMMENT '墙孔、管线、排水等安装限制备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (facility_placement_id),
    KEY idx_facility_placement_property (property_unit_id),
    KEY idx_facility_placement_type (facility_type_id),
    CONSTRAINT fk_facility_placement_property FOREIGN KEY (property_unit_id) REFERENCES property_unit (property_unit_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_facility_placement_type FOREIGN KEY (facility_type_id) REFERENCES facility_type (facility_type_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_facility_position CHECK (
        x_ratio BETWEEN 0 AND 1 AND y_ratio BETWEEN 0 AND 1
        AND width_ratio > 0 AND width_ratio <= 1
        AND height_ratio > 0 AND height_ratio <= 1
        AND x_ratio + width_ratio <= 1
        AND y_ratio + height_ratio <= 1
    ),
    CONSTRAINT ck_facility_rotation CHECK (rotation_degrees BETWEEN -180 AND 180),
    CONSTRAINT ck_facility_anchor_range CHECK (
        (anchor_x_ratio IS NULL OR anchor_x_ratio BETWEEN 0 AND 1)
        AND (anchor_y_ratio IS NULL OR anchor_y_ratio BETWEEN 0 AND 1)
        AND (movement_radius_ratio IS NULL OR movement_radius_ratio BETWEEN 0 AND 1)
    ),
    CONSTRAINT ck_facility_mobility CHECK (
        (mobility_mode = 'FREE' AND anchor_x_ratio IS NULL AND anchor_y_ratio IS NULL AND movement_radius_ratio IS NULL)
        OR (mobility_mode = 'FIXED' AND anchor_x_ratio IS NOT NULL AND anchor_y_ratio IS NOT NULL AND movement_radius_ratio = 0)
        OR (mobility_mode = 'LIMITED' AND anchor_x_ratio IS NOT NULL AND anchor_y_ratio IS NOT NULL AND movement_radius_ratio > 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='整套PropertyUnit户型图上的二维设施覆盖层；无Room实体';
