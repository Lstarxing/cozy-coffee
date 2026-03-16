package com.cozy.order.enums;

/**
 * 温度配置类型
 * 定义产品的温度选项限制
 */
public enum TempType {
    /**
     * 冰/热/温全选 - 大部分饮品
     * v5.3: 兼容前端大写命名 COLD/HOT/WARM 和旧命名 iced/hot/warm
     */
    ALL_OK("全温度", new String[]{"iced", "COLD", "cold", "hot", "HOT", "warm", "WARM"}),
    
    /**
     * 仅限冰 - 用于 Dirty、冰摇系列
     * v5.3: 兼容 COLD/cold/iced
     */
    COLD_ONLY("仅限冰", new String[]{"iced", "COLD", "cold"}),
    
    /**
     * 仅限热 - 用于澳白等奶泡工艺饮品
     * v5.3: 兼容 HOT/hot
     */
    HOT_ONLY("仅限热", new String[]{"hot", "HOT"}),
    
    /**
     * 冰/温，不可烫 - 用于生椰拿铁（椰乳高温易分层）
     * v5.3: 兼容前端大写命名
     */
    NO_HOT("冰/温", new String[]{"iced", "COLD", "cold", "warm", "WARM"});

    private final String description;
    private final String[] allowedValues;

    TempType(String description, String[] allowedValues) {
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
     * 检查给定的温度是否被允许
     * v5.3: 兼容大小写和不同命名（HOT/hot, COLD/cold/iced）
     */
    public boolean isAllowed(String temp) {
        if (temp == null) return false;
        for (String allowed : allowedValues) {
            if (allowed.equalsIgnoreCase(temp)) {
                return true;
            }
        }
        return false;
    }
}
