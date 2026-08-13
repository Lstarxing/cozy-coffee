ALTER TABLE users
    ADD COLUMN openid VARCHAR(64) NULL COMMENT '微信登录 openid' AFTER email;

CREATE UNIQUE INDEX uk_users_openid ON users(openid);
