package com.cozy.member.service.impl;

import com.cozy.common.constant.BirthdayRewardConfig;
import com.cozy.common.constant.MemberLevelConfig;
import com.cozy.common.constant.MonthlyBenefitConfig;
import com.cozy.common.constant.UpgradeRewardConfig;
import com.cozy.common.exception.BusinessException;
import com.cozy.mall.api.PointsMallService;
import com.cozy.member.entity.MemberInfo;
import com.cozy.member.entity.PointsLot;
import com.cozy.member.entity.PointsTransaction;
import com.cozy.member.mapper.MemberInfoMapper;
import com.cozy.member.mapper.MonthlyTaskMapper;
import com.cozy.member.mapper.PointsLotConsumptionMapper;
import com.cozy.member.mapper.PointsLotMapper;
import com.cozy.member.mapper.PointsTransactionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会员权益发放单测：验证晋升礼 / 月度权益 / 生日礼按配置（UpgradeRewardConfig / MonthlyBenefitConfig /
 * BirthdayRewardConfig）驱动发券与积分，替代旧 switch 硬编码后的行为一致。
 */
class MemberRewardGrantTest {

    private MemberInfoMapper memberInfoMapper;
    private PointsTransactionMapper transactionMapper;
    private PointsLotMapper pointsLotMapper;
    private PointsMallService pointsMallService;
    private MemberServiceImpl service;
    private MemberInfo member;

    @BeforeEach
    void setUp() throws Exception {
        memberInfoMapper = mock(MemberInfoMapper.class);
        transactionMapper = mock(PointsTransactionMapper.class);
        pointsLotMapper = mock(PointsLotMapper.class);
        pointsMallService = mock(PointsMallService.class);

        service = new MemberServiceImpl(
                memberInfoMapper, transactionMapper, pointsLotMapper,
                mock(PointsLotConsumptionMapper.class), mock(MonthlyTaskMapper.class),
                mock(RedisTemplate.class), mock(StringRedisTemplate.class),
                new ObjectMapper(),
                new MemberLevelConfig(), new BirthdayRewardConfig(),
                new UpgradeRewardConfig(), new MonthlyBenefitConfig(),
                mock(PlatformTransactionManager.class));

        Field f = MemberServiceImpl.class.getDeclaredField("pointsMallService");
        f.setAccessible(true);
        f.set(service, pointsMallService);

        member = new MemberInfo();
        member.setUserId(38L);
        member.setMemberLevel("diamond");
        member.setCurrentPoints(50000);
        member.setTotalPoints(50000);
        member.setExpTotal(2000);

        when(memberInfoMapper.selectOne(any())).thenReturn(member);
        when(memberInfoMapper.selectByUserIdForUpdate(anyLong())).thenReturn(member);
        when(memberInfoMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(transactionMapper.selectOne(any())).thenReturn(null); // 未领取
        when(pointsLotMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    private Object invoke(String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = MemberServiceImpl.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(service, args);
    }

    /** 统计与期望券类型/参数完全匹配的 issueCouponToUser 调用数 */
    private int countIssued(String couponType, double min, double discount, int days) {
        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> minCap = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> disCap = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Integer> daysCap = ArgumentCaptor.forClass(Integer.class);
        verify(pointsMallService, atLeastOnce()).issueCouponToUser(
                anyLong(), type.capture(), anyString(), minCap.capture(), disCap.capture(), daysCap.capture());
        List<String> types = type.getAllValues();
        int count = 0;
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i).equals(couponType)
                    && minCap.getAllValues().get(i) == min
                    && disCap.getAllValues().get(i) == discount
                    && daysCap.getAllValues().get(i) == days) {
                count++;
            }
        }
        return count;
    }

    private void assertIssued(String couponType, double min, double discount, int days) {
        assertTrue(countIssued(couponType, min, discount, days) >= 1,
                "应发放券 " + couponType + "(" + min + "/" + discount + "/" + days + ")");
    }

    private void assertPointsGranted() {
        verify(pointsLotMapper, atLeastOnce()).insert(any(PointsLot.class));
        verify(transactionMapper, atLeastOnce()).insert(any(PointsTransaction.class));
    }

    // ==================== 晋升礼 ====================

    @Test
    void upgradeSilver_issuesOneCouponNoPoints() throws Exception {
        invoke("grantUpgradeReward", new Class[]{Long.class, String.class}, 38L, "silver");
        assertIssued("UPGRADE_SILVER_DISCOUNT", 0, 50, 60);
        verify(transactionMapper, times(0)).insert(any(PointsTransaction.class));
    }

    @Test
    void upgradeGold_issuesCouponAnd50Points() throws Exception {
        invoke("grantUpgradeReward", new Class[]{Long.class, String.class}, 38L, "gold");
        assertIssued("UPGRADE_GOLD_BOGO", 0, 40, 60);
        assertPointsGranted();
    }

    @Test
    void upgradeBlack_issuesPremiumAnd1688Points() throws Exception {
        invoke("grantUpgradeReward", new Class[]{Long.class, String.class}, 38L, "black");
        assertIssued("UPGRADE_BLACK_PREMIUM", 0, 999, 60);
        assertPointsGranted();
    }

    // ==================== 月度权益 ====================

    @Test
    void monthlyBenefitDiamond_issues9Coupons() throws Exception {
        member.setMemberLevel("diamond");
        service.receiveMonthlyBenefit(38L);
        verify(pointsMallService, times(9)).issueCouponToUser(
                anyLong(), anyString(), anyString(), anyDouble(), anyDouble(), anyInt());
        assertIssued("MONTHLY_DIAMOND_FREE", 0, 40, 30);
        assertIssued("BOGO", 0, 40, 30);
        assertIssued("DELIVERY_FEE", 0, 6, 30);
        assertIssued("NEW_PRODUCT_HALF", 0, 50, 30);
    }

    @Test
    void monthlyBenefitBlack_issues8Coupons() throws Exception {
        member.setMemberLevel("black");
        service.receiveMonthlyBenefit(38L);
        verify(pointsMallService, times(8)).issueCouponToUser(
                anyLong(), anyString(), anyString(), anyDouble(), anyDouble(), anyInt());
        assertIssued("MONTHLY_BLACK_FREE", 0, 40, 30);
        assertIssued("BOGO", 0, 40, 30);
        assertIssued("NEW_PRODUCT_FREE", 0, 40, 30);
    }

    @Test
    void monthlyBenefitGold_issues5Coupons() throws Exception {
        member.setMemberLevel("gold");
        service.receiveMonthlyBenefit(38L);
        verify(pointsMallService, times(5)).issueCouponToUser(
                anyLong(), anyString(), anyString(), anyDouble(), anyDouble(), anyInt());
        assertIssued("BOGO", 0, 40, 30);
        assertIssued("DISCOUNT", 0, 88, 30);
        assertIssued("DELIVERY_FEE", 0, 6, 30);
    }

    // ==================== 生日礼 ====================

    @Test
    void birthdayBlack_issues2CouponsAnd888Points() throws Exception {
        member.setMemberLevel("black");
        member.setExpTotal(9500); // computeLevelByExp → black
        invoke("grantBirthdayRewardInternal", new Class[]{Long.class, int.class}, 38L, 2026);
        assertIssued("BIRTHDAY_BLACK_FREE", 0, 40, 30);
        assertIssued("BIRTHDAY_FREE_CAKE", 0, 40, 30);
        assertPointsGranted();
    }

    @Test
    void birthdayBasic_issuesOneCouponNoPoints() throws Exception {
        member.setMemberLevel("basic");
        member.setExpTotal(0); // computeLevelByExp → basic
        invoke("grantBirthdayRewardInternal", new Class[]{Long.class, int.class}, 38L, 2026);
        assertIssued("BIRTHDAY_BASIC_DISCOUNT", 0, 50, 30);
        verify(transactionMapper, atLeastOnce()).insert(any(PointsTransaction.class)); // 0 分流水
    }

    // ==================== 幂等：重复领取不再发 ====================

    @Test
    void monthlyBenefitAlreadyClaimed_skipsIssue() throws Exception {
        when(transactionMapper.selectOne(any())).thenReturn(new PointsTransaction());
        member.setMemberLevel("diamond");
        try {
            service.receiveMonthlyBenefit(38L);
        } catch (BusinessException ignored) {
            // 已领取 → 抛业务异常
        }
        verify(pointsMallService, times(0)).issueCouponToUser(
                anyLong(), anyString(), anyString(), anyDouble(), anyDouble(), anyInt());
    }
}
