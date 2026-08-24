package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 首单邀请奖励配置（单一事实源，@ConfigurationProperties + application.yml）。
 * 前缀 cozy.user.invite。供 UserServiceImpl.grantInviteRewardOnFirstOrder 使用
 * （被邀请人首单完成时为邀请人发放买一送一券）。
 */
@Data
@ConfigurationProperties(prefix = "cozy.user.invite")
public class InviteRewardConfig {

    /** 邀请人奖励券模板类型 */
    private String couponType = "BOGO";

    /** 使用门槛 */
    private double minAmount = 0;

    /** 优惠金额/封顶 */
    private double discountAmount = 40;

    /** 有效天数 */
    private int validDays = 30;

    /** 幂等唯一 key 前缀（拼 userId_inviterId） */
    private String uniqueKeyPrefix = "invite_firstorder";
}
