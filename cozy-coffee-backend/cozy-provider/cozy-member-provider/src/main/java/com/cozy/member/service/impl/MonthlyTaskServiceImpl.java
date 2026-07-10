package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.MonthlyTaskService;
import com.cozy.member.dto.response.MonthlyTaskDTO;
import com.cozy.member.entity.MonthlyTask;
import com.cozy.member.entity.MonthlyTaskOrder;
import com.cozy.member.mapper.MonthlyTaskMapper;
import com.cozy.member.mapper.MonthlyTaskOrderMapper;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.MonthlyStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
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

    @DubboReference(check = false)
    private OrderService orderService;

    // v5.0: 挑战任务奖励积分（与前端展示一致）
    private static final int REWARD_ORDER = 40; // 打卡达人(4次下单)
    private static final int REWARD_MORNING = 60; // 晨间唤醒(3次10点前下单)
    private static final int REWARD_DELIVERY = 50; // 外卖尝鲜(2笔外卖)
    private static final int REWARD_NEWPRODUCT = 80; // 新品猎人(3款新品)

    @Override
    public void updateMonthlySpent(Long userId, Long orderId, BigDecimal amount) {
        // 向后兼容：调用新方法，默认不补偿外卖和新品
        updateMonthlySpentWithDetails(userId, orderId, amount, false, false);
    }

    /**
     * C3 修复：事务内只做本地更新，Dubbo 远程调用移到事务外。
     */
    @Override
    public void updateMonthlySpentWithDetails(Long userId, Long orderId, BigDecimal amount,
            boolean isDelivery, boolean hasNewProduct) {
        if (userId == null || orderId == null || amount == null) {
            return;
        }

        String month = YearMonth.now().toString();

        // ===== 事务内：本地 DB 操作 =====
        MonthlyTask task = doMonthlySpentInTx(userId, orderId, month, amount);
        if (task == null) {
            return;
        }

        log.info("月度任务更新: userId={}, month={}, orderId={}, amount={}, total={}, isDelivery={}, hasNewProduct={}",
                userId, month, orderId, amount, task.getCurrentSpent(), isDelivery, hasNewProduct);

        // ===== 事务外：Dubbo 远程调用 + 奖励发放 =====
        checkAndGrantRewards(userId, task, orderId, isDelivery, hasNewProduct);
    }

    @Transactional
    private MonthlyTask doMonthlySpentInTx(Long userId, Long orderId, String month, BigDecimal amount) {
        // 1. 防重复: 检查订单是否已计入
        LambdaQueryWrapper<MonthlyTaskOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(MonthlyTaskOrder::getOrderId, orderId);
        if (orderMapper.selectCount(orderWrapper) > 0) {
            log.info("订单已计入月度任务,跳过: orderId={}", orderId);
            return null;
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
            return null;
        }

        // 3. 更新或创建月度任务
        MonthlyTask task = getOrCreateTask(userId, month);
        task.setCurrentSpent(task.getCurrentSpent().add(amount));
        taskMapper.updateById(task);
        return task;
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
                MonthlyStatsDTO stats = orderService.getMonthlyStats(userId);
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
     * v5.3.1 + C3/H5/H6 修复：
     * - C3: 本方法已移到 @Transactional 外，不再持事务调 Dubbo
     * - H5: 4 个挑战任务块抽为 tryGrantChallenge 模板，消除 80 行重复
     * - H6: claimed 标志改为 UPDATE WHERE id=? AND claimed=false（乐观锁），
     *   配合 Phase 2 DuplicateKeyException 兜底，消除并发双发窗口
     */
    private void checkAndGrantRewards(Long userId, MonthlyTask task, Long orderId,
            boolean isDelivery, boolean hasNewProduct) {
        MonthlyStatsDTO stats = null;
        try {
            if (orderService != null) {
                stats = orderService.getMonthlyStats(userId);
            }
        } catch (Exception e) {
            log.warn("获取月度订单统计失败: {}", e.getMessage());
            return;
        }

        if (stats == null) return;

        // v6.2 修复: MQ 解耦后调用时机已晚于订单状态提交，+1 补偿逻辑已移除
        int orderCount = stats.getOrderCount();
        int morningCount = stats.getMorningOrderCount();
        int deliveryCount = stats.getDeliveryOrderCount();
        int newProductCount = stats.getNewProductCount();
        log.info("月度挑战检查: userId={}, orderId={}, orderCount={}, morning={}, delivery={}, newProduct={}",
                userId, orderId, orderCount, morningCount, deliveryCount, newProductCount);

        String month = YearMonth.now().toString();

        tryGrantChallenge(userId, month, orderId,
                orderCount, 4, MonthlyTask::getChallengeOrderClaimed,
                "challenge_order_claimed", "challenge_order",
                REWARD_ORDER, "挑战任务【打卡达人】完成奖励");
        tryGrantChallenge(userId, month, orderId,
                morningCount, 3, MonthlyTask::getChallengeMorningClaimed,
                "challenge_morning_claimed", "challenge_morning",
                REWARD_MORNING, "挑战任务【晨间唤醒】完成奖励");
        tryGrantChallenge(userId, month, orderId,
                deliveryCount, 2, MonthlyTask::getChallengeDeliveryClaimed,
                "challenge_delivery_claimed", "challenge_delivery",
                REWARD_DELIVERY, "挑战任务【外卖尝鲜】完成奖励");
        tryGrantChallenge(userId, month, orderId,
                newProductCount, 3, MonthlyTask::getChallengeNewproductClaimed,
                "challenge_newproduct_claimed", "challenge_newproduct",
                REWARD_NEWPRODUCT, "挑战任务【新品猎人】完成奖励");
    }

    /**
     * H5+H6: 挑战任务奖励通用发放逻辑。
     * 乐观锁 UPDATE WHERE id=? AND claimed=false 防并发双发，
     * DuplicateKeyException 兜底（Phase 2 已加固 consumer 侧）。
     */
    private void tryGrantChallenge(Long userId, String month, Long orderId,
            int actualCount, int threshold,
            java.util.function.Function<MonthlyTask, Boolean> claimedGetter,
            String claimedColumn, String sourceType, int points, String description) {
        if (actualCount < threshold) return;

        MonthlyTask latestTask = getOrCreateTask(userId, month);
        if (Boolean.TRUE.equals(claimedGetter.apply(latestTask))) return;

        try {
            memberService.addPointsWithLot(userId, points, sourceType, orderId, description);

            // H6: 乐观锁 — UPDATE monthly_task SET claimed=1 WHERE id=? AND claimed=0
            LambdaUpdateWrapper<MonthlyTask> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MonthlyTask::getId, latestTask.getId())
                    .apply("AND {0} = 0", claimedColumn);
            updateWrapper.setSql(claimedColumn + " = 1");
            taskMapper.update(null, updateWrapper);

            log.info("{}奖励发放成功: userId={}, points={}", description, userId, points);
        } catch (Exception e) {
            log.error("{}奖励发放失败，不更新claimed标志: userId={}, error={}", description, userId, e.getMessage(), e);
        }
    }
}
