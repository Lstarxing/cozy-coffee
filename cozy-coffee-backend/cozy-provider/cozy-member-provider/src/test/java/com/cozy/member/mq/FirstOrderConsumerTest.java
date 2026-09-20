package com.cozy.member.mq;

import com.cozy.common.constant.FirstOrderRewardConfig;
import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.member.api.MemberService;
import com.cozy.member.mapper.PointsLotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 首单消费者：现在**只管首单积分**这一件事。
 *
 * <p>「邀请奖励资格认领」自 ADR 0001 §8 第 7 步起移到 user 侧（`OrderCompletedInviteConsumer`），
 * 本消费者不再回调 user —— 所以这里不再有"积分失败也要继续调 user"那类守卫，
 * 取而代之的是**结构守卫**：它不该再持有任何 `com.cozy.user` 类型。
 */
class FirstOrderConsumerTest {

    private MemberService memberService;
    private PointsLotMapper pointsLotMapper;
    private FirstOrderConsumer consumer;
    private final FirstOrderRewardConfig config = new FirstOrderRewardConfig();

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        pointsLotMapper = mock(PointsLotMapper.class);
        consumer = new FirstOrderConsumer(memberService, pointsLotMapper, config);
    }

    private OrderCompletedEvent firstOrder() {
        OrderCompletedEvent event = new OrderCompletedEvent();
        event.setUserId(38L);
        event.setOrderId(501L);
        event.setIsFirstOrder(true);
        return event;
    }

    /** 积分已发过 → 跳过写入（重投安全）。 */
    @Test
    void pointsAlreadyGranted_skipsWrite() {
        when(pointsLotMapper.selectCount(any())).thenReturn(1L);

        consumer.onMessage(firstOrder());

        verifyNoInteractions(memberService);
    }

    /** 并发下的唯一键冲突 → 吸收，不抛出（重投会看到已存在而跳过）。 */
    @Test
    void pointsDuplicateKey_isAbsorbed() {
        when(pointsLotMapper.selectCount(any())).thenReturn(0L);
        doThrow(new DuplicateKeyException("duplicate")).when(memberService)
                .addPointsWithLot(anyLong(), anyInt(), anyString(), anyLong(), anyString());

        assertDoesNotThrow(() -> consumer.onMessage(firstOrder()));
    }

    /** 非首单：完全跳过，连积分幂等查询都不做。 */
    @Test
    void notFirstOrder_skipsEverything() {
        OrderCompletedEvent event = firstOrder();
        event.setIsFirstOrder(false);

        consumer.onMessage(event);

        verifyNoInteractions(pointsLotMapper, memberService);
    }

    /**
     * 结构守卫：本消费者不得再持有 user 域类型。
     *
     * <p>ADR §8 第 7 步删掉了 `FirstOrderConsumer` 回调 `userService` 那一跳；
     * 一旦有人把 `@DubboReference UserService` 加回来，这条会立刻红（比"某条 verify 没调到"清楚得多）。
     */
    @Test
    void consumerHoldsNoUserTypes() {
        for (Field field : FirstOrderConsumer.class.getDeclaredFields()) {
            assertFalse(field.getType().getName().startsWith("com.cozy.user"),
                    "FirstOrderConsumer 不该再持有 user 类型：" + field.getName()
                            + "（邀请资格认领归 user 侧自己消费 ORDER_COMPLETED，见 docs/adr/0001 §8 第 7 步）");
        }
    }
}
