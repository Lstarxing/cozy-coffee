package com.cozy.gateway.service;

import com.cozy.common.exception.BusinessException;
import com.cozy.common.mq.OrderPaidEvent;
import com.cozy.gateway.mq.OrderEventProducer;
import com.cozy.member.api.MemberService;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.ShopOrderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付（= 用户侧接单）**成功才**发 ORDER_PAID，失败一定不发 ——
 * 否则商家会收到未付款/失败订单的提醒，正是这次要消除的噪声。
 * <p>
 * 发布点必须在 provider 的 {@code @Transactional} 之外（即本类的 Dubbo 调用之后）：
 * 放进 provider 方法体内就是「提交前发布」，一旦回滚就会通知商家"已付款"。
 */
class OrderCoordinatorServiceTest {

    private OrderService orderService;
    private MemberService memberService;
    private OrderEventProducer orderEventProducer;
    private OrderCoordinatorService coordinator;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        memberService = mock(MemberService.class);
        orderEventProducer = mock(OrderEventProducer.class);
        coordinator = new OrderCoordinatorService(orderEventProducer);
        // 两个 @DubboReference 字段是非 final 的，只能反射注入（同 AdminListServiceTest 的做法）
        ReflectionTestUtils.setField(coordinator, "orderService", orderService);
        ReflectionTestUtils.setField(coordinator, "memberService", memberService);
    }

    private ShopOrderDTO paidOrder() {
        ShopOrderDTO order = new ShopOrderDTO();
        order.setId(310L);
        order.setOrderNo("CF202606");
        order.setPayAmount(new BigDecimal("68.00"));
        order.setTotalQuantity(2);
        return order;
    }

    @Test
    void publishesOrderPaidAfterAcceptSucceeds() {
        when(orderService.acceptUserOrder(310L, 3L)).thenReturn(paidOrder());

        coordinator.acceptUserOrder(3L, 310L);

        ArgumentCaptor<OrderPaidEvent> captor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(orderEventProducer).publishOrderPaid(captor.capture());
        assertEquals(310L, captor.getValue().getOrderId());
        assertEquals("CF202606", captor.getValue().getOrderNo());
        assertEquals(new BigDecimal("68.00"), captor.getValue().getPayAmount());
        assertEquals(2, captor.getValue().getItemCount());
    }

    @Test
    void doesNotPublishOrderPaidWhenAcceptFails() {
        when(orderService.acceptUserOrder(anyLong(), anyLong()))
                .thenThrow(new BusinessException("订单状态流转不合法"));

        assertThrows(BusinessException.class, () -> coordinator.acceptUserOrder(3L, 310L));

        verify(orderEventProducer, never()).publishOrderPaid(any());
    }
}
