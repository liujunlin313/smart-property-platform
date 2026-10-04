-- Stage G: the charge-notice and overdue-bill paths filter bill records by
-- billing period and status before joining bill_item. The existing primary key
-- and per-property-period unique key cannot serve that access pattern.
CREATE INDEX idx_bill_period_status
    ON bill (billing_year, billing_month, status, bill_id);
