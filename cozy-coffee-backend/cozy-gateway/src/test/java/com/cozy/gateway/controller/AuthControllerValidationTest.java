package com.cozy.gateway.controller;

import com.cozy.gateway.exception.GlobalExceptionHandler;
import com.cozy.gateway.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JSR-303 校验试点：验证 LoginRequest 的 @NotBlank / @Size 注解
 * 在 GlobalExceptionHandler 的 MethodArgumentNotValidException handler 下
 * 返回 400 + 字段错误信息。
 */
@WebMvcTest(controllers = AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void login_emptyBody_returns400WithBothFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("用户名不能为空")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("密码不能为空")));
    }

    @Test
    void login_shortPassword_returns400WithSizeError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"13800138000\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("密码长度需在6-20位之间")));
    }

    @Test
    void login_validInput_passesValidation() throws Exception {
        // 校验通过后进入 AuthService.login，mock 返回 token
        org.mockito.Mockito.when(authService.login(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Map.of("token", "test-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"13800138000\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("test-token"));
    }

    @Test
    void wechatDevSession_validInput_returnsToken() throws Exception {
        // 端点守卫：devLoginEnabled 或 wechatConfigured() 任一为真才放行；mock 里固定为真
        when(authService.wechatConfigured()).thenReturn(true);
        // 控制器调用的是 loginWechat(code, deviceId)，不是 loginWechatDev
        when(authService.loginWechat("wx-code", "device_12345678"))
                .thenReturn(java.util.Map.of("token", "wechat-dev-token"));

        mockMvc.perform(post("/api/auth/wechat/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wx-code\",\"deviceId\":\"device_12345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("wechat-dev-token"));
    }

    @Test
    void resetPasswordDev_validInput_callsService() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset-dev")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"13800138000\",\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isOk());

        verify(authService).resetPasswordDev("13800138000", "newpass123");
    }
}
