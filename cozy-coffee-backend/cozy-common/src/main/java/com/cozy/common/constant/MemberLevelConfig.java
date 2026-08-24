package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * 会员等级与保级策略配置（单一事实源，@ConfigurationProperties + application.yml）。
 * 前缀 cozy.member.level，字段默认值即生产当前值，改动只需改 yml 无需改码。
 * 供 member-provider（升级/保级/唤醒/休眠/加速包额度）与 order-provider（分段预估/黑卡加速包/会员日）共用，
 * 消除 MemberServiceImpl 与 OrderRewardService 两处阈值硬编码重复。
 */
@Data
@ConfigurationProperties(prefix = "cozy.member.level")
public class MemberLevelConfig {

    /** 升级 EXP 阈值：basic → silver → gold → diamond → black */
    private int silverExp = 500;
    private int goldExp = 1500;
    private int diamondExp = 4000;
    private int blackExp = 9000;

    /** 保级门槛（年度 EXP）：保级失败则休眠/降级 */
    private int silverKeepExp = 300;
    private int goldKeepExp = 1000;
    private int diamondKeepExp = 2500;
    private int blackKeepExp = 4000;

    /** 唤醒门槛（单月 EXP）：休眠态恢复权益 */
    private int diamondAwakenExp = 600;
    private int blackAwakenExp = 800;

    /** 保级失败落点 EXP（休眠/降级后的 EXP） */
    private int blackDormantExp = 4500; // 黑金休眠保留
    private int diamondDemoteExp = 1499; // 钻石 → 黄金顶端
    private int goldDemoteExp = 500; // 黄金 → 白银
    private int silverDemoteExp = 0; // 白银 → 基础

    /** 黑卡加速包：剩余额度内 1.7x，超出 1.5x，每月额度 300 */
    private BigDecimal accelerateRate = new BigDecimal("1.70");
    private BigDecimal normalRate = new BigDecimal("1.5");
    private BigDecimal accelerateMonthlyCap = new BigDecimal("300");

    /** 会员日（Cozy Day）：每周五积分倍率 +0.5 */
    private BigDecimal cozyDayBonus = new BigDecimal("0.5");
    private DayOfWeek cozyDayOfWeek = DayOfWeek.FRIDAY;

    /** 根据累计 EXP 计算会员等级 */
    public String levelForExp(int exp) {
        if (exp >= blackExp) return "black";
        if (exp >= diamondExp) return "diamond";
        if (exp >= goldExp) return "gold";
        if (exp >= silverExp) return "silver";
        return "basic";
    }

    /** 下一级升级阈值；已是最高级返回 -1 */
    public int nextLevelThreshold(int exp) {
        if (exp < silverExp) return silverExp;
        if (exp < goldExp) return goldExp;
        if (exp < diamondExp) return diamondExp;
        if (exp < blackExp) return blackExp;
        return -1;
    }

    /** 等级对应的升级阈值（basic/未知返回 0，用于判定升降级方向） */
    public int thresholdByLevel(String level) {
        if (level == null) return 0;
        return switch (level.toLowerCase()) {
            case "black" -> blackExp;
            case "diamond" -> diamondExp;
            case "gold" -> goldExp;
            case "silver" -> silverExp;
            default -> 0;
        };
    }
}
