package com.cozy.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplyInviteCodeRequest {

    @NotBlank(message = "邀请码不能为空")
    @Size(min = 8, max = 8, message = "邀请码应为8位字符")
    private String inviteCode;
}
