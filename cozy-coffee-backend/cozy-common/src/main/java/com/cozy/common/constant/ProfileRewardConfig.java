package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 完善个人资料奖励配置（单一事实源，@ConfigurationProperties + application.yml）。
 * 前缀 cozy.user.profile。供 UserServiceImpl.updateProfile 使用
 * （首次补齐手机号+邮箱时发放积分）。
 */
@Data
@ConfigurationProperties(prefix = "cozy.user.profile")
public class ProfileRewardConfig {

    private int points = 20;

    private String sourceType = "profile";

    private String description = "完善个人资料（手机号+邮箱）奖励";
}
