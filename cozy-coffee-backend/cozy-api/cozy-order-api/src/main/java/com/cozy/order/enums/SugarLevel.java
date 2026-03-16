package com.cozy.order.enums;

/**
 * 甜度枚举 (Sugar Level Enums)
 * 用户端可见且数据库存储的标准化值
 * 
 * @version v5.3
 * @since 2026-01-12
 */
public enum SugarLevel {
    /**
     * 全糖 (100%) - 默认标准甜度
     * 适用场景：大部分饮品的默认选项
     */
    STANDARD("全糖", "100%", 100),
    
    /**
     * 少糖 (70%) - 稍微减甜
     * 适用场景：对应白皮书中摩卡/玛奇朵的"最低[少甜]"要求
     */
    LESS("少糖", "70%", 70),
    
    /**
     * 半糖 (50%) - 明显减甜
     * 适用场景：拿铁、美式等常用选项
     */
    HALF("半糖", "50%", 50),
    
    /**
     * 无糖 (0%) - 完全不加糖
     * 适用场景：Dirty、澳白、生椰拿铁等
     */
    NONE("无糖", "0%", 0);

    private final String label;       // 前端展示文案
    private final String description; // 描述/定义
    private final int percentage;     // 百分比值

    SugarLevel(String label, String description, int percentage) {
        this.label = label;
        this.description = description;
        this.percentage = percentage;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public int getPercentage() {
        return percentage;
    }

    /**
     * 从字符串值解析为枚举
     * 兼容多种历史格式：STANDARD/standard/full, LESS/less, HALF/half, NONE/none/LIGHT/light
     * 
     * @param value 甜度值字符串
     * @return 对应的枚举值，无法识别时返回 STANDARD
     */
    public static SugarLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return STANDARD;
        }
        
        String normalized = value.trim().toUpperCase();
        
        // 标准值匹配
        switch (normalized) {
            case "STANDARD":
            case "FULL":
                return STANDARD;
            case "LESS":
                return LESS;
            case "HALF":
                return HALF;
            case "NONE":
                return NONE;
            // 兼容旧数据：LIGHT 映射到 LESS
            case "LIGHT":
                return LESS;
            default:
                // 无法识别的值默认返回 STANDARD
                return STANDARD;
        }
    }

    /**
     * 获取数据库存储的标准代码
     * 
     * @return 标准代码字符串（大写）
     */
    public String getCode() {
        return this.name();
    }
}
