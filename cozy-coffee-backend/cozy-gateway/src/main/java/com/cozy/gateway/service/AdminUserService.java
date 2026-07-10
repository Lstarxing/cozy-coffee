package com.cozy.gateway.service;

import com.cozy.member.api.MemberService;
import com.cozy.user.api.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 管理端用户操作服务。
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    @DubboReference(check = false)
    private final MemberService memberService;

    @DubboReference(check = false)
    private final UserService userService;

    public void adjustPoints(Long userId, int amount, String reason) {
        if (amount == 0) {
            throw new IllegalArgumentException("积分调整数量不能为0");
        }
        memberService.adminAdjustPoints(userId, amount, reason);
    }

    public void updateUserStatus(Long userId, String status) {
        userService.updateUserStatus(userId, status);
    }
}
