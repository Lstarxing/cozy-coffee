package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.MonthlyTaskService;
import com.cozy.member.dto.response.MonthlyTaskDTO;
import com.cozy.member.entity.MonthlyTask;
import com.cozy.member.entity.MonthlyTaskOrder;
import com.cozy.member.mapper.MonthlyTaskMapper;
import com.cozy.member.mapper.MonthlyTaskOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 月度任务服务实现 (v4.2)
 * 
 * 核心规则:
 * - 订单完成时自动更新月度消费
 * - 满300/600/1000逐级自动发放积分
 * - 等级加成: 银+5%, 金+10%, 钻石+15%, 黑+20%
 * - 积分取整: HALF_UP
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class MonthlyTaskServiceImpl implements MonthlyTaskService {

    private final MonthlyTaskMapper taskMapper;
    private final MonthlyTaskOrderMapper orderMapper;
    private final MemberService memberService;

    @org.apache.dubbo.config.annotation.DubboReference(check = false)
    private com.cozy.order.api.OrderService orderService;

    // v5.0: 挑战任务奖励积分（与前端展示一致）
    private static final int REWARD_ORDER = 40; // 打卡达人(4次下单)
    private static final int REWARD_MORNING = 60; // 晨间唤醒(3次10点前下单)
    private static final int REWARD_DELIVERY = 50; // 外卖尝鲜(2笔外卖)
    private static final int REWARD_NEWPRODUCT = 80; // 新品猎人(3款新品)

    @Override
    @Transactional
    public void updateMonthlySpent(Long userId, Long orderId, BigDecimal amount) {
        // 向后兼容：调用新方法，默认不补偿外卖和新品
        updateMonthlySpentWithDetails(userId, orderId, amount, false, false);
    }

    @Override
    @Transactional
    public void updateMonthlySpentWithDetails(Long userId, Long orderId, BigDecimal amount,
            boolean isDelivery, boolean hasNewProduct) {
        if (userId == null || orderId == null || amount == null) {
            return;
        }

        String month = YearMonth.now().toString(); // 2025-12

        // 1. 防重复: 检查订单是否已计入
        LambdaQueryWrapper<MonthlyTaskOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(MonthlyTaskOrder::getOrderId, orderId);
        if (orderMapper.selectCount(orderWrapper) > 0) {
            log.info("订单已计入月度任务,跳过: orderId={}", orderId);
            return;
        }

        // 2. 记录订单 (unique约束防并发)
        try {
            MonthlyTaskOrder taskOrder = new MonthlyTaskOrder();
            taskOrder.setUserId(userId);
            taskOrder.setTaskMonth(month);
            taskOrder.setOrderId(orderId);
            taskOrder.setAmount(amount);
            taskOrder.setCreatedAt(LocalDateTime.now());
            orderMapper.insert(taskOrder);
        } catch (DuplicateKeyException e) {
            log.info("订单重复插入被拦截: orderId={}", orderId);
            return;
        }

        // 3. 更新或创建月度任务
        MonthlyTask task = getOrCreateTask(userId, month);
        task.setCurrentSpent(task.getCurrentSpent().add(amount));
        taskMapper.updateById(task);

        log.info("月度任务更新: userId={}, month={}, orderId={}, amount={}, total={}, isDelivery={}, hasNewProduct={}",
                userId, month, orderId, amount, task.getCurrentSpent(), isDelivery, hasNewProduct);

        // 4. 检查并发放奖励 (逐级) - 传入当前订单属性用于补偿
        checkAndGrantRewards(userId, task, orderId, isDelivery, hasNewProduct);
    }

    @Override
    public MonthlyTaskDTO getCurrentMonthTask(Long userId) {
        String month = YearMonth.now().toString();
        MonthlyTask task = getOrCreateTask(userId, month);

        MonthlyTaskDTO dto = new MonthlyTaskDTO();
        dto.setUserId(userId);
        dto.setTaskMonth(task.getTaskMonth());
        dto.setCurrentSpent(task.getCurrentSpent());
        // 保留旧字段兼容性
        dto.setReward300Claimed(task.getReward300Claimed());
        dto.setReward600Claimed(task.getReward600Claimed());
        dto.setReward1000Claimed(task.getReward1000Claimed());

        // v5.0: 挑战任务完成状态
        dto.setChallengeOrderClaimed(task.getChallengeOrderClaimed() != null ? task.getChallengeOrderClaimed() : false);
        dto.setChallengeMorningClaimed(
                task.getChallengeMorningClaimed() != null ? task.getChallengeMorningClaimed() : false);
        dto.setChallengeDeliveryClaimed(
                task.getChallengeDeliveryClaimed() != null ? task.getChallengeDeliveryClaimed() : false);
        dto.setChallengeNewproductClaimed(
                task.getChallengeNewproductClaimed() != null ? task.getChallengeNewproductClaimed() : false);

        // v5.0: 获取月度订单统计
        try {
            if (orderService != null) {
                com.cozy.order.dto.response.MonthlyStatsDTO stats = orderService.getMonthlyStats(userId);
                if (stats != null) {
                    dto.setMonthlyOrderCount(stats.getOrderCount());
                    dto.setMorningOrderCount(stats.getMorningOrderCount());
                    dto.setCurrentDeliveryOrders(stats.getDeliveryOrderCount());
                    dto.setNewProductCount(stats.getNewProductCount());

                    // =========================================================
                    // v5.3.1 修复: 移除自动补发逻辑，仅在订单完成时触发奖励
                    // 问题: getCurrentMonthTask每次查询都会尝试补发，导致重复发放积分
                    // 解决: 挑战任务奖励仅在checkAndGrantRewards中发放（订单完成触发）
                    // =========================================================
                    // 自动补发逻辑已移除 - 仅展示进度状态
                }
            }
        } catch (Exception e) {
            log.warn("获取月度订单统计失败: {}", e.getMessage());
        }

        return dto;
    }

    /**
     * 安全发放奖励 (无订单关联)
     */
    private void grantRewardSafely(Long userId, int points, String sourceType, String desc) {
        try {
            // 使用 sourceId = 0L 表示系统自动/补发
            memberService.addPointsWithLot(userId, points, sourceType, 0L, desc);
        } catch (Exception e) {
            log.error("奖励补发失败: userId={}, type={}", userId, sourceType, e);
        }
    }

    private MonthlyTask getOrCreateTask(Long userId, String month) {
        LambdaQueryWrapper<MonthlyTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MonthlyTask::getUserId, userId)
                .eq(MonthlyTask::getTaskMonth, month);
        MonthlyTask task = taskMapper.selectOne(wrapper);

        if (task == null) {
            task = new MonthlyTask();
            task.setUserId(userId);
            task.setTaskMonth(month);
            task.setCurrentSpent(BigDecimal.ZERO);
            task.setReward300Claimed(false);
            task.setReward600Claimed(false);
            task.setReward1000Claimed(false);
            // v5.0: 挑战任务初始化
            task.setChallengeOrderClaimed(false);
            task.setChallengeMorningClaimed(false);
            task.setChallengeDeliveryClaimed(false);
            task.setChallengeNewproductClaimed(false);
            task.setCreatedAt(LocalDateTime.now());
            taskMapper.insert(task);
        }

        return task;
    }

    /**
     * v5.3.1: 检查并发放挑战任务积分奖励
     * 根据前端定义的任务（基于完成订单统计，已排除取消订单）：
     * - 打卡达人: 本月完成订单4次 → +40积分
     * - 晨间唤醒: 10点前完成订单3次 → +60积分
     * - 外卖尝鲜: 完成2笔外卖订单 → +50积分
     * - 新品猎人: 尝试3款新品（完成订单） → +80积分
     * 
     * 幂等性保障:
     * - task.challengeXxxClaimed字段防止重复发放
     * - getMonthlyStats已排除cancelled状态订单
     * - addPointsWithLot中的sourceId用于防重
     * 
     * v5.3.3: 每次发放前重新查询最新状态，确保使用最新的 claimed 标志
     * v6.0: 接收当前订单属性进行精确补偿
     */
    private void checkAndGrantRewards(Long userId, MonthlyTask task, Long orderId,
            boolean isDelivery, boolean hasNewProduct) {
        // 获取月度订单统计
        com.cozy.order.dto.response.MonthlyStatsDTO stats = null;
        try {
            if (orderService != null) {
                stats = orderService.getMonthlyStats(userId);
            }
        } catch (Exception e) {
            log.warn("获取月度订单统计失败: {}", e.getMessage());
            return;
        }

        if (stats == null)
            return;

        // v6.2 修复: MQ 解耦后调用时机已晚于订单状态提交，getMonthlyStats 能正确统计到当前订单。
        // 早期 +1 补偿逻辑（针对事务内调用看不到当前订单的情况）已不再需要，
        // 否则会导致重复计数，使挑战任务提前触发奖励。
        int actualOrderCount = stats.getOrderCount();
        int actualMorningCount = stats.getMorningOrderCount();
        int actualDeliveryCount = stats.getDeliveryOrderCount();
        int actualNewProductCount = stats.getNewProductCount();
        log.info("月度挑战检查: userId={}, orderId={}, orderCount={}, morning={}, delivery={}, newProduct={}",
                userId, orderId, actualOrderCount, actualMorningCount, actualDeliveryCount, actualNewProductCount);

        String month = YearMonth.now().toString();

        // 打卡达人: 本月下单4次
        if (actualOrderCount >= 4) {
            // 重新查询最新状态，避免使用过期的内存对象
            MonthlyTask latestTask = getOrCreateTask(userId, month);
            if (latestTask.getChallengeOrderClaimed() == null || !latestTask.getChallengeOrderClaimed()) {
                try {
                    memberService.addPointsWithLot(userId, REWARD_ORDER, "challenge_order",
                            orderId, "挑战任务【打卡达人】完成奖励");
                    // 只有积分发放成功，才更新标志位
                    latestTask.setChallengeOrderClaimed(true);
                    taskMapper.updateById(latestTask);
                    log.info("挑战任务【打卡达人】奖励发放成功: userId={}, points={}", userId, REWARD_ORDER);
                } catch (Exception e) {
                    log.error("打卡达人奖励发放失败，不更新claimed标志: userId={}, error={}", userId, e.getMessage(), e);
                }
            }
        }

        // 晨间唤醒: 10点前下单3次
        if (actualMorningCount >= 3) {
            MonthlyTask latestTask = getOrCreateTask(userId, month);
            if (latestTask.getChallengeMorningClaimed() == null || !latestTask.getChallengeMorningClaimed()) {
                try {
                    memberService.addPointsWithLot(userId, REWARD_MORNING, "challenge_morning",
                            orderId, "挑战任务【晨间唤醒】完成奖励");
                    latestTask.setChallengeMorningClaimed(true);
                    taskMapper.updateById(latestTask);
                    log.info("挑战任务【晨间唤醒】奖励发放成功: userId={}, points={}", userId, REWARD_MORNING);
                } catch (Exception e) {
                    log.error("晨间唤醒奖励发放失败，不更新claimed标志: userId={}, error={}", userId, e.getMessage(), e);
                }
            }
        }

        // 外卖尝鲜: 完成2笔外卖
        if (actualDeliveryCount >= 2) {
            MonthlyTask latestTask = getOrCreateTask(userId, month);
            if (latestTask.getChallengeDeliveryClaimed() == null || !latestTask.getChallengeDeliveryClaimed()) {
                try {
                    memberService.addPointsWithLot(userId, REWARD_DELIVERY, "challenge_delivery",
                            orderId, "挑战任务【外卖尝鲜】完成奖励");
                    latestTask.setChallengeDeliveryClaimed(true);
                    taskMapper.updateById(latestTask);
                    log.info("挑战任务【外卖尝鲜】奖励发放成功: userId={}, points={}", userId, REWARD_DELIVERY);
                } catch (Exception e) {
                    log.error("外卖尝鲜奖励发放失败，不更新claimed标志: userId={}, error={}", userId, e.getMessage(), e);
                }
            }
        }

        // 新品猎人: 尝试3款新品
        if (actualNewProductCount >= 3) {
            MonthlyTask latestTask = getOrCreateTask(userId, month);
            if (latestTask.getChallengeNewproductClaimed() == null || !latestTask.getChallengeNewproductClaimed()) {
                try {
                    memberService.addPointsWithLot(userId, REWARD_NEWPRODUCT, "challenge_newproduct",
                            orderId, "挑战任务【新品猎人】完成奖励");
                    latestTask.setChallengeNewproductClaimed(true);
                    taskMapper.updateById(latestTask);
                    log.info("挑战任务【新品猎人】奖励发放成功: userId={}, points={}", userId, REWARD_NEWPRODUCT);
                } catch (Exception e) {
                    log.error("新品猎人奖励发放失败，不更新claimed标志: userId={}, error={}", userId, e.getMessage(), e);
                }
            }
        }
    }
}
