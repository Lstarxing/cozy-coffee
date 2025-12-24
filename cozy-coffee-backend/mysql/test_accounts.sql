-- =====================================================
-- 管理端测试账号 SQL 脚本
-- 数据库: cozy_user (用户服务分库)
-- 
-- 使用方法:
-- mysql -u root -p cozy_user < test_accounts.sql
-- =====================================================

-- 1. 确保 role 字段存在（如果还没执行迁移脚本）
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'user' 
  COMMENT '用户角色: user-普通用户, admin-管理员';

-- 2. 创建管理员测试账号
-- 密码: admin123 (BCrypt 加密)
-- BCrypt hash of 'admin123': $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
INSERT INTO users (username, password, nickname, phone, role, created_at, updated_at)
VALUES (
    'testadmin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    '测试管理员',
    '13800000001',
    'admin',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    role = 'admin';

-- 3. 创建普通用户测试账号
-- 密码: user123 (BCrypt 加密)
-- BCrypt hash of 'user123': $2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36ZfDJsEWdCoQ/CMpvDdRCy
INSERT INTO users (username, password, nickname, phone, role, created_at, updated_at)
VALUES (
    'testuser',
    '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36ZfDJsEWdCoQ/CMpvDdRCy',
    '测试用户',
    '13800000002',
    'user',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    password = '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36ZfDJsEWdCoQ/CMpvDdRCy',
    role = 'user';

-- 4. 验证创建结果
SELECT id, username, nickname, role, created_at FROM users WHERE username IN ('testadmin', 'testuser');

-- =====================================================
-- 测试账号信息:
-- 管理员: testadmin / admin123
-- 普通用户: testuser / user123
-- =====================================================
