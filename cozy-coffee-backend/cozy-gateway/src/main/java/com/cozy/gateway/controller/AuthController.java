package com.cozy.gateway.controller;

import com.cozy.common.context.UserContext;
import com.cozy.common.result.Result;
import com.cozy.gateway.config.AuthProperties;
import com.cozy.gateway.dto.ApplyInviteCodeRequest;
import com.cozy.gateway.dto.ChangePasswordRequest;
import com.cozy.gateway.dto.DevPasswordResetRequest;
import com.cozy.gateway.dto.InviteCodeValidationResult;
import com.cozy.gateway.dto.WechatDevSessionRequest;
import com.cozy.gateway.service.AuthService;
import com.cozy.gateway.util.AuthUtil;
import com.cozy.user.dto.request.LoginRequest;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthProperties authProperties;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success(null, "注册成功");
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> loginResult = authService.login(loginRequest);
        String token = (String) loginResult.get("token");

        Cookie cookie = new Cookie("cozy_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        return Result.success(loginResult, "登录成功");
    }

    @PostMapping("/wechat/session")
    public Result<Map<String, Object>> wechatSession(@Valid @RequestBody WechatDevSessionRequest request) {
        if (!authProperties.isDevLoginEnabled() && !authService.wechatConfigured()) {
            return Result.forbidden();
        }
        boolean real = authService.wechatConfigured();
        return Result.success(authService.loginWechat(request.getCode(), request.getDeviceId()),
                real ? "微信登录成功" : "微信开发登录成功");
    }

    @PostMapping("/password/reset-dev")
    public Result<Void> resetPasswordDev(@Valid @RequestBody DevPasswordResetRequest request) {
        if (!authProperties.isDevLoginEnabled()) {
            return Result.forbidden();
        }
        authService.resetPasswordDev(request.getUsername(), request.getNewPassword());
        return Result.success(null, "开发环境密码已重置");
    }

    @PostMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(AuthUtil.requireUserId(), request.getOldPassword(), request.getNewPassword());
        return Result.success(null, "密码修改成功，请重新登录");
    }

    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @CookieValue(value = "cozy_token", required = false) String cookieToken,
            HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("cozy_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        String token = cookieToken;
        if (token == null && authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7).trim();
        }
        authService.logout(token);
        return Result.success(null, "退出成功");
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.success(Map.of(
            "userId", AuthUtil.requireUserId(),
            "role", Optional.ofNullable(UserContext.getRole()).orElse("user")
        ));
    }

    @GetMapping("/userinfo")
    public Result<UserDTO> getUserInfo() {
        return Result.success(authService.getUserInfo(AuthUtil.requireUserId()));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        authService.updateProfile(AuthUtil.requireUserId(), request);
        return Result.success(null, "更新成功");
    }

    @PostMapping("/update-profile")
    public Result<Void> updateProfileLegacy(@Valid @RequestBody UpdateProfileRequest request) {
        return updateProfile(request);
    }

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("Gateway is running!");
    }

    @PostMapping("/invite/apply")
    public Result<Void> applyInviteCode(@Valid @RequestBody ApplyInviteCodeRequest request) {
        authService.applyInviteCode(AuthUtil.requireUserId(), request.getInviteCode());
        return Result.success(null, "邀请码填写成功！");
    }

    @GetMapping("/invite/validate")
    public Result<InviteCodeValidationResult> validateInviteCode(@RequestParam String inviteCode) {
        return Result.success(authService.validateInviteCode(inviteCode), "邀请码有效");
    }
}
