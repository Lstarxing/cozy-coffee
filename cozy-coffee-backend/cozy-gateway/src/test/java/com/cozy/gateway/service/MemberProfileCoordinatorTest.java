package com.cozy.gateway.service;

import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.response.CouponSummaryDTO;
import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BFF 组合语义：券数量是次要展示字段，mall 失败不能拖垮会员资料接口。
 */
class MemberProfileCoordinatorTest {

    private MemberService memberService;
    private PointsMallService pointsMallService;
    private MemberProfileCoordinator coordinator;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        pointsMallService = mock(PointsMallService.class);
        coordinator = new MemberProfileCoordinator();
        ReflectionTestUtils.setField(coordinator, "memberService", memberService);
        ReflectionTestUtils.setField(coordinator, "pointsMallService", pointsMallService);
    }

    @Test
    void fillsCouponCountsFromMall() {
        MemberDTO member = new MemberDTO();
        when(memberService.getMemberByUserId(38L)).thenReturn(member);
        CouponSummaryDTO summary = new CouponSummaryDTO();
        summary.setAvailableCount(7);
        summary.setExchangeCount(2);
        when(pointsMallService.getCouponSummary(38L)).thenReturn(summary);

        MemberDTO result = coordinator.getMemberProfile(38L);

        assertEquals(7, result.getCouponCount());
        assertEquals(2, result.getExchangeCouponCount());
    }

    @Test
    void degradesCouponCountsToZeroWhenMallFails() {
        MemberDTO member = new MemberDTO();
        member.setMemberLevel("diamond");
        when(memberService.getMemberByUserId(38L)).thenReturn(member);
        when(pointsMallService.getCouponSummary(anyLong())).thenThrow(new RuntimeException("mall down"));

        MemberDTO result = coordinator.getMemberProfile(38L);

        // 主体信息照常返回，券数量降级为 0
        assertEquals("diamond", result.getMemberLevel());
        assertEquals(0, result.getCouponCount());
        assertEquals(0, result.getExchangeCouponCount());
    }

    @Test
    void returnsNullWhenMemberNotFound() {
        when(memberService.getMemberByUserId(38L)).thenReturn(null);

        assertNull(coordinator.getMemberProfile(38L));
    }
}
