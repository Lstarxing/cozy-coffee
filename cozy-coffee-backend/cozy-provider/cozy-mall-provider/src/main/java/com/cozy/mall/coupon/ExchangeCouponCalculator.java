package com.cozy.mall.coupon;

import com.cozy.common.exception.BusinessException;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.util.CouponRuleUtil;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.CoffeeProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 兑换券/免单券策略：支持 SKU 限制、品类黑名单、指定商品/通兑。
 * rule_json: maxDiscount(封顶，缺省无上限) / value / skuLimit / categoryBlocklist / linkedProductId / scope(CAKE_ONLY)
 */
@Slf4j
@Component("EXCHANGE")
@RequiredArgsConstructor
public class ExchangeCouponCalculator implements CouponCalculator {

    @DubboReference(check = false)
    private OrderService orderService;

    @Override
    public BigDecimal calculate(UserCoupon coupon, BigDecimal orderAmount, List<ItemCheckDTO> items) {
        String ruleJson = coupon.getRuleJson();
        long linkedProductId = CouponRuleUtil.parseLongValue(ruleJson, "linkedProductId");

        // 封顶：优先 maxDiscount，其次 value，都没有则无上限（9999）
        int maxDiscountFromRule = CouponRuleUtil.parseValue(ruleJson, "maxDiscount");
        if (maxDiscountFromRule <= 0) {
            maxDiscountFromRule = CouponRuleUtil.parseValue(ruleJson, "value");
        }
        BigDecimal maxDiscount = maxDiscountFromRule > 0 ? new BigDecimal(maxDiscountFromRule) : new BigDecimal("9999");

        // SKU 限制（去空格精确匹配）
        String cleanJson = ruleJson != null ? ruleJson.replace(" ", "").replace("\n", "").replace("\t", "") : "";
        boolean standardOnly = cleanJson.contains("\"skuLimit\":\"STANDARD_ONLY\"");

        // 品类黑名单（精确匹配数组内容，避免被 description 描述文字误伤）
        boolean blockSoe = false;
        boolean blockSignature = false;
        if (cleanJson.contains("\"categoryBlocklist\"")) {
            int startIdx = cleanJson.indexOf("\"categoryBlocklist\"");
            int arrayStart = cleanJson.indexOf("[", startIdx);
            int arrayEnd = cleanJson.indexOf("]", arrayStart);
            if (arrayStart > 0 && arrayEnd > arrayStart) {
                String blocklistPart = cleanJson.substring(arrayStart, arrayEnd + 1).toLowerCase();
                blockSoe = blocklistPart.contains("\"soe\"") || blocklistPart.contains("\"pour-over\"");
                blockSignature = blocklistPart.contains("\"signature\"");
                log.info("券品类限制解析: blockSoe={}, blockSignature={}, blocklist={}", blockSoe, blockSignature, blocklistPart);
            }
        } else {
            log.info("券无品类限制: ruleJson={}", cleanJson.substring(0, Math.min(200, cleanJson.length())));
        }

        if (linkedProductId > 0) {
            return applyLinkedProduct(coupon, items, linkedProductId, maxDiscount);
        } else {
            return applyGeneral(coupon, items, maxDiscount, standardOnly, blockSoe, blockSignature, cleanJson);
        }
    }

    /** 指定商品兑换券：仅限标准杯，只抵扣标准杯基础价 */
    private BigDecimal applyLinkedProduct(UserCoupon coupon, List<ItemCheckDTO> items,
            long linkedProductId, BigDecimal maxDiscount) {
        if (items != null) {
            for (ItemCheckDTO item : items) {
                if (item.getProductId() != null && item.getProductId() == linkedProductId) {
                    String cupSize = item.getCupSize() != null ? item.getCupSize().toUpperCase() : "STANDARD";
                    if (!cupSize.equals("STANDARD") && !cupSize.equals("MEDIUM")) {
                        throw new BusinessException("此兑换券仅限标准杯使用，请调整杯型后再试");
                    }
                    try {
                        CoffeeProductDTO product = orderService.getProduct(linkedProductId);
                        if (product != null && product.getPrice() != null) {
                            log.info("指定商品兑换券：productId={}, 标准杯价格={}, 实际商品价格={}",
                                    linkedProductId, product.getPrice(), item.getPrice());
                            return product.getPrice().min(maxDiscount);
                        }
                    } catch (Exception e) {
                        log.warn("查询商品标准价格失败，回退到使用商品实际价格: productId={}", linkedProductId, e);
                    }
                    return item.getPrice().min(maxDiscount);
                }
            }
            throw new BusinessException("此兑换券仅限指定商品使用");
        }
        return BigDecimal.ZERO;
    }

    /** 通兑券：自动匹配价格最高的符合条件商品 */
    private BigDecimal applyGeneral(UserCoupon coupon, List<ItemCheckDTO> items, BigDecimal maxDiscount,
            boolean standardOnly, boolean blockSoe, boolean blockSignature, String cleanJson) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxPrice = BigDecimal.ZERO;
        String matchedProductInfo = null;

        String couponName = (coupon.getDisplayTitle() != null ? coupon.getDisplayTitle() : "").toLowerCase();
        boolean isCakeCoupon = cleanJson.contains("\"scope\":\"CAKE_ONLY\"")
                || couponName.contains("烘培") || couponName.contains("烘焙")
                || couponName.contains("甜品") || couponName.contains("蛋糕");

        for (ItemCheckDTO item : items) {
            if (isCakeCoupon) {
                if (!CouponRuleUtil.isBakery(item.getCategory())) {
                    continue;
                }
            } else {
                if (!CouponRuleUtil.isDrink(item.getCategory())) {
                    continue;
                }
            }

            if (item.getCategory() != null) {
                String cat = item.getCategory().toLowerCase();
                if (blockSoe) {
                    boolean isSoe = cat.contains("soe") || cat.contains("手冲") || cat.contains("pour-over") || cat.contains("pour_over");
                    if (isSoe) {
                        log.info("免单券排除SOE/手冲品类: category={}", cat);
                        continue;
                    }
                }
                if (blockSignature && (cat.contains("signature") || cat.contains("特调") || cat.contains("季节限定"))) {
                    log.info("免单券排除特调品类: category={}", cat);
                    continue;
                }
            }

            if (standardOnly && item.getCupSize() != null) {
                String cupSize = item.getCupSize().toUpperCase();
                if (!cupSize.equals("STANDARD") && !cupSize.equals("MEDIUM")) {
                    log.info("免单券仅限标准杯，跳过: cupSize={}", cupSize);
                    continue;
                }
            }

            if (item.getPrice().compareTo(maxPrice) > 0) {
                maxPrice = item.getPrice();
                matchedProductInfo = "productId=" + item.getProductId() + ", cupSize=" + item.getCupSize();
            }
        }

        if (maxPrice.equals(BigDecimal.ZERO)) {
            if (standardOnly) {
                throw new BusinessException("此免单券仅限标准杯饮品使用，请调整杯型后再试");
            }
            if (blockSoe) {
                throw new BusinessException("此免单券不适用于SOE/手冲类产品");
            }
            if (isCakeCoupon) {
                throw new BusinessException("此券仅限烘焙甜品使用");
            }
            throw new BusinessException("通兑券仅限饮品使用");
        }

        log.info("免单券匹配最高价商品: {}, maxPrice={}, discount={}, isCakeCoupon={}",
                matchedProductInfo, maxPrice, maxPrice.min(maxDiscount), isCakeCoupon);
        return maxPrice.min(maxDiscount);
    }
}
