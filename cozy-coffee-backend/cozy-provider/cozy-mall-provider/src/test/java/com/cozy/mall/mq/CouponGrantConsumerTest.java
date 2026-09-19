package com.cozy.mall.mq;

import com.cozy.common.mq.CouponGrantRequestedEvent;
import com.cozy.mall.api.PointsMallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CouponGrantConsumerTest {

    private PointsMallService pointsMallService;
    private CouponGrantConsumer consumer;

    @BeforeEach
    void setUp() {
        pointsMallService = mock(PointsMallService.class);
        consumer = new CouponGrantConsumer(pointsMallService);
    }

    @Test
    void grantsCouponUsingTheEventIdempotencyKey() {
        consumer.onMessage(CouponGrantRequestedEvent.builder()
                .userId(38L).couponType("BOGO").uniqueKey("signin_7day_9")
                .minAmount(0).discountAmount(40).validDays(30).source("signin_7day")
                .build());

        // 幂等键原样透传：mall 侧按 user_coupon.coupon_code 去重，重复投递不会重复发券
        verify(pointsMallService).issueCouponToUser(eq(38L), eq("BOGO"), eq("signin_7day_9"),
                eq(0.0), eq(40.0), eq(30));
    }

    @Test
    void rethrowsOnFailureSoRocketMqCanRedeliver() {
        doThrow(new RuntimeException("db down")).when(pointsMallService)
                .issueCouponToUser(anyLong(), anyString(), anyString(), anyDouble(), anyDouble(), anyInt());

        assertThrows(IllegalStateException.class, () -> consumer.onMessage(
                CouponGrantRequestedEvent.builder()
                        .userId(38L).couponType("BOGO").uniqueKey("signin_7day_9").validDays(30)
                        .build()));
    }

    @Test
    void rejectsEventWithoutIdempotencyKey() {
        assertThrows(IllegalArgumentException.class, () -> consumer.onMessage(
                CouponGrantRequestedEvent.builder().userId(38L).build()));
    }
}
