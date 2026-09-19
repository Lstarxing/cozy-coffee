package com.cozy.user.service.impl;

import com.cozy.common.constant.InviteRewardConfig;
import com.cozy.common.constant.ProfileRewardConfig;
import com.cozy.common.exception.BusinessException;
import com.cozy.member.api.MemberService;
import com.cozy.user.dto.response.UserDTO;
import com.cozy.user.entity.User;
import com.cozy.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
}
