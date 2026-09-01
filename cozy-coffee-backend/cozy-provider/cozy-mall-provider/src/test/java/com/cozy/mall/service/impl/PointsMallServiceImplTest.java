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
import com.cozy.member.api.AddressService;
import com.cozy.member.api.MemberService;
import com.cozy.order.api.OrderService;
import com.cozy.user.api.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

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
}
