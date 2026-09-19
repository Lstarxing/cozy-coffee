package com.cozy.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 发券事件是「member 出箱 → MQ → mall 消费」的跨服务契约。
 * 出箱投递的是 JSON 字符串、消费端按对象反序列化，所以必须验证 JSON 往返不丢字段，
 * 否则会出现"券类型/幂等键为空"之类的残缺对象且难以察觉。
 */
class CouponGrantRequestedEventTest {

    @Test
    void survivesJsonRoundTripWithAllFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CouponGrantRequestedEvent event = CouponGrantRequestedEvent.builder()
                .userId(38L)
                .couponType("BOGO")
                .uniqueKey("signin_7day_9")
                .minAmount(100)
                .discountAmount(40)
                .validDays(30)
                .source("signin_7day")
                .build();

        String json = mapper.writeValueAsString(event);
        CouponGrantRequestedEvent restored = mapper.readValue(json, CouponGrantRequestedEvent.class);

        assertEquals(event.getUserId(), restored.getUserId());
        assertEquals("BOGO", restored.getCouponType());
        assertEquals("signin_7day_9", restored.getUniqueKey());
        assertEquals(100, restored.getMinAmount());
        assertEquals(40, restored.getDiscountAmount());
        assertEquals(30, restored.getValidDays());
        assertEquals("signin_7day", restored.getSource());
    }
}
