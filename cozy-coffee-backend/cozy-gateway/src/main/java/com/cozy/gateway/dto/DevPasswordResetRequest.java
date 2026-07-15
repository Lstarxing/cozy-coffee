package com.cozy.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DevPasswordResetRequest {
    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位之间")
    private String newPassword;
}
