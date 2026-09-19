package com.cozy.member.service.impl;

import com.cozy.common.constant.BirthdayRewardConfig;
import com.cozy.common.constant.MemberLevelConfig;
import com.cozy.common.constant.MonthlyBenefitConfig;
import com.cozy.common.constant.PointsRateConfig;
import com.cozy.common.constant.RedemptionDiscountConfig;
import com.cozy.common.constant.UpgradeRewardConfig;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.entity.MemberInfo;
import com.cozy.member.mapper.MemberInfoMapper;
import com.cozy.member.mapper.MonthlyTaskMapper;
import com.cozy.member.mapper.PointsLotConsumptionMapper;
import com.cozy.member.mapper.PointsLotMapper;
import com.cozy.member.mapper.PointsTransactionMapper;
import com.cozy.member.mq.CouponGrantOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批量会员查询的等级口径：必须与单体 {@code getMemberByUserId} 一致 —— 按 EXP **实时计算**，
 * 而不是读存量 {@code member_level}。
 *
 * <p>背景：管理端用户列表改走批量接口（原先逐用户调单体，是 N+1）。两个方法若口径不同，
 * 存量等级漂移的用户会出现「列表显示 basic、详情显示 gold」，按等级筛选也会筛错。
 *
 * <p>另一个刻意要求：批量读**不得回写**。单体方法发现漂移会自愈回写，批量里有 N 个用户，
 * 回写就变成 N 次写 —— 读接口不该有写副作用。
 */
class MemberBatchQueryLevelTest {

    private MemberInfoMapper memberInfoMapper;
    private MemberServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        memberInfoMapper = mock(MemberInfoMapper.class);
        // MemberLevelConfig 的字段默认值即生产阈值（silver=500 / gold=1500 / diamond=4000 / black=9000）
        service = new MemberServiceImpl(
                memberInfoMapper, mock(PointsTransactionMapper.class), mock(PointsLotMapper.class),
                mock(PointsLotConsumptionMapper.class), mock(MonthlyTaskMapper.class),
                mock(RedisTemplate.class), mock(StringRedisTemplate.class),
                new ObjectMapper(),
                new MemberLevelConfig(), new BirthdayRewardConfig(),
                new UpgradeRewardConfig(), new MonthlyBenefitConfig(),
                mock(CouponGrantOutboxService.class),
                mock(PlatformTransactionManager.class));
    }

    private MemberInfo member(long userId, String storedLevel, Integer expTotal) {
        MemberInfo info = new MemberInfo();
        info.setId(userId);
        info.setUserId(userId);
        info.setMemberLevel(storedLevel);
        info.setExpTotal(expTotal);
        info.setCurrentPoints(100);
        info.setTotalPoints(500);
        return info;
    }

    private MemberDTO queryOne(MemberInfo info) {
        when(memberInfoMapper.selectList(any())).thenReturn(List.of(info));
        Map<Long, MemberDTO> result = service.getMembersByUserIds(Set.of(info.getUserId()));
        return result.get(info.getUserId());
    }

    /** 存量等级是 basic，但 EXP 已到 gold 档 —— 批量查询必须报 gold。 */
    @Test
    void reportsComputedLevelNotStoredLevel() {
        MemberDTO dto = queryOne(member(38L, "basic", 1600));

        assertEquals("gold", dto.getMemberLevel());
        assertEquals(1600, dto.getExpTotal());
    }

    /** 折扣率/积分倍率也必须按**计算后**的等级取，否则等级对了、权益仍错。 */
    @Test
    void ratesFollowComputedLevel() {
        MemberDTO dto = queryOne(member(38L, "basic", 1600));

        assertEquals(PointsRateConfig.getBaseRate("gold"), dto.getPointsRate());
        assertEquals(RedemptionDiscountConfig.getDiscount("gold"), dto.getRedeemDiscount());
    }

    /** 批量读不得产生写副作用（单体路径的自愈回写不能搬到批量里）。 */
    @Test
    void batchQueryDoesNotWriteBack() {
        queryOne(member(38L, "basic", 1600));

        // 带类型的匹配器：MyBatis-Plus 的 updateById/update 有重载，裸 any() 会编译不过
        verify(memberInfoMapper, never()).updateById(any(MemberInfo.class));
        verify(memberInfoMapper, never()).update(any(MemberInfo.class), any());
    }

    /** expTotal 为 null 按 0 处理，落到 basic（与单体一致）。 */
    @Test
    void nullExpIsTreatedAsZero() {
        MemberDTO dto = queryOne(member(38L, "diamond", null));

        assertEquals("basic", dto.getMemberLevel());
        assertEquals(0, dto.getExpTotal());
    }
}
