package com.cozy.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cozy.auth")
public class AuthProperties {
    /** 开发登录/密码重置开关：默认关闭（fail-closed），仅 local profile 显式开启 */
    private boolean devLoginEnabled = false;
}
