package com.cozy.mall.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * 优惠券规则工具（rule_json 解析 + 品类判断），静态方法供 CouponCalculator 策略与 PointsMallServiceImpl 共用。
 * rule_json 统一用 Jackson 反序列化（发券侧生成的都是合法 JSON），替代手写字符串解析。
 */
@Slf4j
public final class CouponRuleUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CouponRuleUtil() {
    }

    private static JsonNode parse(String ruleJson) {
        if (ruleJson == null || ruleJson.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(ruleJson);
        } catch (Exception e) {
            log.error("券规则JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    /** 解析规则 JSON 中的整数值（缺失/非法返回 0；小数截断，如 8.5 -> 8） */
    public static int parseValue(String ruleJson, String key) {
        JsonNode node = field(ruleJson, key);
        return node == null ? 0 : node.asInt();
    }

    /** 解析规则 JSON 中的 long 值（用于 linkedProductId） */
    public static long parseLongValue(String ruleJson, String key) {
        JsonNode node = field(ruleJson, key);
        return node == null ? 0 : node.asLong();
    }

    /** 解析规则 JSON 中的浮点值（用于 discountRate 等） */
    public static double parseDoubleValue(String ruleJson, String key) {
        JsonNode node = field(ruleJson, key);
        return node == null ? 0.0 : node.asDouble();
    }

    private static JsonNode field(String ruleJson, String key) {
        if (key == null) {
            return null;
        }
        JsonNode node = parse(ruleJson);
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(key);
        return (v == null || v.isNull()) ? null : v;
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
