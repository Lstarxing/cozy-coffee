-- =============================================
-- 取餐码迁移脚本
-- 执行前请确保已连接正确数据库
-- =============================================

-- ==================== cozy_order 数据库 ====================
USE cozy_order;

-- 1. 创建取餐码计数器表
CREATE TABLE IF NOT EXISTS pickup_code_counter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID（暂时默认1）',
    business_date DATE NOT NULL COMMENT '营业日期',
    last_seq INT NOT NULL DEFAULT 0 COMMENT '当日最后使用的序号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_business (store_id, business_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='取餐码计数器表';

-- 2. 修改咖啡订单表，增加取餐码相关字段
ALTER TABLE shop_orders 
ADD COLUMN IF NOT EXISTS store_id BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
ADD COLUMN IF NOT EXISTS business_date DATE NULL COMMENT '营业日期',
ADD COLUMN IF NOT EXISTS pickup_code VARCHAR(3) NULL COMMENT '取餐码(001-999)',
ADD COLUMN IF NOT EXISTS pickup_code_generated_at TIMESTAMP NULL COMMENT '取餐码生成时间';

-- 添加取餐码唯一索引（如不存在）
-- 注：MySQL 8.0+ 支持 IF NOT EXISTS，低版本需手动检查
-- ALTER TABLE shop_orders ADD UNIQUE INDEX IF NOT EXISTS uk_pickup_code (store_id, business_date, pickup_code);

-- ==================== cozy_mall 数据库 ====================
USE cozy_mall;

-- 3. 修改积分兑换订单表，增加取餐码和配送方式（仅新增不存在的字段）
ALTER TABLE points_orders
ADD COLUMN IF NOT EXISTS business_date DATE NULL COMMENT '营业日期',
ADD COLUMN IF NOT EXISTS pickup_code VARCHAR(3) NULL COMMENT '取餐码(自提用)',
ADD COLUMN IF NOT EXISTS delivery_type VARCHAR(20) NULL DEFAULT 'pickup' COMMENT '配送方式: pickup/express';

-- 注意：shipping_company, tracking_number, shipped_at 字段已存在，无需重复添加

SELECT '✅ 取餐码迁移完成！' AS status;
