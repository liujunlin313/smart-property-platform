-- Reusable Database V1 query verification.

SELECT
    'Q1_PROPERTY_FULL_PATH' AS query_case,
    pu.property_unit_id,
    CONCAT(c.community_name, ' / ', b.building_name, ' / ', bu.unit_name, ' / ', pu.property_no) AS full_property_path,
    lt.layout_name,
    lt.layout_type,
    pu.gross_area
FROM property_unit pu
JOIN building_unit bu ON bu.building_unit_id = pu.building_unit_id
JOIN building b ON b.building_id = bu.building_id
JOIN community c ON c.community_id = b.community_id
LEFT JOIN layout_template lt ON lt.layout_template_id = pu.layout_template_id
WHERE pu.property_unit_id = 1;

SELECT
    'Q2_ZHANGSAN_OWNERSHIP' AS query_case,
    p.full_name,
    c.community_name,
    b.building_name,
    bu.unit_name,
    pu.property_no,
    o.ownership_share
FROM person p
JOIN ownership o ON o.person_id = p.person_id AND o.end_date IS NULL
JOIN property_unit pu ON pu.property_unit_id = o.property_unit_id
JOIN building_unit bu ON bu.building_unit_id = pu.building_unit_id
JOIN building b ON b.building_id = bu.building_id
JOIN community c ON c.community_id = b.community_id
WHERE p.person_code = 'P-ZHANGSAN'
ORDER BY c.community_id, b.building_id, bu.building_unit_id, pu.property_no;

SELECT
    'Q3_ZHANGSAN_RESIDENCE' AS query_case,
    p.full_name,
    c.community_name,
    b.building_name,
    bu.unit_name,
    pu.property_no,
    r.residence_role
FROM person p
JOIN residence r ON r.person_id = p.person_id AND r.end_date IS NULL
JOIN property_unit pu ON pu.property_unit_id = r.property_unit_id
JOIN building_unit bu ON bu.building_unit_id = pu.building_unit_id
JOIN building b ON b.building_id = bu.building_id
JOIN community c ON c.community_id = b.community_id
WHERE p.person_code = 'P-ZHANGSAN';

SELECT
    'Q4_CROSS_COMMUNITY_OWNER' AS query_case,
    p.full_name,
    COUNT(DISTINCT c.community_id) AS owned_community_count
FROM person p
JOIN ownership o ON o.person_id = p.person_id AND o.end_date IS NULL
JOIN property_unit pu ON pu.property_unit_id = o.property_unit_id
JOIN building_unit bu ON bu.building_unit_id = pu.building_unit_id
JOIN building b ON b.building_id = bu.building_id
JOIN community c ON c.community_id = b.community_id
GROUP BY p.person_id, p.full_name
HAVING COUNT(DISTINCT c.community_id) >= 2;

WITH item_totals AS (
    SELECT bill_id, SUM(amount) AS receivable_amount
    FROM bill_item
    GROUP BY bill_id
), payment_totals AS (
    SELECT bill_id, SUM(amount) AS paid_amount
    FROM payment
    WHERE status = 'SUCCESS'
    GROUP BY bill_id
)
SELECT
    'Q5_BILL_RECEIVABLE_PAID' AS query_case,
    b.bill_no,
    it.receivable_amount,
    COALESCE(pt.paid_amount, 0) AS paid_amount,
    it.receivable_amount - COALESCE(pt.paid_amount, 0) AS outstanding_amount
FROM bill b
JOIN item_totals it ON it.bill_id = b.bill_id
LEFT JOIN payment_totals pt ON pt.bill_id = b.bill_id
ORDER BY b.bill_id;

WITH ranked_reading AS (
    SELECT
        mr.*,
        ROW_NUMBER() OVER (PARTITION BY mr.meter_id ORDER BY mr.reading_at DESC, mr.meter_reading_id DESC) AS rn
    FROM meter_reading mr
)
SELECT
    'Q6_LATEST_METER_READING' AS query_case,
    m.meter_no,
    rr.reading_at,
    rr.reading_value
FROM meter m
JOIN ranked_reading rr ON rr.meter_id = m.meter_id AND rr.rn = 1
ORDER BY m.meter_id;

SELECT
    'Q7_ACTIVE_AGENCY_AUTHORIZATION' AS query_case,
    p.full_name AS owner_name,
    a.agency_name,
    c.community_name,
    pu.property_no,
    au.can_publish,
    au.can_manage_contract
FROM authorization au
JOIN person p ON p.person_id = au.person_id
JOIN agency a ON a.agency_id = au.agency_id
JOIN property_unit pu ON pu.property_unit_id = au.property_unit_id
JOIN building_unit bu ON bu.building_unit_id = pu.building_unit_id
JOIN building b ON b.building_id = bu.building_id
JOIN community c ON c.community_id = b.community_id
WHERE au.status = 'ACTIVE'
  AND DATE('2026-08-20') BETWEEN au.start_date AND COALESCE(au.end_date, DATE('9999-12-31'))
ORDER BY au.authorization_id;

SELECT
    'Q8_MULTI_PARTY_CONTRACT' AS query_case,
    lc.contract_no,
    cp.participant_role,
    GROUP_CONCAT(p.full_name ORDER BY p.person_id SEPARATOR '、') AS participants
FROM lease_contract lc
JOIN contract_participation cp ON cp.lease_contract_id = lc.lease_contract_id
JOIN person p ON p.person_id = cp.person_id
GROUP BY lc.lease_contract_id, lc.contract_no, cp.participant_role
ORDER BY lc.lease_contract_id, cp.participant_role;
