package com.cozy.mall.service.impl;

import com.cozy.common.constant.CouponTemplateConfig;
import com.cozy.mall.coupon.CouponCalculator;
import com.cozy.mall.coupon.CouponCombinationService;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.mapper.CouponRollbackInboxMapper;
import com.cozy.mall.mapper.MonthlyRedemptionMapper;
import com.cozy.mall.mapper.PointsOrderFulfillmentMapper;
import com.cozy.mall.mapper.PointsOrderMapper;
import com.cozy.mall.mapper.PointsProductMapper;
import com.cozy.mall.mapper.UserCouponMapper;
import com.cozy.mall.service.PointsRefundOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 新人券发放的数据库级幂等（ADR 0001 C3）：唯一索引 uk_coupon_code 是最后一道防线，
 * 消费者侧必须把它当成功吸收，而不是把 DuplicateKeyException 抛给上游。
 */
class NewUserCouponIdempotencyTest {

    private UserCouponMapper userCouponMapper;
    private PointsMallServiceImpl service;

    @BeforeEach
    void setUp() {
        userCouponMapper = mock(UserCouponMapper.class);
        service = new PointsMallServiceImpl(
                mock(PointsProductMapper.class), mock(PointsOrderMapper.class),
                mock(MonthlyRedemptionMapper.class), mock(PointsOrderFulfillmentMapper.class),
                userCouponMapper, mock(CouponRollbackInboxMapper.class), mock(PointsRefundOutboxService.class),
                mock(RedisTemplate.class), mock(StringRedisTemplate.class),
                new ObjectMapper(), new CouponTemplateConfig(), new HashMap<String, CouponCalculator>(),
                mock(CouponCombinationService.class));
    }

    /** 并发/重投导致唯一索引冲突时，按幂等吸收，不向上游抛异常。 */
    @Test
    void duplicateKey_isAbsorbedAsIdempotent() {
        when(userCouponMapper.selectCount(any())).thenReturn(0L);
        when(userCouponMapper.insert(any(UserCoupon.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry 'NEW_USER_COUPON_7' for key 'uk_coupon_code'"));

        assertDoesNotThrow(() -> service.issueNewUserCoupon(7L));
    }

    /** 先查命中时直接返回，连 insert 都不该发生。 */
    @Test
    void alreadyIssued_skipsInsertEntirely() {
        when(userCouponMapper.selectCount(any())).thenReturn(1L);

        service.issueNewUserCoupon(7L);

        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    /** 券码即幂等键，必须仍是 NEW_USER_COUPON_{userId}（ADR 0001 C2：不得因事件化重造）。 */
    @Test
    void newUserCoupon_usesLegacyCouponCodeAsIdempotencyKey() {
        when(userCouponMapper.selectCount(any())).thenReturn(0L);
        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);

        service.issueNewUserCoupon(7L);

        verify(userCouponMapper).insert(captor.capture());
        UserCoupon inserted = captor.getValue();
        assertEquals("NEW_USER_COUPON_7", inserted.getCouponCode());
        assertEquals("DISCOUNT", inserted.getCouponType());
    }
}
