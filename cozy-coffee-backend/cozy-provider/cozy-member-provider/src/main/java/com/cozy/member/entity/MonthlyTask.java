package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度任务实体
 */
@Data
@TableName("monthly_tasks")
public class MonthlyTask {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String taskMonth; // YYYY-MM

    private BigDecimal currentSpent;

    @com.baomidou.mybatisplus.annotation.TableField("reward_300_claimed")
    private Boolean reward300Claimed;

    @com.baomidou.mybatisplus.annotation.TableField("reward_600_claimed")
    private Boolean reward600Claimed;

    @com.baomidou.mybatisplus.annotation.TableField("reward_1000_claimed")
    private Boolean reward1000Claimed;

    // v5.0: 挑战任务完成标记
    @com.baomidou.mybatisplus.annotation.TableField("challenge_order_claimed")
    private Boolean challengeOrderClaimed; // 打卡达人(4次下单)

    @com.baomidou.mybatisplus.annotation.TableField("challenge_morning_claimed")
    private Boolean challengeMorningClaimed; // 晨间唤醒(3次10点前下单)

    @com.baomidou.mybatisplus.annotation.TableField("challenge_delivery_claimed")
    private Boolean challengeDeliveryClaimed; // 外卖尝鲜(2笔外卖)

    @com.baomidou.mybatisplus.annotation.TableField("challenge_newproduct_claimed")
    private Boolean challengeNewproductClaimed; // 新品猎人(3款新品)

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
