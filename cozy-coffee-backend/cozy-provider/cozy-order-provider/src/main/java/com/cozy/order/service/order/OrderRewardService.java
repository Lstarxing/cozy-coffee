package com.cozy.order.service.order;

import com.cozy.common.constant.MemberLevelConfig;
import com.cozy.common.constant.PointsRateConfig;
import com.cozy.member.dto.response.MemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 订单积分/EXP 预估计算服务。
 * 从 OrderServiceImpl 抽出（Phase 4.3），消除 1709 行上帝类中的计算逻辑。
 * 等级阈值/加速包/会员日配置统一读 MemberLevelConfig（cozy.member.level）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRewardService {

    private final MemberLevelConfig memberLevelConfig;

    /**
     * 获取会员等级对应的积分倍率。
     * basic=1.0x silver=1.1x gold=1.2x diamond=1.3x black=1.5x
     * 周五会员日额外 +0.5x
     */
    public BigDecimal getPointsRate(String level) {
        BigDecimal baseRate = PointsRateConfig.getBaseRate(level);

        // 会员日（Cozy Day）加成：倍率 + cozyDayBonus
        if (isCozyDay()) {
            baseRate = baseRate.add(memberLevelConfig.getCozyDayBonus());
            log.debug("会员日加成生效: 原倍率+{}, 当前等级={}", memberLevelConfig.getCozyDayBonus(), level);
        }
        return baseRate;
    }

    /**
     * 判断今天是否为会员日 (Cozy Day)：cozy.member.level.cozy-day-of-week
     */
    public boolean isCozyDay() {
        return LocalDate.now().getDayOfWeek() == memberLevelConfig.getCozyDayOfWeek();
    }

    /**
     * 黑卡加速包：剩余额度内 accelerateRate 倍积分，超出部分 normalRate 倍
     * 倍率与每月额度见 cozy.member.level（accelerate-rate/normal-rate/accelerate-monthly-cap）
     *
     * @param payAmount           本次支付金额
     * @param accelerateRemaining 加速包剩余额度（由 MemberService 维护，每月重置）
     */
    public int calculateBlackCardPoints(BigDecimal payAmount, BigDecimal accelerateRemaining) {
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        BigDecimal accelerateRate = memberLevelConfig.getAccelerateRate();
        BigDecimal normalRate = memberLevelConfig.getNormalRate();

        BigDecimal remainingCap = accelerateRemaining != null ? accelerateRemaining.max(BigDecimal.ZERO)
                : memberLevelConfig.getAccelerateMonthlyCap();

        BigDecimal acceleratedAmount = payAmount.min(remainingCap);
        BigDecimal normalAmount = payAmount.subtract(acceleratedAmount);

        int acceleratedPoints = acceleratedAmount.multiply(accelerateRate)
                .setScale(0, RoundingMode.HALF_UP).intValue();
        int normalPoints = normalAmount.multiply(normalRate)
                .setScale(0, RoundingMode.HALF_UP).intValue();

        int totalPoints = acceleratedPoints + normalPoints;

        log.info("黑卡加速包计算明细: payAmount={}, accelerateRemaining={}, accelerated={}@{}, normal={}@{}, total={}",
                payAmount, accelerateRemaining, acceleratedAmount, accelerateRate, normalAmount, normalRate,
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

    /**
     * 奖励预估（全等级分段）：以 rewardBase（实付 - 配送费）为基数。
     * 按 currentExp → currentExp + 本次EXP 跨越的等级阈值逐段计倍率（阈值见 cozy.member.level）；
     * 黑卡段走加速包（已黑卡用会员剩余额度，本单升级黑卡按满额起算）；
     * 非黑卡段按对应等级倍率（含会员日加成）。
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
        boolean alreadyBlack = currentExp >= memberLevelConfig.getBlackExp();
        BigDecimal blackBudget = alreadyBlack && member.getMonthlyAccelerateRemaining() != null
                ? member.getMonthlyAccelerateRemaining()
                : memberLevelConfig.getAccelerateMonthlyCap();

        int remainingExp = est.expEarned;
        int expCursor = currentExp;
        long totalPoints = 0;

        while (remainingExp > 0) {
            int nextThreshold = memberLevelConfig.nextLevelThreshold(expCursor);
            int segmentExp = nextThreshold > 0 ? Math.min(nextThreshold - expCursor, remainingExp) : remainingExp;
            String segLevel = memberLevelConfig.levelForExp(expCursor);

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

}
