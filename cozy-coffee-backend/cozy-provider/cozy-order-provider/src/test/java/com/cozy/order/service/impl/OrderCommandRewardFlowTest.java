package com.cozy.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cozy.common.exception.BusinessException;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ShopOrderItemMapper;
import com.cozy.order.mapper.ShopOrderMapper;
import com.cozy.order.mq.OrderCompletedEventPublisher;
import com.cozy.order.service.order.OrderCommandService;
import com.cozy.order.service.converter.OrderDtoEnricher;
import com.cozy.order.service.order.OrderRewardService;
import com.cozy.order.service.order.PickupCodeService;
import com.cozy.order.service.infra.OrderCancelledEventPublisher;
import com.cozy.order.service.infra.OrderTimeoutIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 订单履约流程单测：出餐（自提/外送）、用户确认取餐/收货、发奖 CAS、幂等与归属校验。
 */
class OrderCommandRewardFlowTest {

    private ShopOrderMapper orderMapper;
    private ShopOrderItemMapper orderItemMapper;
    private OrderRewardService rewardService;
    private OrderDtoEnricher enricher;
    private OrderTimeoutIndexer timeoutIndexer;
    private OrderCancelledEventPublisher eventPublisher;
    private TransactionTemplate transactionTemplate;
    private OrderCompletedEventPublisher publisher;
    private OrderCommandService commandService;

    @BeforeEach
    void setUp() {
        orderMapper = mock(ShopOrderMapper.class);
        orderItemMapper = mock(ShopOrderItemMapper.class);
        rewardService = mock(OrderRewardService.class);
        enricher = mock(OrderDtoEnricher.class);
        timeoutIndexer = mock(OrderTimeoutIndexer.class);
        eventPublisher = mock(OrderCancelledEventPublisher.class);
        transactionTemplate = mock(TransactionTemplate.class);
        publisher = mock(OrderCompletedEventPublisher.class);
        commandService = new OrderCommandService(
                orderMapper, orderItemMapper, mock(CoffeeProductMapper.class),
                mock(PickupCodeService.class), rewardService, enricher,
                timeoutIndexer, eventPublisher,
                transactionTemplate, publisher);

        // 事务模板：直接执行 lambda 并返回其结果
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(mock(TransactionStatus.class));
        });
        // 商品/首单/新品检查默认值
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        when(orderMapper.selectCount(any())).thenReturn(1L);
        // DTO 映射
        when(enricher.toOrderDTO(any(), any())).thenAnswer(inv -> {
            ShopOrder o = inv.getArgument(0);
            ShopOrderDTO dto = new ShopOrderDTO();
            dto.setId(o.getId());
            dto.setStatus(o.getStatus());
            dto.setRewardsGranted(o.getRewardsGranted());
            dto.setExpEarned(o.getExpEarned());
            dto.setPointsEarned(o.getPointsEarned());
            return dto;
        });
    }

    // ==================== 出餐：仅履约不发奖 ====================

    @Test
    void completeOrder_pickupMarksCompletedWithoutPublishing() {
        ShopOrder order = order(108L, "preparing", "TAKEOUT", null, false, new BigDecimal("30"));
        when(orderMapper.selectById(108L)).thenReturn(order);

        ShopOrderDTO dto = commandService.completeOrder(108L);

        assertEquals("completed", dto.getStatus());
        assertFalse(Boolean.TRUE.equals(order.getRewardsGranted()));
        assertNull(order.getExpEarned());
        verify(publisher, never()).publish(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void completeOrder_deliveryEntersDeliveringWithoutPublishing() {
        ShopOrder order = order(109L, "preparing", "DELIVERY", null, false, new BigDecimal("30"));
        when(orderMapper.selectById(109L)).thenReturn(order);

        ShopOrderDTO dto = commandService.completeOrder(109L);

        assertEquals("delivering", dto.getStatus());
        verify(publisher, never()).publish(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    // ==================== 用户确认：归属校验 ====================

    @Test
    void confirmUserOrder_rejectsOtherOwnersOrder() {
        ShopOrder order = order(110L, "completed", "TAKEOUT", null, false, new BigDecimal("30"));
        order.setUserId(1L);
        when(orderMapper.selectById(110L)).thenReturn(order);

        BusinessException error = assertThrows(BusinessException.class,
                () -> commandService.confirmUserOrder(110L, 2L));
        assertTrue(error.getMessage().contains("无权确认"));
    }

    // ==================== 发奖：CAS 赢家发事件 ====================

    @Test
    void grantRewards_winnerSetsRewardsAndPublishes() {
        ShopOrder order = order(111L, "completed", "TAKEOUT", null, false, new BigDecimal("100"));
        order.setUserId(7L);
        when(orderMapper.selectById(anyLong())).thenReturn(order);
        when(orderMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(rewardService.getPointsRate("basic")).thenReturn(new BigDecimal("1.0"));

        commandService.grantRewards(111L);

        ArgumentCaptor<ShopOrder> updateCaptor = ArgumentCaptor.forClass(ShopOrder.class);
        verify(orderMapper).update(updateCaptor.capture(), any(Wrapper.class));
        assertEquals(Integer.valueOf(100), updateCaptor.getValue().getExpEarned());
        assertEquals(Integer.valueOf(100), updateCaptor.getValue().getPointsEarned());
        assertEquals(Boolean.TRUE, updateCaptor.getValue().getRewardsGranted());
        assertEquals("completed", updateCaptor.getValue().getStatus());

        verify(publisher).publish(eq(order), eq(100), eq(100), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void grantRewards_alreadyGranted_isIdempotentNoPublish() {
        ShopOrder order = order(112L, "completed", "TAKEOUT", 112L, true, new BigDecimal("100"));
        when(orderMapper.selectById(anyLong())).thenReturn(order);

        commandService.grantRewards(112L);

        verify(orderMapper, never()).update(any(), any(Wrapper.class));
        verify(publisher, never()).publish(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void grantRewards_casLoserDoesNotPublish() {
        ShopOrder order = order(113L, "completed", "TAKEOUT", 113L, false, new BigDecimal("100"));
        when(orderMapper.selectById(anyLong())).thenReturn(order);
        when(orderMapper.update(any(), any(Wrapper.class))).thenReturn(0);
        when(rewardService.getPointsRate("basic")).thenReturn(new BigDecimal("1.0"));

        commandService.grantRewards(113L);

        verify(orderMapper).update(any(), any(Wrapper.class));
        verify(publisher, never()).publish(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void grantRewards_wrongStatusDoesNotPublish() {
        ShopOrder order = order(114L, "preparing", "TAKEOUT", 114L, false, new BigDecimal("100"));
        when(orderMapper.selectById(anyLong())).thenReturn(order);

        commandService.grantRewards(114L);

        verify(orderMapper, never()).update(any(), any(Wrapper.class));
        verify(publisher, never()).publish(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    // ==================== 工具方法 ====================

    private ShopOrder order(Long id, String status, String diningMethod, Long expPointsId,
                            boolean rewardsGranted, BigDecimal payAmount) {
        ShopOrder order = new ShopOrder();
        order.setId(id);
        order.setOrderNo("CC" + id);
        order.setUserId(7L);
        order.setStatus(status);
        order.setDiningMethod(diningMethod);
        order.setPayAmount(payAmount);
        order.setTotalAmount(payAmount);
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setRewardsGranted(rewardsGranted);
        if (expPointsId != null) {
            order.setExpEarned(expPointsId.intValue());
            order.setPointsEarned(expPointsId.intValue());
        }
        return order;
    }
}
