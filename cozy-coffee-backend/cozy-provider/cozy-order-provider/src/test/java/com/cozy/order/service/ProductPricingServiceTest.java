package com.cozy.order.service;
import com.cozy.order.service.product.ProductPricingService;
import com.cozy.order.service.product.ProductAddonResolver;
import com.cozy.order.service.product.ProductRuleValidator;

import com.cozy.order.dto.response.AddonGroupDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.entity.CoffeeProductAddon;
import com.cozy.order.entity.CoffeeProductAddonGroup;
import com.cozy.order.entity.ProductAddon;
import com.cozy.order.mapper.CoffeeProductAddonGroupMapper;
import com.cozy.order.mapper.CoffeeProductAddonMapper;
import com.cozy.order.mapper.ProductAddonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Product Rule Matrix（P1E-B）
 * 合法 / 非法组合 + 价格断言：定价 = 规格价（size_type）+ price_delta，与选规格总表一致。
 * 覆盖：拿铁 / 燕麦拿铁 / 生椰（COLD_ONLY）/ 摩卡（MIN_LESS_SWEET 无 SYRUP）。
 */
class ProductPricingServiceTest {

    // ── 数据构造 ──────────────────────────────────────────

    private static CoffeeProduct product(Long id, String sizeType, String tempType, String sugarType,
            String medium, String large) {
        CoffeeProduct p = new CoffeeProduct();
        p.setId(id);
        p.setName("P" + id);
        p.setStatus("active");
        p.setSizeType(sizeType);
        p.setTempType(tempType);
        p.setSugarType(sugarType);
        p.setPrice(new BigDecimal(medium));
        p.setPriceMedium(new BigDecimal(medium));
        p.setPriceLarge(new BigDecimal(large));
        return p;
    }

    private static CoffeeProductAddonGroup group(long id, String category, String mode, int min, int max) {
        CoffeeProductAddonGroup g = new CoffeeProductAddonGroup();
        g.setId(id);
        g.setCategory(category);
        g.setSelectionMode(mode);
        g.setMinSelect(min);
        g.setMaxSelect(max);
        return g;
    }

    private static CoffeeProductAddon binding(long groupId, long addonId, boolean isDefault, String delta) {
        CoffeeProductAddon b = new CoffeeProductAddon();
        b.setGroupId(groupId);
        b.setAddonId(addonId);
        b.setIsDefault(isDefault);
        b.setPriceDelta(new BigDecimal(delta));
        b.setSortOrder(1);
        return b;
    }

    private static ProductAddon addon(long id, String code, String name) {
        ProductAddon a = new ProductAddon();
        a.setId(id);
        a.setCode(code);
        a.setName(name);
        return a;
    }

    private static ProductAddonResolver resolver(List<CoffeeProductAddonGroup> groups,
            List<CoffeeProductAddon> bindings, List<ProductAddon> addons) {
        CoffeeProductAddonGroupMapper groupMapper = mock(CoffeeProductAddonGroupMapper.class);
        when(groupMapper.selectList(any())).thenReturn(groups);
        CoffeeProductAddonMapper productAddonMapper = mock(CoffeeProductAddonMapper.class);
        when(productAddonMapper.selectList(any())).thenReturn(bindings);
        ProductAddonMapper addonMapper = mock(ProductAddonMapper.class);
        when(addonMapper.selectList(any())).thenReturn(addons);
        return new ProductAddonResolver(groupMapper, productAddonMapper, addonMapper, new ObjectMapper());
    }

    private static ProductPricingService pricing(List<CoffeeProductAddonGroup> groups,
            List<CoffeeProductAddon> bindings, List<ProductAddon> addons) {
        return new ProductPricingService(new ProductRuleValidator(), resolver(groups, bindings, addons));
    }

    // ── 经典拿铁：MILK 1/1 + SHOT 0/1 + SYRUP 0/1（互斥）+ OTHER 0/1 ──
    private static List<CoffeeProductAddonGroup> latteGroups() {
        return List.of(
                group(1L, "MILK", "SINGLE", 1, 1),
                group(2L, "SHOT", "SINGLE", 0, 1),
                group(3L, "SYRUP", "SINGLE", 0, 1),
                group(4L, "OTHER", "MULTI", 0, 1));
    }

    private static List<CoffeeProductAddon> latteBindings() {
        return List.of(
                binding(1L, 101L, true, "0"),   // WHOLE_MILK 默认
                binding(1L, 102L, false, "3"),  // OAT_MILK
                binding(2L, 103L, false, "5"),  // EXTRA_SHOT
                binding(3L, 104L, false, "4"),  // VANILLA_SYRUP
                binding(3L, 105L, false, "4"),  // CARAMEL_SYRUP
                binding(4L, 106L, false, "3")); // EXTRA_FOAM
    }

    private static List<ProductAddon> latteAddons() {
        return List.of(
                addon(101L, "WHOLE_MILK", "全脂奶"),
                addon(102L, "OAT_MILK", "燕麦奶"),
                addon(103L, "EXTRA_SHOT", "额外浓缩"),
                addon(104L, "VANILLA_SYRUP", "香草糖浆"),
                addon(105L, "CARAMEL_SYRUP", "焦糖糖浆"),
                addon(106L, "EXTRA_FOAM", "加奶泡"));
    }

    // ── 燕麦拿铁：MILK 默认燕麦 +0 / 全脂 +0 ──
    private static List<CoffeeProductAddonGroup> oatGroups() {
        return List.of(group(11L, "MILK", "SINGLE", 1, 1));
    }

    private static List<CoffeeProductAddon> oatBindings() {
        return List.of(
                binding(11L, 201L, true, "0"),  // OAT_MILK 默认
                binding(11L, 202L, false, "0")); // WHOLE_MILK +0
    }

    private static List<ProductAddon> oatAddons() {
        return List.of(addon(201L, "OAT_MILK", "燕麦奶"), addon(202L, "WHOLE_MILK", "全脂奶"));
    }

    // ── 生椰拿铁：COLD_ONLY，椰奶默认 / 燕麦 +3 ──
    private static List<CoffeeProductAddonGroup> coconutGroups() {
        return List.of(group(21L, "MILK", "SINGLE", 1, 1));
    }

    private static List<CoffeeProductAddon> coconutBindings() {
        return List.of(
                binding(21L, 301L, true, "0"),  // COCONUT_MILK 默认
                binding(21L, 302L, false, "3")); // OAT_MILK
    }

    private static List<ProductAddon> coconutAddons() {
        return List.of(addon(301L, "COCONUT_MILK", "椰奶"), addon(302L, "OAT_MILK", "燕麦奶"));
    }

    // ── 摩卡：MIN_LESS_SWEET，无 SYRUP 组 ──
    private static List<CoffeeProductAddonGroup> mochaGroups() {
        return List.of(
                group(31L, "MILK", "SINGLE", 1, 1),
                group(32L, "SHOT", "SINGLE", 0, 1));
    }

    private static List<CoffeeProductAddon> mochaBindings() {
        return List.of(
                binding(31L, 401L, true, "0"),  // WHOLE_MILK 默认
                binding(31L, 402L, false, "3"), // OAT_MILK
                binding(32L, 403L, false, "5")); // EXTRA_SHOT
    }

    private static List<ProductAddon> mochaAddons() {
        return List.of(
                addon(401L, "WHOLE_MILK", "全脂奶"),
                addon(402L, "OAT_MILK", "燕麦奶"),
                addon(403L, "EXTRA_SHOT", "额外浓缩"));
    }

    // ── 经典拿铁价格矩阵 ───────────────────────────────────
    @Test
    void latteMediumOat() {
        var s = pricing(latteGroups(), latteBindings(), latteAddons());
        var p = product(1L, "MEDIUM_LARGE", "HOT_COLD", "FREE_CHOICE", "28", "32");
        var r = s.price(p, "MEDIUM", "HOT", "STANDARD", "[{\"code\":\"OAT_MILK\"}]");
        assertTrue(r.valid(), r.error());
        assertEquals(new BigDecimal("28"), r.basePrice());
        assertEquals(new BigDecimal("3"), r.addonFee());
        assertEquals(new BigDecimal("31"), r.finalPrice());
        assertTrue(r.normalizedAddonsJson().contains("\"code\":\"OAT_MILK\""));
    }

    @Test
    void latteLargeOatShot() {
        var s = pricing(latteGroups(), latteBindings(), latteAddons());
        var p = product(1L, "MEDIUM_LARGE", "HOT_COLD", "FREE_CHOICE", "28", "32");
        var r = s.price(p, "LARGE", "HOT", "STANDARD", "[{\"code\":\"OAT_MILK\"},{\"code\":\"EXTRA_SHOT\"}]");
        assertTrue(r.valid(), r.error());
        assertEquals(new BigDecimal("32"), r.basePrice()); // 大杯 32，非 28+3
        assertEquals(new BigDecimal("8"), r.addonFee());   // OAT 3 + SHOT 5
        assertEquals(new BigDecimal("40"), r.finalPrice());
    }

    @Test
    void latteNoAddonInjectsDefaultMilk() {
        var s = pricing(latteGroups(), latteBindings(), latteAddons());
        var p = product(1L, "MEDIUM_LARGE", "HOT_COLD", "FREE_CHOICE", "28", "32");
        var r = s.price(p, "MEDIUM", "HOT", "STANDARD", "[]");
        assertTrue(r.valid(), r.error());
        assertEquals(new BigDecimal("28"), r.finalPrice());
        assertTrue(r.normalizedAddonsJson().contains("WHOLE_MILK")); // 默认全脂注入
        assertEquals(new BigDecimal("0"), r.addonFee());
    }

    @Test
    void latteSyrupMutualExclusive() {
        var s = pricing(latteGroups(), latteBindings(), latteAddons());
        var p = product(1L, "MEDIUM_LARGE", "HOT_COLD", "FREE_CHOICE", "28", "32");
        var r = s.price(p, "MEDIUM", "HOT", "STANDARD",
                "[{\"code\":\"VANILLA_SYRUP\"},{\"code\":\"CARAMEL_SYRUP\"}]");
        assertFalse(r.valid());
    }

    @Test
    void latteDuplicateAddonRejected() {
        var s = pricing(latteGroups(), latteBindings(), latteAddons());
        var p = product(1L, "MEDIUM_LARGE", "HOT_COLD", "FREE_CHOICE", "28", "32");
        var r = s.price(p, "MEDIUM", "HOT", "STANDARD",
                "[{\"code\":\"OAT_MILK\"},{\"code\":\"OAT_MILK\"}]");
        assertFalse(r.valid());
    }

    @Test
    void latteUnboundAddonRejectedNoFallback() {
        var s = pricing(latteGroups(), latteBindings(), latteAddons());
        var p = product(1L, "MEDIUM_LARGE", "HOT_COLD", "FREE_CHOICE", "28", "32");
        var r = s.price(p, "MEDIUM", "HOT", "STANDARD", "[{\"code\":\"SOY_MILK\"}]");
        assertFalse(r.valid()); // 未绑定 → 拒绝，不回退 product_addons.price
    }

    @Test
    void latteLegacySpecialMilkTranslated() {
        var s = pricing(latteGroups(), latteBindings(), latteAddons());
        var p = product(1L, "MEDIUM_LARGE", "HOT_COLD", "FREE_CHOICE", "28", "32");
        var r = s.price(p, "MEDIUM", "HOT", "STANDARD",
                "[{\"code\":\"SPECIAL_MILK\",\"name\":\"OAT\",\"price\":999}]");
        assertTrue(r.valid(), r.error());
        assertEquals(new BigDecimal("3"), r.addonFee()); // price 999 不可信，取 price_delta 3
        assertEquals(new BigDecimal("31"), r.finalPrice());
    }

    // ── 燕麦拿铁：默认燕麦 +0、切全脂 +0 ──
    @Test
    void oatLatteDefaultAndWholeMilkFree() {
        var s = pricing(oatGroups(), oatBindings(), oatAddons());
        var p = product(2L, "MEDIUM_LARGE", "HOT_COLD", "FREE_CHOICE", "32", "35");
        var r1 = s.price(p, "MEDIUM", "HOT", "STANDARD", "[]");
        assertTrue(r1.valid(), r1.error());
        assertEquals(new BigDecimal("32"), r1.finalPrice()); // 默认燕麦 +0
        var r2 = s.price(p, "MEDIUM", "HOT", "STANDARD", "[{\"code\":\"WHOLE_MILK\"}]");
        assertTrue(r2.valid(), r2.error());
        assertEquals(new BigDecimal("32"), r2.finalPrice()); // 切全脂 +0 不降价
    }

    // ── 生椰：COLD_ONLY、椰奶默认 +0 / 燕麦 +3 ──
    @Test
    void coconutLatteColdOnlyAndPrice() {
        var s = pricing(coconutGroups(), coconutBindings(), coconutAddons());
        var p = product(3L, "MEDIUM_LARGE", "COLD_ONLY", "FREE_CHOICE", "32", "35");
        var r1 = s.price(p, "MEDIUM", "COLD", "STANDARD", "[]");
        assertTrue(r1.valid(), r1.error());
        assertEquals(new BigDecimal("32"), r1.finalPrice()); // 椰奶默认 +0
        var r2 = s.price(p, "MEDIUM", "COLD", "STANDARD", "[{\"code\":\"OAT_MILK\"}]");
        assertTrue(r2.valid(), r2.error());
        assertEquals(new BigDecimal("35"), r2.finalPrice()); // +燕麦 3
        var r3 = s.price(p, "MEDIUM", "HOT", "STANDARD", "[]");
        assertFalse(r3.valid()); // 生椰不可热
    }

    // ── 摩卡：MIN_LESS_SWEET 不可去糖、无 SYRUP ──
    @Test
    void mochaPriceAndNoSugarRemovalNoSyrup() {
        var s = pricing(mochaGroups(), mochaBindings(), mochaAddons());
        var p = product(4L, "MEDIUM_LARGE", "HOT_COLD", "MIN_LESS_SWEET", "32", "36");
        var r1 = s.price(p, "MEDIUM", "HOT", "STANDARD", "[{\"code\":\"OAT_MILK\"},{\"code\":\"EXTRA_SHOT\"}]");
        assertTrue(r1.valid(), r1.error());
        assertEquals(new BigDecimal("40"), r1.finalPrice()); // 32 + OAT 3 + SHOT 5
        var r2 = s.price(p, "MEDIUM", "HOT", "NO_ADDED_SUGAR", "[]");
        assertFalse(r2.valid()); // MIN_LESS_SWEET 不可不另外加糖
        var r3 = s.price(p, "MEDIUM", "HOT", "STANDARD", "[{\"code\":\"VANILLA_SYRUP\"}]");
        assertFalse(r3.valid()); // 摩卡无 SYRUP 组
    }

    // ── 菜单 / 详情 addonGroups 形状（P2-1）──────────────────
    @Test
    void menuGroups_returnGroupStructureWithPriceDelta() {
        var r = resolver(latteGroups(), latteBindings(), latteAddons());
        var groups = r.loadMenuGroups(1L);
        assertEquals(4, groups.size()); // MILK / SHOT / SYRUP / OTHER
        AddonGroupDTO milk = groups.get(0);
        assertEquals("MILK", milk.getCategory());
        assertEquals(1, milk.getMinSelect());
        assertEquals(1, milk.getMaxSelect());
        assertEquals(2, milk.getItems().size());
        var oat = milk.getItems().stream()
                .filter(i -> "OAT_MILK".equals(i.getCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("3"), oat.getPriceDelta());
        assertFalse(oat.getIsDefault());
        // 摩卡 / 玛奇朵无 SYRUP 组：MILK + SHOT 两组
        var mochaGroups = resolver(mochaGroups(), mochaBindings(), mochaAddons()).loadMenuGroups(4L);
        assertEquals(2, mochaGroups.size());
        assertTrue(mochaGroups.stream().noneMatch(g -> "SYRUP".equals(g.getCategory())));
    }
}
