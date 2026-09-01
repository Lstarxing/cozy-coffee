package com.cozy.gateway.controller;

import com.cozy.gateway.config.AuthProperties;
import com.cozy.gateway.exception.GlobalExceptionHandler;
import com.cozy.gateway.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * dev-login 关闭时：重置密码接口必须 403，且不触达 AuthService。
 */
@WebMvcTest(controllers = AuthController.class)
@Import({GlobalExceptionHandler.class, ConfigurationPropertiesAutoConfiguration.class})
@EnableConfigurationProperties(AuthProperties.class)
@TestPropertySource(properties = "cozy.auth.dev-login-enabled=false")
class AuthSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void resetPasswordDev_whenDevLoginDisabled_returns403() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset-dev")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"13800138000\",\"newPassword\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        org.mockito.Mockito.verify(authService, org.mockito.Mockito.never())
                .resetPasswordDev(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
