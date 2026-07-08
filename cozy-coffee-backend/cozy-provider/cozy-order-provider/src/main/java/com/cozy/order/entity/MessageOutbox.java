package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Outbox 模式的本地消息表。
 * 与订单状态变更在同一本地事务内写入，独立任务异步扫表重投 MQ，保证最终一致性。
 */
@Data
@TableName("message_outbox")
public class MessageOutbox {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务聚合 ID（orderId） */
    private Long aggregateId;

    /** 消息类型，如 coupon_rollback */
    private String messageType;

    /** RocketMQ topic */
    private String topic;

    /** RocketMQ tag */
    private String tag;

    /** 消息体 JSON */
    private String payload;

    /** PENDING / SENT / CONFIRMED */
    private String status;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
