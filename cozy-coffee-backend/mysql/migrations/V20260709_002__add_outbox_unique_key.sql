-- DC5: Add unique key to message_outbox to prevent duplicate event publishes
USE cozy_order;

-- Drop the non-unique index (will be replaced by unique key)
ALTER TABLE message_outbox
  DROP INDEX idx_aggregate;

-- Add unique key
ALTER TABLE message_outbox
  ADD UNIQUE KEY uk_aggregate_type (aggregate_id, message_type);
