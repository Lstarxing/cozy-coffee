package com.cozy.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.order.entity.CoffeeProductAddon;
import com.cozy.order.entity.CoffeeProductAddonGroup;
import com.cozy.order.entity.ProductAddon;
import com.cozy.order.mapper.CoffeeProductAddonGroupMapper;
import com.cozy.order.mapper.CoffeeProductAddonMapper;
import com.cozy.order.mapper.ProductAddonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V2 加料权威解析（P1E）
 * 前端 id/code/name/price 全部不可信：code 为身份、id 若存在须与 code 一致、name/price 不参与定价。
 * 价格只取 coffee_product_addon.price_delta；未绑定 addon 一律拒绝（不回退 product_addons.price）。
 * 组内去重、min/max 校验、SINGLE 上限、默认项注入（min_select>0 且未选 → 注入 is_default）。
 * 兼容旧格式：SPECIAL_MILK（前端合成码）按 name 翻译为真实码，价格仍由 price_delta 决定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAddonResolver {

    private final CoffeeProductAddonGroupMapper groupMapper;
    private final CoffeeProductAddonMapper productAddonMapper;
    private final ProductAddonMapper addonMapper;
    private final ObjectMapper objectMapper;

    /** 前端合成码 → 真实加料码（过渡期兼容请求格式，不兼容价格语义） */
    private static final Map<String, String> LEGACY_MILK_CODES = Map.of(
            "OAT", "OAT_MILK",
            "COCONUT", "COCONUT_MILK",
            "SOY", "SOY_MILK");

    /** 解析结果：是否合法 + 加料费 + 规范化成交快照 + 逐项价格（尊享通兑券免加料用） */
    public record AddonResolution(boolean valid, String error, BigDecimal fee,
            String normalizedJson, List<BigDecimal> addonPrices) {
        static AddonResolution invalid(String error) {
            return new AddonResolution(false, error, BigDecimal.ZERO, "[]", List.of());
        }
        static AddonResolution empty() {
            return new AddonResolution(true, null, BigDecimal.ZERO, "[]", List.of());
        }
        static AddonResolution ok(BigDecimal fee, String json, List<BigDecimal> prices) {
            return new AddonResolution(true, null, fee, json, prices);
        }
    }

    public AddonResolution resolve(Long productId, String addonsJson) {
        List<CoffeeProductAddonGroup> groups = loadGroups(productId);
        if (groups.isEmpty()) {
            return hasSubmittedAddons(addonsJson)
                    ? AddonResolution.invalid("该商品不支持加料")
                    : AddonResolution.empty();
        }

        Map<String, ResolvedAddon> byCode = loadResolvedAddons(groups);

        List<SubmittedAddon> submitted = parseSubmitted(addonsJson);
        if (submitted == null) {
            return AddonResolution.invalid("加料数据格式错误");
        }

        // 去重 + 翻译 + id/code 一致性 + 绑定校验
        Map<String, ResolvedAddon> selected = new LinkedHashMap<>();
        for (SubmittedAddon s : submitted) {
            String code = normalizeCode(s);
            if (code == null) {
                return AddonResolution.invalid("未知加料: " + s.name());
            }
            if (selected.containsKey(code)) {
                return AddonResolution.invalid("加料重复: " + code);
            }
            ResolvedAddon resolved = byCode.get(code);
            if (resolved == null) {
                return AddonResolution.invalid("该商品不支持该加料: " + code);
            }
            if (s.id() != null && !s.id().equals(resolved.addonId())) {
                return AddonResolution.invalid("加料标识不一致: " + code);
            }
            selected.put(code, resolved);
        }

        // 组级 min/max + 默认项注入；按组顺序输出快照
        Map<String, CoffeeProductAddonGroup> groupByCategory = new LinkedHashMap<>();
        for (CoffeeProductAddonGroup g : groups) {
            groupByCategory.put(g.getCategory(), g);
        }

        List<ResolvedAddon> finalAddons = new ArrayList<>();
        BigDecimal fee = BigDecimal.ZERO;
        List<BigDecimal> addonPrices = new ArrayList<>();
        for (CoffeeProductAddonGroup group : groups) {
            List<ResolvedAddon> inGroup = selected.values().stream()
                    .filter(a -> a.category().equals(group.getCategory()))
                    .toList();
            int count = inGroup.size();
            if (count > group.getMaxSelect()) {
                return AddonResolution.invalid(group.getCategory() + " 组最多选择 " + group.getMaxSelect() + " 项");
            }
            finalAddons.addAll(inGroup);
            if (count < group.getMinSelect()) {
                ResolvedAddon def = byCode.values().stream()
                        .filter(a -> a.category().equals(group.getCategory()) && a.isDefault())
                        .findFirst().orElse(null);
                if (def != null) {
                    finalAddons.add(def);
                } else {
                    return AddonResolution.invalid(group.getCategory() + " 组缺少必选项");
                }
            }
        }

        // 定价 + 规范化成交快照（含默认项，price = price_delta 实际增量）
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (ResolvedAddon a : finalAddons) {
            fee = fee.add(a.priceDelta());
            addonPrices.add(a.priceDelta());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("code", a.code());
            entry.put("name", a.name());
            entry.put("price", a.priceDelta());
            snapshot.add(entry);
        }
        try {
            return AddonResolution.ok(fee, objectMapper.writeValueAsString(snapshot), addonPrices);
        } catch (Exception e) {
            log.warn("加料快照序列化失败: productId={}", productId, e);
            return AddonResolution.invalid("加料快照序列化失败");
        }
    }

    private List<CoffeeProductAddonGroup> loadGroups(Long productId) {
        return groupMapper.selectList(new LambdaQueryWrapper<CoffeeProductAddonGroup>()
                .eq(CoffeeProductAddonGroup::getProductId, productId)
                .orderByAsc(CoffeeProductAddonGroup::getSortOrder));
    }

    /** 构建 code → 解析结果（含组约束、addon 主数据、price_delta） */
    private Map<String, ResolvedAddon> loadResolvedAddons(List<CoffeeProductAddonGroup> groups) {
        List<Long> groupIds = groups.stream().map(CoffeeProductAddonGroup::getId).toList();
        List<CoffeeProductAddon> bindings = productAddonMapper.selectList(
                new LambdaQueryWrapper<CoffeeProductAddon>().in(CoffeeProductAddon::getGroupId, groupIds));
        if (bindings.isEmpty()) {
            return Map.of();
        }
        List<Long> addonIds = bindings.stream().map(CoffeeProductAddon::getAddonId).distinct().toList();
        List<ProductAddon> addons = addonMapper.selectList(
                new LambdaQueryWrapper<ProductAddon>().in(ProductAddon::getId, addonIds));
        Map<Long, ProductAddon> addonById = new LinkedHashMap<>();
        for (ProductAddon a : addons) {
            addonById.put(a.getId(), a);
        }
        Map<Long, CoffeeProductAddonGroup> groupById = new LinkedHashMap<>();
        for (CoffeeProductAddonGroup g : groups) {
            groupById.put(g.getId(), g);
        }

        Map<String, ResolvedAddon> byCode = new LinkedHashMap<>();
        for (CoffeeProductAddon b : bindings) {
            CoffeeProductAddonGroup group = groupById.get(b.getGroupId());
            ProductAddon addon = addonById.get(b.getAddonId());
            if (group == null || addon == null) {
                continue;
            }
            byCode.put(addon.getCode(), new ResolvedAddon(
                    group.getCategory(), group.getMinSelect(), group.getMaxSelect(),
                    addon.getId(), addon.getCode(), addon.getName(),
                    b.getPriceDelta(), Boolean.TRUE.equals(b.getIsDefault())));
        }
        return byCode;
    }

    private boolean hasSubmittedAddons(String addonsJson) {
        if (addonsJson == null || addonsJson.isBlank()) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(addonsJson);
            return node.isArray() && node.size() > 0;
        } catch (Exception e) {
            return true; // 无法解析视为提交了内容
        }
    }

    /** 解析前端 addon 意图；null 表示格式错误。只保留 id/code/name（name 仅用于 SPECIAL_MILK 翻译） */
    private List<SubmittedAddon> parseSubmitted(String addonsJson) {
        if (addonsJson == null || addonsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(addonsJson);
            if (!node.isArray()) {
                return null;
            }
            List<SubmittedAddon> list = new ArrayList<>();
            for (JsonNode addon : node) {
                Long id = addon.hasNonNull("id") ? addon.get("id").asLong() : null;
                String code = addon.hasNonNull("code") ? addon.get("code").asText() : null;
                String name = addon.hasNonNull("name") ? addon.get("name").asText() : null;
                list.add(new SubmittedAddon(id, code, name));
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    /** 归一化 code：SPECIAL_MILK 按 name 翻译为真实码；否则转大写 */
    private String normalizeCode(SubmittedAddon s) {
        String code = s.code();
        if (code == null || code.isBlank()) {
            return null;
        }
        String upper = code.trim().toUpperCase();
        if ("SPECIAL_MILK".equals(upper)) {
            return s.name() == null ? null : LEGACY_MILK_CODES.get(s.name().trim().toUpperCase());
        }
        return upper;
    }

    private record SubmittedAddon(Long id, String code, String name) {
    }

    private record ResolvedAddon(String category, int minSelect, int maxSelect,
            Long addonId, String code, String name, BigDecimal priceDelta, boolean isDefault) {
    }
}
