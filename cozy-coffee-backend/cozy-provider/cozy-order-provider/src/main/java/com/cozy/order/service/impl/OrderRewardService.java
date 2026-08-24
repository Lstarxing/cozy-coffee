package com.cozy.order.service.impl;

import com.cozy.common.constant.PointsRateConfig;
import com.cozy.member.dto.response.MemberDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.DayOfWeek;

/**
 * 订单积分/EXP 预估计算服务。
 * 从 OrderServiceImpl 抽出（Phase 4.3），消除 1709 行上帝类中的计算逻辑。
 */
@Slf4j
@Component
public class OrderRewardService {

    /**
     * 获取会员等级对应的积分倍率。
     * basic=1.0x silver=1.1x gold=1.2x diamond=1.3x black=1.5x
     * 周五会员日额外 +0.5x
     */
    public BigDecimal getPointsRate(String level) {
        BigDecimal baseRate = PointsRateConfig.getBaseRate(level);

        // v6.1 会员日: 周五积分倍率 +0.5x
        if (isCozyDay()) {
            baseRate = baseRate.add(new BigDecimal("0.5"));
            log.debug("会员日加成生效: 原倍率+0.5, 当前等级={}", level);
        }
        return baseRate;
    }

    /**
     * 判断今天是否为会员日 (Cozy Day) -- v6.1: 每周五
     */
    public boolean isCozyDay() {
        return LocalDate.now().getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    /**
     * 黑卡加速包：加速包剩余额度内 1.70 倍积分，超出部分 1.5 倍（v5.0）
     *
     * @param payAmount           本次支付金额
     * @param accelerateRemaining 加速包剩余额度（由 MemberService 维护，每月重置为300）
     */
    public int calculateBlackCardPoints(BigDecimal payAmount, BigDecimal accelerateRemaining) {
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        final BigDecimal ACCELERATE_RATE = new BigDecimal("1.70");
        final BigDecimal NORMAL_RATE = new BigDecimal("1.5");

        BigDecimal remainingCap = accelerateRemaining != null ? accelerateRemaining.max(BigDecimal.ZERO)
                : new BigDecimal("300");

        BigDecimal acceleratedAmount = payAmount.min(remainingCap);
        BigDecimal normalAmount = payAmount.subtract(acceleratedAmount);

        int acceleratedPoints = acceleratedAmount.multiply(ACCELERATE_RATE)
                .setScale(0, RoundingMode.HALF_UP).intValue();
        int normalPoints = normalAmount.multiply(NORMAL_RATE)
                .setScale(0, RoundingMode.HALF_UP).intValue();

        int totalPoints = acceleratedPoints + normalPoints;

        log.info("黑卡加速包计算明细: payAmount={}, accelerateRemaining={}, accelerated={}@{}, normal={}@{}, total={}",
                payAmount, accelerateRemaining, acceleratedAmount, ACCELERATE_RATE, normalAmount, NORMAL_RATE,
                totalPoints);

        return totalPoints;
    }

    // ============================================================
    // 全等级分段奖励预估（确认页展示 与 下单时落库 共用同一套口径）
    // ============================================================

    public static class RewardEstimate {
        public int expEarned;
        public int pointsEarned;
        public BigDecimal effectiveRate;
    }

    private static final int SILVER_THRESHOLD = 500;
    private static final int GOLD_THRESHOLD = 1500;
    private static final int DIAMOND_THRESHOLD = 4000;
    private static final int BLACK_THRESHOLD = 9000;

    /**
     * 奖励预估（全等级分段）：以 rewardBase（实付 - 配送费）为基数。
     * 按 currentExp → currentExp + 本次EXP 跨越的等级阈值（500/1500/4000/9000）逐段计倍率；
     * 黑卡段走加速包（已黑卡用会员剩余额度，本单升级黑卡按满额 300 起算）；
     * 非黑卡段按对应等级倍率（含周五会员日加成）。
     */
    public RewardEstimate estimateRewards(BigDecimal rewardBase, MemberDTO member) {
        RewardEstimate est = new RewardEstimate();
        if (rewardBase == null || rewardBase.compareTo(BigDecimal.ZERO) <= 0) {
            est.expEarned = 0;
            est.pointsEarned = 0;
            est.effectiveRate = BigDecimal.ONE;
            return est;
        }
        est.expEarned = rewardBase.setScale(0, RoundingMode.HALF_UP).intValue();
        if (member == null || est.expEarned <= 0) {
            est.pointsEarned = est.expEarned;
            est.effectiveRate = BigDecimal.ONE;
            return est;
        }

        int currentExp = member.getExpTotal() != null ? member.getExpTotal() : 0;
        boolean alreadyBlack = currentExp >= BLACK_THRESHOLD;
        BigDecimal blackBudget = alreadyBlack && member.getMonthlyAccelerateRemaining() != null
                ? member.getMonthlyAccelerateRemaining()
                : new BigDecimal("300");

        int remainingExp = est.expEarned;
        int expCursor = currentExp;
        long totalPoints = 0;

        while (remainingExp > 0) {
            int nextThreshold = nextLevelThreshold(expCursor);
            int segmentExp = nextThreshold > 0 ? Math.min(nextThreshold - expCursor, remainingExp) : remainingExp;
            String segLevel = levelForExp(expCursor);

            if ("black".equals(segLevel)) {
                totalPoints += calculateBlackCardPoints(new BigDecimal(segmentExp), blackBudget);
            } else {
                totalPoints += new BigDecimal(segmentExp)
                        .multiply(getPointsRate(segLevel))
                        .setScale(0, RoundingMode.HALF_UP).longValue();
            }

            expCursor += segmentExp;
            remainingExp -= segmentExp;
        }

        est.pointsEarned = (int) totalPoints;
        est.effectiveRate = new BigDecimal(est.pointsEarned).divide(rewardBase, 2, RoundingMode.HALF_UP);
        return est;
    }

    private int nextLevelThreshold(int exp) {
        if (exp < SILVER_THRESHOLD) return SILVER_THRESHOLD;
        if (exp < GOLD_THRESHOLD) return GOLD_THRESHOLD;
        if (exp < DIAMOND_THRESHOLD) return DIAMOND_THRESHOLD;
        if (exp < BLACK_THRESHOLD) return BLACK_THRESHOLD;
        return -1;
    }

    private String levelForExp(int exp) {
        if (exp >= BLACK_THRESHOLD) return "black";
        if (exp >= DIAMOND_THRESHOLD) return "diamond";
        if (exp >= GOLD_THRESHOLD) return "gold";
        if (exp >= SILVER_THRESHOLD) return "silver";
        return "basic";
    }
}
