package com.cozy.member.dto.response;

import lombok.Data;
import java.math.BigDecimal;

import java.io.Serializable;

/**
 * 月度任务DTO (v4.2)
 */
@Data
public class MonthlyTaskDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String taskMonth; // YYYY-MM

    private BigDecimal currentSpent; // 当月消费

    private Boolean reward300Claimed; // 满300奖励已发放

    private Boolean reward600Claimed; // 满600奖励已发放

    private Boolean reward1000Claimed; // 满1000奖励已发放

    // 前端展示用
    private int progress300; // 满300进度百分比

    private int progress600; // 满600进度百分比

    private int progress1000; // 满1000进度百分比

    private int points300; // 300档可获积分(含等级加成)

    private int points600; // 600档可获积分

    private int points1000; // 1000档可获积分

    // v5.0 新增任务 (Task 3: 月度挑战任务)
    private Integer currentDeliveryOrders; // 外卖尝鲜 (2笔)
    private Integer monthlyOrderCount; // 打卡达人 (4次)
    private Integer morningOrderCount; // 晨间唤醒 (3次)
    private Integer newProductCount; // 新品猎人 (3款)

    // v5.0: 挑战任务完成状态（已发放积分）
    private Boolean challengeOrderClaimed; // 打卡达人已完成
    private Boolean challengeMorningClaimed; // 晨间唤醒已完成
    private Boolean challengeDeliveryClaimed; // 外卖尝鲜已完成
    private Boolean challengeNewproductClaimed; // 新品猎人已完成
}
