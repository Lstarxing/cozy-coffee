DROP INDEX uk_users_openid ON users;

ALTER TABLE users DROP COLUMN openid;
