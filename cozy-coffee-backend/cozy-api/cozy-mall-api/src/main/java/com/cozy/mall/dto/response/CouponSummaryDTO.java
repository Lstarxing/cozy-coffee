package com.cozy.mall.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户券包汇总（供 BFF 组合会员资料用）。
 * 在 mall 内部直接聚合，避免把整张券列表跨服务拉过来再计数。
 */
@Data
public class CouponSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 可用券数量：status=ISSUED 且未过期 */
    private Integer availableCount;

    /** 其中可兑换/兑换券（coupon_type=EXCHANGE）的数量，是 availableCount 的子集 */
    private Integer exchangeCount;
}
