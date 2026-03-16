package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度任务订单去重实体
 */
@Data
@TableName("monthly_task_orders")
public class MonthlyTaskOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String taskMonth; // YYYY-MM

    private Long orderId;

    private BigDecimal amount;

    private LocalDateTime createdAt;
}
