USE cozy_member;

-- Revert monthly_spent_month to VARCHAR(7)
ALTER TABLE member_info
  ADD COLUMN monthly_spent_month_str VARCHAR(7) NULL AFTER monthly_spent_month;

UPDATE member_info
  SET monthly_spent_month_str = DATE_FORMAT(monthly_spent_month, '%Y-%m')
  WHERE monthly_spent_month IS NOT NULL;

ALTER TABLE member_info DROP COLUMN monthly_spent_month;
ALTER TABLE member_info
  CHANGE COLUMN monthly_spent_month_str monthly_spent_month VARCHAR(7) NULL
    COMMENT '当前月度消费统计月份 (YYYY-MM)';
