package com.cozy.member.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
public class MemberDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;

    // EXP（成长值，仅升级用）
    private Integer expTotal;

    // POINT（积分，可兑换）
    private Integer currentPoints;

    // 历史累计积分（展示用，不作为升级依据）
    private Integer totalPoints;

    // 会员等级
    private String memberLevel;

    // 当前等级基础积分倍率（与 PointsRateConfig 单一事实源一致，不含会员日加成）
    private BigDecimal pointsRate;

    // 积分商城兑换折扣（与 RedemptionDiscountConfig 单一事实源一致）
    private BigDecimal redeemDiscount;

    // 各等级积分倍率/兑换折扣（权益页对比展示，与 PointsRateConfig/RedemptionDiscountConfig 一致）
    private List<LevelBenefitItem> levelBenefits;

    @Data
    public static class LevelBenefitItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String level;
        private BigDecimal pointsRate;
        private BigDecimal redeemDiscount;
        // 升级门槛 EXP（MemberLevelConfig.thresholdByLevel，basic 为 0）
        private Integer threshold;
        // 月权益文案（MonthlyBenefitConfig.levels.*.benefitName）
        private String monthlyBenefit;
        // 生日礼遇文案（BirthdayRewardConfig.levels.*.benefitName）
        private String birthdayBenefit;
    }

    // 月度消费统计（用于黑卡加速包）
    private BigDecimal monthlySpent;
    private String monthlySpentMonth;
    private BigDecimal monthlyAccelerateRemaining;
    // 黑卡加速包每月额度（MemberLevelConfig.accelerateMonthlyCap，供前端展示进度）
    private BigDecimal accelerateMonthlyCap;

    // 签到相关
    private LocalDate lastSigninDate;
    private Integer consecutiveSignDays;

    // 即将到期积分（近30天）
    private Integer expiringPoints;

    // 优惠券数量（可用）
    private Integer couponCount;

    // 兑换券数量（可用）
    private Integer exchangeCouponCount;

    // 用户基础信息 (Added for Order Service access)
    // 用户基础信息 (Added for Order Service access)
    private String nickname;
    private String phone;
    private String avatar;

    // v5.0 月度挑战任务统计 (兜底数据源)
    private Integer monthlyDeliveryOrders; // 外卖
    private Integer monthlyOrderCount; // 打卡
    private Integer morningOrderCount; // 晨间
    private Integer newProductCount; // 新品

    // v5.3 月度挑战任务状态 (兜底数据源)
    private Boolean challengeOrderClaimed;
    private Boolean challengeMorningClaimed;
    private Boolean challengeDeliveryClaimed;
    private Boolean challengeNewproductClaimed;

    // Alias for memberLevel for compatibility if needed
    public String getLevel() {
        return memberLevel;
    }
}
