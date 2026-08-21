ALTER TABLE shop_orders
    ADD COLUMN completed_at datetime NULL
        COMMENT '完成时间（自提出餐/确认收货时写入，自提兜底自动确认计时）'
        AFTER expected_delivery_at;
