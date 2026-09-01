package com.cozy.mall.service.impl;

import com.cozy.common.constant.CouponTemplateConfig;
import com.cozy.mall.coupon.CouponCalculator;
import com.cozy.mall.coupon.CouponCombinationService;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.mapper.MonthlyRedemptionMapper;
import com.cozy.mall.mapper.PointsOrderFulfillmentMapper;
import com.cozy.mall.mapper.PointsOrderMapper;
import com.cozy.mall.mapper.PointsProductMapper;
import com.cozy.mall.mapper.UserCouponMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 发券模板重构单测：验证 issueCouponToUser 按 CouponTemplateConfig 生成 type/ruleJson/展示文案，
 * 与旧 if-else 行为一致（含 BOGO/DISCOUNT 拦截顺序、FULL_REDUCE 兜底、动态标题）。
 */
class CouponTemplateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserCouponMapper userCouponMapper;
    private PointsMallServiceImpl service;
    private final AtomicLong seq = new AtomicLong();

    @BeforeEach
    void setUp() {
        userCouponMapper = mock(UserCouponMapper.class);
        service = new PointsMallServiceImpl(
                mock(PointsProductMapper.class), mock(PointsOrderMapper.class),
                mock(MonthlyRedemptionMapper.class), mock(PointsOrderFulfillmentMapper.class),
                userCouponMapper, mock(RedisTemplate.class), mock(StringRedisTemplate.class),
                objectMapper, new CouponTemplateConfig(), new HashMap<String, CouponCalculator>(),
                mock(CouponCombinationService.class));
    }

    private UserCoupon issue(String couponType, double minAmount, double discountAmount) {
        when(userCouponMapper.selectCount(any())).thenReturn(0L);
        service.issueCouponToUser(1L, couponType,
                "t_" + couponType + "_" + seq.incrementAndGet(), minAmount, discountAmount, 7);
        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper, atLeastOnce()).insert(captor.capture());
        List<UserCoupon> all = captor.getAllValues();
        return all.get(all.size() - 1);
    }

    private JsonNode rule(String ruleJson) throws Exception {
        return objectMapper.readTree(ruleJson);
    }

    private void assertInt(JsonNode node, String field, int expected) {
        assertTrue(node.has(field), "ruleJson 应含字段 " + field + ": " + node);
        assertEquals(expected, node.get(field).asInt(), "字段 " + field);
    }

    private void assertStr(JsonNode node, String field, String expected) {
        assertTrue(node.has(field), "ruleJson 应含字段 " + field + ": " + node);
        assertEquals(expected, node.get(field).asText(), "字段 " + field);
    }

    private void assertAbsent(JsonNode node, String field) {
        assertFalse(node.has(field), "ruleJson 不应含字段 " + field + ": " + node);
    }

    // ==================== 静态 EXCHANGE 模板 ====================

    @Test
    void staticExchangeTemplates() throws Exception {
        // 黑金月度全通兑免单券：固定 maxDiscount=40 + skuLimit ALL + 排除 SOE
        UserCoupon black = issue("MONTHLY_BLACK_FREE", 0, 40);
        assertEquals("EXCHANGE", black.getCouponType());
        JsonNode r1 = rule(black.getRuleJson());
        assertInt(r1, "maxDiscount", 40);
        assertStr(r1, "skuLimit", "ALL");
        assertEquals("SPECIALTY", r1.get("categoryBlocklist").get(0).asText());
        assertEquals("黑金月度全通兑免单券", black.getDisplayTitle());

        // 钻石月度：STANDARD_ONLY
        UserCoupon diamond = issue("MONTHLY_DIAMOND_FREE", 0, 40);
        assertStr(rule(diamond.getRuleJson()), "skuLimit", "STANDARD_ONLY");

        // 生日黑金：固定 40（不随 discountAmount）
        UserCoupon bBlack = issue("BIRTHDAY_BLACK_FREE", 0, 50);
        assertInt(rule(bBlack.getRuleJson()), "maxDiscount", 40);

        // 生日钻石：maxDiscount 取 discountAmount
        UserCoupon bDiamond = issue("BIRTHDAY_DIAMOND_FREE", 0, 50);
        assertInt(rule(bDiamond.getRuleJson()), "maxDiscount", 50);

        // 生日黄金：排除 SIGNATURE + SPECIALTY（V2 分类）
        UserCoupon bGold = issue("BIRTHDAY_GOLD_FREE", 0, 40);
        JsonNode g = rule(bGold.getRuleJson());
        assertEquals(2, g.get("categoryBlocklist").size());
        assertStr(g, "skuLimit", "STANDARD_ONLY");

        // 钻石升级礼
        UserCoupon upDiamond = issue("UPGRADE_DIAMOND_STANDARD_FREE", 0, 40);
        assertInt(rule(upDiamond.getRuleJson()), "maxDiscount", 40);

        // 标准免单券（通用）
        UserCoupon stdFree = issue("STANDARD_FREE", 0, 40);
        assertInt(rule(stdFree.getRuleJson()), "maxDiscount", 40);

        // 免费蛋糕券：CAKE_ONLY
        UserCoupon cake = issue("FREE_CAKE", 0, 40);
        JsonNode c = rule(cake.getRuleJson());
        assertStr(c, "scope", "CAKE_ONLY");
        assertInt(c, "maxDiscount", 40);

        // 黑金尊享：freeAddon=1，无 maxDiscount
        UserCoupon premium = issue("UPGRADE_BLACK_PREMIUM", 0, 0);
        JsonNode p = rule(premium.getRuleJson());
        assertInt(p, "freeAddon", 1);
        assertStr(p, "skuLimit", "ALL");
        assertAbsent(p, "maxDiscount");
    }

    // ==================== 静态 value 模板 ====================

    @Test
    void staticValueTemplates() throws Exception {
        UserCoupon shot = issue("SHOT", 0, 0);
        assertEquals("SHOT", shot.getCouponType());
        assertInt(rule(shot.getRuleJson()), "value", 5);
        assertEquals("免费加浓缩券", shot.getDisplayTitle());

        UserCoupon fee = issue("DELIVERY_FEE", 0, 0);
        assertInt(rule(fee.getRuleJson()), "value", 3);

        UserCoupon half = issue("NEW_PRODUCT_HALF", 0, 0);
        assertInt(rule(half.getRuleJson()), "maxDiscount", 20);

        UserCoupon free = issue("NEW_PRODUCT_FREE", 0, 0);
        assertInt(rule(free.getRuleJson()), "maxDiscount", 40);

        UserCoupon cakeHalf = issue("CAKE_HALF", 0, 0);
        JsonNode ch = rule(cakeHalf.getRuleJson());
        assertInt(ch, "value", 50);
        assertStr(ch, "scope", "CAKE_ONLY");

        UserCoupon silver = issue("UPGRADE_SILVER_DISCOUNT", 0, 0);
        JsonNode sv = rule(silver.getRuleJson());
        assertInt(sv, "value", 50);
        assertInt(sv, "maxDiscountAmount", 20);
        assertStr(sv, "limit", "SINGLE_ITEM");
        assertStr(sv, "scope", "DRINK_ONLY");

        UserCoupon birthdayDiscount = issue("BIRTHDAY_BASIC_DISCOUNT", 0, 0);
        JsonNode bd = rule(birthdayDiscount.getRuleJson());
        assertEquals(0.5, bd.get("discountRate").asDouble(), 0.001);
        assertInt(bd, "maxDiscountAmount", 20);
        assertEquals("🎂基础会员生日5折券", birthdayDiscount.getDisplayTitle());
    }

    // ==================== BOGO 与拦截顺序 ====================

    @Test
    void bogoAndInterceptOrder() throws Exception {
        // 普通 BOGO：maxDiscount 取 discountAmount，标题"买一赠一券"
        UserCoupon bogo = issue("BOGO", 0, 40);
        assertEquals("BOGO", bogo.getCouponType());
        assertInt(rule(bogo.getRuleJson()), "maxDiscount", 40);
        assertEquals("买一赠一券", bogo.getDisplayTitle());

        // BIRTHDAY_SILVER_BOGO 含 BOGO 子串，被 BOGO 模板拦截（与旧 if-else 一致）→ 无 scope
        UserCoupon bBogo = issue("BIRTHDAY_SILVER_BOGO", 0, 40);
        JsonNode bb = rule(bBogo.getRuleJson());
        assertInt(bb, "maxDiscount", 40);
        assertAbsent(bb, "scope");
        assertEquals("生日买一赠一券", bBogo.getDisplayTitle());

        // UPGRADE_GOLD_BOGO 同样被 BOGO 拦截 → 无 scope
        UserCoupon upBogo = issue("UPGRADE_GOLD_BOGO", 0, 40);
        assertAbsent(rule(upBogo.getRuleJson()), "scope");
    }

    // ==================== FREE_DRINK 生日排除 ====================

    @Test
    void freeDrinkBirthdayExclusion() throws Exception {
        UserCoupon normal = issue("FREE_DRINK", 0, 40);
        assertEquals("EXCHANGE", normal.getCouponType());
        JsonNode n = rule(normal.getRuleJson());
        assertInt(n, "maxDiscount", 40);
        assertAbsent(n, "categoryBlocklist");
        assertEquals("全场饮品通兑券", normal.getDisplayTitle());
        assertEquals("任选饮品 | 封顶¥40", normal.getDisplaySubTitle());

        UserCoupon birthday = issue("BIRTHDAY_FREE_DRINK", 0, 40);
        JsonNode b = rule(birthday.getRuleJson());
        assertEquals("SPECIALTY", b.get("categoryBlocklist").get(0).asText());
        assertEquals("生日免单券", birthday.getDisplayTitle());
        assertEquals("排除精品咖啡 | 封顶¥40", birthday.getDisplaySubTitle());
    }

    // ==================== EXCHANGE_ 关联商品 ====================

    @Test
    void exchangeFromCode() throws Exception {
        UserCoupon coupon = issue("EXCHANGE_123", 0, 0);
        assertEquals("EXCHANGE", coupon.getCouponType());
        JsonNode r = rule(coupon.getRuleJson());
        assertInt(r, "linkedProductId", 123);
        // 无 orderService 时商品名兜底
        assertEquals("商品兑换券", coupon.getDisplayTitle());
        assertEquals("限标准杯，升杯加料需补差价", coupon.getDisplaySubTitle());
    }

    // ==================== 通用折扣券 rate 计算 ====================

    @Test
    void genericDiscountRate() throws Exception {
        // DISCOUNT_SINGLE：rate=7 → 7折
        UserCoupon single = issue("DISCOUNT_SINGLE", 0, 7);
        assertEquals("DISCOUNT", single.getCouponType());
        JsonNode s = rule(single.getRuleJson());
        assertInt(s, "discountRate", 7);
        assertStr(s, "limit", "SINGLE_ITEM");
        assertEquals("7折券", single.getDisplayTitle());
        assertEquals("限单件商品", single.getDisplaySubTitle());

        // HALF_PRICE：强制 0.5 → 5折
        UserCoupon half = issue("HALF_PRICE", 0, 0);
        assertEquals(0.5, rule(half.getRuleJson()).get("discountRate").asDouble(), 0.001);
        assertEquals("5折券", half.getDisplayTitle());

        // 普通折扣：rate=8.8（ruleJson 存折数）→ 8.8折
        UserCoupon normal = issue("DISCOUNT_88", 0, 8.8);
        assertEquals(8.8, rule(normal.getRuleJson()).get("discountRate").asDouble(), 0.001);
        assertEquals("8.8折券", normal.getDisplayTitle());
        assertEquals("全场饮品", normal.getDisplaySubTitle());
    }

    // ==================== FULL_REDUCE 兜底 ====================

    @Test
    void fullReduceFallback() throws Exception {
        UserCoupon coupon = issue("FULL_REDUCE", 35, 10);
        assertEquals("FULL_REDUCE", coupon.getCouponType());
        JsonNode r = rule(coupon.getRuleJson());
        assertInt(r, "minOrderAmount", 35);
        assertInt(r, "value", 10);
        assertEquals("满35减10", coupon.getDisplayTitle());
        assertEquals("满35可用", coupon.getDisplaySubTitle());

        UserCoupon noThreshold = issue("FULL_REDUCE", 0, 5);
        assertEquals("5元代金券", noThreshold.getDisplayTitle());
        assertEquals("无门槛", noThreshold.getDisplaySubTitle());
    }
}
