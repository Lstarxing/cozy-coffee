package com.cozy.order.service.impl;

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
        if (level == null) return BigDecimal.ONE;
        BigDecimal baseRate = switch (level.toLowerCase()) {
            case "basic" -> BigDecimal.ONE;
            case "silver" -> new BigDecimal("1.1");
            case "gold" -> new BigDecimal("1.2");
            case "diamond" -> new BigDecimal("1.3");
            case "black" -> new BigDecimal("1.5");
            default -> BigDecimal.ONE;
        };

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
}
