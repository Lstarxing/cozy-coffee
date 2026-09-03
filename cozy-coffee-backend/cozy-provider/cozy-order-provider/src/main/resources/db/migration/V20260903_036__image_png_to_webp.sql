-- 商品图由 PNG 统一切换为 MinIO 中的 WebP 对象（image_url 扩展名 .png -> .webp）
-- 前置：MinIO 桶 cozycoffee 已按 images/v2/**.webp 灌好对应对象；前端 IMAGE_BASE 已指向 MinIO。
UPDATE coffee_products
   SET image_url = REPLACE(image_url, '.png', '.webp')
 WHERE image_url LIKE '/images/v2/%.png';
