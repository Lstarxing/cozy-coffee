package com.cozy.gateway.service;

import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.response.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端用户资料组合（BFF）：主体信息来自 user 域，等级与积分来自 member 域。
 *
 * <p>等级/积分原先由 user-provider 在 {@code listAllUsers()} 里逐用户反查 member —— 管理端列表是
 * N+1 RPC，且 member 不可用时静默降级成 basic/0/0（连日志都没有），管理端看到的是"所有人都是 basic、
 * 0 积分"的假数据。改为在网关聚合后：user 域不再持有会员字段，列表只发生一次 member 批量调用。
 *
 * <p>失败语义：member 查询失败时<b>整体降级</b>为 basic/0/0，并留一条<b>聚合级</b>日志
 * ——比原先的静默降级强，又不做逐用户告警（否则一次故障刷出 N 条日志）。
 * 单个用户没有会员记录时同样落到 basic/0/0：basic 本就是最低等级，这比让 null 漏到管理端更准确。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserProfileCoordinator {

    private static final String DEFAULT_LEVEL = "basic";
    private static final int DEFAULT_POINTS = 0;

    @DubboReference(check = false)
    private UserService userService;

    // 等级/积分是次要展示字段：给有限超时，别让管理端列表被 member 拖满默认的 30s。
    // retries = 0 —— 批量调用已一次覆盖整页用户，重试只会把卡顿翻倍。
    @DubboReference(check = false, timeout = 3000, retries = 0)
    private MemberService memberService;

    /**
     * 批量补齐列表中每个用户的会员等级与积分（原地修改）。
     * 空列表直接返回，不发起 member 调用 —— 管理端筛选后为空是常态。
     */
    public void enrichMemberSummaries(List<UserDTO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Set<Long> userIds = users.stream()
                .map(UserDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, MemberDTO> members;
        try {
            members = memberService.getMembersByUserIds(userIds);
        } catch (Exception e) {
            log.warn("会员摘要批量查询失败，{} 个用户的等级/积分降级为 {}/{}: error={}",
                    users.size(), DEFAULT_LEVEL, DEFAULT_POINTS, e.getMessage());
            users.forEach(this::applyDefault);
            return;
        }

        for (UserDTO user : users) {
            MemberDTO member = members == null ? null : members.get(user.getId());
            if (member == null) {
                applyDefault(user);
                continue;
            }
            user.setMemberLevel(member.getMemberLevel());
            user.setCurrentPoints(member.getCurrentPoints());
            user.setTotalPoints(member.getTotalPoints());
        }
    }

    /** 单个用户详情：user 主体信息 + member 等级/积分。 */
    public UserDTO getUserDetail(Long userId) {
        UserDTO user = userService.getUserDetail(userId);
        if (user == null) {
            return null;
        }
        try {
            MemberDTO member = memberService.getMemberByUserId(userId);
            if (member == null) {
                applyDefault(user);
            } else {
                user.setMemberLevel(member.getMemberLevel());
                user.setCurrentPoints(member.getCurrentPoints());
                user.setTotalPoints(member.getTotalPoints());
            }
        } catch (Exception e) {
            log.warn("会员信息查询失败，用户 {} 的等级/积分降级为 {}/{}: error={}",
                    userId, DEFAULT_LEVEL, DEFAULT_POINTS, e.getMessage());
            applyDefault(user);
        }
        return user;
    }

    private void applyDefault(UserDTO user) {
        user.setMemberLevel(DEFAULT_LEVEL);
        user.setCurrentPoints(DEFAULT_POINTS);
        user.setTotalPoints(DEFAULT_POINTS);
    }
}
