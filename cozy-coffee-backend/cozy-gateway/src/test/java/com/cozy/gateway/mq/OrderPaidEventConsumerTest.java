package com.cozy.gateway.mq;

import com.cozy.common.mq.OrderPaidEvent;
import com.cozy.gateway.cache.AdminOrderCacheEvictor;
import com.cozy.gateway.sse.SseEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * ORDER_PAID 是「商家可以开始做这一单了」的**唯一**信号：必须既清缓存、又推管理端 SSE。
 * 若哪天它退化成只清缓存，商家就再也收不到提醒了 —— 这条测试专门守这件事
 * （对照：OrderCreatedEventConsumer 已刻意不推 SSE）。
 */
class OrderPaidEventConsumerTest {

    private SseEventPublisher sseEventPublisher;
    private AdminOrderCacheEvictor cacheEvictor;
    private OrderPaidEventConsumer consumer;

    @BeforeEach
    void setUp() {
        sseEventPublisher = mock(SseEventPublisher.class);
        cacheEvictor = mock(AdminOrderCacheEvictor.class);
        consumer = new OrderPaidEventConsumer(sseEventPublisher, cacheEvictor);
    }

    @Test
    void publishesSseAndEvictsCacheOnPaid() {
        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(310L)
                .orderNo("CF202609211125275585653")
                .username("苏瑞鑫")
                .payAmount(new BigDecimal("68.00"))
                .itemCount(2)
                .build();

        consumer.onMessage(event);

        verify(cacheEvictor).evictAll();
        verify(sseEventPublisher).publishNewOrderEnhanced(
                310L, "CF202609211125275585653", "苏瑞鑫", new BigDecimal("68.00"), 2);
    }

    @Test
    void ignoresMalformedEvent() {
        consumer.onMessage(null);
        // 缺 orderId：不足以定位订单，什么都不该做
        consumer.onMessage(OrderPaidEvent.builder().orderNo("CF2026").build());

        verifyNoInteractions(cacheEvictor);
        verifyNoInteractions(sseEventPublisher);
    }
}
