-- 订单优惠券抵扣明细（每张券名称 + 抵扣金额 + 是否主券），JSON 数组快照
-- 供订单详情页逐条展示优惠明细（与确认页 preview.couponDetails 同构）
ALTER TABLE `shop_orders`
    ADD COLUMN `coupon_details` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL
        COMMENT '优惠券抵扣明细 JSON: [{"title":"券名","discount":金额,"main":是否主券}]' AFTER `applied_addon_coupon_ids`;
