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
    void relay_pendingMessage_refundsAndMarksSent() {
        PointsRefundOutbox msg = pendingMsg(1L, 500L, 7L, 100, 0);
        when(mapper.selectPendingDue(any(), anyInt())).thenReturn(List.of(msg));

        service.relayPendingRefunds();

        verify(memberService).refundPointsByConsumption(7L, 100, "redeem", 500L, "取消退款");
        verify(mapper).markSent(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void relay_refundFails_schedulesRetry() {
        PointsRefundOutbox msg = pendingMsg(2L, 501L, 7L, 80, 0);
        when(mapper.selectPendingDue(any(), anyInt())).thenReturn(List.of(msg));
        doThrow(new RuntimeException("member unavailable")).when(memberService)
                .refundPointsByConsumption(7L, 80, "redeem", 501L, "取消退款");

        service.relayPendingRefunds();

        verify(mapper).markFailed(eq(2L), eq("PENDING"), eq(1),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    private PointsRefundOutbox pendingMsg(Long id, Long orderId, Long userId, int points, int retryCount) {
        PointsRefundOutbox m = new PointsRefundOutbox();
        m.setId(id);
        m.setOrderId(orderId);
        m.setUserId(userId);
        m.setPoints(points);
        m.setConsumeType("redeem");
        m.setDescription("取消退款");
        m.setRetryCount(retryCount);
        m.setStatus("PENDING");
        m.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        return m;
    }
}
