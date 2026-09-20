package com.cozy.user.mq;

import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.user.api.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 订单完成 → 邀请资格认领（ADR 0001 §8 第 7 步的新拓扑）。
 *
 * <p>这一步原先由 member 的 `FirstOrderConsumer` 通过 Dubbo 回调 user 完成，
 * 构成 `member → user` 反向依赖；现在 user 自己消费 `ORDER_COMPLETED`。
 */
class OrderCompletedInviteConsumerTest {

    private UserService userService;
    private OrderCompletedInviteConsumer consumer;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        consumer = new OrderCompletedInviteConsumer(userService);
    }

    private OrderCompletedEvent order(boolean firstOrder) {
        OrderCompletedEvent event = new OrderCompletedEvent();
        event.setUserId(38L);
        event.setOrderId(501L);
        event.setIsFirstOrder(firstOrder);
        return event;
    }

    /** 首单 → 认领资格（幂等条件更新与入队都在 grantInviteRewardOnFirstOrder 内部）。 */
    @Test
    void firstOrderClaimsQualification() {
        when(userService.grantInviteRewardOnFirstOrder(38L)).thenReturn(true);

        consumer.onMessage(order(true));

        verify(userService).grantInviteRewardOnFirstOrder(38L);
    }

    /** 非首单 → 完全不碰。与原来 member 侧回调的条件一致：邀请奖励绑在**首单**上。 */
    @Test
    void nonFirstOrderIsIgnored() {
        consumer.onMessage(order(false));

        verifyNoInteractions(userService);
    }

    /**
     * 认领失败必须**抛出** → RocketMQ 重投。
     *
     * <p>吞掉等于消息被误 ack，而首单只有一次，用户再也拿不到邀请券
     * （这条语义与迁移前 member 侧的写法一致，不能因为换了消费者就退回"吞异常"）。
     */
    @Test
    void failurePropagatesForRetry() {
        when(userService.grantInviteRewardOnFirstOrder(38L))
                .thenThrow(new IllegalStateException("user db down"));

        assertThrows(IllegalStateException.class, () -> consumer.onMessage(order(true)));
    }
}
