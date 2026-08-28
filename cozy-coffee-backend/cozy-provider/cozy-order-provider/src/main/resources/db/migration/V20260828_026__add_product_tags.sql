-- V2 商品体系 · Tags 标签列（3.1：商品可有多个 Tag，一个 Primary Category）
-- 展示用标签，暂不按维度拆独立字段（后续做筛选/推荐再拆）；TOP1 随真实销售数据动态化，不静态录入
-- 存储 JSON 数组：["NEW","COLD","FRUITY"]
ALTER TABLE `coffee_products`
  ADD COLUMN `tags` JSON NULL COMMENT '标签（展示用）：["NEW","COLD"]，TOP1 数据驱动不静态录入' AFTER `serving_desc`;
