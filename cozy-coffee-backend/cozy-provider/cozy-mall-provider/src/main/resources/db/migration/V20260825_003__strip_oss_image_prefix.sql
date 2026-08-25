-- 图片字段统一为相对路径：去掉 OSS 域名前缀，前端统一拼接 IMAGE_BASE
-- OSS 到期临时本地托管，历史数据存的是 OSS 全路径，切存储时只需改前端 IMAGE_BASE 配置
UPDATE `points_products`
SET `image_url` = REPLACE(`image_url`, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com', '')
WHERE `image_url` LIKE 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com%';

UPDATE `points_orders`
SET `product_image` = REPLACE(`product_image`, 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com', '')
WHERE `product_image` LIKE 'https://cozycoffee-srx.oss-cn-hangzhou.aliyuncs.com%';
