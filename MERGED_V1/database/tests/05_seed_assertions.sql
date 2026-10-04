-- Each row must return PASS. The verification runner fails if any row is FAIL.

SELECT 'A01 community count >= 2' AS assertion_name,
       IF((SELECT COUNT(*) FROM community) >= 2, 'PASS', 'FAIL') AS result
UNION ALL
SELECT 'A02 each community has at least two buildings',
       IF((SELECT MIN(building_count) FROM (
              SELECT community_id, COUNT(*) AS building_count
              FROM building GROUP BY community_id
          ) x) >= 2, 'PASS', 'FAIL')
UNION ALL
SELECT 'A03 each building has at least two units',
       IF((SELECT MIN(unit_count) FROM (
              SELECT building_id, COUNT(*) AS unit_count
              FROM building_unit GROUP BY building_id
          ) x) >= 2, 'PASS', 'FAIL')
UNION ALL
SELECT 'A04 each building unit has multiple properties',
       IF((SELECT MIN(property_count) FROM (
              SELECT building_unit_id, COUNT(*) AS property_count
              FROM property_unit GROUP BY building_unit_id
          ) x) >= 2, 'PASS', 'FAIL')
UNION ALL
SELECT 'A05 Zhang San owns properties in multiple communities',
       IF((
           SELECT COUNT(DISTINCT c.community_id)
           FROM ownership o
           JOIN person p ON p.person_id = o.person_id
           JOIN property_unit pu ON pu.property_unit_id = o.property_unit_id
           JOIN building_unit bu ON bu.building_unit_id = pu.building_unit_id
           JOIN building b ON b.building_id = bu.building_id
           JOIN community c ON c.community_id = b.community_id
           WHERE p.person_code = 'P-ZHANGSAN' AND o.end_date IS NULL
       ) >= 2, 'PASS', 'FAIL')
UNION ALL
SELECT 'A06 Zhang San current residence differs from every owned property',
       IF(NOT EXISTS (
           SELECT 1
           FROM residence r
           JOIN person p ON p.person_id = r.person_id
           JOIN ownership o ON o.person_id = r.person_id
                            AND o.property_unit_id = r.property_unit_id
                            AND o.end_date IS NULL
           WHERE p.person_code = 'P-ZHANGSAN' AND r.end_date IS NULL
       ), 'PASS', 'FAIL')
UNION ALL
SELECT 'A07 authorization is ternary and seeded',
       IF((SELECT COUNT(*) FROM authorization WHERE person_id IS NOT NULL AND property_unit_id IS NOT NULL AND agency_id IS NOT NULL) >= 3, 'PASS', 'FAIL')
UNION ALL
SELECT 'A08 contract has one landlord and supports multiple tenants',
       IF((SELECT COUNT(*) FROM contract_participation WHERE lease_contract_id = 1 AND participant_role = 'LANDLORD') = 1
          AND (SELECT COUNT(*) FROM contract_participation WHERE lease_contract_id = 1 AND participant_role = 'TENANT') >= 2, 'PASS', 'FAIL')
UNION ALL
SELECT 'A09 BillItem weak key has dependent rows',
       IF((SELECT COUNT(*) FROM bill_item) >= 1, 'PASS', 'FAIL')
UNION ALL
SELECT 'A10 metered bill items trace two readings',
       IF((SELECT COUNT(*) FROM bill_item WHERE previous_reading_id IS NOT NULL AND current_reading_id IS NOT NULL) >= 4, 'PASS', 'FAIL')
UNION ALL
SELECT 'A11 parking rights belong to people, not vehicles',
       IF(EXISTS (
           SELECT 1 FROM parking_right pr
           JOIN person p ON p.person_id = pr.person_id
           JOIN parking_space ps ON ps.parking_space_id = pr.parking_space_id
       ), 'PASS', 'FAIL')
UNION ALL
SELECT 'A12 facility placements use whole-property coordinates',
       IF((SELECT COUNT(*) FROM facility_placement WHERE property_unit_id IS NOT NULL) >= 5, 'PASS', 'FAIL')
UNION ALL
SELECT 'A13 limited air conditioner movement is represented',
       IF(EXISTS (
           SELECT 1 FROM facility_placement fp
           JOIN facility_type ft ON ft.facility_type_id = fp.facility_type_id
           WHERE ft.facility_code = 'AIR_CONDITIONER'
             AND fp.mobility_mode = 'LIMITED'
             AND fp.anchor_x_ratio IS NOT NULL
             AND fp.movement_radius_ratio > 0
       ), 'PASS', 'FAIL')
UNION ALL
SELECT 'A14 no Room or Occupancy table was introduced',
       IF((SELECT COUNT(*) FROM information_schema.tables
           WHERE table_schema = DATABASE()
             AND table_name IN ('room', 'occupancy')) = 0, 'PASS', 'FAIL');
