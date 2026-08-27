package com.cozy.member.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 会员权益面板聚合视图（权益页单一数据源）。
 * <p>
 * 回答「我是谁 · 我有什么 · 我升级能得到什么」，并携带全部等级对比。
 * 由 {@code getMemberOverview} 组装，字段来自
 * MemberLevelConfig / PointsRateConfig / RedemptionDiscountConfig / MonthlyBenefitConfig / BirthdayRewardConfig。
 */
@Data
public class MemberOverviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前等级身份 */
    private CurrentLevel currentLevel;

    /** 当前等级可享权益（信息型权益只给状态，行动型带 action） */
    private List<BenefitItem> benefits;

    /** 升级预告（进度 + 激励 + 权益差异） */
    private UpgradePreview upgradePreview;

    /** 全部等级对比（同 MemberDTO.levelBenefits） */
    private List<MemberDTO.LevelBenefitItem> allLevels;

    @Data
    public static class CurrentLevel implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;          // basic/silver/gold/diamond/black
        private String name;        // 基础会员/白银会员…
        private Integer exp;        // 当前 EXP
        private Integer nextLevelExp; // 下一级门槛；已满级为 null
    }

    @Data
    public static class BenefitItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String type;        // POINT_MULTIPLIER / REDEEM_DISCOUNT / MONTHLY_REWARD / BIRTHDAY / COZY_DAY
        private String title;       // 消费积分 / 积分兑换 / 每月权益 / 生日礼遇 / 会员日
        private String value;       // 1.5× / 9.5 折 / 券名…
        private String description;
        private String action;      // null（仅展示）/ mall（去商城）/ claim（领取）
        private Boolean canClaim;   // 月权益领取按钮可用性
    }

    @Data
    public static class UpgradePreview implements Serializable {
        private static final long serialVersionUID = 1L;
        private Boolean isMax;
        private String nextLevelName;
        private Integer remainingExp;
        private Integer percentage;   // 当前段内进度 0-100
        private List<String> newBenefits; // 升级后新增权益描述
    }
}
