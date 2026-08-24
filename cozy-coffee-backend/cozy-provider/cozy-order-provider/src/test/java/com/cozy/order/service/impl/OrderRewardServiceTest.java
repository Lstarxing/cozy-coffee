package com.cozy.order.service.impl;

import com.cozy.common.constant.PointsRateConfig;
import com.cozy.member.dto.response.MemberDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * OrderRewardService.estimateRewards / getPointsRate 全场景单测。
 * 覆盖：不同等级平坦倍率、跨级分段（500/1500/4000/9000）、黑卡加速包（余量内 1.7 / 超出 1.5 / 升级满额 300）、
 * 会员日加成、券后基数（rewardBase）、零值/空会员兜底。
 * <p>
 * 非黑卡段的期望倍率用 getPointsRate 动态取（自动包含会员日），保证测试与当天星期无关。
 */
class OrderRewardServiceTest {

    private OrderRewardService service;

    @BeforeEach
    void setUp() {
        service = new OrderRewardService();
    }

    // ==================== getPointsRate ====================

    @Test
    void getPointsRate_matchesSharedConfigPlusFriday() {
        for (String level : new String[]{"basic", "silver", "gold", "diamond", "black", null}) {
            BigDecimal expected = PointsRateConfig.getBaseRate(level)
                    .add(service.isCozyDay() ? new BigDecimal("0.5") : BigDecimal.ZERO);
            assertEquals(expected, service.getPointsRate(level), "level=" + level);
        }
    }

    // ==================== 边界 / 兜底 ====================

    @Test
    void zeroRewardBase_returnsZero() {
        OrderRewardService.RewardEstimate est = estimate(0, member(100, "silver", null));
        assertEquals(0, est.expEarned);
        assertEquals(0, est.pointsEarned);
    }

    @Test
    void nullMember_fallsBackToOneToOne() {
        OrderRewardService.RewardEstimate est = service.estimateRewards(new BigDecimal("100"), null);
        assertEquals(100, est.expEarned);
        assertEquals(100, est.pointsEarned);
        assertEquals(0, BigDecimal.ONE.compareTo(est.effectiveRate));
    }

    @Test
    void decimalRewardBase_expRoundsHalfUp() {
        OrderRewardService.RewardEstimate est = service.estimateRewards(new BigDecimal("22.5"), member(100, "basic", null));
        assertEquals(23, est.expEarned);
    }

    // ==================== 平坦等级（不跨级，整单按当前等级倍率） ====================

    @Test
    void basicMember_flatOneToOne() {
        assertEstimate(100, "basic", 100, round(100, "basic"));
    }

    @Test
    void silverMember_flatRate() {
        assertEstimate(600, "silver", 100, round(100, "silver"));
    }

    @Test
    void goldMember_flatRate() {
        assertEstimate(1600, "gold", 100, round(100, "gold"));
    }

    @Test
    void diamondMember_flatRate() {
        assertEstimate(4100, "diamond", 100, round(100, "diamond"));
    }

    // ==================== 跨级分段（大单恰好跨阈值） ====================

    @Test
    void basicCrossingSilver_segmented() {
        // exp 450 → 550，跨 500：前 50 basic、后 50 silver
        assertEstimate(450, "basic", 100, round(50, "basic") + round(50, "silver"));
    }

    @Test
    void silverCrossingGold_segmented() {
        // exp 1450 → 1550，跨 1500：前 50 silver、后 50 gold
        assertEstimate(1450, "silver", 100, round(50, "silver") + round(50, "gold"));
    }

    @Test
    void goldCrossingDiamond_segmented() {
        // exp 3950 → 4150，跨 4000：前 50 gold、后 150 diamond
        assertEstimate(3950, "gold", 200, round(50, "gold") + round(150, "diamond"));
    }

    @Test
    void diamondCrossingBlack_segmentedWithAccelerate() {
        // exp 8950 → 9050，跨 9000：前 50 diamond、后 50 黑卡段（升级，加速包满额 300 → 1.7x）
        assertEstimate(8950, "diamond", 100, round(50, "diamond") + blackPoints(50, "300"));
    }

    @Test
    void singleOrderCrossesTwoThresholds_segmented() {
        // exp 450 → 1550，跨 500 与 1500：50 basic + 1000 silver + 50 gold
        assertEstimate(450, "basic", 1100,
                round(50, "basic") + round(1000, "silver") + round(50, "gold"));
    }

    // ==================== 黑卡加速包 ====================

    @Test
    void blackWithinAccelerateRemaining_uses1_7x() {
        // 已黑卡，加速包余量 200，¥50：全部落在余量内 → 50×1.7
        assertEstimateBlack(9500, "200", 50, blackPoints(50, "200"));
    }

    @Test
    void blackPartiallyBeyondRemaining_1_7then1_5() {
        // 余量 30，¥50：30×1.7 + 20×1.5
        assertEstimateBlack(9500, "30", 50, blackPoints(50, "30"));
    }

    @Test
    void blackZeroRemaining_uses1_5x() {
        assertEstimateBlack(9500, "0", 50, blackPoints(50, "0"));
    }

    // ==================== effectiveRate ====================

    @Test
    void effectiveRate_isBlendedPointsOverRewardBase() {
        OrderRewardService.RewardEstimate est = service.estimateRewards(
                new BigDecimal("100"), member(1450, "silver", null));
        BigDecimal expected = new BigDecimal(est.pointsEarned).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(est.effectiveRate));
    }

    // ==================== 工具方法 ====================

    private void assertEstimate(int expTotal, String level, int amount, int expectedPoints) {
        OrderRewardService.RewardEstimate est = service.estimateRewards(
                new BigDecimal(amount), member(expTotal, level, null));
        assertEquals(amount, est.expEarned);
        assertEquals(expectedPoints, est.pointsEarned);
    }

    private void assertEstimateBlack(int expTotal, String accelerate, int amount, int expectedPoints) {
        OrderRewardService.RewardEstimate est = service.estimateRewards(
                new BigDecimal(amount), member(expTotal, "black", accelerate));
        assertEquals(amount, est.expEarned);
        assertEquals(expectedPoints, est.pointsEarned);
    }

    private OrderRewardService.RewardEstimate estimate(int amount, MemberDTO member) {
        return service.estimateRewards(new BigDecimal(amount), member);
    }

    private MemberDTO member(int expTotal, String level, String accelerate) {
        MemberDTO member = new MemberDTO();
        member.setExpTotal(expTotal);
        member.setMemberLevel(level);
        if (accelerate != null) {
            member.setMonthlyAccelerateRemaining(new BigDecimal(accelerate));
        }
        return member;
    }

    /** 非黑卡段：segmentExp × 该等级倍率（自动含会员日） */
    private int round(int exp, String level) {
        return new BigDecimal(exp).multiply(service.getPointsRate(level))
                .setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /** 黑卡段：走加速包计算（1.7x 余量内 / 1.5x 超出） */
    private int blackPoints(int exp, String budget) {
        return service.calculateBlackCardPoints(new BigDecimal(exp), new BigDecimal(budget));
    }
}
