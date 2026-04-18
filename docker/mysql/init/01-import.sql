-- Import each schema dump into its target database during first-time initialization.

USE cozy_user;
SOURCE /docker-entrypoint-initdb.d/raw/cozy_user.sql;

USE cozy_member;
SOURCE /docker-entrypoint-initdb.d/raw/cozy_member.sql;

USE cozy_order;
SOURCE /docker-entrypoint-initdb.d/raw/cozy_order.sql;

USE cozy_mall;
SOURCE /docker-entrypoint-initdb.d/raw/cozy_mall.sql;
