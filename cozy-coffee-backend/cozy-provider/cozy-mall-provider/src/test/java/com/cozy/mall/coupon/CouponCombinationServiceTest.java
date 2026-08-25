package com.cozy.mall.coupon;

import com.cozy.common.constant.CouponStackingConfig;
import com.cozy.common.exception.BusinessException;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.dto.response.CouponCombinationResult;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.mapper.UserCouponMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 优惠券组合引擎单测：主券/辅券/独占分类、组合校验、preview 与 use 共用逻辑、freeAddon。
 */
class CouponCombinationServiceTest {

    private UserCouponMapper userCouponMapper;
    private CouponCombinationService service;

    @BeforeEach
    void setUp() {
        userCouponMapper = mock(UserCouponMapper.class);

        CouponStackingConfig stacking = new CouponStackingConfig();
        Map<String, String> categories = new HashMap<>();
        categories.put("DELIVERY_FEE", "ADDON");
        categories.put("SHOT", "ADDON");
        stacking.setCategories(categories);
        stacking.setMaxDeliveryFee(1);

        Map<String, CouponCalculator> calculators = new HashMap<>();
        calculators.put("DISCOUNT", new DiscountCouponCalculator());
        calculators.put("FULL_REDUCE", new FullReduceCouponCalculator());
        calculators.put("SHOT", new ShotCouponCalculator());
        calculators.put("DELIVERY_FEE", new DeliveryFeeCouponCalculator());

        service = new CouponCombinationService(userCouponMapper, stacking, calculators);
    }

    // ==================== 组合规则 ====================

    @Test
    void singleMain_valid() {
        stubCoupons(coupon(1L, "D1", "DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\"}"));
        CouponCombinationResult r = service.preview(1L, List.of("D1"),
                new BigDecimal("60"), BigDecimal.ZERO, List.of(), drinks(30, 30));
        // 60 * 0.5 = 30
        assertEquals(0, new BigDecimal("30").compareTo(r.getMainDiscount()));
        assertEquals(1L, r.getMainCouponId().longValue());
        assertTrue(r.getAddonCouponIds().isEmpty());
    }

    @Test
    void mainPlusAddon_discountSplit() {
        stubCoupons(coupon(1L, "D1", "DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\"}"),
                coupon(2L, "S1", "SHOT", "{\"value\":5}"));
        List<ItemCheckDTO> items = new ArrayList<>();
        items.add(item(1L, new BigDecimal("30"), "drink", 1, true)); // 加浓缩
        items.add(item(2L, new BigDecimal("20"), "drink", 1, false));
        CouponCombinationResult r = service.preview(1L, List.of("D1", "S1"),
                new BigDecimal("50"), BigDecimal.ZERO, List.of(), items);
        // 主券 50*0.5=25；辅券 SHOT=5
        assertEquals(0, new BigDecimal("25").compareTo(r.getMainDiscount()));
        assertEquals(0, new BigDecimal("5").compareTo(r.getAddonDiscount()));
        assertEquals(2L, r.getAddonCouponIds().get(0).longValue());
    }

    @Test
    void twoMain_rejected() {
        stubCoupons(coupon(1L, "D1", "DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\"}"),
                coupon(2L, "D2", "DISCOUNT", "{\"value\":80,\"scope\":\"DRINK_ONLY\"}"));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.preview(1L, List.of("D1", "D2"), new BigDecimal("60"), BigDecimal.ZERO,
                        List.of(), drinks(30, 30)));
        assertTrue(e.getMessage().contains("主券最多使用一张"));
    }

    @Test
    void exclusiveAlone_validAsMain() {
        stubCoupons(coupon(1L, "NEW1", "DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\",\"exclusive\":true}"));
        CouponCombinationResult r = service.preview(1L, List.of("NEW1"),
                new BigDecimal("30"), BigDecimal.ZERO, List.of(), drinks(30));
        // EXCLUSIVE 仍按主券口径计算：30*0.5=15
        assertEquals(0, new BigDecimal("15").compareTo(r.getMainDiscount()));
        assertTrue(r.isExclusive());
    }

    @Test
    void exclusivePlusOther_rejected() {
        stubCoupons(coupon(1L, "NEW1", "DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\",\"exclusive\":true}"),
                coupon(2L, "D1", "DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\"}"));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.preview(1L, List.of("NEW1", "D1"), new BigDecimal("60"), BigDecimal.ZERO,
                        List.of(), drinks(30, 30)));
        assertTrue(e.getMessage().contains("不可与其他券叠加"));
    }

    @Test
    void deliveryFeePlusMain_valid() {
        stubCoupons(coupon(1L, "D1", "DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\"}"),
                coupon(2L, "DF1", "DELIVERY_FEE", "{\"value\":3}"));
        CouponCombinationResult r = service.preview(1L, List.of("D1", "DF1"),
                new BigDecimal("60"), BigDecimal.ZERO, List.of(), drinks(30, 30));
        assertEquals(0, new BigDecimal("30").compareTo(r.getMainDiscount()));
        assertEquals(0, new BigDecimal("3").compareTo(r.getDeliveryFeeDiscount()));
    }

    @Test
    void twoDeliveryFee_rejected() {
        stubCoupons(coupon(1L, "DF1", "DELIVERY_FEE", "{\"value\":3}"),
                coupon(2L, "DF2", "DELIVERY_FEE", "{\"value\":3}"));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.preview(1L, List.of("DF1", "DF2"), new BigDecimal("60"), BigDecimal.ZERO,
                        List.of(), drinks(30, 30)));
        assertTrue(e.getMessage().contains("配送费券最多使用 1 张"));
    }

    @Test
    void shotWithinExtraShotLimit_valid() {
        stubCoupons(coupon(1L, "S1", "SHOT", "{\"value\":5}"));
        List<ItemCheckDTO> items = new ArrayList<>();
        items.add(item(1L, new BigDecimal("30"), "drink", 1, true));
        CouponCombinationResult r = service.preview(1L, List.of("S1"),
                new BigDecimal("30"), BigDecimal.ZERO, List.of(), items);
        assertEquals(0, new BigDecimal("5").compareTo(r.getAddonDiscount()));
    }

    @Test
    void shotExceedsExtraShotLimit_rejected() {
        stubCoupons(coupon(1L, "S1", "SHOT", "{\"value\":5}"),
                coupon(2L, "S2", "SHOT", "{\"value\":5}"));
        List<ItemCheckDTO> items = new ArrayList<>();
        items.add(item(1L, new BigDecimal("30"), "drink", 1, true)); // 仅 1 杯加浓缩
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.preview(1L, List.of("S1", "S2"), new BigDecimal("30"), BigDecimal.ZERO,
                        List.of(), items));
        assertTrue(e.getMessage().contains("加浓缩券数量超过订单可用数量"));
    }

    @Test
    void duplicateCode_rejected() {
        stubCoupons(coupon(1L, "D1", "DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\"}"));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.preview(1L, List.of("D1", "D1"), new BigDecimal("60"), BigDecimal.ZERO,
                        List.of(), drinks(30, 30)));
        assertTrue(e.getMessage().contains("不能重复选择"));
    }

    @Test
    void unconfiguredType_defaultsMain() {
        // BOGO 未进 categories 配置 → 默认 MAIN；两张 BOGO 视为双主券被拒
        stubCoupons(coupon(1L, "B1", "BOGO", "{\"maxDiscount\":40}"),
                coupon(2L, "B2", "BOGO", "{\"maxDiscount\":40}"));
        assertThrows(BusinessException.class,
                () -> service.preview(1L, List.of("B1", "B2"), new BigDecimal("60"), BigDecimal.ZERO,
                        List.of(), drinks(30, 30)));
    }

    @Test
    void stackingField_overridesTypeMapping() {
        // SHOT 类型本应 ADDON，但实例 stacking=MAIN → 按主券处理（不被 SHOT 上限约束）
        stubCoupons(coupon(1L, "S1", "SHOT", "{\"value\":5,\"stacking\":\"MAIN\"}"),
                coupon(2L, "S2", "SHOT", "{\"value\":5}"));
        List<ItemCheckDTO> items = new ArrayList<>();
        items.add(item(1L, new BigDecimal("30"), "drink", 1, true));
        CouponCombinationResult r = service.preview(1L, List.of("S1", "S2"),
                new BigDecimal("30"), BigDecimal.ZERO, List.of(), items);
        // S1 主券 5，S2 辅券 5；SHOT 上限只数 ADDON 的 S2 → 1 杯足够
        assertEquals(0, new BigDecimal("5").compareTo(r.getMainDiscount()));
        assertEquals(0, new BigDecimal("5").compareTo(r.getAddonDiscount()));
    }

    // ==================== 金额 ====================

    @Test
    void fullReduce_usesCouponBase() {
        stubCoupons(coupon(1L, "F1", "FULL_REDUCE", "{\"minOrderAmount\":35,\"value\":10}"));
        CouponCombinationResult r = service.preview(1L, List.of("F1"),
                new BigDecimal("40"), BigDecimal.ZERO, List.of(), drinks(20, 20));
        // 基数 40 ≥ 满35 → 减10
        assertEquals(0, new BigDecimal("10").compareTo(r.getMainDiscount()));
    }

    @Test
    void mainFreeAddon_topN() {
        // 主券 ruleJson 带 freeAddon:2，addonPrices=[10,5,3] → 免最高 2 个 = 15
        stubCoupons(coupon(1L, "P1", "EXCHANGE", "{\"maxDiscount\":40,\"freeAddon\":2}"));
        CouponCombinationResult r = service.preview(1L, List.of("P1"),
                new BigDecimal("30"), new BigDecimal("18"),
                List.of(new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("3")), drinks(30));
        assertEquals(0, new BigDecimal("15").compareTo(r.getMainDiscount()));
    }

    @Test
    void previewMatchesUse_andFreezes() {
        // preview 与 use 共用同一组合逻辑：相同输入相同折扣；use 整组冻结
        stubCoupons(coupon(1L, "D1", "DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\"}"),
                coupon(2L, "S1", "SHOT", "{\"value\":5}"),
                coupon(1L, "D1", "DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\"}"),
                coupon(2L, "S1", "SHOT", "{\"value\":5}"));
        List<ItemCheckDTO> items = new ArrayList<>();
        items.add(item(1L, new BigDecimal("30"), "drink", 1, true));
        items.add(item(2L, new BigDecimal("20"), "drink", 1, false));

        CouponCombinationResult p = service.preview(1L, List.of("D1", "S1"),
                new BigDecimal("50"), BigDecimal.ZERO, List.of(), items);
        CouponCombinationResult u = service.use(1L, List.of("D1", "S1"),
                new BigDecimal("50"), BigDecimal.ZERO, List.of(), items);

        assertEquals(0, p.getMainDiscount().compareTo(u.getMainDiscount()));
        assertEquals(0, p.getAddonDiscount().compareTo(u.getAddonDiscount()));

        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper, times(2)).updateById(captor.capture());
        for (UserCoupon c : captor.getAllValues()) {
            assertEquals("FROZEN", c.getStatus());
        }
    }

    // ==================== 工具 ====================

    private UserCoupon coupon(Long id, String code, String type, String ruleJson) {
        UserCoupon c = new UserCoupon();
        c.setId(id);
        c.setUserId(1L);
        c.setCouponCode(code);
        c.setCouponType(type);
        c.setRuleJson(ruleJson);
        c.setStatus("ISSUED");
        c.setExpiresAt(LocalDateTime.now().plusDays(7));
        return c;
    }

    private ItemCheckDTO item(long productId, BigDecimal price, String category, int qty, boolean extraShot) {
        ItemCheckDTO i = new ItemCheckDTO();
        i.setProductId(productId);
        i.setPrice(price);
        i.setCategory(category);
        i.setQuantity(qty);
        i.setCupSize("STANDARD");
        i.setIsNewProduct(false);
        if (extraShot) {
            i.setModifiersJson("{\"extraShot\":true}");
        }
        return i;
    }

    private List<ItemCheckDTO> drinks(double... prices) {
        List<ItemCheckDTO> list = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            list.add(item(i + 1L, BigDecimal.valueOf(prices[i]), "drink", 1, false));
        }
        return list;
    }

    private void stubCoupons(UserCoupon... coupons) {
        Deque<UserCoupon> queue = new ArrayDeque<>(Arrays.asList(coupons));
        when(userCouponMapper.selectOne(any())).thenAnswer(inv -> queue.poll());
    }
}
