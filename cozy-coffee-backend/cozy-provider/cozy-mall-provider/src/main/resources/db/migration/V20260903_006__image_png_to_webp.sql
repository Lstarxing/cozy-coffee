-- 积分商城商品/兑换单图由 PNG 统一切换为 MinIO 中的 WebP 对象（.png -> .webp）
-- 前置：MinIO 桶 cozycoffee 已按 images/seed/**.webp 灌好对应对象；前端 IMAGE_BASE 已指向 MinIO。
UPDATE points_products
   SET image_url = REPLACE(image_url, '.png', '.webp')
 WHERE image_url LIKE '/images/seed/%.png';

UPDATE points_orders
   SET product_image = REPLACE(product_image, '.png', '.webp')
 WHERE product_image LIKE '/images/seed/%.png';
