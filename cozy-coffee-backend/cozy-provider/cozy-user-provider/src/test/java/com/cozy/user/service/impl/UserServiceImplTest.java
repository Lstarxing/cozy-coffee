package com.cozy.user.service.impl;

import com.cozy.common.constant.ProfileRewardConfig;
import com.cozy.common.exception.BusinessException;
import com.cozy.common.mq.InviteRewardEarnedEvent;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.WelcomeGiftEligibleEvent;
import com.cozy.member.api.MemberService;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;
import com.cozy.user.entity.User;
import com.cozy.user.mapper.UserMapper;
import com.cozy.user.mq.UserEventOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * user 域读接口的边界守卫：listAllUsers/getUserDetail 只返回 user 域字段。
 *
 * <p>会员等级/积分改由网关 AdminUserProfileCoordinator 组合（见 docs/adr/0001）。
 * 这两条测试防的是"逐用户反查 member"的 N+1 悄悄长回来 —— 那种写法在 member 不可用时会
 * 静默降级成 basic/0/0，管理端看到的是假数据。
 */
class UserServiceImplTest {

    private UserMapper userMapper;
    private MemberService memberService;
    private UserEventOutboxService userEventOutboxService;
    private UserServiceImpl userService;
    private ProfileRewardConfig profileRewardConfig;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        memberService = mock(MemberService.class);
        userEventOutboxService = mock(UserEventOutboxService.class);
        profileRewardConfig = new ProfileRewardConfig();
        userService = new UserServiceImpl(userMapper, mock(StringRedisTemplate.class),
                profileRewardConfig, userEventOutboxService);
        ReflectionTestUtils.setField(userService, "memberService", memberService);
    }

    private User entity(long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("u" + id);
        user.setNickname("nick" + id);
        return user;
    }

    @Test
    void listAllUsersDoesNotQueryMember() {
        when(userMapper.selectList(any())).thenReturn(List.of(entity(1L), entity(2L)));

        List<UserDTO> users = userService.listAllUsers();

        assertEquals(2, users.size());
        assertEquals("u1", users.get(0).getUsername());
        assertNull(users.get(0).getMemberLevel());
        assertNull(users.get(0).getCurrentPoints());
        assertNull(users.get(0).getTotalPoints());
        verifyNoInteractions(memberService);
    }

    @Test
    void getUserDetailDoesNotQueryMember() {
        when(userMapper.selectById(7L)).thenReturn(entity(7L));

        UserDTO dto = userService.getUserDetail(7L);

        assertEquals("u7", dto.getUsername());
        assertNull(dto.getMemberLevel());
        assertNull(dto.getCurrentPoints());
        assertNull(dto.getTotalPoints());
        verifyNoInteractions(memberService);
    }

    /** 用户不存在仍是业务失败，不因为去掉会员查询而改变。 */
    @Test
    void getUserDetailStillThrowsWhenUserMissing() {
        when(userMapper.selectById(7L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> userService.getUserDetail(7L));
    }

    // ==================== updateProfile 的事务边界（ADR 0001 的 C1） ====================

    /**
     * 完善资料奖励必须等事务提交后才派发。
     * 原先是裸 CompletableFuture，事务一回滚积分就已经加出去了、无法撤销。
     */
    @Test
    void profileRewardIsDispatchedOnlyAfterCommit() {
        when(userMapper.selectById(7L)).thenReturn(entity(7L)); // phone/email 为空 -> 算首次填写

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("13800000000");
        request.setEmail("probe@example.com");

        TransactionSynchronizationManager.initSynchronization();
        try {
            userService.updateProfile(7L, request);

            // 事务还没提交：奖励绝不能已经发出去
            verify(memberService, never()).addPointsWithLot(anyLong(), anyInt(), any(), anyLong(), any());

            // 模拟提交
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        // 提交后才发；runAsync 是异步的，用 timeout 而不是立刻断言
        verify(memberService, timeout(2000).times(1)).addPointsWithLot(
                7L,
                profileRewardConfig.getPoints(),
                profileRewardConfig.getSourceType(),
                7L,
                profileRewardConfig.getDescription());
        verify(memberService, never()).addPoints(anyLong(), anyInt(), any(), any());
    }

    /** 生日权益同理：提交前不能发。 */
    @Test
    void birthdayRewardIsDispatchedOnlyAfterCommit() {
        when(userMapper.selectById(7L)).thenReturn(entity(7L));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setBirthday("1990-05-20");

        TransactionSynchronizationManager.initSynchronization();
        try {
            userService.updateProfile(7L, request);

            verify(memberService, never()).grantBirthdayReward(anyLong());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(memberService, timeout(2000).times(1)).grantBirthdayReward(7L);
    }

    /**
     * 结构性守卫：updateProfile 必须是 @Transactional。
     *
     * <p>{@code @Transactional} 的真实效果只有在 Spring 容器里才体现，而本模块的单测不起容器
     * （起容器的集成测试依赖 MySQL/Redis/Nacos，CI 里被排除）。所以这里退而用反射守住注解本身 ——
     * 它一旦被摘掉，"业务行与 outbox 行同事务" 这个前提就假了，而这正是 ADR 0001 C1 要防的。
     */
    @Test
    void updateProfileIsTransactional() throws Exception {
        Method method = UserServiceImpl.class.getMethod("updateProfile", Long.class, UpdateProfileRequest.class);

        assertTrue(method.isAnnotationPresent(Transactional.class),
                "updateProfile 必须带 @Transactional：否则 updateById 与后续写入各自 autocommit（见 ADR 0001 C1）");
    }

    // ==================== 第 5 步：邀请券改为事件投递（ADR 0001 §8 第 5 步） ====================

    private User inviteeWithInviter(long id, long inviterId) {
        User user = entity(id);
        user.setInvitedBy(inviterId);
        user.setInviteRewardGranted(false);
        return user;
    }

    /** 首次触发：条件更新认领成功 → 恰好入队一次；事件四要素正确；且不再同步调 mall 发券。 */
    @Test
    void firstTriggerClaimsQualificationAndEnqueuesOnce() {
        when(userMapper.selectById(38L)).thenReturn(inviteeWithInviter(38L, 7L));
        when(userMapper.claimInviteReward(38L)).thenReturn(1);
        ArgumentCaptor<InviteRewardEarnedEvent> captor = ArgumentCaptor.forClass(InviteRewardEarnedEvent.class);

        assertTrue(userService.grantInviteRewardOnFirstOrder(38L));

        verify(userEventOutboxService, times(1))
                .publish(eq(MqTags.INVITE_REWARD_EARNED), eq(38L), captor.capture());
        InviteRewardEarnedEvent event = captor.getValue();
        assertEquals(38L, event.getInviteeUserId());
        assertEquals(7L, event.getInviterId());
        assertEquals("invite_firstorder_38_7", event.getUniqueKey()); // C2：沿用既有业务键，不重造
        assertNotNull(event.getOccurredAt());

        // 券的面额/门槛/有效期不进事件载荷（由 mall 从模板取）。
        // "不再同步调 mall" 不在这里断言 —— 已由 userServiceHoldsNoMallTypes 结构性守住。
    }

    /** 重复 / 并发触发：条件更新返回 0 → 不重复入队、返回 false。 */
    @Test
    void alreadyClaimedQualificationIsNotEnqueuedAgain() {
        when(userMapper.selectById(38L)).thenReturn(inviteeWithInviter(38L, 7L));
        when(userMapper.claimInviteReward(38L)).thenReturn(0);

        assertFalse(userService.grantInviteRewardOnFirstOrder(38L));

        verifyNoInteractions(userEventOutboxService);
    }

    /** 没有邀请人：直接返回 false，连认领都不该尝试。 */
    @Test
    void userWithoutInviterNeverClaims() {
        when(userMapper.selectById(38L)).thenReturn(entity(38L)); // invitedBy == null

        assertFalse(userService.grantInviteRewardOnFirstOrder(38L));

        verify(userMapper, never()).claimInviteReward(anyLong());
        verifyNoInteractions(userEventOutboxService);
    }

    /**
     * 入队失败必须让异常**逃出方法** —— 外层 {@code @Transactional} 才会把那次"认领"一起回滚。
     *
     * <p>单测不起 Spring 容器，回滚本身观察不到；这里守的是"没被 catch 掉"这个前提。
     * 若这里又写成 catch→return false，资格会被白白认领、券永远发不出去（且调用方不会再重试首单）。
     */
    @Test
    void enqueueFailurePropagatesSoThatQualificationRollsBack() {
        when(userMapper.selectById(38L)).thenReturn(inviteeWithInviter(38L, 7L));
        when(userMapper.claimInviteReward(38L)).thenReturn(1);
        doThrow(new IllegalStateException("outbox insert failed"))
                .when(userEventOutboxService).publish(any(), anyLong(), any());

        assertThrows(IllegalStateException.class, () -> userService.grantInviteRewardOnFirstOrder(38L));
    }

    /** 结构性守卫："认领 + 入队同事务"的前提就是这个注解（ADR 0001 §2）。 */
    @Test
    void grantInviteRewardIsTransactional() throws Exception {
        Method method = UserServiceImpl.class.getMethod("grantInviteRewardOnFirstOrder", Long.class);

        assertTrue(method.isAnnotationPresent(Transactional.class),
                "认领资格与 outbox 入队必须同事务，否则入队失败会留下'已认领却没券'的用户");
    }

    // ==================== 第 6 步（新人券）：注册改为事件投递 ====================

    /**
     * 注册必须在【本事务内】写出 welcome_gift_eligible，键沿用既有业务键（ADR C2）。
     *
     * <p>不变量：注册事务回滚时事件行也不存在（同生共死）；投递失败有 outbox 行兜底，
     * 不会再出现旧写法那种"RPC 失败就没有任何记录、券永久丢失"。
     */
    @Test
    void registerEnqueuesWelcomeGiftWithLegacyKey() {
        when(userMapper.selectCount(any())).thenReturn(0L); // 账号不重名 + 会员码不碰撞
        doAnswer(invocation -> {
            ((User) invocation.getArgument(0)).setId(99L); // 模拟自增主键回填
            return 1;
        }).when(userMapper).insert(any(User.class));
        ArgumentCaptor<WelcomeGiftEligibleEvent> captor =
                ArgumentCaptor.forClass(WelcomeGiftEligibleEvent.class);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("13900000001");
        request.setPassword("secret123");
        userService.register(request);

        verify(userEventOutboxService, times(1))
                .publish(eq(MqTags.WELCOME_GIFT_ELIGIBLE), eq(99L), captor.capture());
        WelcomeGiftEligibleEvent event = captor.getValue();
        assertEquals(99L, event.getUserId());
        // 字面量而不是 UserEventKeys.welcomeGift(...)：C2 要守的就是"键的口径不许变"，
        // 用生成它的同一个工具去断言等于没断言。
        assertEquals("NEW_USER_COUPON_99", event.getUniqueKey());
        assertNotNull(event.getOccurredAt());

        // 本子项只切新人券：会员仍走同步 RPC（USER_CREATED 是第 6 步最后一项）
        verify(memberService, timeout(2000).times(1)).createMember(99L);
    }

    /**
     * 结构性守卫（评审第 4 条）：user 侧不得再直接调用 mall。
     *
     * <p>新人券与邀请券都已改由事件驱动，本类不该再持有任何 {@code com.cozy.mall} 类型 ——
     * 一旦有人把同步 RPC 加回来（哪怕只是 import 一个 DTO），这条会立刻红。
     * 移除 Maven 依赖与加禁令是另一步，这里先守住代码层。
     */
    @Test
    void userServiceHoldsNoMallTypes() {
        for (Field field : UserServiceImpl.class.getDeclaredFields()) {
            assertFalse(field.getType().getName().startsWith("com.cozy.mall"),
                    "UserServiceImpl 不该再持有 mall 类型：" + field.getName()
                            + "（写侧必须走事件，见 docs/adr/0001 §8）");
        }
    }
}
