package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.BirthdayRewardConfig;
import com.cozy.common.constant.MemberLevelConfig;
import com.cozy.common.constant.MonthlyBenefitConfig;
import com.cozy.common.constant.UpgradeRewardConfig;
import com.cozy.member.entity.MemberInfo;
import com.cozy.member.entity.PointsLot;
import com.cozy.member.entity.PointsTransaction;
import com.cozy.member.mapper.MemberInfoMapper;
import com.cozy.member.mapper.MonthlyTaskMapper;
import com.cozy.member.mapper.PointsLotConsumptionMapper;
import com.cozy.member.mapper.PointsLotMapper;
import com.cozy.member.mapper.PointsTransactionMapper;
import com.cozy.member.mq.CouponGrantOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ADR 0001 C7：带来源键的加分必须能安全吸收重复投递。 */
@SuppressWarnings({"rawtypes", "unchecked"})
class AddPointsWithLotIdempotencyTest {

    private MemberInfoMapper memberInfoMapper;
    private PointsTransactionMapper transactionMapper;
    private PointsLotMapper pointsLotMapper;
    private MemberServiceImpl service;
    private MemberInfo member;

    @BeforeEach
    void setUp() {
        memberInfoMapper = mock(MemberInfoMapper.class);
        transactionMapper = mock(PointsTransactionMapper.class);
        pointsLotMapper = mock(PointsLotMapper.class);

        service = new MemberServiceImpl(
                memberInfoMapper,
                transactionMapper,
                pointsLotMapper,
                mock(PointsLotConsumptionMapper.class),
                mock(MonthlyTaskMapper.class),
                mock(RedisTemplate.class),
                mock(StringRedisTemplate.class),
                new ObjectMapper(),
                new MemberLevelConfig(),
                new BirthdayRewardConfig(),
                new UpgradeRewardConfig(),
                new MonthlyBenefitConfig(),
                mock(CouponGrantOutboxService.class),
                mock(PlatformTransactionManager.class));

        member = new MemberInfo();
        member.setUserId(7L);
        member.setCurrentPoints(100);
        member.setTotalPoints(100);
        when(memberInfoMapper.selectByUserIdForUpdate(7L)).thenReturn(member);
    }

    @Test
    void replayedProfileRewardUpdatesBalanceLotAndTransactionOnlyOnce() {
        PointsTransaction existing = new PointsTransaction();
        existing.setUserId(7L);
        existing.setSourceType("profile");
        existing.setSourceId(7L);
        when(transactionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null, existing);

        service.addPointsWithLot(7L, 20, "profile", 7L, "完善个人资料奖励");
        service.addPointsWithLot(7L, 20, "profile", 7L, "完善个人资料奖励");

        assertEquals(120, member.getCurrentPoints());
        assertEquals(120, member.getTotalPoints());
        verify(memberInfoMapper, times(1)).updateById(member);
        verify(pointsLotMapper, times(1)).insert(any(PointsLot.class));
        verify(transactionMapper, times(1)).insert(any(PointsTransaction.class));
    }

    @Test
    void locksMemberRowBeforeCheckingIdempotencyRecord() {
        when(transactionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(new PointsTransaction());

        service.addPointsWithLot(7L, 20, "profile", 7L, "完善个人资料奖励");

        InOrder order = inOrder(memberInfoMapper, transactionMapper);
        order.verify(memberInfoMapper).selectByUserIdForUpdate(7L);
        order.verify(transactionMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void memberLookupUsesForUpdateLock() throws Exception {
        Method method = MemberInfoMapper.class.getMethod("selectByUserIdForUpdate", Long.class);
        Select select = method.getAnnotation(Select.class);

        assertTrue(select != null && String.join(" ", select.value()).toUpperCase().contains("FOR UPDATE"),
                "同一用户的并发奖励必须先锁 member_info 行，再检查幂等流水");
    }
}
