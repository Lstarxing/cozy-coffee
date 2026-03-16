package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员信息实体（v4.0 迁移后新增 expTotal）
 * v5.0: 新增保级/休眠机制字段
 */
@Data
@TableName("member_info")
public class MemberInfo {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String memberLevel;
    private Integer totalPoints;
    private Integer currentPoints;

    // v4.0 新增：EXP 成长值（仅升级用）
    private Integer expTotal;

    private Integer consecutiveSignDays;
    private LocalDate lastSigninDate;

    // 月度消费统计（用于黑卡加速包）
    private BigDecimal monthlySpent;
    private String monthlySpentMonth;
    private BigDecimal monthlyAccelerateRemaining;

    // v5.0 保级/休眠机制
    private String memberStatus; // ACTIVE / DORMANT（休眠态）
    private Integer annualExp; // 年度累计 EXP（用于保级判定）
    private Integer lastSettlementYear; // 上次结算年份

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
