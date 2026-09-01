package com.cozy.mall.coupon;

import com.cozy.common.exception.BusinessException;
import com.cozy.common.constant.CouponTemplateConfig;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.entity.UserCoupon;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 用券策略单测：9 类券的抵扣计算（行为与旧 calculateCouponDiscount 分支一致）。
 */
class CouponCalculatorTest {

    private UserCoupon coupon(String type, String ruleJson) {
        UserCoupon c = new UserCoupon();
        c.setCouponType(type);
        c.setRuleJson(ruleJson);
        return c;
    }

    private ItemCheckDTO item(Long id, BigDecimal price, String category, Integer qty, String cupSize, Boolean isNew) {
        ItemCheckDTO i = new ItemCheckDTO();
        i.setProductId(id);
        i.setPrice(price);
        i.setCategory(category);
        i.setQuantity(qty);
        i.setCupSize(cupSize);
        i.setIsNewProduct(isNew);
        return i;
    }

    private ItemCheckDTO drink(long id, BigDecimal price) {
        return item(id, price, "drink", 1, "STANDARD", false);
    }

    // ==================== EXCHANGE 通兑 ====================

    @Test
    void exchangeGeneral() {
        ExchangeCouponCalculator calc = new ExchangeCouponCalculator();
        // 选最高价饮品，封顶 40
        BigDecimal d = calc.calculate(coupon("EXCHANGE", "{\"maxDiscount\":40}"),
                BigDecimal.ZERO, List.of(drink(1, new BigDecimal("20")), drink(2, new BigDecimal("30"))));
        assertEquals(0, new BigDecimal("30").compareTo(d));
        // 封顶 25
        BigDecimal d2 = calc.calculate(coupon("EXCHANGE", "{\"maxDiscount\":25}"),
                BigDecimal.ZERO, List.of(drink(1, new BigDecimal("30"))));
        assertEquals(0, new BigDecimal("25").compareTo(d2));
        // 无封顶时用极大值
        BigDecimal d3 = calc.calculate(coupon("EXCHANGE", "{}"),
                BigDecimal.ZERO, List.of(drink(1, new BigDecimal("30"))));
        assertEquals(0, new BigDecimal("30").compareTo(d3));
    }

    @Test
    void exchangeBlockSoeAndStandardOnly() {
        ExchangeCouponCalculator calc = new ExchangeCouponCalculator();
        // 排除 SOE（V2 分类 = SPECIALTY）
        BigDecimal d = calc.calculate(coupon("EXCHANGE",
                        "{\"maxDiscount\":40,\"categoryBlocklist\":[\"soe\",\"pour-over\"]}"),
                BigDecimal.ZERO,
                List.of(item(1L, new BigDecimal("30"), "SPECIALTY", 1, "STANDARD", false),
                        drink(2, new BigDecimal("20"))));
        assertEquals(0, new BigDecimal("20").compareTo(d));

        // 仅标准杯：大杯跳过
        BigDecimal d2 = calc.calculate(coupon("EXCHANGE",
                        "{\"maxDiscount\":40,\"skuLimit\":\"STANDARD_ONLY\"}"),
                BigDecimal.ZERO,
                List.of(item(1L, new BigDecimal("30"), "drink", 1, "LARGE", false),
                        drink(2, new BigDecimal("20"))));
        assertEquals(0, new BigDecimal("20").compareTo(d2));
    }

    @Test
    void exchangeCakeCoupon() {
        ExchangeCouponCalculator calc = new ExchangeCouponCalculator();
        // 蛋糕券只匹配烘焙，封顶 40
        BigDecimal d = calc.calculate(coupon("EXCHANGE", "{\"scope\":\"CAKE_ONLY\",\"maxDiscount\":40}"),
                BigDecimal.ZERO,
                List.of(drink(1, new BigDecimal("30")),
                        item(2L, new BigDecimal("50"), "cake", 1, "STANDARD", false)));
        assertEquals(0, new BigDecimal("40").compareTo(d));
    }

    @Test
    void exchangeLinkedProductFallback() {
        ExchangeCouponCalculator calc = new ExchangeCouponCalculator();
        // 指定商品券：orderService 为空时回退到商品实际价
        BigDecimal d = calc.calculate(coupon("EXCHANGE", "{\"linkedProductId\":123}"),
                BigDecimal.ZERO, List.of(item(123L, new BigDecimal("28"), "drink", 1, "MEDIUM", false)));
        assertEquals(0, new BigDecimal("28").compareTo(d));
    }

    // ==================== DISCOUNT ====================

    @Test
    void discountDrinkOnlyAndSingleItem() {
        DiscountCouponCalculator calc = new DiscountCouponCalculator();
        // DRINK_ONLY 5 折作用于饮品总额
        BigDecimal d = calc.calculate(coupon("DISCOUNT", "{\"value\":50,\"scope\":\"DRINK_ONLY\"}"),
                BigDecimal.ZERO, List.of(drink(1, new BigDecimal("30")), drink(2, new BigDecimal("30"))));
        assertEquals(0, new BigDecimal("30").compareTo(d)); // 60 * 0.5

        // SINGLE_ITEM 作用于最贵单杯
        BigDecimal d2 = calc.calculate(coupon("DISCOUNT", "{\"value\":50,\"limit\":\"SINGLE_ITEM\"}"),
                BigDecimal.ZERO, List.of(drink(1, new BigDecimal("20")), drink(2, new BigDecimal("50"))));
        assertEquals(0, new BigDecimal("25").compareTo(d2)); // 50 * 0.5
    }

    @Test
    void discountRateAndCap() {
        DiscountCouponCalculator calc = new DiscountCouponCalculator();
        // discountRate 0.5 = 5 折
        BigDecimal d = calc.calculate(coupon("DISCOUNT", "{\"discountRate\":0.5,\"scope\":\"DRINK_ONLY\"}"),
                BigDecimal.ZERO, List.of(drink(1, new BigDecimal("60"))));
        assertEquals(0, new BigDecimal("30").compareTo(d));

        // 封顶 20
        BigDecimal d2 = calc.calculate(coupon("DISCOUNT",
                        "{\"value\":50,\"scope\":\"DRINK_ONLY\",\"maxDiscountAmount\":20}"),
                BigDecimal.ZERO, List.of(drink(1, new BigDecimal("100"))));
        assertEquals(0, new BigDecimal("20").compareTo(d2));
    }

    @Test
    void discountCakeOnly() {
        DiscountCouponCalculator calc = new DiscountCouponCalculator();
        BigDecimal d = calc.calculate(coupon("DISCOUNT", "{\"value\":50,\"scope\":\"CAKE_ONLY\"}"),
                BigDecimal.ZERO, List.of(drink(1, new BigDecimal("30")),
                        item(2L, new BigDecimal("40"), "cake", 1, "STANDARD", false)));
        assertEquals(0, new BigDecimal("20").compareTo(d)); // 40 * 0.5
    }

    // ==================== 其余类型 ====================

    @Test
    void fullReduce() {
        FullReduceCouponCalculator calc = new FullReduceCouponCalculator();
        assertEquals(0, new BigDecimal("10").compareTo(
                calc.calculate(coupon("FULL_REDUCE", "{\"minOrderAmount\":35,\"value\":10}"),
                        new BigDecimal("100"), null)));
        assertThrows(BusinessException.class, () ->
                calc.calculate(coupon("FULL_REDUCE", "{\"minOrderAmount\":35,\"value\":10}"),
                        new BigDecimal("30"), null));
    }

    @Test
    void bogo() {
        BogoCouponCalculator calc = new BogoCouponCalculator();
        // 最低价免单，封顶 40
        BigDecimal d = calc.calculate(coupon("BOGO", "{\"maxDiscount\":40}"),
                BigDecimal.ZERO, List.of(drink(1, new BigDecimal("15")), drink(2, new BigDecimal("30"))));
        assertEquals(0, new BigDecimal("15").compareTo(d));
        // 封顶 10
        BigDecimal d2 = calc.calculate(coupon("BOGO", "{\"maxDiscount\":10}"),
                BigDecimal.ZERO, List.of(drink(1, new BigDecimal("15")), drink(2, new BigDecimal("30"))));
        assertEquals(0, new BigDecimal("10").compareTo(d2));
    }

    @Test
    void shot() {
        ShotCouponCalculator calc = new ShotCouponCalculator();
        ItemCheckDTO withShot = drink(1, new BigDecimal("10"));
        withShot.setModifiersJson("{\"extraShot\":true}");
        assertEquals(0, new BigDecimal("5").compareTo(
                calc.calculate(coupon("SHOT", "{\"value\":5}"), BigDecimal.ZERO, List.of(withShot))));
        assertThrows(BusinessException.class, () ->
                calc.calculate(coupon("SHOT", "{\"value\":5}"), BigDecimal.ZERO, List.of(drink(1, new BigDecimal("10")))));
    }

    @Test
    void deliveryFee() {
        DeliveryFeeCouponCalculator calc = new DeliveryFeeCouponCalculator();
        assertEquals(0, new BigDecimal("3").compareTo(
                calc.calculate(coupon("DELIVERY_FEE", "{\"value\":3}"), BigDecimal.ZERO, null)));
        assertEquals(0, new BigDecimal("3").compareTo(
                calc.calculate(coupon("DELIVERY_FEE", "{}"), BigDecimal.ZERO, null)));
    }

    @Test
    void newProductHalfAndFree() {
        NewProductHalfCouponCalculator half = new NewProductHalfCouponCalculator();
        NewProductFreeCouponCalculator free = new NewProductFreeCouponCalculator();
        ItemCheckDTO newItem = item(1L, new BigDecimal("50"), "drink", 1, "STANDARD", true);

        // 半价封顶 20
        assertEquals(0, new BigDecimal("20").compareTo(
                half.calculate(coupon("NEW_PRODUCT_HALF", "{}"), BigDecimal.ZERO, List.of(newItem))));
        // 免单封顶 40
        assertEquals(0, new BigDecimal("40").compareTo(
                free.calculate(coupon("NEW_PRODUCT_FREE", "{}"), BigDecimal.ZERO, List.of(newItem))));
    }

    @Test
    void cakeHalf() {
        CakeHalfCouponCalculator calc = new CakeHalfCouponCalculator();
        // 5 折封顶 50
        ItemCheckDTO cake = item(1L, new BigDecimal("120"), "cake", 1, "STANDARD", false);
        assertEquals(0, new BigDecimal("50").compareTo(
                calc.calculate(coupon("CAKE_HALF", "{}"), BigDecimal.ZERO, List.of(cake))));
        ItemCheckDTO small = item(2L, new BigDecimal("30"), "cake", 1, "STANDARD", false);
        assertEquals(0, new BigDecimal("15").compareTo(
                calc.calculate(coupon("CAKE_HALF", "{}"), BigDecimal.ZERO, List.of(small))));
    }

    // ==================== 真实发券模板配置 → 抵扣（闭环） ====================

    @Test
    void monthlyDiamondFree_fromCouponTemplateConfig() {
        // MONTHLY_DIAMOND_FREE 模板（cozy.mall.coupon-template）→ ruleJson → 免单抵扣
        CouponTemplateConfig config = new CouponTemplateConfig();
        CouponTemplateConfig.CouponTemplate t = config.match("MONTHLY_DIAMOND_FREE");
        String blocklist = t.getCategoryBlocklist().stream()
                .map(s -> "\"" + s + "\"").collect(Collectors.joining(","));
        String ruleJson = String.format("{\"maxDiscount\":%d,\"skuLimit\":\"%s\",\"categoryBlocklist\":[%s]}",
                t.getMaxDiscount(), t.getSkuLimit(), blocklist);

        BigDecimal d = new ExchangeCouponCalculator().calculate(
                coupon("EXCHANGE", ruleJson), BigDecimal.ZERO,
                List.of(drink(1, new BigDecimal("30")), drink(2, new BigDecimal("20"))));
        // 选最高价饮品 30，封顶 40 → 抵扣 30
        assertEquals(0, new BigDecimal("30").compareTo(d));
    }

    @Test
    void bogo_fromCouponTemplateConfig() {
        // BOGO 模板：useDiscountAmountAsMaxDiscount → 发券时 discountAmount=40 写入 maxDiscount → 免最低价
        CouponTemplateConfig config = new CouponTemplateConfig();
        CouponTemplateConfig.CouponTemplate t = config.match("BOGO");
        String ruleJson = "{\"maxDiscount\":40}";

        BigDecimal d = new BogoCouponCalculator().calculate(
                coupon("BOGO", ruleJson), BigDecimal.ZERO,
                List.of(drink(1, new BigDecimal("15")), drink(2, new BigDecimal("30"))));
        // 低价免单 → 抵扣 15
        assertEquals(0, new BigDecimal("15").compareTo(d));
    }
}
