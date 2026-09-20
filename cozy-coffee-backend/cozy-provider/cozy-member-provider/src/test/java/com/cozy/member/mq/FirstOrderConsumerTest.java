package com.cozy.member.mq;

import com.cozy.common.constant.FirstOrderRewardConfig;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.member.api.MemberService;
import com.cozy.member.mapper.PointsLotMapper;
import com.cozy.user.api.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 首单消费的两个 P1 守卫（2026-09-20 评审提出）。
 *
 * <p>核心：**积分与邀请奖励是两件独立的事**。
 * <ul>
 *   <li>积分已发 / 并发冲突 → 只跳过积分写入，**必须继续**调 user —— 否则"积分成功 + 邀请侧失败"
 *       之后重投会被提前 return 截断，邀请券永久丢失</li>
 *   <li>邀请侧失败 → **抛出**（RocketMQ 重投），不能吞成 warn —— 吞掉等于消息被误 ack，
 *       而首单只有一次，用户再也拿不到邀请券</li>
 * </ul>
 */
class FirstOrderConsumerTest {

    private MemberService memberService;
    private PointsLotMapper pointsLotMapper;
    private UserService userService;
    private FirstOrderConsumer consumer;
    private final FirstOrderRewardConfig config = new FirstOrderRewardConfig();

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        pointsLotMapper = mock(PointsLotMapper.class);
        userService = mock(UserService.class);
        consumer = new FirstOrderConsumer(memberService, pointsLotMapper, config);
        ReflectionTestUtils.setField(consumer, "userService", userService);
    }

    private OrderCompletedEvent firstOrder() {
        OrderCompletedEvent event = new OrderCompletedEvent();
        event.setUserId(38L);
        event.setOrderId(501L);
        event.setIsFirstOrder(true);
        return event;
    }

    /** 积分已发过（例如上次投递积分成功、邀请侧失败）→ 跳过积分写入，但**必须**继续调 user。 */
    @Test
    void pointsAlreadyGranted_stillCallsInviteReward() {
        when(pointsLotMapper.selectCount(any())).thenReturn(1L);

        consumer.onMessage(firstOrder());

        verify(memberService, never()).addPointsWithLot(
                anyLong(), anyInt(), anyString(), anyLong(), anyString());
        verify(userService, times(1)).grantInviteRewardOnFirstOrder(38L);
    }

    /** 积分并发冲突（唯一键拦截）→ 同样只跳过积分，继续调 user。 */
    @Test
    void pointsDuplicateKey_stillCallsInviteReward() {
        when(pointsLotMapper.selectCount(any())).thenReturn(0L);
        doThrow(new DuplicateKeyException("duplicate")).when(memberService)
                .addPointsWithLot(anyLong(), anyInt(), anyString(), anyLong(), anyString());

        consumer.onMessage(firstOrder());

        verify(userService, times(1)).grantInviteRewardOnFirstOrder(38L);
    }

    /** 邀请侧失败必须抛出 → 触发 RocketMQ 重投。 */
    @Test
    void inviteRewardFailure_propagates() {
        when(pointsLotMapper.selectCount(any())).thenReturn(0L);
        when(userService.grantInviteRewardOnFirstOrder(38L))
                .thenThrow(new IllegalStateException("user-provider down"));

        assertThrows(IllegalStateException.class, () -> consumer.onMessage(firstOrder()));
    }

    /** 非首单：完全跳过，连积分幂等查询都不做。 */
    @Test
    void notFirstOrder_skipsEverything() {
        OrderCompletedEvent event = firstOrder();
        event.setIsFirstOrder(false);

        consumer.onMessage(event);

        verifyNoInteractions(pointsLotMapper, memberService, userService);
    }
}
