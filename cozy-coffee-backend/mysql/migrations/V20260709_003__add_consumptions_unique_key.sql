-- DC6: Add unique key to points_lot_consumptions to prevent duplicate FIFO deductions
USE cozy_member;

-- Keep idx_consume (consume_type, consume_id) for non-unique lookups
-- Add new unique key with 3 columns
ALTER TABLE points_lot_consumptions
  ADD UNIQUE KEY uk_consume_lot (consume_type, consume_id, lot_id);
