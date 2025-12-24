package com.cozy.gateway.controller;

import com.cozy.common.context.UserContext;
import com.cozy.common.result.Result;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.request.LoginRequest;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @DubboReference(check = false)
    private UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        try {
            userService.register(request);
            return Result.success(null, "注册成功");
        } catch (Exception e) {
            log.error("注册失败", e);
            return Result.fail(friendlyErrorMessage(e, "注册"));
        }
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            String token = userService.login(request);
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            return Result.success(data, "登录成功");
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.fail(friendlyErrorMessage(e, "登录"));
        }
    }

    @GetMapping("/userinfo")
    public Result<UserDTO> getUserInfo() {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            UserDTO userDTO = userService.getUserById(userId);
            return Result.success(userDTO);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            userService.updateProfile(userId, request);
            return Result.success(null, "更新成功");
        } catch (Exception e) {
            log.error("更新资料失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/update-profile")
    public Result<Void> updateProfileLegacy(@Valid @RequestBody UpdateProfileRequest request) {
        return updateProfile(request);
    }

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("Gateway is running!");
    }

    /**
     * 转换异常为友好的错误信息
     */
    private String friendlyErrorMessage(Exception e, String operation) {
        String msg = e.getMessage();
        if (msg == null) {
            return operation + "失败，请稍后重试";
        }

        // 处理常见数据库异常
        if (msg.contains("Duplicate entry") || msg.contains("账号已存在")) {
            return "该账号已被注册，请换一个账号";
        }
        if (msg.contains("Connection refused") || msg.contains("timeout")) {
            return "服务繁忙，请稍后重试";
        }

        // 返回原始消息（如果是友好的RuntimeException）
        return msg;
    }

    // ========== 邀请码功能 API ==========

    /**
     * 填写邀请码获取积分
     */
    @PostMapping("/invite/apply")
    public Result<Void> applyInviteCode(@RequestBody ApplyInviteCodeRequest request) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            userService.applyInviteCode(userId, request.getInviteCode());
            return Result.success(null, "邀请码填写成功！双方都已获得积分奖励");
        } catch (Exception e) {
            log.error("填写邀请码失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 验证邀请码是否有效
     */
    @GetMapping("/invite/validate")
    public Result<InviteCodeValidationResult> validateInviteCode(@RequestParam String inviteCode) {
        try {
            UserDTO inviter = userService.getUserByInviteCode(inviteCode);
            if (inviter == null) {
                return Result.fail("邀请码无效");
            }
            InviteCodeValidationResult result = new InviteCodeValidationResult();
            result.setValid(true);
            result.setInviterNickname(inviter.getNickname());
            return Result.success(result, "邀请码有效");
        } catch (Exception e) {
            log.error("验证邀请码失败", e);
            return Result.fail(e.getMessage());
        }
    }

    // 请求/响应类
    public static class ApplyInviteCodeRequest {
        private String inviteCode;

        public String getInviteCode() {
            return inviteCode;
        }

        public void setInviteCode(String inviteCode) {
            this.inviteCode = inviteCode;
        }
    }

    public static class InviteCodeValidationResult {
        private boolean valid;
        private String inviterNickname;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getInviterNickname() {
            return inviterNickname;
        }

        public void setInviterNickname(String inviterNickname) {
            this.inviterNickname = inviterNickname;
        }
    }
}
