package com.cozy.common.constant;

import java.math.BigDecimal;

/**
 * 会员等级基础积分倍率（单一事实源）。
 * 供 OrderRewardService.getPointsRate（叠加会员日加成）、MemberDTO.pointsRate 共用，
 * 避免两端各自维护倍率表。
 * basic=1.0 silver=1.1 gold=1.2 diamond=1.3 black=1.5；周五会员日 +0.5 由业务侧叠加。
 */
public final class PointsRateConfig {

    private PointsRateConfig() {
    }

    public static BigDecimal getBaseRate(String level) {
        if (level == null) {
            return BigDecimal.ONE;
        }
        return switch (level.toLowerCase()) {
            case "silver" -> new BigDecimal("1.1");
            case "gold" -> new BigDecimal("1.2");
            case "diamond" -> new BigDecimal("1.3");
            case "black" -> new BigDecimal("1.5");
            default -> BigDecimal.ONE;
        };
    }
}
