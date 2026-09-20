package com.cozy.user.service.impl;

import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.common.exception.BusinessException;
import com.cozy.common.mq.BirthdaySetEvent;
import com.cozy.common.mq.InviteRewardEarnedEvent;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.ProfileCompletedEvent;
import com.cozy.common.mq.WelcomeGiftEligibleEvent;
import com.cozy.member.api.MemberService;
import com.cozy.user.dto.request.LoginRequest;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;
import com.cozy.user.entity.User;
import com.cozy.user.mapper.UserMapper;
import com.cozy.user.mq.UserEventOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    private StringRedisTemplate stringRedisTemplate;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        memberService = mock(MemberService.class);
        userEventOutboxService = mock(UserEventOutboxService.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        userService = new UserServiceImpl(userMapper, stringRedisTemplate, userEventOutboxService);
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
     * 完善资料奖励改为事件驱动：事件在**事务内**入队（不再依赖 AfterCommit），也不再同步调 member。
     *
     * <p>奖励规则（20 积分 / source_type=profile / 文案）**归消费端持有** —— 事件只表达"资料已完善"，
     * 所以这里只断言键，不断言积分值（那个值在 member 侧的 `cozy.user.profile`，两边配置已核对一致）。
     */
    @Test
    void profileCompletionEnqueuesEventInsideTransaction() {
        when(userMapper.selectById(7L)).thenReturn(entity(7L)); // phone/email 为空 -> 算首次填写
        ArgumentCaptor<ProfileCompletedEvent> captor = ArgumentCaptor.forClass(ProfileCompletedEvent.class);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("13800000000");
        request.setEmail("probe@example.com");

        userService.updateProfile(7L, request);

        // 不需要 afterCommit：事件与资料行同事务写入（这正是 C7 的前提："业务行与 outbox 行同事务"）
        verify(userEventOutboxService, times(1))
                .publish(eq(MqTags.PROFILE_COMPLETED), eq(7L), captor.capture());
        ProfileCompletedEvent event = captor.getValue();
        assertEquals(7L, event.getUserId());
        assertEquals("profile_completed_7", event.getUniqueKey()); // 字面量：C2 守的就是键的口径
        assertNotNull(event.getOccurredAt());

        verify(memberService, never()).addPointsWithLot(anyLong(), anyInt(), any(), anyLong(), any());
    }

    /**
     * 设置生日 = 在**事务内**落一条 `birthday_set`，年度由生产者盖章（ADR 0001 C2 + C8）。
     *
     * <p>与完善资料奖励不同，这里**不再**走 `AfterCommit` + 异步 RPC：
     * 事件行必须与资料行同事务（提交前就已写入），这正是事务性 outbox 的意义。
     * 年度也绝不能留给消费者用 `now()` 重算 —— 跨年重投会算出下一年度的键，同笔权益重发。
     */
    @Test
    void settingBirthdayEnqueuesEventWithStampedYear() {
        when(userMapper.selectById(7L)).thenReturn(entity(7L));
        ArgumentCaptor<BirthdaySetEvent> captor = ArgumentCaptor.forClass(BirthdaySetEvent.class);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setBirthday("1990-05-20");

        userService.updateProfile(7L, request);

        // 不需要 afterCommit：事件在事务内就已入队
        verify(userEventOutboxService, times(1))
                .publish(eq(MqTags.BIRTHDAY_SET), eq(7L), captor.capture());
        BirthdaySetEvent event = captor.getValue();
        assertEquals(7L, event.getUserId());
        int benefitYear = LocalDate.now().getYear();
        assertEquals(benefitYear, event.getBenefitYear());
        // 字面量而不是 UserEventKeys.birthday(...)：C2 守的就是"键的口径不许变"
        assertEquals("birthday_7_" + benefitYear, event.getUniqueKey());
        assertNotNull(event.getOccurredAt());

        // 生日权益不再同步调 member（两个重载都不许出现）
        verify(memberService, never()).grantBirthdayReward(anyLong());
        verify(memberService, never()).grantBirthdayReward(anyLong(), anyInt());
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

    // ==================== P1：禁用用户必须立即撤销会话 ====================

    private User userWithStatus(long id, String status, Integer tokenVersion) {
        User user = entity(id);
        user.setStatus(status);
        user.setTokenVersion(tokenVersion);
        return user;
    }

    /** 让 {@code scan("cozy:auth:session:*")} 依次吐出给定的「会话键 → 归属用户 id」。 */
    @SuppressWarnings("unchecked")
    private void stubSessions(Map<String, String> sessionKeyToUserId) {
        Cursor<String> cursor = mock(Cursor.class);
        Iterator<String> keys = sessionKeyToUserId.keySet().iterator();
        when(cursor.hasNext()).thenAnswer(invocation -> keys.hasNext());
        when(cursor.next()).thenAnswer(invocation -> keys.next());
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        sessionKeyToUserId.forEach((key, userId) -> when(valueOps.get(key)).thenReturn(userId));
        when(stringRedisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    /**
     * 禁用 = 状态置 disabled + tokenVersion++ + 删该用户的会话与当前 token 指针。
     *
     * <p>本类只验证"删了哪些键"；**"原 token 随后拿 401"由本地 E2E 验证**
     * （401 需要真容器跑 JwtAuthInterceptor，本模块的单测不起容器）。
     */
    @Test
    void disableRevokesSessionsAndTokenPointer() {
        when(userMapper.selectById(42L)).thenReturn(userWithStatus(42L, "active", 0));
        stubSessions(Map.of("cozy:auth:session:mine", "42", "cozy:auth:session:other", "7"));

        userService.updateUserStatus(42L, "disabled");

        verify(stringRedisTemplate).delete(RedisKeyConstants.userCurrentTokenById(42L));
        verify(stringRedisTemplate).delete("cozy:auth:session:mine");
        // 别人的会话一根都不能碰
        verify(stringRedisTemplate, never()).delete("cozy:auth:session:other");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("disabled", captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getTokenVersion());
    }

    /** 重复禁用：依旧把残留会话清干净（幂等），tokenVersion 继续递增。 */
    @Test
    void repeatedDisableStillRevokes() {
        when(userMapper.selectById(42L)).thenReturn(userWithStatus(42L, "disabled", 5));
        stubSessions(Map.of("cozy:auth:session:leaked", "42"));

        userService.updateUserStatus(42L, "disabled");

        verify(stringRedisTemplate).delete("cozy:auth:session:leaked");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(6, captor.getValue().getTokenVersion());
    }

    /** 启用不碰任何会话（尤其不能顺手踢掉别人的），也不递增 tokenVersion、不恢复旧 token。 */
    @Test
    void enableDoesNotTouchSessions() {
        when(userMapper.selectById(42L)).thenReturn(userWithStatus(42L, "disabled", 3));

        userService.updateUserStatus(42L, "active");

        verifyNoInteractions(stringRedisTemplate);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("active", captor.getValue().getStatus());
        assertEquals(3, captor.getValue().getTokenVersion());
    }

    /** tokenVersion 为 null（旧数据）按 0 处理，不能 NPE。 */
    @Test
    void disableToleratesNullTokenVersion() {
        when(userMapper.selectById(42L)).thenReturn(userWithStatus(42L, "active", null));
        stubSessions(Map.of());

        userService.updateUserStatus(42L, "disabled");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getTokenVersion());
    }

    /**
     * Redis 撤销失败必须让异常**逃出方法** —— 否则会出现"接口报禁用成功、会话其实还在"。
     *
     * <p>单测不起 Spring 容器，事务回滚本身观察不到；这里守的是"没被 catch 掉"这个前提
     * （与 `enqueueFailurePropagatesSoThatQualificationRollsBack` 同一个思路）。
     */
    @Test
    void revokeFailurePropagatesSoStatusRollsBack() {
        when(userMapper.selectById(42L)).thenReturn(userWithStatus(42L, "active", 0));
        doThrow(new RedisConnectionFailureException("redis down"))
                .when(stringRedisTemplate).delete(anyString());

        assertThrows(RedisConnectionFailureException.class,
                () -> userService.updateUserStatus(42L, "disabled"));
    }

    /** 已禁用用户不能重新登录（既有行为，补进来让 P1 的回归面完整）。 */
    @Test
    void disabledUserCannotLogin() {
        User user = userWithStatus(42L, "disabled", 1);
        user.setPassword("irrelevant-hash");
        when(userMapper.selectOne(any())).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("u42");
        request.setPassword("secret123");

        assertThrows(BusinessException.class, () -> userService.login(request));
    }

    // ---- 凭据变更同样必须"强制"撤销（与禁用共用 revokeAllSessions）----

    /**
     * 改密成功后必须**强制**撤销（而不是尽力而为）。
     *
     * <p>判据：强制版会删 {@code cozy:auth:user:token:{id}} 指针，尽力版不会 ——
     * 这是两者唯一可观测的差别，所以用它当断言。
     */
    @Test
    void changePasswordRevokesSessionsForcibly() {
        User user = userWithStatus(42L, "active", 0);
        user.setPassword(new BCryptPasswordEncoder().encode("OldPass123"));
        when(userMapper.selectById(42L)).thenReturn(user);
        stubSessions(Map.of());

        userService.changePassword(42L, "OldPass123", "NewPass456");

        verify(stringRedisTemplate).delete(RedisKeyConstants.userCurrentTokenById(42L));
    }

    /** 开发用重置密码同理（它此前连 @Transactional 都没有，见下面的结构守卫）。 */
    @Test
    void resetPasswordDevRevokesSessionsForcibly() {
        User user = userWithStatus(42L, "active", 0);
        when(userMapper.selectOne(any())).thenReturn(user);
        stubSessions(Map.of());

        userService.resetPasswordDev("u42", "NewPass456");

        verify(stringRedisTemplate).delete(RedisKeyConstants.userCurrentTokenById(42L));
    }

    /** 改密时 Redis 撤销失败必须抛出去 → 密码更新随事务回滚，不能"密码改了但旧会话还在"。 */
    @Test
    void changePasswordRevokeFailurePropagates() {
        User user = userWithStatus(42L, "active", 0);
        user.setPassword(new BCryptPasswordEncoder().encode("OldPass123"));
        when(userMapper.selectById(42L)).thenReturn(user);
        doThrow(new RedisConnectionFailureException("redis down"))
                .when(stringRedisTemplate).delete(anyString());

        assertThrows(RedisConnectionFailureException.class,
                () -> userService.changePassword(42L, "OldPass123", "NewPass456"));
    }

    /** 重置密码同理；没有 @Transactional 时会出现"报错但密码已变"，所以下面还有结构守卫。 */
    @Test
    void resetPasswordDevRevokeFailurePropagates() {
        when(userMapper.selectOne(any())).thenReturn(userWithStatus(42L, "active", 0));
        doThrow(new RedisConnectionFailureException("redis down"))
                .when(stringRedisTemplate).delete(anyString());

        assertThrows(RedisConnectionFailureException.class,
                () -> userService.resetPasswordDev("u42", "NewPass456"));
    }

    /**
     * 结构性守卫：`resetPasswordDev` 必须是 `@Transactional`。
     *
     * <p>它原来没有 —— 那样"密码已提交、随后 Redis 撤销失败"会留下"接口报错但密码已变"。
     * 加了事务，撤销失败才能把密码更新一起回滚（真实回滚要起容器，这里只守注解本身）。
     */
    @Test
    void resetPasswordDevIsTransactional() throws Exception {
        Method method = UserServiceImpl.class.getMethod("resetPasswordDev", String.class, String.class);

        assertTrue(method.isAnnotationPresent(Transactional.class),
                "resetPasswordDev 必须带 @Transactional：否则 Redis 撤销失败会留下'报错但密码已变'");
    }
}
