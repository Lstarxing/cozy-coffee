-- Import each schema dump into its target database during first-time initialization.

CREATE DATABASE IF NOT EXISTS cozy_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cozy_member CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cozy_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cozy_mall CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cozy_user;
SOURCE /docker-entrypoint-initdb.d/raw/cozy_user.sql;

USE cozy_member;
SOURCE /docker-entrypoint-initdb.d/raw/cozy_member.sql;

USE cozy_order;
SOURCE /docker-entrypoint-initdb.d/raw/cozy_order.sql;

USE cozy_mall;
SOURCE /docker-entrypoint-initdb.d/raw/cozy_mall.sql;
