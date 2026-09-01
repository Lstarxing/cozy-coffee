package com.cozy.order.enums;

/**
 * 商品维度的甜度配置参数 (Product Sugar Configuration)
 * 用于控制前端给用户展示哪些甜度选项（风控逻辑）
 * 
 * 基于《CozyCoffee 会员积分体系白皮书 (v5.3)》第十一章：菜单 SKU 配置总表
 * 
 * @version v5.3.1
 * @since 2026-01-12
 */
public enum SugarType {
    /**
     * 自由选择 - 允许所有4个甜度选项
     * 适用商品：美式、拿铁、燕麦拿铁等基础饮品
     * 允许选项：[STANDARD, LESS, HALF, NONE]
     * 
     * v5.3: 兼容历史数据中的多种命名格式
     */
    FREE_CHOICE("自由选择", new String[]{"STANDARD", "standard", "full", "LESS", "less", "HALF", "half", "NONE", "none", "NO_ADDED_SUGAR", "no_added_sugar", "LIGHT", "light"}),
    
    /**
     * 锁定无糖 - 仅允许无糖选项
     * 适用商品：Dirty (脏咖)、手冲精品 (SOE)、澳白
     * 允许选项：[NONE, NO_ADDED_SUGAR]
     *
     * 业务逻辑：这些产品的风味体验要求不添加任何糖分，即「不另外加糖」（奶基底商品自带乳糖，
     * 无糖 NONE 与不另外加糖 NO_ADDED_SUGAR 是两个概念，此处指后者；纯黑咖两种语义等价）。
     * 兼容 V2 糖度值 NO_ADDED_SUGAR——前端对无糖限定商品发送该值。
     */
    NO_SUGAR_ONLY("仅不另外加糖", new String[]{"NONE", "none", "NO_ADDED_SUGAR", "no_added_sugar"}),
    
    /**
     * 最低少甜 (不可去糖) - 不允许无糖选项
     * 适用商品：摩卡、焦糖玛奇朵、冰摇荔枝咖啡等含糖浆/酱料产品
     * 允许选项：[STANDARD, LESS, HALF]
     * 
     * 业务逻辑：这些产品含巧克力酱、焦糖酱等糖浆类原料，物理上无法做到完全无糖
     * 确保产品的风味底线，避免客诉
     */
    MIN_LESS_SWEET("最低少甜", new String[]{"STANDARD", "standard", "full", "LESS", "less", "HALF", "half", "LIGHT", "light"});

    private final String description;
    private final String[] allowedValues;

    SugarType(String description, String[] allowedValues) {
        this.description = description;
        this.allowedValues = allowedValues;
    }

    public String getDescription() {
        return description;
    }

    public String[] getAllowedValues() {
        return allowedValues;
    }

    /**
     * 规范展示值（前端渲染用，区别于校验别名 getAllowedValues）。
     * NO_SUGAR_ONLY 返回空数组——糖度固定「不另外加糖」，前端据此隐藏糖度行。
     */
    public String[] canonicalValues() {
        switch (this) {
            case FREE_CHOICE:
                return new String[]{"STANDARD", "LESS", "HALF", "NO_ADDED_SUGAR"};
            case NO_SUGAR_ONLY:
                return new String[0];
            case MIN_LESS_SWEET:
                return new String[]{"STANDARD", "LESS", "HALF"};
            default:
                return new String[0];
        }
    }

    /**
     * 检查给定的甜度是否被允许
     * v5.3.1: 兼容大小写和不同命名（STANDARD/standard/full, LIGHT/LESS/less）
     * 
     * @param sugar 甜度值字符串
     * @return 是否允许该甜度
     */
    public boolean isAllowed(String sugar) {
        if (sugar == null) return false;
        for (String allowed : allowedValues) {
            if (allowed.equalsIgnoreCase(sugar)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取标准化的允许选项列表（使用 SugarLevel 枚举）
     * 
     * @return SugarLevel 数组
     */
    public SugarLevel[] getAllowedLevels() {
        switch (this) {
            case FREE_CHOICE:
                return new SugarLevel[]{SugarLevel.STANDARD, SugarLevel.LESS, SugarLevel.HALF, SugarLevel.NONE};
            case NO_SUGAR_ONLY:
                return new SugarLevel[]{SugarLevel.NONE};
            case MIN_LESS_SWEET:
                return new SugarLevel[]{SugarLevel.STANDARD, SugarLevel.LESS, SugarLevel.HALF};
            default:
                return new SugarLevel[]{SugarLevel.STANDARD};
        }
    }
}
