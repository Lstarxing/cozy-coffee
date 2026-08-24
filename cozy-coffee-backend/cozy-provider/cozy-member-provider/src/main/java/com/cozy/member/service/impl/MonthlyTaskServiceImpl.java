package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cozy.common.constant.MonthlyChallengeConfig;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.MonthlyTaskService;
import com.cozy.member.dto.response.MonthlyChallengeDTO;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 月度任务服务实现
 *
 * 核心规则:
 * - 订单完成时自动更新月度消费
 * - 月度挑战任务（打卡达人/晨间唤醒/外卖尝鲜/新品猎人）达标自动发放积分
 * - 挑战配置（key/title/description/target/reward）单一事实源在 MonthlyChallengeConfig（cozy.member.monthly-challenge）
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class MonthlyTaskServiceImpl implements MonthlyTaskService {

    private final MonthlyTaskMapper taskMapper;
    private final MonthlyTaskOrderMapper orderMapper;
    private final MemberService memberService;
    private final TransactionTemplate transactionTemplate;

    // 挑战任务配置（单一事实源 @ConfigurationProperties，见 cozy.member.monthly-challenge）
    private final MonthlyChallengeConfig monthlyChallengeConfig;

    @DubboReference(check = false)
    private OrderService orderService;

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

    private MonthlyTask doMonthlySpentInTx(Long userId, Long orderId, String month, BigDecimal amount) {
        return transactionTemplate.execute(status -> {
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
        });
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
        MonthlyStatsDTO stats = null;
        try {
            if (orderService != null) {
                stats = orderService.getMonthlyStats(userId);
                if (stats != null) {
                    dto.setMonthlyOrderCount(stats.getOrderCount());
                    dto.setMorningOrderCount(stats.getMorningOrderCount());
                    dto.setCurrentDeliveryOrders(stats.getDeliveryOrderCount());
                    dto.setNewProductCount(stats.getNewProductCount());
                }
            }
        } catch (Exception e) {
            log.warn("获取月度订单统计失败: {}", e.getMessage());
        }

        // 挑战任务配置（单一事实源：含 target/reward，移动端不再硬编码）
        dto.setChallenges(buildChallenges(task, stats));

        return dto;
    }

    /** 按配置组装挑战任务列表（单一事实源：MonthlyChallengeConfig） */
    private List<MonthlyChallengeDTO> buildChallenges(MonthlyTask task, MonthlyStatsDTO stats) {
        List<MonthlyChallengeDTO> list = new ArrayList<>();
        if (task == null || stats == null) {
            return list;
        }
        for (MonthlyChallengeConfig.ChallengeItem item : monthlyChallengeConfig.getItems()) {
            list.add(challenge(item.getKey(), item.getTitle(), item.getDescription(),
                    item.getTarget(), item.getReward(),
                    statsCount(stats, item.getStatsField()),
                    isChallengeClaimed(task, item.getKey())));
        }
        return list;
    }

    /** 按 statsField 从月度订单统计取当前计数 */
    private int statsCount(MonthlyStatsDTO stats, String field) {
        if (stats == null) return 0;
        return switch (field) {
            case "orderCount" -> stats.getOrderCount();
            case "morningOrderCount" -> stats.getMorningOrderCount();
            case "deliveryOrderCount" -> stats.getDeliveryOrderCount();
            case "newProductCount" -> stats.getNewProductCount();
            default -> 0;
        };
    }

    /** 挑战 key → monthly_task 对应 claimed 字段 */
    private boolean isChallengeClaimed(MonthlyTask task, String key) {
        return switch (key) {
            case "order" -> Boolean.TRUE.equals(task.getChallengeOrderClaimed());
            case "morning" -> Boolean.TRUE.equals(task.getChallengeMorningClaimed());
            case "delivery" -> Boolean.TRUE.equals(task.getChallengeDeliveryClaimed());
            case "newproduct" -> Boolean.TRUE.equals(task.getChallengeNewproductClaimed());
            default -> false;
        };
    }

    /** 挑战 key → claimed 读取函数（乐观锁判定用） */
    private Function<MonthlyTask, Boolean> claimGetter(String key) {
        return switch (key) {
            case "order" -> MonthlyTask::getChallengeOrderClaimed;
            case "morning" -> MonthlyTask::getChallengeMorningClaimed;
            case "delivery" -> MonthlyTask::getChallengeDeliveryClaimed;
            case "newproduct" -> MonthlyTask::getChallengeNewproductClaimed;
            default -> t -> false;
        };
    }

    private MonthlyChallengeDTO challenge(String key, String title, String description, int target, int reward,
            int current, boolean claimed) {
        MonthlyChallengeDTO c = new MonthlyChallengeDTO();
        c.setKey(key);
        c.setTitle(title);
        c.setDescription(description);
        c.setTarget(target);
        c.setReward(reward);
        c.setCurrent(current);
        c.setClaimed(claimed);
        return c;
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

        log.info("月度挑战检查: userId={}, orderId={}, orderCount={}, morning={}, delivery={}, newProduct={}",
                userId, orderId, stats.getOrderCount(), stats.getMorningOrderCount(),
                stats.getDeliveryOrderCount(), stats.getNewProductCount());

        String month = YearMonth.now().toString();

        for (MonthlyChallengeConfig.ChallengeItem item : monthlyChallengeConfig.getItems()) {
            tryGrantChallenge(userId, month, orderId,
                    statsCount(stats, item.getStatsField()), item.getTarget(),
                    claimGetter(item.getKey()),
                    "challenge_" + item.getKey() + "_claimed",
                    "challenge_" + item.getKey(),
                    item.getReward(),
                    "挑战任务【" + item.getTitle() + "】完成奖励");
        }
    }

    /**
     * H5+H6: 挑战任务奖励通用发放逻辑。
     * 乐观锁 UPDATE WHERE id=? AND claimed=false 防并发双发，
     * DuplicateKeyException 兜底（Phase 2 已加固 consumer 侧）。
     */
    private void tryGrantChallenge(Long userId, String month, Long orderId,
            int actualCount, int threshold,
            Function<MonthlyTask, Boolean> claimedGetter,
            String claimedColumn, String sourceType, int points, String description) {
        if (actualCount < threshold) return;

        MonthlyTask latestTask = getOrCreateTask(userId, month);
        if (Boolean.TRUE.equals(claimedGetter.apply(latestTask))) return;

        try {
            memberService.addPointsWithLot(userId, points, sourceType, orderId, description);

            // H6: 乐观锁 — UPDATE monthly_task SET claimed=1 WHERE id=? AND claimed=0
            // 注意：claimedColumn 是内部常量列名，必须字面量内联。apply 的 {0} 是参数占位，
            // 会把列名当绑定值生成 'challenge_order_claimed'=0（恒假），且 "AND " 前缀会产生重复 AND ——
            // 原写法导致 claimed 永远更新失败，后续订单再达阈值会重复发放奖励。
            LambdaUpdateWrapper<MonthlyTask> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MonthlyTask::getId, latestTask.getId())
                    .apply(claimedColumn + " = 0");
            updateWrapper.setSql(claimedColumn + " = 1");
            taskMapper.update(null, updateWrapper);

            log.info("{}奖励发放成功: userId={}, points={}", description, userId, points);
        } catch (Exception e) {
            log.error("{}奖励发放失败，不更新claimed标志: userId={}, error={}", description, userId, e.getMessage(), e);
        }
    }
}
