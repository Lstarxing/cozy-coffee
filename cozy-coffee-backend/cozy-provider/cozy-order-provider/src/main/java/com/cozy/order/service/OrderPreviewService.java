package com.cozy.order.service;

import com.cozy.common.exception.BusinessErrorCode;
import com.cozy.common.exception.BusinessException;
import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.dto.response.CouponUsageResult;
import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.CartCheckResultDTO;
import com.cozy.order.dto.response.CheckoutPreviewDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPreviewService {

    private static final String PRICING_RULE_VERSION = "v1";
    private static final Long FIXED_STORE_ID = 1L;

    private final CoffeeProductMapper productMapper;
    private final ProductSkuValidationService skuValidationService;
    private final ObjectMapper objectMapper;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    public CartCheckResultDTO preview(Long userId, String memberLevel, CartCheckRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.VALIDATION_ERROR, "用户未登录");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(BusinessErrorCode.VALIDATION_ERROR, "请选择商品");
        }
        if (request.getStoreId() != null && !FIXED_STORE_ID.equals(request.getStoreId())) {
            throw new BusinessException(BusinessErrorCode.STORE_CLOSED, "当前门店不可用");
        }

        List<Long> invalidItems = new ArrayList<>();
        List<Long> changedItems = new ArrayList<>();
        List<ItemCheckDTO> itemChecks = new ArrayList<>();
        List<CanonicalLine> canonicalLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal baseSubtotal = BigDecimal.ZERO;
        BigDecimal memberDiscount = BigDecimal.ZERO;
        BigDecimal addonsTotal = BigDecimal.ZERO;

        for (OrderItemRequest item : request.getItems()) {
            if (item == null || item.getProductId() == null) {
                throw new BusinessException(BusinessErrorCode.VALIDATION_ERROR, "商品ID不能为空");
            }
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            if (quantity < 1 || quantity > 10) {
                throw new BusinessException(BusinessErrorCode.VALIDATION_ERROR, "单商品购买数量需在1-10之间");
            }

            CoffeeProduct product = productMapper.selectById(item.getProductId());
            if (product == null || !"active".equals(product.getStatus())) {
                invalidItems.add(item.getProductId());
                continue;
            }
            String skuError = skuValidationService.validateSkuOptions(product, item.getCupSize(),
                    item.getSugarLevel(), item.getTemperature());
            if (skuError != null) {
                invalidItems.add(item.getProductId());
                continue;
            }

            BigDecimal unitPrice = product.getPrice();
            if ("LARGE".equalsIgnoreCase(normalize(item.getCupSize()))) {
                unitPrice = unitPrice.add(new BigDecimal("3.00"));
            }
            BigDecimal lineBase = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal lineAddons = parseAddonsFee(item.getAddonsJson()).multiply(BigDecimal.valueOf(quantity));
            BigDecimal lineTotal = lineBase.add(lineAddons);
            subtotal = subtotal.add(lineTotal);
            baseSubtotal = baseSubtotal.add(lineBase);
            addonsTotal = addonsTotal.add(lineAddons);

            if ("black".equalsIgnoreCase(memberLevel) && "soe".equals(product.getCategory())) {
                memberDiscount = memberDiscount.add(lineBase.multiply(new BigDecimal("0.15"))
                        .setScale(2, RoundingMode.HALF_UP));
            }

            itemChecks.add(new ItemCheckDTO(product.getId(), unitPrice, product.getCategory(), quantity,
                    buildModifiersJson(item), defaultString(item.getCupSize(), "STANDARD"),
                    Boolean.TRUE.equals(product.getIsNewProduct())));
            canonicalLines.add(new CanonicalLine(product, item, quantity, unitPrice, lineAddons));
        }

        BigDecimal discount = memberDiscount;
        if (invalidItems.isEmpty()) {
            discount = discount.add(previewCoupons(userId, request, baseSubtotal.subtract(memberDiscount), addonsTotal,
                    itemChecks));
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        CheckoutPreviewDTO preview = new CheckoutPreviewDTO();
        preview.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        preview.setDiscount(discount.setScale(2, RoundingMode.HALF_UP));
        preview.setPayable(subtotal.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        preview.setPreviewToken(buildToken(memberLevel, request, canonicalLines));
        preview.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        CartCheckResultDTO result = new CartCheckResultDTO();
        result.setChangedItems(changedItems);
        result.setInvalidItems(invalidItems);
        result.setPreview(preview);
        return result;
    }

    public CheckoutPreviewDTO validateForCreate(Long userId, String memberLevel, CreateOrderRequest request) {
        CartCheckRequest check = new CartCheckRequest();
        check.setItems(request.getItems());
        check.setCouponCode(request.getCouponCode());
        check.setAddonCouponCodes(request.getAddonCouponCodes());
        check.setStoreId(request.getStoreId());
        check.setPickupTime(request.getPickupTime());
        CartCheckResultDTO result = preview(userId, memberLevel, check);
        if (!result.getInvalidItems().isEmpty()) {
            throw new BusinessException(BusinessErrorCode.ITEM_OFFLINE,
                    "购物车中包含已下架或规格失效的商品");
        }
        if (request.getPreviewToken() != null && !request.getPreviewToken().isBlank()
                && !request.getPreviewToken().equals(result.getPreview().getPreviewToken())) {
            throw new BusinessException(BusinessErrorCode.PREVIEW_EXPIRED, "商品或价格已变化，请重新确认订单");
        }
        return result.getPreview();
    }

    private BigDecimal previewCoupons(Long userId, CartCheckRequest request, BigDecimal couponBase,
            BigDecimal addonsTotal, List<ItemCheckDTO> items) {
        BigDecimal discount = BigDecimal.ZERO;
        try {
            if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
                CouponUsageResult main = pointsMallService.previewCouponWithResult(userId,
                        request.getCouponCode().trim(), couponBase, items);
                discount = discount.add(nullSafe(main.getDiscountAmount()));
                int freeAddons = main.getFreeAddonCount();
                if (freeAddons > 0 && addonsTotal.signum() > 0) {
                    discount = discount.add(calculateFreeAddons(request.getItems(), freeAddons));
                }
            }
            if (request.getAddonCouponCodes() != null) {
                for (String code : request.getAddonCouponCodes()) {
                    if (code == null || code.isBlank()) continue;
                    CouponUsageResult addon = pointsMallService.previewCouponWithResult(userId, code.trim(),
                            couponBase.add(addonsTotal), items);
                    if (!"DELIVERY_FEE".equals(addon.getCouponType())) {
                        discount = discount.add(nullSafe(addon.getDiscountAmount()));
                    }
                }
            }
            return discount;
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "优惠券不可用";
            BusinessErrorCode code = message.contains("过期") ? BusinessErrorCode.COUPON_EXPIRED
                    : BusinessErrorCode.ITEM_CHANGED;
            throw new BusinessException(code, message);
        }
    }

    private BigDecimal calculateFreeAddons(List<OrderItemRequest> items, int count) {
        List<BigDecimal> prices = new ArrayList<>();
        for (OrderItemRequest item : items) {
            String json = item.getAddonsJson();
            if (json == null || json.isBlank()) continue;
            try {
                JsonNode addons = objectMapper.readTree(json);
                if (addons.isArray()) {
                    for (JsonNode addon : addons) {
                        if (addon.has("price")) prices.add(addon.get("price").decimalValue());
                    }
                }
            } catch (Exception ignored) {
                // Invalid addon JSON contributes no price, matching the existing creation path.
            }
        }
        prices.sort(Comparator.reverseOrder());
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < Math.min(count, prices.size()); i++) result = result.add(prices.get(i));
        return result;
    }

    private BigDecimal parseAddonsFee(String json) {
        if (json == null || json.isBlank()) return BigDecimal.ZERO;
        try {
            JsonNode addons = objectMapper.readTree(json);
            BigDecimal total = BigDecimal.ZERO;
            if (addons.isArray()) {
                for (JsonNode addon : addons) {
                    if (addon.has("price")) total = total.add(addon.get("price").decimalValue());
                }
            }
            return total;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String buildToken(String memberLevel, CartCheckRequest request, List<CanonicalLine> lines) {
        lines.sort(Comparator.comparing(CanonicalLine::canonical));
        StringBuilder canonical = new StringBuilder(PRICING_RULE_VERSION)
                .append('|').append(normalize(memberLevel))
                .append('|').append(request.getStoreId() == null ? FIXED_STORE_ID : request.getStoreId())
                .append('|').append(normalize(request.getPickupTime()))
                .append('|').append(normalize(request.getCouponCode()));
        if (request.getAddonCouponCodes() != null) {
            request.getAddonCouponCodes().stream().map(this::normalize).sorted()
                    .forEach(code -> canonical.append('|').append(code));
        }
        for (CanonicalLine line : lines) canonical.append('|').append(line.canonical());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build preview token", e);
        }
    }

    private String buildModifiersJson(OrderItemRequest item) {
        boolean extraShot = "STRONG".equalsIgnoreCase(item.getCoffeeStrength())
                || normalize(item.getAddonsJson()).contains("extra_shot");
        return "{\"extraShot\":" + extraShot + "}";
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record CanonicalLine(CoffeeProduct product, OrderItemRequest item, int quantity,
            BigDecimal unitPrice, BigDecimal addonAmount) {
        String canonical() {
            return product.getId() + ":" + product.getUpdatedAt() + ":" + unitPrice + ":" + quantity + ":"
                    + clean(item.getCupSize()) + ":" + clean(item.getSugarLevel()) + ":"
                    + clean(item.getTemperature()) + ":" + clean(item.getCoffeeStrength()) + ":"
                    + clean(item.getOptionsJson()) + ":" + clean(item.getAddonsJson()) + ":" + addonAmount;
        }

        private static String clean(String value) {
            return value == null ? "" : value.trim().toLowerCase();
        }
    }
}
