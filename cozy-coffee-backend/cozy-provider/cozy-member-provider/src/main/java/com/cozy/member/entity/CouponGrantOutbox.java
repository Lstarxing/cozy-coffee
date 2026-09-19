package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发券请求的本地消息表（Outbox）。
 * 与会员业务数据在同一本地事务内写入，独立任务扫表重投 MQ，保证"发券请求最终一定发出"。
 */
@Data
@TableName("coupon_grant_outbox")
public class CouponGrantOutbox {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务幂等键，与 mall 侧 user_coupon.coupon_code 一致；本表唯一索引防重复入队 */
    private String uniqueKey;

    private Long userId;

    /** 事件载荷 JSON（CouponGrantRequestedEvent） */
    private String payload;

    /** PENDING / SENT / DEAD */
    private String status;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
