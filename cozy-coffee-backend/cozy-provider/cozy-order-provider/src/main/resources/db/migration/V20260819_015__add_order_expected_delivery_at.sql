ALTER TABLE shop_orders
    ADD COLUMN expected_delivery_at DATETIME NULL
        COMMENT '外送预计送达时间（配送到点自动确认已完成）'
        AFTER delivery_fee_waived_reason;

CREATE INDEX idx_shop_orders_delivery_eta
    ON shop_orders(status, expected_delivery_at);
