package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.common.exception.BusinessException;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.PointsMallService;
import java.math.BigDecimal;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.dto.response.PointsTransactionDTO;
import com.cozy.member.entity.MemberInfo;
import com.cozy.member.entity.MonthlyTask;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.MonthlyStatsDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.response.UserDTO;
import com.cozy.member.entity.PointsLot;
import com.cozy.member.entity.PointsLotConsumption;
import com.cozy.member.entity.PointsTransaction;
import com.cozy.member.mapper.MemberInfoMapper;
import com.cozy.member.mapper.MonthlyTaskMapper;
import com.cozy.member.mapper.PointsLotConsumptionMapper;
import com.cozy.member.mapper.PointsLotMapper;
import com.cozy.member.mapper.PointsTransactionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 会员服务实现
 * v4.0: 支持 EXP/POINT 双账户、积分批次 FIFO、会员等级升级
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberInfoMapper memberInfoMapper;
    private final PointsTransactionMapper transactionMapper;
    private final PointsLotMapper pointsLotMapper;
    private final PointsLotConsumptionMapper consumptionMapper;
    private final MonthlyTaskMapper monthlyTaskMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 等级门槛（EXP）- v5.0 白皮书
    private static final int SILVER_THRESHOLD = 500; // 0-499 basic → 500-1499 silver
    private static final int GOLD_THRESHOLD = 1500; // 1500-3999 gold
    private static final int DIAMOND_THRESHOLD = 4000; // 4000-8999 diamond
    private static final int BLACK_THRESHOLD = 9000; // 9000+ black

    @DubboReference(check = false)
    private UserService userService;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    @DubboReference(check = false)
    private OrderService orderService;

    private final PlatformTransactionManager transactionManager;

    @Override
    public MemberDTO getMemberByUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        String cacheKey = RedisKeyConstants.memberProfileByUserId(userId);
        try {
            Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
            if (cachedObj != null) {
                if (cachedObj instanceof MemberDTO) {
                    return (MemberDTO) cachedObj;
                }
                if (cachedObj instanceof Map) {
                    return objectMapper.convertValue(cachedObj, MemberDTO.class);
                }
                if (cachedObj instanceof String) {
                    return objectMapper.readValue((String) cachedObj, MemberDTO.class);
                }
            }
        } catch (Exception e) {
            log.warn("读取Redis会员缓存失败: userId={}", userId, e);
        }

        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberInfo::getUserId, userId);
        MemberInfo info = memberInfoMapper.selectOne(wrapper);

        if (info == null) {
            log.info("会员信息不存在，自动创建: userId={}", userId);
            createMember(userId);
            info = memberInfoMapper.selectOne(wrapper);
        }

        MemberDTO dto = new MemberDTO();
        dto.setId(info.getId());
        dto.setUserId(info.getUserId());

        int expTotal = info.getExpTotal() != null ? info.getExpTotal() : 0;
        dto.setExpTotal(expTotal);
        dto.setCurrentPoints(info.getCurrentPoints());
        dto.setTotalPoints(info.getTotalPoints());

        // 实时根据 EXP 计算等级（而非读取存储值）
        String computedLevel = computeLevelByExp(expTotal);
        dto.setMemberLevel(computedLevel);

        // 如果计算出的等级与存储值不同，同步更新数据库
        if (!computedLevel.equals(info.getMemberLevel())) {
            info.setMemberLevel(computedLevel);
            memberInfoMapper.updateById(info);
            log.info("等级自动同步: userId={}, exp={}, oldLevel={}, newLevel={}",
                    userId, expTotal, info.getMemberLevel(), computedLevel);
        }

        dto.setLastSigninDate(info.getLastSigninDate());
        dto.setConsecutiveSignDays(info.getConsecutiveSignDays());

        // 月度消费统计
        dto.setMonthlySpent(info.getMonthlySpent() != null ? info.getMonthlySpent() : BigDecimal.ZERO);
        dto.setMonthlySpentMonth(info.getMonthlySpentMonth());
        dto.setMonthlyAccelerateRemaining(
                info.getMonthlyAccelerateRemaining() != null ? info.getMonthlyAccelerateRemaining()
                        : new BigDecimal("300"));

        // 即将到期积分（近30天）
        dto.setExpiringPoints(getExpiringPoints(userId, 30));

        // 获取优惠券数量（可用状态）
        try {
            if (pointsMallService != null) {
                var coupons = pointsMallService.getUserCoupons(userId, "available");
                if (coupons != null) {
                    dto.setCouponCount(coupons.size());
                    // 统计 EXCHANGE 类型的券数量
                    long exchangeCount = coupons.stream()
                            .filter(c -> "EXCHANGE".equals(c.getCouponType()))
                            .count();
                    dto.setExchangeCouponCount((int) exchangeCount);
                } else {
                    dto.setCouponCount(0);
                    dto.setExchangeCouponCount(0);
                }
            }
        } catch (Exception e) {
            log.warn("获取用户优惠券数量失败: userId={}", userId, e);
            dto.setCouponCount(0);
            dto.setExchangeCouponCount(0);
        }

        // Populate User Info (Nickname, Phone, Avatar) via UserService
        try {
            if (userService != null) {
                UserDTO user = userService.getUserById(userId);
                if (user != null) {
                    dto.setNickname(user.getNickname());
                    dto.setPhone(user.getPhone());
                    dto.setAvatar(user.getAvatar());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user info for member: userId={}, error={}", userId, e.getMessage());
        }

        // v5.3: Populate Monthly Challenge Stats (兜底数据源)
        try {
            if (orderService != null) {
                MonthlyStatsDTO stats = orderService.getMonthlyStats(userId);
                if (stats != null) {
                    dto.setMonthlyOrderCount(stats.getOrderCount());
                    dto.setMorningOrderCount(stats.getMorningOrderCount());
                    dto.setMonthlyDeliveryOrders(stats.getDeliveryOrderCount());
                    dto.setNewProductCount(stats.getNewProductCount());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch monthly stats for member: userId={}, error={}", userId, e.getMessage());
        }

        // v5.3: Populate Monthly Challenge Status (只查询，不发放奖励)
        try {
            if (monthlyTaskMapper != null) {
                String month = java.time.YearMonth.now().toString();
                LambdaQueryWrapper<MonthlyTask> taskWrapper = new LambdaQueryWrapper<>();
                taskWrapper.eq(MonthlyTask::getUserId, userId)
                        .eq(MonthlyTask::getTaskMonth, month);
                MonthlyTask task = monthlyTaskMapper.selectOne(taskWrapper);

                if (task != null) {
                    // 仅填充任务完成状态，不进行补发逻辑
                    dto.setChallengeOrderClaimed(task.getChallengeOrderClaimed());
                    dto.setChallengeMorningClaimed(task.getChallengeMorningClaimed());
                    dto.setChallengeDeliveryClaimed(task.getChallengeDeliveryClaimed());
                    dto.setChallengeNewproductClaimed(task.getChallengeNewproductClaimed());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch monthly task status for member: userId={}, error={}", userId, e.getMessage());
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, dto, 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入Redis会员缓存失败: userId={}", userId, e);
        }

        return dto;
    }

    @Override
    public Map<Long, MemberDTO> getMembersByUserIds(Set<Long> userIds) {
        Map<Long, MemberDTO> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        log.info("批量获取会员信息: userIds count={}", userIds.size());

        // 1. 批量查询会员基础信息
        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MemberInfo::getUserId, userIds);
        List<MemberInfo> members = memberInfoMapper.selectList(wrapper);

        // 2. 批量查询用户信息 (nickname, phone, avatar) - 单次 Dubbo RPC
        Map<Long, UserDTO> userMap = new HashMap<>();
        try {
            if (userService != null) {
                List<UserDTO> users = userService.getUsersByIds(userIds);
                if (users != null) {
                    for (UserDTO user : users) {
                        if (user != null && user.getId() != null) {
                            userMap.put(user.getId(), user);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("批量获取用户信息失败: {}", e.getMessage());
        }

        // 3. 组装 DTO
        for (MemberInfo info : members) {
            MemberDTO dto = new MemberDTO();
            dto.setId(info.getId());
            dto.setUserId(info.getUserId());
            dto.setExpTotal(info.getExpTotal());
            dto.setCurrentPoints(info.getCurrentPoints());
            dto.setTotalPoints(info.getTotalPoints());
            dto.setMemberLevel(info.getMemberLevel());

            // 填充用户信息
            UserDTO user = userMap.get(info.getUserId());
            if (user != null) {
                dto.setNickname(user.getNickname());
                dto.setPhone(user.getPhone());
                dto.setAvatar(user.getAvatar());
            }

            result.put(info.getUserId(), dto);
        }

        log.info("批量获取会员信息完成: 查询{}个, 返回{}个", userIds.size(), result.size());
        return result;
    }

    @Override
    public void createMember(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        // 检查是否已存在
        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberInfo::getUserId, userId);
        if (memberInfoMapper.selectCount(wrapper) > 0) {
            return;
        }

        // v5.3: 新用户注册不再直接赠送积分，改为首单完成后赠送
        // int registerBonus = 200; -> 0
        int registerBonus = 0;

        MemberInfo info = new MemberInfo();
        info.setUserId(userId);
        info.setMemberLevel("basic");
        info.setTotalPoints(0);
        info.setCurrentPoints(0);
        info.setExpTotal(0); // EXP 初始为 0
        info.setConsecutiveSignDays(0);
        memberInfoMapper.insert(info);
        evictMemberProfileCache(userId);

        // v5.3: 初始积分为0，不需要创建积分批次和流水
        log.info("会员创建成功(初始无积分): userId={}", userId);
    }

    @Override
    @Transactional
    public void addPoints(Long userId, int points, String sourceType, String description) {
        if (userId == null || points == 0)
            return;

        // 如果是管理员调整，直接导流到新逻辑确保一致性
        if ("admin_adjust".equals(sourceType)) {
            adminAdjustPoints(userId, points, description);
            return;
        }

        // 其他旧逻辑调用（如退款等非 lot 模式，理论上以后应全部改为 lot 模式）
        MemberInfo member = memberInfoMapper.selectByUserIdForUpdate(userId);
        if (member == null) {
            throw new BusinessException("会员信息不存在");
        }

        if (points < 0 && member.getCurrentPoints() < Math.abs(points)) {
            throw new BusinessException("积分不足");
        }

        member.setCurrentPoints(member.getCurrentPoints() + points);
        if (points > 0) {
            member.setTotalPoints(member.getTotalPoints() + points);
        }
        memberInfoMapper.updateById(member);

        recordTransaction(userId, points, member.getCurrentPoints(), sourceType, null, description);
        evictMemberProfileCache(userId);
    }

    @Override
    @Transactional
    public void adminAdjustPoints(Long userId, int delta, String reason) {
        if (userId == null || delta == 0)
            return;

        log.info("管理员调整积分开始: userId={}, delta={}, reason={}", userId, delta, reason);
        // 1. 悲观锁 MemberInfo
        MemberInfo member = memberInfoMapper.selectByUserIdForUpdate(userId);
        if (member == null) {
            throw new BusinessException("会员信息不存在");
        }

        if (delta > 0) {
            // 加分逻辑：创建新批次
            addPointsWithLotInternal(member, delta, "admin", null, reason);
        } else {
            // 扣分逻辑：FIFO 扣减现存批次
            int absDelta = Math.abs(delta);
            if (member.getCurrentPoints() < absDelta) {
                throw new BusinessException("积分不足，无法完成调整（当前余额: " + member.getCurrentPoints() + "）");
            }

            // 执行 FIFO 扣减
            consumePointsFIFOInternal(member, absDelta, "admin_debit", null, reason);
        }

        // 自动检查并更新等级
        checkAndUpgradeLevel(userId);
        evictMemberProfileCache(userId);
        log.info("管理员调整积分成功: userId={}, newPoints={}", userId, member.getCurrentPoints());
    }

    @Override
    public void fixPointsConsistency(Long userId) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        if (userId == null) {
            // 全局修复：每个用户独立事务，单个失败不影响其他用户
            List<Long> inconsistentUserIds = memberInfoMapper.findInconsistentUserIds();
            log.info("开始全局一致性修复，预计用户数: {}", inconsistentUserIds.size());
            for (Long uid : inconsistentUserIds) {
                try {
                    txTemplate.executeWithoutResult(status -> doRepairUserPoints(uid));
                } catch (Exception e) {
                    log.error("修复用户 {} 积分失败，跳过: {}", uid, e.getMessage());
                }
            }
            return;
        }
        txTemplate.executeWithoutResult(status -> doRepairUserPoints(userId));
    }

    private void doRepairUserPoints(Long userId) {
        MemberInfo member = memberInfoMapper.selectByUserIdForUpdate(userId);
        if (member == null)
            return;

        // 计算 Lot 总额（锁批次）
        List<PointsLot> lots = pointsLotMapper.selectAvailableLotsForUpdate(userId);
        int sumLots = lots.stream().mapToInt(PointsLot::getRemaining).sum();
        int currentPoints = member.getCurrentPoints();

        int diff = currentPoints - sumLots;
        if (diff == 0)
            return;

        log.info("发现积分不一致: userId={}, member.current={}, lots.sum={}, diff={}", userId, currentPoints, sumLots, diff);

        if (diff > 0) {
            // 补齐缺失的 Lot
            PointsLot lot = new PointsLot();
            lot.setUserId(userId);
            lot.setInitialAmount(diff);
            lot.setRemaining(diff);
            lot.setSourceType("admin_repair");
            lot.setExpiresAt(LocalDateTime.now().plusDays(365));
            lot.setCreatedAt(LocalDateTime.now());
            pointsLotMapper.insert(lot);

            recordTransaction(userId, diff, currentPoints, "admin_repair", null, "一致性修复：补齐缺失的积分批次");
        } else {
            // 消耗多余的 Lot (不影响 current_points)
            int remainingToConsume = Math.abs(diff);
            for (PointsLot lot : lots) {
                if (remainingToConsume <= 0)
                    break;
                int consume = Math.min(remainingToConsume, lot.getRemaining());
                lot.setRemaining(lot.getRemaining() - consume);
                pointsLotMapper.updateById(lot);
                remainingToConsume -= consume;
            }
            recordTransaction(userId, 0, currentPoints, "admin_repair", null, "一致性修复：清理多余的积分批次 (" + Math.abs(diff) + ")");
        }
    }

    @Override
    @Transactional
    public void addPointsWithLot(Long userId, int points, String sourceType, Long sourceId, String description) {
        if (userId == null || points <= 0)
            return;

        MemberInfo member = memberInfoMapper.selectByUserIdForUpdate(userId);
        if (member == null) {
            throw new BusinessException("会员信息不存在");
        }

        addPointsWithLotInternal(member, points, sourceType, sourceId, description);
    }

    private void addPointsWithLotInternal(MemberInfo member, int points, String sourceType, Long sourceId,
            String description) {
        Long userId = member.getUserId();

        // 幂等性检查：如果已存在相同 source_type + source_id 的交易记录，直接返回
        if (sourceId != null && sourceId > 0) {
            LambdaQueryWrapper<PointsTransaction> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PointsTransaction::getUserId, userId)
                   .eq(PointsTransaction::getSourceType, sourceType)
                   .eq(PointsTransaction::getSourceId, sourceId)
                   .last("LIMIT 1");
            PointsTransaction existing = transactionMapper.selectOne(wrapper);
            if (existing != null) {
                log.info("积分已发放过，跳过: userId={}, sourceType={}, sourceId={}", userId, sourceType, sourceId);
                return;
            }
        }

        // 1. 更新主表余额
        member.setCurrentPoints(member.getCurrentPoints() + points);
        member.setTotalPoints(member.getTotalPoints() + points);
        memberInfoMapper.updateById(member);

        // 2. 创建积分批次（365天有效期）
        PointsLot lot = new PointsLot();
        lot.setUserId(userId);
        lot.setInitialAmount(points);
        lot.setRemaining(points);
        lot.setSourceType(sourceType);
        lot.setSourceId(sourceId);
        lot.setExpiresAt(LocalDateTime.now().plusDays(365));
        lot.setCreatedAt(LocalDateTime.now());
        pointsLotMapper.insert(lot);

        // 3. 记录积分流水（包含 sourceId 用于唯一约束）
        recordTransaction(userId, points, member.getCurrentPoints(), sourceType, sourceId, description);
        evictMemberProfileCache(userId);

        log.info("积分发放成功: userId={}, points={}, sourceType={}, sourceId={}, lotId={}", 
                 userId, points, sourceType, sourceId, lot.getId());
    }

    @Override
    @Transactional
    public void addExp(Long userId, int exp, Long orderId) {
        if (userId == null || exp <= 0) {
            return;
        }

        MemberInfo member = memberInfoMapper.selectByUserIdForUpdate(userId);
        if (member == null)
            return;

        int oldExp = member.getExpTotal() != null ? member.getExpTotal() : 0;
        member.setExpTotal(oldExp + exp);

        // ============================================
        // 逻辑修正：加速包不再直接由 (300 - 当月总消费) 计算
        // 改为：仅在用户是“黑卡”时，或者本次消费导致升级为“黑卡”时，扣减额度
        // ============================================
        String currentMonth = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")
                .format(java.time.LocalDate.now());

        // 跨月处理：如果是新月份，先把加速包重置为 300
        if (!currentMonth.equals(member.getMonthlySpentMonth())) {
            member.setMonthlySpentMonth(currentMonth);
            member.setMonthlySpent(BigDecimal.ZERO);
            member.setMonthlyAccelerateRemaining(new BigDecimal("300"));
        }

        int newExpTotal = oldExp + exp;
        BigDecimal payAmount = new BigDecimal(exp);

        // 计算本次消费中落在“黑卡”区间的部分
        BigDecimal acceleratedSpentInThisOrder = BigDecimal.ZERO;
        if (oldExp >= BLACK_THRESHOLD) {
            // 原本就是黑卡，全部消费都在加速区间
            acceleratedSpentInThisOrder = payAmount;
        } else if (newExpTotal > BLACK_THRESHOLD) {
            // 本次升级黑卡，仅计算超出 10000 的部分
            acceleratedSpentInThisOrder = new BigDecimal(newExpTotal - BLACK_THRESHOLD);
        }

        // 仅当有“黑卡区间”消费时才扣减加速包
        if (acceleratedSpentInThisOrder.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remaining = member.getMonthlyAccelerateRemaining() != null
                    ? member.getMonthlyAccelerateRemaining()
                    : new BigDecimal("300");
            member.setMonthlyAccelerateRemaining(remaining.subtract(acceleratedSpentInThisOrder).max(BigDecimal.ZERO));
        }

        // 同步更新当月总消费统计（仅用于显示）
        member.setMonthlySpent(member.getMonthlySpent().add(payAmount));

        // 更新累计 EXP 和 数据库记录
        member.setExpTotal(newExpTotal);
        memberInfoMapper.updateById(member);

        // 检查并升级等级（确保 newLevel 正确存储）
        checkAndUpgradeLevel(userId);
        evictMemberProfileCache(userId);

        log.info("EXP结算成功: userId={}, 当前EXP={}, 当月累计消费={}, 加速包剩余={}",
                userId, member.getExpTotal(), member.getMonthlySpent(), member.getMonthlyAccelerateRemaining());
    }

    @Override
    @Transactional
    public boolean consumePointsFIFO(Long userId, int points, String consumeType, Long consumeId) {
        if (userId == null || points <= 0) {
            return false;
        }

        MemberInfo member = memberInfoMapper.selectByUserIdForUpdate(userId);
        if (member == null) {
            throw new BusinessException("会员信息不存在");
        }

        if (member.getCurrentPoints() < points) {
            throw new BusinessException("积分不足，当前积分: " + member.getCurrentPoints());
        }

        consumePointsFIFOInternal(member, points, consumeType, consumeId, "积分兑换扣减");
        return true;
    }

    private void consumePointsFIFOInternal(MemberInfo member, int points, String consumeType, Long consumeId,
            String description) {
        Long userId = member.getUserId();

        // =============================================
        // 【FIFO排序】获取可用批次，SQL层面保证 expires_at ASC, id ASC
        // 使用 FOR UPDATE 加行锁，确保并发安全
        // =============================================
        List<PointsLot> lots = pointsLotMapper.selectAvailableLotsForUpdate(userId);

        int remainingToConsume = points;

        // =============================================
        // 【跨批次扣减】循环扣减直到 remainingToConsume 归零
        // =============================================
        for (PointsLot lot : lots) {
            if (remainingToConsume <= 0)
                break;

            // 计算本批次应扣减的份额
            int consume = Math.min(remainingToConsume, lot.getRemaining());

            // =============================================
            // 【CAS并发保护】使用原子扣减，防止并发超扣
            // SQL: UPDATE ... SET remaining = remaining - ? WHERE id = ? AND remaining >= ?
            // =============================================
            int affected = pointsLotMapper.casDeductRemaining(lot.getId(), consume);
            if (affected == 0) {
                // CAS 失败：该批次已被其他线程抢占，跳过并尝试下一个批次
                log.warn("CAS扣减失败(已被抢占): lotId={}, attemptedDeduct={}", lot.getId(), consume);
                continue;
            }

            // 记录扣减明细
            PointsLotConsumption consumption = new PointsLotConsumption();
            consumption.setUserId(userId);
            consumption.setLotId(lot.getId());
            consumption.setConsumeAmount(consume);
            consumption.setConsumeType(consumeType);
            consumption.setConsumeId(consumeId);
            consumption.setCreatedAt(LocalDateTime.now());
            consumptionMapper.insert(consumption);

            remainingToConsume -= consume;

            log.debug("FIFO扣减: lotId={}, consume={}, lotRemaining={}",
                    lot.getId(), consume, lot.getRemaining() - consume);
        }

        if (remainingToConsume > 0) {
            // 若所有批次都不够扣（极端情况），强制报错触发回滚
            throw new BusinessException(
                    "积分批次余额不足(已扣" + (points - remainingToConsume) + "，还需" + remainingToConsume + ")，请稍后重试");
        }

        // 2. 更新主表余额
        member.setCurrentPoints(member.getCurrentPoints() - points);
        memberInfoMapper.updateById(member);

        // 3. 记录积分流水
        recordTransaction(userId, -points, member.getCurrentPoints(), consumeType, null, description);
        evictMemberProfileCache(userId);

        log.info("FIFO 积分扣减成功: userId={}, points={}", userId, points);
    }

    @Override
    public int getExpiringPoints(Long userId, int days) {
        if (userId == null || days <= 0) {
            return 0;
        }

        LocalDateTime deadline = LocalDateTime.now().plusDays(days);
        LambdaQueryWrapper<PointsLot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsLot::getUserId, userId)
                .gt(PointsLot::getRemaining, 0)
                .gt(PointsLot::getExpiresAt, LocalDateTime.now())
                .le(PointsLot::getExpiresAt, deadline);
        List<PointsLot> lots = pointsLotMapper.selectList(wrapper);

        return lots.stream().mapToInt(PointsLot::getRemaining).sum();
    }

    @Override
    public List<PointsTransactionDTO> getPointsTransactions(Long userId, int limit) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        LambdaQueryWrapper<PointsTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsTransaction::getUserId, userId)
                .orderByDesc(PointsTransaction::getCreatedAt);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PointsTransaction> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, safeLimit);
        return transactionMapper.selectPage(page, wrapper).getRecords().stream()
                .map(this::toTransactionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void checkAndUpgradeLevel(Long userId) {
        if (userId == null) {
            return;
        }

        MemberInfo member = getMemberInfoByUserId(userId);
        int expTotal = member.getExpTotal() != null ? member.getExpTotal() : 0;
        String currentLevel = member.getMemberLevel();
        String newLevel;

        if (expTotal >= BLACK_THRESHOLD) {
            newLevel = "black";
        } else if (expTotal >= DIAMOND_THRESHOLD) {
            newLevel = "diamond";
        } else if (expTotal >= GOLD_THRESHOLD) {
            newLevel = "gold";
        } else if (expTotal >= SILVER_THRESHOLD) {
            newLevel = "silver";
        } else {
            newLevel = "basic";
        }

        if (!newLevel.equals(currentLevel)) {
            // Update level first
            member.setMemberLevel(newLevel);
            memberInfoMapper.updateById(member);
            evictMemberProfileCache(userId);
            log.info("会员等级升级: userId={}, {} -> {}, exp={}", userId, currentLevel, newLevel, expTotal);

            // v5.3: 发放晋升礼包 (One-off)
            // 只有在"升级"（级别变高）时才发放，降级不发。
            // 简单的等级比较：basic < silver < gold < diamond < black
            // 由于 newLevel != currentLevel，且代码结构是已根据 EXP 算出的 newLevel，
            // 如果 expTotal 增加了导致升级，这里会触发。
            // 但如果是 periodic settlement 导致的降级，也会触发 update。
            // 只有当 newLevel 对应的 exp 阈值 > currentLevel 对应的 exp 阈值时才是升级。
            // 简单判定：利用阈值比较
            int currentThreshold = getThresholdByLevel(currentLevel);
            int newThreshold = getThresholdByLevel(newLevel);

            if (newThreshold > currentThreshold) {
                grantUpgradeReward(userId, newLevel);
            }
        }
    }

    private int getThresholdByLevel(String level) {
        return switch (level) {
            case "black" -> BLACK_THRESHOLD;
            case "diamond" -> DIAMOND_THRESHOLD;
            case "gold" -> GOLD_THRESHOLD;
            case "silver" -> SILVER_THRESHOLD;
            default -> 0;
        };
    }

    private void grantUpgradeReward(Long userId, String level) {
        String uniqueKey = "upgrade_" + level + "_" + userId;
        try {
            switch (level) {
                case "silver" -> {
                    // 白银: 单饮品5折券 - 限单杯，全场饮品，最高抵¥20，有效期60天
                    pointsMallService.issueCouponToUser(userId, "UPGRADE_SILVER_DISCOUNT", uniqueKey, 0, 50, 60);
                    log.info("获得白银晋升礼包: userId={}, 单饮品5折券(最高抵¥20)", userId);
                }
                case "gold" -> {
                    // 黄金: 买一赠一券(赠品杯最高抵¥40) + 50积分，有效期60天
                    pointsMallService.issueCouponToUser(userId, "UPGRADE_GOLD_BOGO", uniqueKey + "_bogo", 0, 40, 60);
                    grantOneOffPoints(userId, 50, uniqueKey, "黄金晋升奖励");
                    log.info("获得黄金晋升礼包: userId={}, BOGO券(赠品杯最高¥40) + 50积分", userId);
                }
                case "diamond" -> {
                    // 钻石: 标准饮品免单券(仅限标准杯，排除SOE/手冲，最高¥40) + 100积分，有效期60天
                    pointsMallService.issueCouponToUser(userId, "UPGRADE_DIAMOND_STANDARD_FREE", uniqueKey + "_free", 0, 40, 60);
                    grantOneOffPoints(userId, 100, uniqueKey, "钻石晋升奖励");
                    log.info("获得钻石晋升礼包: userId={}, 标准饮品免单券(限标准杯，排除SOE) + 100积分", userId);
                }
                case "black" -> {
                    // v5.6 T4_ULT_FREE: 黑金晋升礼 - 尊享通兑券(不限杯型，全品类含SOE，含1份免费加料，无上限) + 1688积分
                    pointsMallService.issueCouponToUser(userId, "UPGRADE_BLACK_PREMIUM", uniqueKey + "_premium", 0, 999, 60);
                    grantOneOffPoints(userId, 1688, uniqueKey, "黑金晋升奖励");
                    log.info("获得黑金晋升礼包: userId={}, 尊享通兑券(T4_ULT_FREE) + 1688积分", userId);
                }
            }
        } catch (Exception e) {
            // Unique Index 冲突意味着已领取过 (One-off)，忽略
            log.info("晋升礼包已领取或发放失败(One-off): userId={}, level={}", userId, level);
        }
    }

    private void grantOneOffPoints(Long userId, int points, String uniqueSourceIdStr, String desc) {
        // 使用 SHA-256 截取 8 字节生成稳定的正 long，避免 String.hashCode() 碰撞
        long sourceId;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(uniqueSourceIdStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            sourceId = 0;
            for (int i = 0; i < 8; i++) {
                sourceId = (sourceId << 8) | (hash[i] & 0xFF);
            }
            sourceId = Math.abs(sourceId);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }

        // 通过 sourceType + sourceId 进行幂等检查
        LambdaQueryWrapper<PointsTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsTransaction::getUserId, userId)
                .eq(PointsTransaction::getSourceType, "upgrade_reward")
                .eq(PointsTransaction::getSourceId, sourceId);

        if (transactionMapper.selectCount(wrapper) == 0) {
            addPointsWithLot(userId, points, "upgrade_reward", sourceId, desc);
        }
    }

    // ==================== 辅助方法 ====================

    private MemberInfo getMemberInfoByUserId(Long userId) {
        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberInfo::getUserId, userId);
        MemberInfo member = memberInfoMapper.selectOne(wrapper);

        if (member == null) {
            // 自动创建会员信息（用于手动插入数据库的用户）
            log.info("会员信息不存在，自动创建: userId={}", userId);
            member = new MemberInfo();
            member.setUserId(userId);
            member.setMemberLevel("basic");
            member.setCurrentPoints(0);
            member.setTotalPoints(0);
            member.setExpTotal(0);
            member.setConsecutiveSignDays(0);
            memberInfoMapper.insert(member);
        }

        return member;
    }

    private void recordTransaction(Long userId, int amount, int balanceAfter, String type, Long sourceId, String desc) {
        PointsTransaction t = new PointsTransaction();
        t.setUserId(userId);
        t.setChangeAmount(amount);
        t.setBalanceAfter(balanceAfter);
        t.setSourceType(type);
        t.setSourceId(sourceId); // 关联ID，用于唯一约束防重
        t.setDescription(desc);
        t.setCreatedAt(LocalDateTime.now());
        transactionMapper.insert(t);
    }

    private PointsTransactionDTO toTransactionDTO(PointsTransaction entity) {
        PointsTransactionDTO dto = new PointsTransactionDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setChangeAmount(entity.getChangeAmount());
        dto.setBalanceAfter(entity.getBalanceAfter());
        dto.setSourceType(entity.getSourceType());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private void evictMemberProfileCache(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(RedisKeyConstants.memberProfileByUserId(userId));
        } catch (Exception e) {
            log.warn("清理Redis会员缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 根据 EXP 计算会员等级
     */
    private String computeLevelByExp(int expTotal) {
        if (expTotal >= BLACK_THRESHOLD) {
            return "black";
        } else if (expTotal >= DIAMOND_THRESHOLD) {
            return "diamond";
        } else if (expTotal >= GOLD_THRESHOLD) {
            return "gold";
        } else if (expTotal >= SILVER_THRESHOLD) {
            return "silver";
        } else {
            return "basic";
        }
    }

    /**
     * 每日凌晨 00:30 执行生日福利发放
     */
    /**
     * 每日凌晨 00:30 执行检查
     * 修正：每月1日统一发放当月所有生日用户的福利
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 30 0 * * ?")
    public void processBirthdayRewards() {
        log.info("开始处理生日福利发放任务...");
        java.time.LocalDate today = java.time.LocalDate.now();

        // 需求：生日月 1 号自动发放
        if (today.getDayOfMonth() != 1) {
            return;
        }

        int month = today.getMonthValue();
        int year = today.getYear();
        log.info("每月1日，开始发放本月({})所有会员生日福利", month);

        try {
            // 遍历 1-31 日查找本月所有生日用户（聚合所有当月寿星）
            Set<Long> userIds = new HashSet<>();
            for (int d = 1; d <= 31; d++) {
                try {
                    List<Long> ids = userService.findUsersByBirthday(month, d);
                    if (ids != null)
                        userIds.addAll(ids);
                } catch (Exception ignore) {
                    // 忽略无效日期（如2月30日）
                }
            }

            if (userIds.isEmpty()) {
                log.info("本月无过生日用户");
                return;
            }

            log.info("本月共有 {} 位寿星", userIds.size());
            for (Long userId : userIds) {
                try {
                    // grantBirthdayRewardInternal 会处理幂等性（birthday_userId_year）
                    grantBirthdayRewardInternal(userId, year);
                } catch (Exception e) {
                    log.error("发放生日福利失败: userId={}, error={}", userId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("处理本月生日福利任务失败", e);
        }
    }

    /**
     * 设置生日时立即发放权益包（供 UserService 调用）
     * 配置：500 积分 (后续可从 system_config 表读取)
     * 幂等性：source_type=birthday_gift, source_id=userId+year 确保每年只能领取一次
     * 
     * @return true 如果发放成功，false 如果已领取过
     */
    @Override
    public boolean grantBirthdayReward(Long userId) {
        if (userId == null) {
            log.warn("grantBirthdayReward: userId is null");
            return false;
        }

        int currentYear = java.time.LocalDate.now().getYear();
        try {
            grantBirthdayRewardInternal(userId, currentYear);
            log.info("生日权益包发放成功: userId={}, year={}", userId, currentYear);
            return true;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                log.info("用户已领取过今年的生日权益: userId={}", userId);
            } else {
                log.error("生日权益包发放失败: userId={}, error={}", userId, e.getMessage());
            }
            return false;
        }
    }

    /**
     * 内部方法：发放生日权益 (v5.3 阶梯权益)
     * - basic: 单饮品5折券 ×1 (限标准杯)
     * - silver: 买一赠一券(BOGO) ×1
     * - gold: 标准饮品免单券 ×1 (不可选特调/SOE)
     * - diamond: 全通兑免单券 ×1 + 切片蛋糕5折券 ×1
     * - black: 全通兑免单券 ×1 + 免费切片蛋糕券 ×1 + 888积分
     */
    private void grantBirthdayRewardInternal(Long userId, int year) {
        // 获取会员等级
        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberInfo::getUserId, userId);
        MemberInfo member = memberInfoMapper.selectOne(wrapper);
        String level = (member != null) ? computeLevelByExp(member.getExpTotal()) : "basic";
        int currentPoints = (member != null) ? member.getCurrentPoints() : 0;

        // 幂等性ID：用户ID_年份
        String baseKey = "birthday_" + userId + "_" + year;
        long sourceId = Math.abs((long) baseKey.hashCode());

        try {
            switch (level) {
                case "black" -> {
                    // v5.6 T3_ALL_FREE: 黑金生日全通兑免单券×1(不限杯型，含特调，排除SOE，封顶¥40) + 免费切片蛋糕券×1 + 888积分
                    pointsMallService.issueCouponToUser(userId, "BIRTHDAY_BLACK_FREE", baseKey + "_free", 0, 40, 30);
                    pointsMallService.issueCouponToUser(userId, "BIRTHDAY_FREE_CAKE", baseKey + "_cake", 0, 40, 30);
                    addPointsWithLot(userId, 888, "birthday_gift", sourceId, "🎂 生日快乐！黑金会员专属888积分贺礼已发放");
                    log.info("黑金生日权益发放: userId={}, 全通兑免单券(¥40封顶)+免费蛋糕券+888积分", userId);
                }
                case "diamond" -> {
                    // v5.6 T2_PRE_FREE: 钻石生日优选饮品免单券×1(限标准杯，含特调，排除SOE，封顶¥40) + 蛋糕5折券×1
                    pointsMallService.issueCouponToUser(userId, "BIRTHDAY_DIAMOND_FREE", baseKey + "_free", 0, 40, 30);
                    pointsMallService.issueCouponToUser(userId, "BIRTHDAY_CAKE_HALF", baseKey + "_cake_half", 0, 50, 30);
                    recordTransaction(userId, 0, currentPoints, "birthday_gift", null, "生日快乐！钻石会员获赠优选饮品免单券及蛋糕5折券");
                    log.info("钻石生日权益发放: userId={}, 优选饮品免单券(¥40封顶)+蛋糕5折券", userId);
                }
                case "gold" -> {
                    // v5.6 T1_STD_FREE: 黄金生日标准饮品免单券×1(限标准杯，排除特调&SOE，封顶¥40)
                    pointsMallService.issueCouponToUser(userId, "BIRTHDAY_GOLD_FREE", baseKey + "_free", 0, 40, 30);
                    recordTransaction(userId, 0, currentPoints, "birthday_gift", null, "生日快乐！黄金会员获赠标准饮品免单券");
                    log.info("黄金生日权益发放: userId={}, 标准饮品免单券x1(最高抵扣¥40)", userId);
                }
                case "silver" -> {
                    // v5.6 T5_BOGO: 白银生日买一赠一券×1(全品类，赠品封顶¥40)
                    pointsMallService.issueCouponToUser(userId, "BIRTHDAY_SILVER_BOGO", baseKey + "_bogo", 0, 40, 30);
                    recordTransaction(userId, 0, currentPoints, "birthday_gift", null, "生日快乐！白银会员获赠买一赠一券");
                    log.info("白银生日权益发放: userId={}, 买一赠一券x1(全场饮品)", userId);
                }
                default -> {
                    // v5.6 T6_50_OFF Override: 基础会员生日5折券×1(限标准杯，封顶¥20)
                    pointsMallService.issueCouponToUser(userId, "BIRTHDAY_BASIC_DISCOUNT", baseKey + "_discount", 0, 50, 30);
                    recordTransaction(userId, 0, currentPoints, "birthday_gift", null, "生日快乐！获赠单饮品5折券(限标准杯)");
                    log.info("基础生日权益发放: userId={}, 单饮品5折券x1(限标准杯)", userId);
                }
            }
        } catch (Exception e) {
            log.warn("生日权益发放失败: userId={}, level={}, error={}", userId, level, e.getMessage());
        }
    }

    // ==================== v5.0 保级/休眠机制 ====================

    // 保级门槛（年度 EXP）
    private static final int SILVER_KEEP_THRESHOLD = 300;
    private static final int GOLD_KEEP_THRESHOLD = 1000;
    private static final int DIAMOND_KEEP_THRESHOLD = 2500;
    private static final int BLACK_KEEP_THRESHOLD = 4000;

    // 唤醒门槛（单月 EXP）
    private static final int DIAMOND_AWAKEN_THRESHOLD = 600;
    private static final int BLACK_AWAKEN_THRESHOLD = 800;

    /**
     * v5.0: 年度保级判定（每年1月1日凌晨执行）
     * 仅修改状态位而不改变等级字段，实现"休眠"而非"降级"
     * 分页处理，避免一次性加载所有会员导致 OOM
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 1 1 ?")
    @Transactional
    public void processYearlySettlement() {
        int currentYear = java.time.LocalDate.now().getYear();
        int lastYear = currentYear - 1;

        log.info("开始执行 {} 年度保级判定...", lastYear);

        int processedCount = 0;
        int dormantCount = 0;
        int pageNum = 1;
        final int pageSize = 500;

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<MemberInfo> page;
        do {
            page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
            LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByAsc(MemberInfo::getId);
            memberInfoMapper.selectPage(page, wrapper);

            List<MemberInfo> members = page.getRecords();
            if (members == null || members.isEmpty()) {
                break;
            }

            for (MemberInfo member : members) {
                try {
                    // 跳过已结算过的
                    if (member.getLastSettlementYear() != null && member.getLastSettlementYear() >= lastYear) {
                        continue;
                    }

                    int annualExp = member.getAnnualExp() != null ? member.getAnnualExp() : 0;
                    String level = member.getMemberLevel();
                    boolean needDormant = false;

                    switch (level) {
                        case "black" -> {
                            if (annualExp < BLACK_KEEP_THRESHOLD) {
                                // 进入黑金休眠态，保持 4500 EXP
                                member.setMemberStatus("DORMANT");
                                member.setExpTotal(4500);
                                needDormant = true;
                                log.info("黑金休眠: userId={}, annualExp={}", member.getUserId(), annualExp);
                            }
                        }
                        case "diamond" -> {
                            if (annualExp < DIAMOND_KEEP_THRESHOLD) {
                                // 降至黄金顶端 (1499 EXP)
                                member.setMemberLevel("gold");
                                member.setExpTotal(1499);
                                log.info("钻石降级: userId={}, annualExp={}", member.getUserId(), annualExp);
                            }
                        }
                        case "gold" -> {
                            if (annualExp < GOLD_KEEP_THRESHOLD) {
                                // 降至白银 (500 EXP)
                                member.setMemberLevel("silver");
                                member.setExpTotal(500);
                                log.info("黄金降级: userId={}, annualExp={}", member.getUserId(), annualExp);
                            }
                        }
                        case "silver" -> {
                            if (annualExp < SILVER_KEEP_THRESHOLD) {
                                // 降至基础 (0 EXP)
                                member.setMemberLevel("basic");
                                member.setExpTotal(0);
                                log.info("白银降级: userId={}, annualExp={}", member.getUserId(), annualExp);
                            }
                        }
                    }

                    // 重置年度累计
                    member.setAnnualExp(0);
                    member.setLastSettlementYear(lastYear);
                    memberInfoMapper.updateById(member);

                    processedCount++;
                    if (needDormant)
                        dormantCount++;

                } catch (Exception e) {
                    log.error("保级判定失败: userId={}, error={}", member.getUserId(), e.getMessage());
                }
            }

            pageNum++;
        } while (page.getRecords() != null && !page.getRecords().isEmpty() && page.getCurrent() < page.getPages());

        log.info("年度保级判定完成: 处理{}人, 休眠{}人", processedCount, dormantCount);
    }

    /**
     * v5.0: 唤醒检查（订单完成时调用）
     * 如果当月累计达标，立即恢复完整权益
     */
    public void checkAndAwaken(Long userId, int monthlyExp) {
        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberInfo::getUserId, userId);
        MemberInfo member = memberInfoMapper.selectOne(wrapper);

        if (member == null || !"DORMANT".equals(member.getMemberStatus())) {
            return;
        }

        String level = member.getMemberLevel();
        boolean shouldAwaken = false;

        if ("black".equals(level) && monthlyExp >= BLACK_AWAKEN_THRESHOLD) {
            shouldAwaken = true;
        } else if ("diamond".equals(computeLevelByExp(member.getExpTotal()))
                && monthlyExp >= DIAMOND_AWAKEN_THRESHOLD) {
            // 钻石休眠后降到gold，但exp仍在钻石范围
            shouldAwaken = true;
            member.setMemberLevel("diamond");
        }

        if (shouldAwaken) {
            member.setMemberStatus("ACTIVE");
            memberInfoMapper.updateById(member);
            log.info("会员唤醒成功: userId={}, level={}", userId, level);

            // 可选：补发当月权益包
            // grantMonthlyReward(userId, level);
        }
    }

    // ==================== v5.5: 手动领取月度权益 ====================

    @Override
    public Map<String, Object> getMonthlyBenefitStatus(Long userId) {
        if (userId == null)
            return Map.of("claimed", false, "canClaim", false, "benefitName", "");

        // 1. 获取当月 Key
        String currentMonthKey = java.time.format.DateTimeFormatter.ofPattern("yyyyMM")
                .format(java.time.LocalDate.now());
        String sourceType = "monthly_benefit_" + currentMonthKey;

        // 2. 检查是否已领取 (查流水)
        LambdaQueryWrapper<PointsTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsTransaction::getUserId, userId)
                .eq(PointsTransaction::getSourceType, sourceType);
        PointsTransaction trans = transactionMapper.selectOne(wrapper);
        boolean claimed = trans != null;

        // v5.3: 如果已领取，尝试解析领取时的等级，以便前端提示"升级后下月生效"
        String claimedLevel = null;
        if (claimed && trans.getDescription() != null && trans.getDescription().contains(":")) {
            try {
                claimedLevel = trans.getDescription().substring(trans.getDescription().lastIndexOf(":") + 1);
            } catch (Exception ignore) {
            }
        }

        // 3. 检查资格
        MemberInfo member = getMemberInfoByUserId(userId);
        String level = member.getMemberLevel();
        // v5.3: 所有等级（含基础）均有月度权益
        boolean canClaim = true;

        String benefitName = switch (level) {
            case "basic" -> "免费加浓缩券×1";
            case "silver" -> "配送费抵扣券×1 + 加浓缩券×2";
            case "gold" -> "BOGO×1 + 8.8折券×2 + 配送费抵扣券×2";
            case "diamond" -> "免单券×1(限标准杯，排除特调/SOE) + BOGO×2 + 配送费抵扣券×5 + 新品5折券×1";
            case "black" -> "免单券×2(全品类，不限杯型) + BOGO×5 + 无限免运费 + 新品免费券×1";
            default -> "免费加浓缩券×1";
        };

        Map<String, Object> result = new HashMap<>();
        result.put("claimed", claimed);
        result.put("canClaim", canClaim);
        result.put("benefitName", benefitName);
        result.put("currentLevel", level); // Return current level
        result.put("claimedLevel", claimedLevel); // Return claimed level (if any)
        return result;
    }

    @Override
    @Transactional
    public void receiveMonthlyBenefit(Long userId) {
        if (userId == null)
            return;

        Map<String, Object> status = getMonthlyBenefitStatus(userId);
        if ((boolean) status.get("claimed")) {
            throw new BusinessException("本月权益已领取");
        }
        if (!(boolean) status.get("canClaim")) {
            throw new BusinessException("当前等级暂无可领取的月度权益");
        }

        String currentMonthKey = java.time.format.DateTimeFormatter.ofPattern("yyyyMM")
                .format(java.time.LocalDate.now());
        String sourceType = "monthly_benefit_" + currentMonthKey;
        String uniqueKeyBase = userId + "_" + currentMonthKey;

        MemberInfo member = getMemberInfoByUserId(userId);
        String level = member.getMemberLevel();

        try {
            switch (level) {
                case "basic" -> {
                    // v5.3 基础: 免费加浓缩券×1
                    pointsMallService.issueCouponToUser(userId, "SHOT", uniqueKeyBase + "_shot", 0, 5, 30);
                }
                case "silver" -> {
                    // v5.3 白银: 配送费抵扣券×1 + 免费加浓缩券×2
                    pointsMallService.issueCouponToUser(userId, "DELIVERY_FEE", uniqueKeyBase + "_del", 0, 6, 30);
                    pointsMallService.issueCouponToUser(userId, "SHOT", uniqueKeyBase + "_shot1", 0, 5, 30);
                    pointsMallService.issueCouponToUser(userId, "SHOT", uniqueKeyBase + "_shot2", 0, 5, 30);
                }
                case "gold" -> {
                    // v5.3 黄金: BOGO×1 + 8.8折券×2 + 配送费抵扣券×2
                    pointsMallService.issueCouponToUser(userId, "BOGO", uniqueKeyBase + "_bogo", 0, 40, 30);
                    pointsMallService.issueCouponToUser(userId, "DISCOUNT", uniqueKeyBase + "_dis1", 0, 88, 30); // 88表示8.8折
                    pointsMallService.issueCouponToUser(userId, "DISCOUNT", uniqueKeyBase + "_dis2", 0, 88, 30);
                    pointsMallService.issueCouponToUser(userId, "DELIVERY_FEE", uniqueKeyBase + "_del1", 0, 6, 30);
                    pointsMallService.issueCouponToUser(userId, "DELIVERY_FEE", uniqueKeyBase + "_del2", 0, 6, 30);
                }
                case "diamond" -> {
                    // v5.6 钻石月度权益：T2_PRE_FREE×1(限标准杯，含特调，排除SOE，封顶¥40) + BOGO×2 + 配送费抵扣券×5 + 新品5折券×1
                    pointsMallService.issueCouponToUser(userId, "MONTHLY_DIAMOND_FREE", uniqueKeyBase + "_free", 0, 40, 30);
                    pointsMallService.issueCouponToUser(userId, "BOGO", uniqueKeyBase + "_bogo1", 0, 40, 30);
                    pointsMallService.issueCouponToUser(userId, "BOGO", uniqueKeyBase + "_bogo2", 0, 40, 30);
                    for (int i = 1; i <= 5; i++) {
                        pointsMallService.issueCouponToUser(userId, "DELIVERY_FEE", uniqueKeyBase + "_del" + i, 0, 6,
                                30);
                    }
                    pointsMallService.issueCouponToUser(userId, "NEW_PRODUCT_HALF", uniqueKeyBase + "_new", 0, 50, 30);
                }
                case "black" -> {
                    // v5.6 黑金月度权益：T3_ALL_FREE×2(不限杯型，含特调，排除SOE，封顶¥40) + BOGO×5 + 无限免运费(系统自动) + 新品免费券×1
                    pointsMallService.issueCouponToUser(userId, "MONTHLY_BLACK_FREE", uniqueKeyBase + "_free1", 0, 40, 30);
                    pointsMallService.issueCouponToUser(userId, "MONTHLY_BLACK_FREE", uniqueKeyBase + "_free2", 0, 40, 30);
                    for (int i = 1; i <= 5; i++) {
                        pointsMallService.issueCouponToUser(userId, "BOGO", uniqueKeyBase + "_bogo" + i, 0, 40, 30);
                    }
                    // v5.3 黑金无限免运费：无需发券，由系统在创建外卖订单时自动判断等级并抵扣配送费
                    // （已在 OrderServiceImpl.createOrder 中实现黑金自动免配送费逻辑）
                    pointsMallService.issueCouponToUser(userId, "NEW_PRODUCT_FREE", uniqueKeyBase + "_new_free", 0, 40,
                            30);
                }
                default -> {
                    // 默认: 免费加浓缩券×1
                    pointsMallService.issueCouponToUser(userId, "SHOT", uniqueKeyBase + "_shot", 0, 5, 30);
                }
            }

            // Record Transaction (0 points change)
            // v5.3: Append level to description for frontend upgrade tip logic
            recordTransaction(userId, 0, member.getCurrentPoints(), sourceType, null,
                    "领取" + currentMonthKey + "月度等级权益:" + level);

            log.info("用户领取月度权益成功: userId={}, level={}, month={}", userId, level, currentMonthKey);
        } catch (Exception e) {
            log.error("月度权益发放失败", e);
            throw new BusinessException("权益发放失败，请稍后重试");
        }
    }
}
