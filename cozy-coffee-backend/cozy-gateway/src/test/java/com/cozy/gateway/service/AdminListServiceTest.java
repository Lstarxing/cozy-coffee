package com.cozy.gateway.service;

import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.response.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 管理端用户列表：会员数据在网关补齐，且**只补一次**。
 * 用真实的 coordinator + mock 的 memberService，好让"先补后过滤"的顺序也被真实地验证到。
 */
class AdminListServiceTest {

    private UserService userService;
    private MemberService memberService;
    private AdminListService listService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userService = mock(UserService.class);
        memberService = mock(MemberService.class);

        AdminUserProfileCoordinator coordinator = new AdminUserProfileCoordinator();
        ReflectionTestUtils.setField(coordinator, "userService", userService);
        ReflectionTestUtils.setField(coordinator, "memberService", memberService);

        listService = new AdminListService(mock(RedisTemplate.class), new ObjectMapper(), coordinator);
        ReflectionTestUtils.setField(listService, "userService", userService);
    }

    private UserDTO user(long id) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setUsername("u" + id);
        return dto;
    }

    private MemberDTO member(String level) {
        MemberDTO dto = new MemberDTO();
        dto.setMemberLevel(level);
        dto.setCurrentPoints(10);
        dto.setTotalPoints(100);
        return dto;
    }

    @Test
    void listUsersEnrichesMembersWithSingleBatchCall() {
        when(userService.listAllUsers()).thenReturn(new ArrayList<>(List.of(user(1L), user(2L))));
        when(memberService.getMembersByUserIds(any()))
                .thenReturn(Map.of(1L, member("gold"), 2L, member("silver")));

        List<UserDTO> result = listService.listUsers(null, null, null, null);

        assertEquals(2, result.size());
        verify(memberService, times(1)).getMembersByUserIds(any());
        assertEquals("gold", result.get(0).getMemberLevel());
        assertEquals("silver", result.get(1).getMemberLevel());
    }

    /** 顺序保证：按 memberLevel 过滤必须在批量补齐之后，否则等级恒为 null、筛选结果永远为空。 */
    @Test
    void memberLevelFilterRunsAfterEnrichment() {
        when(userService.listAllUsers()).thenReturn(new ArrayList<>(List.of(user(1L), user(2L))));
        when(memberService.getMembersByUserIds(any()))
                .thenReturn(Map.of(1L, member("gold"), 2L, member("silver")));

        List<UserDTO> result = listService.listUsers(null, "gold", null, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void emptyUserListDoesNotCallMember() {
        when(userService.listAllUsers()).thenReturn(new ArrayList<>());

        List<UserDTO> result = listService.listUsers(null, null, null, null);

        assertTrue(result.isEmpty());
        verifyNoInteractions(memberService);
    }

    /** member 挂掉时整批降级为 basic，列表本身仍可用（不因会员域故障变成 500）。 */
    @Test
    void listUsersStillWorksWhenMemberFails() {
        when(userService.listAllUsers()).thenReturn(new ArrayList<>(List.of(user(1L))));
        when(memberService.getMembersByUserIds(any())).thenThrow(new RuntimeException("member down"));

        List<UserDTO> result = listService.listUsers(null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("basic", result.get(0).getMemberLevel());
    }
}
