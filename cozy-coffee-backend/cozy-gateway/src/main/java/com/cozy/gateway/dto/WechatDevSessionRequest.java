package com.cozy.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatDevSessionRequest {
    @NotBlank(message = "微信登录 code 不能为空")
    private String code;

    @NotBlank(message = "开发设备标识不能为空")
    private String deviceId;
}
