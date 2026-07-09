USE cozy_order;
ALTER TABLE message_outbox
  DROP INDEX uk_aggregate_type;
ALTER TABLE message_outbox
  ADD INDEX idx_aggregate (aggregate_id);
