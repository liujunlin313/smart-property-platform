-- Charge notifications require recipient-scoped structured data. Existing notification
-- history remains unchanged and ordinary notifications keep this value NULL.
ALTER TABLE notification_recipient
    ADD COLUMN payload_json JSON NULL AFTER delivery_status;
