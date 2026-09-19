package com.cozy.user.service.impl;

import com.cozy.common.constant.InviteRewardConfig;
import com.cozy.common.constant.ProfileRewardConfig;
import com.cozy.common.exception.BusinessException;
import com.cozy.member.api.MemberService;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;
import com.cozy.user.entity.User;
import com.cozy.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        memberService = mock(MemberService.class);
        userService = new UserServiceImpl(userMapper, mock(StringRedisTemplate.class),
                mock(InviteRewardConfig.class), mock(ProfileRewardConfig.class));
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
            verify(memberService, never()).addPoints(anyLong(), anyInt(), any(), any());

            // 模拟提交
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        // 提交后才发；runAsync 是异步的，用 timeout 而不是立刻断言
        verify(memberService, timeout(2000).times(1)).addPoints(anyLong(), anyInt(), any(), any());
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
}
