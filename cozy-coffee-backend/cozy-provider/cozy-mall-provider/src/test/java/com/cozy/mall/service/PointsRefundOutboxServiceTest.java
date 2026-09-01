package com.cozy.mall.service;

import com.cozy.mall.entity.PointsRefundOutbox;
import com.cozy.mall.mapper.PointsRefundOutboxMapper;
import com.cozy.member.api.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PointsRefundOutboxServiceTest {

    @Mock private PointsRefundOutboxMapper mapper;
    @Mock private MemberService memberService;
    @InjectMocks private PointsRefundOutboxService service;

    @BeforeEach
    void injectDubboRef() {
        ReflectionTestUtils.setField(service, "memberService", memberService);
    }

    @Test
    void relay_claimedMessage_refundsAndMarksSent() {
        PointsRefundOutbox msg = new PointsRefundOutbox();
        msg.setId(1L);
        msg.setOrderId(500L);
        msg.setUserId(7L);
        msg.setPoints(100);
        msg.setConsumeType("redeem");
        msg.setDescription("取消退款");
        msg.setRetryCount(0);
        when(mapper.selectRelayCandidates(any(), any(), anyInt())).thenReturn(List.of(msg));
        when(mapper.claim(eq(1L), any(), any())).thenReturn(1);

        service.relayPendingRefunds();

        verify(memberService).refundPointsByConsumption(7L, 100, "redeem", 500L, "取消退款");
        verify(mapper).markSent(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void relay_refundFails_releasesLeaseAndSchedulesRetry() {
        PointsRefundOutbox msg = new PointsRefundOutbox();
        msg.setId(2L);
        msg.setOrderId(501L);
        msg.setUserId(7L);
        msg.setPoints(80);
        msg.setConsumeType("redeem");
        msg.setDescription("取消退款");
        msg.setRetryCount(0);
        when(mapper.selectRelayCandidates(any(), any(), anyInt())).thenReturn(List.of(msg));
        when(mapper.claim(eq(2L), any(), any())).thenReturn(1);
        doThrow(new RuntimeException("member unavailable")).when(memberService)
                .refundPointsByConsumption(7L, 80, "redeem", 501L, "取消退款");

        service.relayPendingRefunds();

        verify(mapper).markFailed(eq(2L), eq("PENDING"), eq(1),
                any(LocalDateTime.class), eq("member unavailable"), any(LocalDateTime.class));
    }

    @Test
    void listDeadRefunds_clampsLimitAndMapsAuditFields() {
        PointsRefundOutbox msg = new PointsRefundOutbox();
        msg.setId(2L);
        msg.setOrderId(501L);
        msg.setUserId(7L);
        msg.setPoints(80);
        msg.setStatus("DEAD");
        msg.setRetryCount(10);
        msg.setManualRetryCount(1);
        msg.setLastError("member unavailable");
        when(mapper.selectDeadBatch(200)).thenReturn(List.of(msg));

        var result = service.listDeadRefunds(999);

        assertEquals(1, result.size());
        assertEquals(501L, result.get(0).getOrderId());
        assertEquals(1, result.get(0).getManualRetryCount());
    }

    @Test
    void retryDeadRefund_usesCasAndRecordsOperator() {
        when(mapper.retryDead(any(), any(), any())).thenReturn(1);

        service.retryDeadRefund(2L, 9L);

        verify(mapper).retryDead(eq(2L), eq(9L), any(LocalDateTime.class));
    }

    @Test
    void retryDeadRefund_nonDead_rejects() {
        when(mapper.retryDead(any(), any(), any())).thenReturn(0);

        assertThrows(com.cozy.common.exception.BusinessException.class,
                () -> service.retryDeadRefund(2L, 9L));
    }
}
