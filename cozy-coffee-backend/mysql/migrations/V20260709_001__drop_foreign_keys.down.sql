-- Rollback: re-add the 2 foreign keys

USE cozy_order;
ALTER TABLE shop_order_items
  ADD CONSTRAINT fk_shop_order_items_order_id
    FOREIGN KEY (order_id) REFERENCES shop_orders(id)
    ON DELETE CASCADE;

USE cozy_member;
ALTER TABLE points_lot_consumptions
  ADD CONSTRAINT fk_points_lot_consumptions_lot_id
    FOREIGN KEY (lot_id) REFERENCES points_lots(id)
    ON DELETE RESTRICT;
