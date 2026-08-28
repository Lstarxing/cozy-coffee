package com.cozy.order.enums;

/**
 * 温度配置类型
 * 定义产品的温度选项限制（v2：砍"温"，新单禁止产生 WARM，历史订单快照保留）
 */
public enum TempType {
    /**
     * 热/冰 - 大部分饮品（原 ALL_OK，砍温：去掉 WARM）
     */
    HOT_COLD("热/冰", new String[]{"iced", "COLD", "cold", "hot", "HOT"}),

    /**
     * 仅限冰 - Dirty、冰摇、生椰等；原 NO_HOT（冰/温）并入：不可热即冰
     */
    COLD_ONLY("仅限冰", new String[]{"iced", "COLD", "cold"}),

    /**
     * 仅限热 - 澳白等奶泡工艺饮品
     */
    HOT_ONLY("仅限热", new String[]{"hot", "HOT"});

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
     * 检查给定的温度是否被允许（v2：已砍温，WARM 不在任何 allowedValues 中）
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
