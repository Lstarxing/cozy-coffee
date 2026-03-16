package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 积分批次实体（FIFO）
 */
@Data
@TableName("points_lots")
public class PointsLot {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer initialAmount;
    private Integer remaining;
    private String sourceType; // order_completed/signin/register/admin/migration
    private Long sourceId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
