package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.dto.ApplyInviteCodeRequest;
import com.cozy.gateway.dto.InviteCodeValidationResult;
import com.cozy.gateway.service.AuthService;
import com.cozy.gateway.util.AuthUtil;
import com.cozy.user.dto.request.LoginRequest;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success(null, "注册成功");
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request), "登录成功");
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Result.success(null, "退出成功");
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
