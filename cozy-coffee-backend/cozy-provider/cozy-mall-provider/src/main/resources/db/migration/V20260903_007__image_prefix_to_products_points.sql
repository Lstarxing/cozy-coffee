-- MinIO key 规范重排：积分兑换商品图 /images/seed/兑换商品(URL编码) -> /images/products/points
-- DB 存的是 URL 编码的相对路径（'/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/...'）
UPDATE points_products
   SET image_url = REPLACE(image_url,
       '/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/', '/images/products/points/')
 WHERE image_url LIKE '/images/seed/%';

UPDATE points_orders
   SET product_image = REPLACE(product_image,
       '/images/seed/%E5%85%91%E6%8D%A2%E5%95%86%E5%93%81/', '/images/products/points/')
 WHERE product_image LIKE '/images/seed/%';
