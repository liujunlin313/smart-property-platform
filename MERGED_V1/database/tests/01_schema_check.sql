-- Schema inspection evidence. Read-only.

SELECT VERSION() AS mysql_version, DATABASE() AS database_name;

SHOW TABLES;

SELECT COUNT(*) AS application_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_name <> 'flyway_schema_history';

SELECT constraint_type, COUNT(*) AS constraint_count
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
  AND table_name <> 'flyway_schema_history'
GROUP BY constraint_type
ORDER BY constraint_type;

SELECT
    IF(non_unique = 0, 'UNIQUE_SECONDARY', 'NON_UNIQUE_SECONDARY') AS index_kind,
    COUNT(*) AS index_count
FROM (
    SELECT DISTINCT table_name, index_name, non_unique
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name <> 'flyway_schema_history'
      AND index_name <> 'PRIMARY'
) indexes_by_name
GROUP BY non_unique
ORDER BY non_unique;

SELECT table_name, column_name, column_type, is_nullable, column_key, extra
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN (
      'layout_template', 'property_unit', 'ownership', 'residence',
      'authorization', 'bill_item', 'parking_right', 'facility_placement'
  )
ORDER BY table_name, ordinal_position;

DESCRIBE community;
DESCRIBE property_unit;
DESCRIBE ownership;
DESCRIBE residence;
DESCRIBE authorization;
DESCRIBE bill_item;
