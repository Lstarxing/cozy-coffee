package com.cozy.order.enums;

/**
 * 杯型配置类型
 * 定义产品的杯型规格选项
 */
public enum SizeType {
    /**
     * 标准杯/固定杯型 - 用于澳白、Dirty、特调等单规格产品
     * v5.3: 前端统一使用大写 STANDARD，后端兼容
     */
    DEFAULT("标准杯", new String[]{"standard", "STANDARD"}),
    
    /**
     * 中杯、大杯可选 - 用于大部分意式咖啡
     * v5.3: 标准杯即中杯，接受 STANDARD/standard/medium
     */
    MEDIUM_LARGE("中杯/大杯", new String[]{"standard", "STANDARD", "medium", "large", "LARGE"}),
    
    /**
     * 小杯、中杯、大杯全选 - 扩展选项（预留）
     * v5.3: 兼容大小写
     */
    ALL_SIZES("全规格", new String[]{"small", "SMALL", "medium", "MEDIUM", "large", "LARGE"});

    private final String description;
    private final String[] allowedValues;

    SizeType(String description, String[] allowedValues) {
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
     * 检查给定的尺寸是否被允许
     * v5.3: 兼容大小写和旧命名 (STANDARD/standard/medium)
     */
    public boolean isAllowed(String size) {
        if (size == null) return false;
        
        for (String allowed : allowedValues) {
            if (allowed.equalsIgnoreCase(size)) {
                return true;
            }
        }
        return false;
    }
}
