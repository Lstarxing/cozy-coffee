ALTER TABLE user_addresses
    ADD COLUMN label VARCHAR(10) NULL
        COMMENT '地址标签：HOME家 COMPANY公司 SCHOOL学校'
        AFTER district;
