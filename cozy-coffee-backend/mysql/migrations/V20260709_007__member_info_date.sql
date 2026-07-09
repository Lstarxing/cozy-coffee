-- DH3: member_info.monthly_spent_month VARCHAR(7) -> DATE
-- (charset fix deferred -- CONVERT TO CHARACTER SET strips comments, too risky for Phase 1)
USE cozy_member;

-- Step 1: Add new DATE column
ALTER TABLE member_info
  ADD COLUMN monthly_spent_month_new DATE NULL AFTER monthly_spent_month;

-- Step 2: Convert VARCHAR(7) 'YYYY-MM' to DATE (first of month)
UPDATE member_info
  SET monthly_spent_month_new = CASE
    WHEN monthly_spent_month IS NOT NULL AND monthly_spent_month REGEXP '^[0-9]{4}-[0-9]{2}$'
      THEN STR_TO_DATE(CONCAT(monthly_spent_month, '-01'), '%Y-%m-%d')
    ELSE NULL
  END;

-- Step 3: Drop old VARCHAR column
ALTER TABLE member_info DROP COLUMN monthly_spent_month;

-- Step 4: Rename new column to original name
ALTER TABLE member_info
  CHANGE COLUMN monthly_spent_month_new monthly_spent_month DATE NULL
    COMMENT '当前月度消费统计月份 (DATE, 月初)';
