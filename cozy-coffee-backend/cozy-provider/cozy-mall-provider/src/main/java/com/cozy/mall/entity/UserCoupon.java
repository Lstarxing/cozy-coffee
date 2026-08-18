package com.cozy.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户券实例实体
 */
@Data
@TableName("user_coupons")
public class UserCoupon {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long sourcePointsOrderId;
    private String couponCode;
    private String couponType; // EXCHANGE/DISCOUNT/FULL_REDUCE
    private String ruleJson;
    private String status; // ISSUED(可使用) / FROZEN(待支付冻结) / USED(已核销) / EXPIRED(已过期)
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private Long usedShopOrderId;
    private LocalDateTime createdAt;
    
    // v5.3.1: 前端显示字段（从ruleJson提取，方便前端直接使用）
    private String displayTitle;    // 显示标题（如"生日5折券"）
    private String displaySubTitle; // 显示副标题（如"限标准杯"）
}
