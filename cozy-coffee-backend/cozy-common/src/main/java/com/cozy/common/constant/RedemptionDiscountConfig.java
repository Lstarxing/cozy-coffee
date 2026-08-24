package com.cozy.common.constant;

import java.math.BigDecimal;

/**
 * 积分商城兑换折扣（单一事实源）：会员等级对应的兑换积分折扣。
 * 供 PointsMallServiceImpl.calculateCost、MemberDTO.redeemDiscount 共用，
 * 避免后端/移动端/web 各自维护折扣表。
 * black 0.85 / diamond 0.90 / gold 0.95 / silver 0.98 / basic 1.0
 */
public final class RedemptionDiscountConfig {

    private RedemptionDiscountConfig() {
    }

    public static BigDecimal getDiscount(String level) {
        if (level == null) {
            return BigDecimal.ONE;
        }
        return switch (level.toLowerCase()) {
            case "black" -> new BigDecimal("0.85");
            case "diamond" -> new BigDecimal("0.90");
            case "gold" -> new BigDecimal("0.95");
            case "silver" -> new BigDecimal("0.98");
            default -> BigDecimal.ONE;
        };
    }
}
