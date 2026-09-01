package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.exception.BusinessException;
import com.cozy.member.entity.MemberInfo;
import com.cozy.member.entity.PointsTransaction;
import com.cozy.member.mapper.MemberInfoMapper;
import com.cozy.member.mapper.PointsLotMapper;
import com.cozy.member.mapper.PointsTransactionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumeIdempotencyTest {

    @Mock private MemberInfoMapper memberInfoMapper;
    @Mock private PointsTransactionMapper transactionMapper;
    @Mock private PointsLotMapper pointsLotMapper;
    @InjectMocks private MemberServiceImpl memberService;

    private MemberInfo member(int currentPoints) {
        MemberInfo m = new MemberInfo();
        m.setUserId(1L);
        m.setCurrentPoints(currentPoints);
        return m;
    }

    @Test
    void consumePointsFIFO_sameConsumeIdAlreadyConsumed_returnsTrueWithoutDeducting() {
        when(memberInfoMapper.selectByUserIdForUpdate(1L)).thenReturn(member(500));
        PointsTransaction existing = new PointsTransaction();
        existing.setChangeAmount(-100); // 原消费 100 分
        when(transactionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        boolean result = memberService.consumePointsFIFO(1L, 100, "redeem", 999L);

        assertTrue(result);
        verify(pointsLotMapper, never()).selectAvailableLotsForUpdate(any());
    }

    @Test
    void consumePointsFIFO_sameConsumeIdDifferentPoints_throwsConflict() {
        when(memberInfoMapper.selectByUserIdForUpdate(1L)).thenReturn(member(500));
        PointsTransaction existing = new PointsTransaction();
        existing.setChangeAmount(-50); // 原消费 50 分
        when(transactionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThrows(BusinessException.class,
                () -> memberService.consumePointsFIFO(1L, 100, "redeem", 999L));
    }
}
