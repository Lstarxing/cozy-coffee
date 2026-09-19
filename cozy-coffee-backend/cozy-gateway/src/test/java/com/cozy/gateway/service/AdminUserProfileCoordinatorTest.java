package com.cozy.gateway.service;

import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.response.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 管理端用户会员字段的 BFF 组合语义。
 * 原先这些字段由 user-provider 逐用户反查 member（N+1），且失败时静默降级成假数据。
 */
class AdminUserProfileCoordinatorTest {

    private UserService userService;
    private MemberService memberService;
    private AdminUserProfileCoordinator coordinator;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        memberService = mock(MemberService.class);
        coordinator = new AdminUserProfileCoordinator();
        ReflectionTestUtils.setField(coordinator, "userService", userService);
        ReflectionTestUtils.setField(coordinator, "memberService", memberService);
    }

    private UserDTO user(long id) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setUsername("u" + id);
        return dto;
    }

    private MemberDTO member(String level, int currentPoints, int totalPoints) {
        MemberDTO dto = new MemberDTO();
        dto.setMemberLevel(level);
        dto.setCurrentPoints(currentPoints);
        dto.setTotalPoints(totalPoints);
        return dto;
    }

    /** 列表只发生一次 member 批量调用（原先是逐用户 N+1）。 */
    @Test
    void batchEnrichUsesSingleMemberCall() {
        List<UserDTO> users = new ArrayList<>(List.of(user(1L), user(2L), user(3L)));
        when(memberService.getMembersByUserIds(any())).thenReturn(Map.of(
                1L, member("gold", 120, 800),
                2L, member("silver", 30, 300)));

        coordinator.enrichMemberSummaries(users);

        verify(memberService, times(1)).getMembersByUserIds(any());
        verifyNoMoreInteractions(memberService);
        assertEquals("gold", users.get(0).getMemberLevel());
        assertEquals(120, users.get(0).getCurrentPoints());
        assertEquals(800, users.get(0).getTotalPoints());
        assertEquals("silver", users.get(1).getMemberLevel());
    }

    /** member 返回的 Map 缺某个 userId 时，该用户落到 basic/0/0 —— 不让 null 漏到管理端。 */
    @Test
    void userWithoutMemberRecordFallsBackToBasic() {
        List<UserDTO> users = new ArrayList<>(List.of(user(1L), user(2L)));
        when(memberService.getMembersByUserIds(any())).thenReturn(Map.of(1L, member("gold", 120, 800)));

        coordinator.enrichMemberSummaries(users);

        assertEquals("gold", users.get(0).getMemberLevel());
        assertEquals("basic", users.get(1).getMemberLevel());
        assertEquals(0, users.get(1).getCurrentPoints());
        assertEquals(0, users.get(1).getTotalPoints());
    }

    /** member 不可用时整批降级为 basic/0/0，并留一条聚合日志（原先连日志都没有）。 */
    @Test
    void degradesWholeBatchWhenMemberFails() {
        List<UserDTO> users = new ArrayList<>(List.of(user(1L), user(2L)));
        when(memberService.getMembersByUserIds(any())).thenThrow(new RuntimeException("member down"));

        coordinator.enrichMemberSummaries(users);

        users.forEach(u -> {
            assertEquals("basic", u.getMemberLevel());
            assertEquals(0, u.getCurrentPoints());
            assertEquals(0, u.getTotalPoints());
        });
    }

    /** 空列表（含只有 null id 的情况）不发起 member 调用。 */
    @Test
    void emptyListDoesNotCallMember() {
        coordinator.enrichMemberSummaries(new ArrayList<>());
        coordinator.enrichMemberSummaries(null);
        coordinator.enrichMemberSummaries(new ArrayList<>(List.of(new UserDTO())));

        verifyNoInteractions(memberService);
    }

    /** 单体详情：user 主体 + member 等级/积分，扁平 UserDTO 结构不变。 */
    @Test
    void detailCombinesUserAndMember() {
        when(userService.getUserDetail(7L)).thenReturn(user(7L));
        when(memberService.getMemberByUserId(7L)).thenReturn(member("diamond", 500, 4000));

        UserDTO result = coordinator.getUserDetail(7L);

        assertEquals("u7", result.getUsername());
        assertEquals("diamond", result.getMemberLevel());
        assertEquals(500, result.getCurrentPoints());
        assertEquals(4000, result.getTotalPoints());
    }

    @Test
    void detailDegradesToBasicWhenMemberFails() {
        when(userService.getUserDetail(7L)).thenReturn(user(7L));
        when(memberService.getMemberByUserId(anyLong())).thenThrow(new RuntimeException("member down"));

        UserDTO result = coordinator.getUserDetail(7L);

        assertEquals("u7", result.getUsername());
        assertEquals("basic", result.getMemberLevel());
        assertEquals(0, result.getCurrentPoints());
        assertEquals(0, result.getTotalPoints());
    }

    @Test
    void detailReturnsNullWhenUserMissing() {
        when(userService.getUserDetail(7L)).thenReturn(null);

        assertNull(coordinator.getUserDetail(7L));
        verifyNoInteractions(memberService);
    }
}
