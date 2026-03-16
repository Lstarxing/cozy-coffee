package com.cozy.member.api;

import com.cozy.member.dto.response.MonthlyTaskDTO;

import java.math.BigDecimal;

/**
 * 月度任务服务接口 (v4.2)
 */
public interface MonthlyTaskService {

    /**
     * 更新用户月度消费并检查任务达成
     * 
     * @param userId  用户ID
     * @param orderId 订单ID (用于去重)
     * @param amount  订单实付金额
     */
    void updateMonthlySpent(Long userId, Long orderId, BigDecimal amount);

    /**
     * v6.0: 更新用户月度消费并检查任务达成（带订单属性，用于精确补偿事务隔离问题）
     * 
     * @param userId        用户ID
     * @param orderId       订单ID (用于去重)
     * @param amount        订单实付金额
     * @param isDelivery    是否外卖订单
     * @param hasNewProduct 是否包含新品
     */
    void updateMonthlySpentWithDetails(Long userId, Long orderId, BigDecimal amount,
            boolean isDelivery, boolean hasNewProduct);

    /**
     * 获取用户当月任务进度
     * 
     * @param userId 用户ID
     * @return 月度任务进度
     */
    MonthlyTaskDTO getCurrentMonthTask(Long userId);
}
