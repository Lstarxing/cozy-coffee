-- 兼容上线前已经写入 outbox、但 payload 尚无 rollbackEventId 的券回滚消息。
-- 订单取消用 order:{orderId}；订单落库失败用 operation:{aggregateId}。
UPDATE `message_outbox`
SET `payload` = JSON_SET(
        `payload`,
        '$.rollbackEventId',
        CASE
          WHEN JSON_EXTRACT(`payload`, '$.orderId') IS NULL
               OR JSON_TYPE(JSON_EXTRACT(`payload`, '$.orderId')) = 'NULL'
            THEN CONCAT('operation:', `aggregate_id`)
          ELSE CONCAT('order:', `aggregate_id`)
        END)
WHERE `message_type` = 'coupon_rollback'
  AND JSON_EXTRACT(`payload`, '$.rollbackEventId') IS NULL;
