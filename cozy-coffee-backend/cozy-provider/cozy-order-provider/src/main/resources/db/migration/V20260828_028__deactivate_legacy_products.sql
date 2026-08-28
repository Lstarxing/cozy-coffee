-- 修复 023 旧行下线 bug：utf8mb4_unicode_ci 大小写不敏感下，
-- 旧行小写分类（espresso/signature/bakery）被 `NOT IN ('ESPRESSO',...)` 误判为匹配而保留 active，
-- 导致旧菜单与新 V2 菜单共存（重复商品）。
-- 用 BINARY 大小写敏感比对：旧小写分类行全部下线，V2 大写分类行保留。
UPDATE `coffee_products` SET `status`='inactive'
WHERE `status`='active'
  AND BINARY `category` NOT IN ('ESPRESSO','MILK','SIGNATURE','SPECIALTY','NON_COFFEE','BAKERY');

-- ══════════════════════════════════════════════════════════════
-- 数据验收（本迁移应用后手动跑，期望全部通过）
-- ══════════════════════════════════════════════════════════════
-- ① active 商品 = 32（全部 V2 大写分类）
-- SELECT COUNT(*) FROM coffee_products WHERE status='active';  -- 期望 32
-- ② 无旧小写分类残留 active
-- SELECT COUNT(*) FROM coffee_products
-- WHERE status='active' AND BINARY category NOT IN ('ESPRESSO','MILK','SIGNATURE','SPECIALTY','NON_COFFEE','BAKERY');  -- 期望 0
