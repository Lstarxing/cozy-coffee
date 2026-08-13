package com.cozy.gateway.service;

import com.cozy.gateway.dto.InviteCodeValidationResult;
import com.cozy.common.exception.NotFoundException;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.request.LoginRequest;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 认证编排服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @DubboReference(check = false, timeout = 3000, retries = 0)
    private UserService userService;

    private final WechatService wechatService;

    public Map<String, Object> login(LoginRequest request) {
        return Map.of("token", userService.login(request));
    }

    public boolean wechatConfigured() {
        return wechatService.isConfigured();
    }

    /**
     * 微信登录：配置了 appid/secret 走真 code2Session；否则退回开发登录。
     */
    public Map<String, Object> loginWechat(String code, String deviceId) {
        if (wechatService.isConfigured()) {
            String openid = wechatService.code2Session(code);
            return Map.of("token", userService.loginWechat(openid));
        }
        return Map.of("token", userService.loginWechatDev(deviceId));
    }

    public Map<String, Object> loginWechatDev(String deviceId) {
        String token = userService.loginWechatDev(deviceId);
        return Map.of("token", token);
    }

    public void resetPasswordDev(String username, String newPassword) {
        userService.resetPasswordDev(username, newPassword);
    }

    public void register(RegisterRequest request) {
        userService.register(request);
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            userService.logout(token.trim());
        }
    }

    public UserDTO getUserInfo(Long userId) {
        UserDTO user = userService.getUserById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return user;
    }

    public void updateProfile(Long userId, UpdateProfileRequest request) {
        userService.updateProfile(userId, request);
    }

    public void applyInviteCode(Long userId, String inviteCode) {
        userService.applyInviteCode(userId, inviteCode);
    }

    public InviteCodeValidationResult validateInviteCode(String inviteCode) {
        UserDTO inviter = userService.getUserByInviteCode(inviteCode);
        if (inviter == null) {
            throw new NotFoundException("邀请码无效");
        }
        InviteCodeValidationResult result = new InviteCodeValidationResult();
        result.setValid(true);
        result.setInviterNickname(inviter.getNickname());
        return result;
    }
}
