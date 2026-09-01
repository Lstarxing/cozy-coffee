package com.cozy.order.service;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.mq.OutboxService;
import com.cozy.order.service.infra.OrderCancelledEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class OrderCancelledEventPublisherTest {

    private OutboxService outboxService;
    private OrderCancelledEventPublisher publisher;

    @BeforeEach
    void setUp() {
        outboxService = Mockito.mock(OutboxService.class);
        publisher = new OrderCancelledEventPublisher(outboxService, new ObjectMapper());
    }

    @Test
    void publishCouponRollbackEvent_orderInsertFailed_usesNonNullFallbackAggregateId() {
        ShopOrder order = new ShopOrder();
        order.setUserId(43L);
        order.setAppliedCouponId(999L); // 订单未落库：id 为 null

        publisher.publishCouponRollbackEvent(order, 10001L); // fallback operationId

        ArgumentCaptor<Long> aggCaptor = ArgumentCaptor.forClass(Long.class);
        verify(outboxService).publish(eq(MqTopics.ORDER_EVENTS), eq(MqTags.ORDER_CANCELLED),
                eq("coupon_rollback"), aggCaptor.capture(), Mockito.any());
        assertNotNull(aggCaptor.getValue());
        assertEquals(10001L, aggCaptor.getValue().longValue());
    }
}
