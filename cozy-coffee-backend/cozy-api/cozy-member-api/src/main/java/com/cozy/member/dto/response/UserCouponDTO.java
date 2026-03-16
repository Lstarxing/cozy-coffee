package com.cozy.member.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户券实例 DTO
 */
@Data
public class UserCouponDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String couponCode;
    private String couponType; // EXCHANGE/DISCOUNT/FULL_REDUCE
    private String ruleJson;
    private String status; // ISSUED/USED/EXPIRED
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;

    // 解析后的规则字段（便于前端显示）
    private String productName;
    private Integer value; // 券面值或折扣值
    private Integer minAmount; // 满减门槛（满减券用）
    private String description; // v5.2: 券描述（从 ruleJson 解析）

    // v5.2: 前端兼容字段
    private String title; // 对应前端 coupon.title
    private String desc; // 对应前端 coupon.desc

    // 是否可用
    private Boolean available;
    private String unavailableReason;

    // v2.0: 前端直接展示用，后端算好给前端
    private String displayTitle; // e.g., "5折", "¥10", "买一送一"
    private String displaySubTitle; // e.g., "最高抵扣20元", "满35可用"
    private String labelColor; // e.g., "#FF6B35" (高亮新人券)
}
