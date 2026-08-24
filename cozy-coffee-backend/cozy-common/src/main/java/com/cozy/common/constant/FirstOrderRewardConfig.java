package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 首单奖励配置（单一事实源，@ConfigurationProperties + application.yml）。
 * 前缀 cozy.member.first-order。供 FirstOrderConsumer 使用（新用户首单完成发放积分）。
 */
@Data
@ConfigurationProperties(prefix = "cozy.member.first-order")
public class FirstOrderRewardConfig {

    /** 首单奖励积分 */
    private int points = 200;

    /** 积分来源类型（幂等去重 key 用） */
    private String sourceType = "first_order_bonus";
}
