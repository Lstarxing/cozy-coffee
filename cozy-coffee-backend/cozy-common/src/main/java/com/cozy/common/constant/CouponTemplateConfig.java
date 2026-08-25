package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 优惠券发券模板配置（单一事实源，@ConfigurationProperties + application.yml）。
 * 前缀 cozy.mall.coupon-template.templates，键 = 匹配前缀（沿用旧 if-else 的 contains 语义）。
 * 供 PointsMallServiceImpl.issueCouponToUser 使用：模板驱动生成 ruleJson/type/展示文案，
 * 取代 22+ 个硬编码 if-else 分支，新券只需加配置。
 *
 * 匹配顺序 = 列表顺序（必须与旧 if-else 分支顺序一致，避免行为漂移）；
 * displayTitle 缺省表示动态生成（BOGO 生日 / EXCHANGE_ 商品名 / FREE_DRINK / 折扣券折数）；
 * displaySubTitle 支持 {discountAmount}/{minAmount} 占位替换。
 * 默认值即生产当前值，yml 配置会整体覆盖 templates。
 */
@Data
@ConfigurationProperties(prefix = "cozy.mall.coupon-template")
public class CouponTemplateConfig {

    private List<CouponTemplate> templates = defaultTemplates();

    /** 按前缀匹配（列表顺序，第一个命中即返回）；未命中返回 null（走 FULL_REDUCE 兜底） */
    public CouponTemplate match(String couponType) {
        if (couponType == null || templates == null) {
            return null;
        }
        for (CouponTemplate t : templates) {
            if (t.getTemplateCode() != null && couponType.contains(t.getTemplateCode())) {
                return t;
            }
        }
        return null;
    }

    @Data
    public static class CouponTemplate {
        /** 匹配前缀（couponType.contains 命中） */
        private String templateCode;
        /** 算法类型：BOGO/DISCOUNT/EXCHANGE/FULL_REDUCE/SHOT/DELIVERY_FEE/NEW_PRODUCT_HALF/NEW_PRODUCT_FREE */
        private String type;
        /** 固定 value（SHOT/DELIVERY_FEE/CAKE_HALF 等） */
        private Integer value;
        /** 固定 maxDiscount（封顶金额） */
        private Integer maxDiscount;
        /** 为 true 时 maxDiscount 取入参 discountAmount（覆盖固定值） */
        private Boolean useDiscountAmountAsMaxDiscount;
        /** 固定折扣率（如 0.5 = 5 折） */
        private Double discountRate;
        /** DISCOUNT 券封顶金额 */
        private Integer maxDiscountAmount;
        /** 品类范围：DRINK_ONLY / CAKE_ONLY */
        private String scope;
        /** 杯型限制：STANDARD_ONLY / ALL */
        private String skuLimit;
        /** 数量限制：SINGLE_ITEM */
        private String limit;
        /** 排除类目（如 soe/pour-over/signature） */
        private List<String> categoryBlocklist = new ArrayList<>();
        /** 附加项：freeAddon=1 */
        private Boolean freeAddon;
        /** 叠加类别：MAIN / ADDON / EXCLUSIVE；缺省按类型映射（CouponStackingConfig）或 MAIN 处理 */
        private String stacking;
        /** 为 true 时商品 ID 从 couponType 尾部解析（EXCHANGE_123） */
        private Boolean linkedProductFromCode;
        /** 展示标题；缺省表示代码动态生成 */
        private String displayTitle;
        /** 展示副标题；支持 {discountAmount}/{minAmount} 占位 */
        private String displaySubTitle;
    }

    private static List<CouponTemplate> defaultTemplates() {
        List<CouponTemplate> list = new ArrayList<>();
        list.add(t("BOGO", "BOGO", null, null, true, null, null, null, null, null, null, null, false, null, "买一送一 | 封顶¥{discountAmount}"));
        list.add(t("SHOT", "SHOT", 5, null, null, null, null, null, null, null, null, null, false, "免费加浓缩券", "抵扣¥5"));
        list.add(t("DELIVERY_FEE", "DELIVERY_FEE", 3, null, null, null, null, null, null, null, null, null, false, "配送费抵扣券", "免运费"));
        list.add(t("NEW_PRODUCT_HALF", "NEW_PRODUCT_HALF", null, 20, null, null, null, null, null, null, null, null, false, "新品5折券", "封顶¥20"));
        list.add(t("NEW_PRODUCT_FREE", "NEW_PRODUCT_FREE", null, 40, null, null, null, null, null, null, null, null, false, "新品免单券", "封顶¥40"));
        list.add(t("FREE_DRINK", "EXCHANGE", null, null, true, null, null, null, null, null, null, null, false, null, null));
        list.add(t("EXCHANGE_", "EXCHANGE", null, null, null, null, null, null, null, null, null, null, true, null, "限标准杯，升杯加料需补差价"));
        list.add(t("FREE_CAKE", "EXCHANGE", null, null, true, null, null, "CAKE_ONLY", null, null, null, null, false, "烘培甜品免单券", "封顶¥{discountAmount}"));
        list.add(t("CAKE_HALF", "DISCOUNT", 50, null, null, null, null, "CAKE_ONLY", null, null, null, null, false, "烘培甜品5折券", "限烘焙甜品"));
        list.add(t("UPGRADE_SILVER_DISCOUNT", "DISCOUNT", 50, null, null, null, 20, "DRINK_ONLY", null, "SINGLE_ITEM", null, null, false, "晋升白银5折券", "限饮品 | 封顶¥20"));
        list.add(t("UPGRADE_GOLD_BOGO", "BOGO", null, null, true, null, null, "DRINK_ONLY", null, null, null, null, false, "晋升黄金买一赠一券", "封顶¥{discountAmount}"));
        list.add(t("UPGRADE_DIAMOND_STANDARD_FREE", "EXCHANGE", null, null, true, null, null, null, "STANDARD_ONLY", null, Arrays.asList("soe", "pour-over"), null, false, "晋升钻石优选饮品免单券", "限标准杯 | 封顶¥{discountAmount}"));
        list.add(t("UPGRADE_BLACK_PREMIUM", "EXCHANGE", null, null, null, null, null, null, "ALL", null, null, true, false, "黑金尊享通兑券", "不限杯型 | 含SOE | 无封顶"));
        list.add(t("MONTHLY_BLACK_FREE", "EXCHANGE", null, 40, null, null, null, null, "ALL", null, Arrays.asList("soe", "pour-over"), null, false, "黑金月度全通兑免单券", "不限杯型 | 封顶¥40"));
        list.add(t("MONTHLY_DIAMOND_FREE", "EXCHANGE", null, 40, null, null, null, null, "STANDARD_ONLY", null, Arrays.asList("soe", "pour-over"), null, false, "钻石月度优选饮品免单券", "限标准杯 | 封顶¥40"));
        list.add(t("BIRTHDAY_BLACK_FREE", "EXCHANGE", null, 40, null, null, null, null, "ALL", null, Arrays.asList("soe", "pour-over"), null, false, "🎂黑金生日全通兑免单券", "不限杯型 | 封顶¥40"));
        list.add(t("BIRTHDAY_DIAMOND_FREE", "EXCHANGE", null, null, true, null, null, null, "STANDARD_ONLY", null, Arrays.asList("soe", "pour-over"), null, false, "🎂钻石生日优选饮品免单券", "限标准杯 | 封顶¥{discountAmount}"));
        list.add(t("BIRTHDAY_GOLD_FREE", "EXCHANGE", null, null, true, null, null, null, "STANDARD_ONLY", null, Arrays.asList("signature", "soe", "pour-over"), null, false, "🎂黄金生日标准饮品免单券", "限标准杯/不含特调、SOE"));
        list.add(t("BIRTHDAY_SILVER_BOGO", "BOGO", null, 40, null, null, null, "DRINK_ONLY", null, null, null, null, false, "🎂白银生日买一赠一券", "封顶¥40"));
        list.add(t("BIRTHDAY_BASIC_DISCOUNT", "DISCOUNT", null, null, null, 0.5, 20, "DRINK_ONLY", "STANDARD_ONLY", "SINGLE_ITEM", null, null, false, "🎂基础会员生日5折券", "限标准杯 | 封顶¥20"));
        list.add(t("STANDARD_FREE", "EXCHANGE", null, null, true, null, null, null, "STANDARD_ONLY", null, Arrays.asList("soe", "pour-over"), null, false, "标准饮品免单券", "封顶¥{discountAmount}"));
        // 通用折扣券（catch-all，rate 计算与折数标题由代码动态生成）
        list.add(t("HALF_PRICE", "DISCOUNT", null, null, null, null, null, null, null, null, null, null, false, null, null));
        list.add(t("DISCOUNT", "DISCOUNT", null, null, null, null, null, null, null, null, null, null, false, null, null));
        return list;
    }

    private static CouponTemplate t(String code, String type, Integer value, Integer maxDiscount,
            Boolean useDiscountAmountAsMax, Double discountRate, Integer maxDiscountAmount,
            String scope, String skuLimit, String limit, List<String> blocklist,
            Boolean freeAddon, Boolean linkedFromCode, String title, String subTitle) {
        CouponTemplate c = new CouponTemplate();
        c.setTemplateCode(code);
        c.setType(type);
        c.setValue(value);
        c.setMaxDiscount(maxDiscount);
        c.setUseDiscountAmountAsMaxDiscount(useDiscountAmountAsMax);
        c.setDiscountRate(discountRate);
        c.setMaxDiscountAmount(maxDiscountAmount);
        c.setScope(scope);
        c.setSkuLimit(skuLimit);
        c.setLimit(limit);
        c.setCategoryBlocklist(blocklist != null ? new ArrayList<>(blocklist) : new ArrayList<>());
        c.setFreeAddon(freeAddon);
        c.setLinkedProductFromCode(linkedFromCode);
        c.setDisplayTitle(title);
        c.setDisplaySubTitle(subTitle);
        return c;
    }
}
