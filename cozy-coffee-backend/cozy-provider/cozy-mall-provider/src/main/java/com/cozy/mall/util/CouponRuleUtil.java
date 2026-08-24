package com.cozy.mall.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 优惠券规则工具（rule_json 解析 + 品类判断），静态方法供 CouponCalculator 策略与 PointsMallServiceImpl 共用。
 */
@Slf4j
public final class CouponRuleUtil {

    private CouponRuleUtil() {
    }

    /**
     * 解析规则 JSON 中的整数值（处理冒号前后空格；带小数点截断为整数，如 8.5 -> 8）
     */
    public static int parseValue(String ruleJson, String key) {
        if (ruleJson == null || key == null) {
            return 0;
        }
        try {
            String search = "\"" + key + "\"";
            int idx = ruleJson.indexOf(search);
            if (idx >= 0) {
                int colonPos = idx + search.length();
                while (colonPos < ruleJson.length()
                        && (ruleJson.charAt(colonPos) == ' ' || ruleJson.charAt(colonPos) == '\t')) {
                    colonPos++;
                }
                if (colonPos < ruleJson.length() && ruleJson.charAt(colonPos) == ':') {
                    colonPos++;
                    while (colonPos < ruleJson.length()
                            && (ruleJson.charAt(colonPos) == ' ' || ruleJson.charAt(colonPos) == '\t')) {
                        colonPos++;
                    }
                    int start = colonPos;
                    int end = start;
                    while (end < ruleJson.length()
                            && (Character.isDigit(ruleJson.charAt(end)) || ruleJson.charAt(end) == '.')) {
                        end++;
                    }
                    if (end > start) {
                        double doubleVal = Double.parseDouble(ruleJson.substring(start, end));
                        return (int) doubleVal;
                    }
                }
            }
            log.debug("JSON解析跳过（未找到key）: key={}", key);
        } catch (Exception e) {
            log.error("JSON解析异常: key={}, json={}", key, ruleJson, e);
        }
        return 0;
    }

    /**
     * 解析规则 JSON 中的 long 值（用于 linkedProductId）
     */
    public static long parseLongValue(String ruleJson, String key) {
        if (ruleJson == null || key == null) {
            return 0;
        }
        try {
            String search = "\"" + key + "\"";
            int idx = ruleJson.indexOf(search);
            if (idx >= 0) {
                int colonPos = idx + search.length();
                while (colonPos < ruleJson.length()
                        && (ruleJson.charAt(colonPos) == ' ' || ruleJson.charAt(colonPos) == '\t')) {
                    colonPos++;
                }
                if (colonPos < ruleJson.length() && ruleJson.charAt(colonPos) == ':') {
                    colonPos++;
                    while (colonPos < ruleJson.length()
                            && (ruleJson.charAt(colonPos) == ' ' || ruleJson.charAt(colonPos) == '\t')) {
                        colonPos++;
                    }
                    int start = colonPos;
                    int end = start;
                    while (end < ruleJson.length() && Character.isDigit(ruleJson.charAt(end))) {
                        end++;
                    }
                    if (end > start) {
                        return Long.parseLong(ruleJson.substring(start, end));
                    }
                }
            }
            log.debug("JSON解析跳过（未找到key）: key={}", key);
        } catch (Exception e) {
            log.error("JSON解析异常: key={}, json={}", key, ruleJson, e);
        }
        return 0;
    }

    /**
     * 解析规则 JSON 中的浮点值（用于 discountRate 等）
     */
    public static double parseDoubleValue(String ruleJson, String key) {
        if (ruleJson == null || key == null) {
            return 0.0;
        }
        try {
            String searchKey = "\"" + key + "\":";
            int keyIndex = ruleJson.indexOf(searchKey);
            if (keyIndex >= 0) {
                int colonPos = keyIndex + searchKey.length();
                while (colonPos < ruleJson.length()
                        && (ruleJson.charAt(colonPos) == ' ' || ruleJson.charAt(colonPos) == '\t')) {
                    colonPos++;
                }
                int start = colonPos;
                int end = start;
                while (end < ruleJson.length()
                        && (Character.isDigit(ruleJson.charAt(end)) || ruleJson.charAt(end) == '.'
                                || ruleJson.charAt(end) == '-')) {
                    end++;
                }
                if (end > start) {
                    return Double.parseDouble(ruleJson.substring(start, end));
                }
            }
        } catch (Exception e) {
            log.debug("解析浮点数失败: key={}, error={}", key, e.getMessage());
        }
        return 0.0;
    }

    public static boolean isBakery(String category) {
        if (category == null) {
            return false;
        }
        String c = category.toLowerCase();
        return c.contains("bakery") || c.contains("dessert") || c.contains("cake") || c.contains("food");
    }

    public static boolean isDrink(String category) {
        if (category == null) {
            return true;
        }
        String c = category.toLowerCase();
        return !c.contains("bakery") && !c.contains("dessert") && !c.contains("food");
    }
}
