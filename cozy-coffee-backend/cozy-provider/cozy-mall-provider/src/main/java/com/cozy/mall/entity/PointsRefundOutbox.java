package com.cozy.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_refund_outbox")
public class PointsRefundOutbox {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long userId;
    private Integer points;
    private String consumeType;
    private String description;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
