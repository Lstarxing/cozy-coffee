-- =====================================================
-- 管理端角色鉴权迁移脚本
-- =====================================================

-- 1. 为 users 表添加 role 字段
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user' 
  COMMENT '用户角色: user-普通用户, admin-管理员';

-- 2. 为测试设置第一个用户为管理员
UPDATE users SET role = 'admin' WHERE id = 1;

-- 3. 添加索引以优化角色查询
CREATE INDEX idx_users_role ON users(role);
