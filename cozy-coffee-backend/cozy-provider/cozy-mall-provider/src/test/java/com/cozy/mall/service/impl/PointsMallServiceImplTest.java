package com.cozy.mall.service.impl;

import com.cozy.common.constant.CouponTemplateConfig;
import com.cozy.common.exception.BusinessException;
import com.cozy.mall.coupon.CouponCalculator;
import com.cozy.mall.coupon.CouponCombinationService;
import com.cozy.mall.dto.request.RedeemRequest;
import com.cozy.mall.entity.PointsProduct;
import com.cozy.mall.mapper.MonthlyRedemptionMapper;
import com.cozy.mall.mapper.PointsOrderFulfillmentMapper;
import com.cozy.mall.mapper.PointsOrderMapper;
import com.cozy.mall.mapper.PointsProductMapper;
import com.cozy.mall.mapper.UserCouponMapper;
import com.cozy.mall.mapper.CouponRollbackInboxMapper;
import com.cozy.mall.service.PointsRefundOutboxService;
import com.cozy.member.api.AddressService;
import com.cozy.member.api.MemberService;
import com.cozy.order.api.OrderService;
import com.cozy.user.api.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsMallServiceImplTest {

    @Mock private PointsProductMapper productMapper;
    @Mock private PointsOrderMapper orderMapper;
    @Mock private MonthlyRedemptionMapper monthlyRedemptionMapper;
    @Mock private PointsOrderFulfillmentMapper fulfillmentMapper;
    @Mock private UserCouponMapper userCouponMapper;
    @Mock private CouponRollbackInboxMapper couponRollbackInboxMapper;
    @Mock private PointsRefundOutboxService pointsRefundOutboxService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private CouponTemplateConfig couponTemplateConfig;
    @Mock private Map<String, CouponCalculator> couponCalculators;
    @Mock private CouponCombinationService couponCombinationService;
    @Mock private MemberService memberService;
    @Mock private AddressService addressService;
    @Mock private OrderService orderService;
    @Mock private UserService userService;

    @InjectMocks private PointsMallServiceImpl pointsMallService;

    @BeforeEach
    void injectDubboRefs() {
        // @DubboReference 字段不在 @RequiredArgsConstructor 构造里，@InjectMocks 不会注入
        ReflectionTestUtils.setField(pointsMallService, "memberService", memberService);
        ReflectionTestUtils.setField(pointsMallService, "addressService", addressService);
        ReflectionTestUtils.setField(pointsMallService, "orderService", orderService);
        ReflectionTestUtils.setField(pointsMallService, "userService", userService);
    }

    private PointsProduct product(Long id, int stock, int pointsPrice) {
        PointsProduct p = new PointsProduct();
        p.setId(id);
        p.setStatus("active");
        p.setStock(stock);
        p.setPointsPrice(pointsPrice);
        return p;
    }

    @Test
    void redeem_stockInsufficient_deductStockReturnsZero_throws() {
        // 库存行存在且充足，但条件更新返回 0 —— 模拟并发下库存已被其他请求扣光
        when(productMapper.selectById(100L)).thenReturn(product(100L, 5, 100));
        when(productMapper.deductStock(100L, 1)).thenReturn(0);

        RedeemRequest req = new RedeemRequest();
        req.setProductId(100L);
        req.setQuantity(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pointsMallService.redeem(1L, req));
        assertTrue(ex.getMessage().contains("库存不足"));
    }

    @Test
    void redeem_negativeQuantity_rejected() {
        RedeemRequest req = new RedeemRequest();
        req.setProductId(100L);
        req.setQuantity(-1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pointsMallService.redeem(1L, req));
        assertTrue(ex.getMessage().contains("兑换数量不合法"));
    }

    @Test
    void cancelOrder_winner_restoresStockAndRefundsOnce() {
        com.cozy.mall.entity.PointsOrder order = new com.cozy.mall.entity.PointsOrder();
        order.setId(500L);
        order.setUserId(1L);
        order.setProductId(100L);
        order.setQuantity(2);
        order.setPointsCost(100);
        order.setStatus("pending");
        order.setProductName("测试商品");

        when(orderMapper.selectById(500L)).thenReturn(order);
        when(orderMapper.cancelOrderIfPending(org.mockito.ArgumentMatchers.eq(500L),
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(productMapper.addStock(100L, 2)).thenReturn(1); // 原子恢复，不再读后覆盖

        pointsMallService.cancelOrder(500L, 1L);

        org.mockito.Mockito.verify(pointsRefundOutboxService).enqueue(
                org.mockito.ArgumentMatchers.eq(500L),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.eq("redeem"),
                org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.verify(productMapper).addStock(100L, 2);
    }

    @Test
    void cancelOrder_alreadyCancelled_returnsWithoutDoubleRestore() {
        com.cozy.mall.entity.PointsOrder order = new com.cozy.mall.entity.PointsOrder();
        order.setId(500L);
        order.setUserId(1L);
        order.setStatus("cancelled");
        when(orderMapper.selectById(500L)).thenReturn(order);
        when(orderMapper.cancelOrderIfPending(org.mockito.ArgumentMatchers.eq(500L),
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(0);

        pointsMallService.cancelOrder(500L, 1L);

        org.mockito.Mockito.verify(pointsRefundOutboxService, org.mockito.Mockito.never())
                .enqueue(org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.verify(productMapper, org.mockito.Mockito.never()).addStock(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void rollbackCoupons_duplicateEvent_skipsCouponMutation() {
        when(couponRollbackInboxMapper.insertIfAbsent(
                org.mockito.ArgumentMatchers.eq("operation:1"), org.mockito.ArgumentMatchers.any())).thenReturn(0);

        pointsMallService.rollbackCoupons("operation:1", null, 1L, 10L, java.util.List.of(11L));

        org.mockito.Mockito.verify(userCouponMapper, org.mockito.Mockito.never()).selectById(
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rollbackCoupons_missingUser_rejectsBeforeRecordingInbox() {
        assertThrows(BusinessException.class,
                () -> pointsMallService.rollbackCoupons("operation:1", null, null, 10L, java.util.List.of()));

        org.mockito.Mockito.verifyNoInteractions(couponRollbackInboxMapper);
    }

    @Test
    void rollbackCoupons_ownerMismatch_failsInsteadOfAcknowledgingEvent() {
        com.cozy.mall.entity.UserCoupon coupon = new com.cozy.mall.entity.UserCoupon();
        coupon.setId(10L);
        coupon.setUserId(2L);
        coupon.setStatus("FROZEN");
        when(couponRollbackInboxMapper.insertIfAbsent(
                org.mockito.ArgumentMatchers.eq("operation:1"), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(userCouponMapper.selectById(10L)).thenReturn(coupon);

        assertThrows(BusinessException.class,
                () -> pointsMallService.rollbackCoupons("operation:1", null, 1L, 10L, java.util.List.of()));
        org.mockito.Mockito.verify(userCouponMapper, org.mockito.Mockito.never()).updateById(
                org.mockito.ArgumentMatchers.any(com.cozy.mall.entity.UserCoupon.class));
    }

    @Test
    void rollbackCoupons_firstDelivery_rollsBackWholeCouponSet() {
        com.cozy.mall.entity.UserCoupon main = new com.cozy.mall.entity.UserCoupon();
        main.setId(10L);
        main.setUserId(1L);
        main.setStatus("FROZEN");
        com.cozy.mall.entity.UserCoupon addon = new com.cozy.mall.entity.UserCoupon();
        addon.setId(11L);
        addon.setUserId(1L);
        addon.setStatus("USED");
        when(couponRollbackInboxMapper.insertIfAbsent(
                org.mockito.ArgumentMatchers.eq("order:500"), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(userCouponMapper.selectById(10L)).thenReturn(main);
        when(userCouponMapper.selectById(11L)).thenReturn(addon);

        pointsMallService.rollbackCoupons("order:500", 500L, 1L, 10L, java.util.List.of(11L));

        org.junit.jupiter.api.Assertions.assertEquals("ISSUED", main.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("ISSUED", addon.getStatus());
        org.mockito.Mockito.verify(userCouponMapper).updateById(main);
        org.mockito.Mockito.verify(userCouponMapper).updateById(addon);
    }
}
