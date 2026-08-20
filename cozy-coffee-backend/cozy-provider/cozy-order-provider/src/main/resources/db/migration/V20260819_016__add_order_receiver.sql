ALTER TABLE shop_orders
    ADD COLUMN receiver_name VARCHAR(50) NULL
        COMMENT '收货人姓名（外送快照）'
        AFTER expected_delivery_at,
    ADD COLUMN receiver_phone VARCHAR(20) NULL
        COMMENT '收货人电话（外送快照）'
        AFTER receiver_name,
    ADD COLUMN receiver_address VARCHAR(255) NULL
        COMMENT '收货详细地址（外送快照）'
        AFTER receiver_phone;
