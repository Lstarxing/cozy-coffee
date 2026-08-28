package com.cozy.order.service.product;

import com.cozy.order.entity.CoffeeProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品统一定价核心（P1E）
 * preview / create 两条下单链路共享：规格校验 → 基础价（按 size_type）→ 加料权威价 → 最终价 + 成交快照。
 * 基础价只读商品价格列（DEFAULT→price / MEDIUM_LARGE→price_medium·price_large），
 * 禁止前端传价、禁止硬编码大杯加价。
 */
@Service
@RequiredArgsConstructor
public class ProductPricingService {

    private final ProductRuleValidator ruleValidator;
    private final ProductAddonResolver addonResolver;

    public PriceResult price(CoffeeProduct product, String size, String temp, String sugar, String addonsJson) {
        String specError = ruleValidator.validateSpecs(product, size, sugar, temp);
        if (specError != null) {
            return PriceResult.invalid(specError);
        }
        BigDecimal base = resolveBasePrice(product, size);
        ProductAddonResolver.AddonResolution addon = addonResolver.resolve(product.getId(), addonsJson);
        if (!addon.valid()) {
            return PriceResult.invalid(addon.error());
        }
        return new PriceResult(true, null, base, addon.fee(), base.add(addon.fee()),
                addon.normalizedJson(), addon.addonPrices());
    }

    private BigDecimal resolveBasePrice(CoffeeProduct product, String size) {
        if ("MEDIUM_LARGE".equals(product.getSizeType())) {
            return "LARGE".equalsIgnoreCase(size) ? product.getPriceLarge() : product.getPriceMedium();
        }
        return product.getPrice(); // DEFAULT（含烘焙单份）
    }

    public record PriceResult(boolean valid, String error,
            BigDecimal basePrice, BigDecimal addonFee, BigDecimal finalPrice,
            String normalizedAddonsJson, List<BigDecimal> addonPrices) {
        static PriceResult invalid(String error) {
            return new PriceResult(false, error, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "[]", List.of());
        }
    }
}
