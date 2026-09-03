-- MinIO key 规范重排：咖啡商品图 images/v2 -> images/products/coffee
-- 配套：种子包 docker/minio-seed/images/products/coffee 已按新目录灌入
UPDATE coffee_products
   SET image_url = REPLACE(image_url, '/images/v2/', '/images/products/coffee/')
 WHERE image_url LIKE '/images/v2/%';
