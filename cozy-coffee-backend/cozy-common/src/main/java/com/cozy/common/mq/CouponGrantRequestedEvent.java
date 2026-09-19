package com.cozy.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 会员域请求发放优惠券（member → mall）。
 *
 * <p>为什么放在 common 而不是 cozy-mall-api：生产方是 member-provider，若事件类住在 mall 的 api 模块，
 * member 就必须依赖 cozy-mall-api —— 那正是本次要砍掉的反向依赖。**跨域消费的事件契约需要中立模块承载**，
 * 同理订单事件也留在本包（member/mall 都要消费）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponGrantRequestedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    /** 券模板类型，见 cozy.mall.coupon-template 配置 */
    private String couponType;

    /** 业务幂等键：mall 侧按 user_coupon.coupon_code 去重，重复投递不会重复发券 */
    private String uniqueKey;

    private double minAmount;

    private double discountAmount;

    private int validDays;

    /** 来源（signin_7day / birthday / monthly_benefit / level_upgrade），用于日志与排查 */
    private String source;
}
