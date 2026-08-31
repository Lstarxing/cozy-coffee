package com.cozy.order.service.order;
import com.cozy.order.service.product.ProductPricingService;

import com.cozy.common.exception.BusinessErrorCode;
import com.cozy.common.exception.BusinessException;
import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.dto.response.CouponCombinationResult;
import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.CartCheckResultDTO;
import com.cozy.order.dto.response.CheckoutPreviewDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.service.order.OrderRewardService;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPreviewer {

    private static final String PRICING_RULE_VERSION = "v1";
    private static final Long FIXED_STORE_ID = 1L;

    private final CoffeeProductMapper productMapper;
    private final ObjectMapper objectMapper;
    private final OrderRewardService orderRewardService;
    private final ProductPricingService productPricingService;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    @DubboReference(check = false)
    private MemberService memberService;

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
        List<BigDecimal> addonPrices = new ArrayList<>(); // 原始加料价（尊享通兑券 freeAddon 免加料用）

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
            // V2 统一定价核心（P1E）：规格校验 + 基础价（按 size_type / brew_method）+ 加料权威价 + 成交快照
            ProductPricingService.PriceResult priceResult = productPricingService.price(
                    product, item.getCupSize(), item.getTemperature(),
                    item.getSugarLevel(), item.getBrewMethod(), item.getAddonsJson());
            if (!priceResult.valid()) {
                invalidItems.add(item.getProductId());
                continue;
            }
            BigDecimal unitPrice = priceResult.basePrice();
            BigDecimal lineBase = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal lineAddons = priceResult.addonFee().multiply(BigDecimal.valueOf(quantity));
            addonPrices.addAll(priceResult.addonPrices());
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
            canonicalLines.add(new CanonicalLine(product, item, quantity, unitPrice, lineAddons,
                    priceResult.normalizedAddonsJson()));
        }

        BigDecimal discount = memberDiscount;
        CouponCombinationResult combo = null;
        if (invalidItems.isEmpty()) {
            combo = previewCoupons(userId, request, baseSubtotal.subtract(memberDiscount), addonsTotal,
                    addonPrices, itemChecks);
        }
        if (combo != null) {
            discount = discount.add(nullSafe(combo.getMainDiscount()))
                    .add(nullSafe(combo.getAddonDiscount()));
        }

        // 配送费与优惠合计：与 create 逐行同口径
        //  - 外送 3 元；黑金会员免运费（配送费计入商品抵扣）；配送费券从配送费扣除
        //  - 配送费券抵扣计入优惠合计，实付=小计−优惠合计+配送费，等价于 create 的
        //    (商品−主券−附加券)+(配送费−配送券)
        BigDecimal deliveryFee = BigDecimal.ZERO;
        if ("DELIVERY".equalsIgnoreCase(request.getDiningMethod())) {
            deliveryFee = new BigDecimal("3");
            if ("black".equalsIgnoreCase(memberLevel)) {
                discount = discount.add(deliveryFee); // 黑金免运费：配送费计入抵扣
            }
        }
        BigDecimal deliveryFeeDiscount = combo != null ? nullSafe(combo.getDeliveryFeeDiscount()) : BigDecimal.ZERO;
        if (deliveryFeeDiscount.signum() > 0 && !"DELIVERY".equalsIgnoreCase(request.getDiningMethod())) {
            throw new BusinessException(BusinessErrorCode.ITEM_CHANGED, "配送费券仅限外卖订单使用");
        }
        discount = discount.add(deliveryFeeDiscount);

        BigDecimal payable = subtotal.subtract(discount).add(deliveryFee).max(BigDecimal.ZERO);

        CheckoutPreviewDTO preview = new CheckoutPreviewDTO();
        preview.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        // 展示用优惠合计：最高不超过 小计+配送费（避免负实付时优惠虚高）
        BigDecimal displayDiscount = discount.compareTo(subtotal.add(deliveryFee)) > 0
                ? subtotal.add(deliveryFee) : discount;
        preview.setDiscount(displayDiscount.setScale(2, RoundingMode.HALF_UP));
        preview.setDeliveryFee(deliveryFee.setScale(2, RoundingMode.HALF_UP));
        preview.setPayable(payable.setScale(2, RoundingMode.HALF_UP));
        preview.setPreviewToken(buildToken(memberLevel, request, canonicalLines));
        preview.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        if (combo != null && combo.getDetails() != null) {
            preview.setCouponDetails(combo.getDetails().stream()
                    .map(d -> {
                        CheckoutPreviewDTO.CouponPreviewItem item = new CheckoutPreviewDTO.CouponPreviewItem();
                        item.setTitle(d.getTitle());
                        item.setDiscount(d.getDiscount());
                        item.setMain(d.isMain());
                        return item;
                    })
                    .collect(Collectors.toList()));
        }

        // 可得积分/成长值预估（与下单落库同一口径：基数=实付−配送费）
        try {
            MemberDTO member = memberService.getMemberByUserId(userId);
            BigDecimal rewardBase = payable.subtract(deliveryFee).max(BigDecimal.ZERO);
            OrderRewardService.RewardEstimate est = orderRewardService.estimateRewards(rewardBase, member);
            preview.setPointsEarned(est.pointsEarned);
            preview.setExpEarned(est.expEarned);
        } catch (Exception e) {
            log.warn("预览奖励预估失败: userId={}", userId, e);
            int exp = payable.setScale(0, RoundingMode.HALF_UP).intValue();
            preview.setExpEarned(exp);
            preview.setPointsEarned(exp);
        }

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
        // 配送方式必须透传：preview 的配送费/配送费券校验依赖它，漏传会导致下单校验与预览口径不一致
        check.setDiningMethod(request.getDiningMethod());
        check.setDeliveryAddressId(request.getDeliveryAddressId());
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

    private CouponCombinationResult previewCoupons(Long userId, CartCheckRequest request, BigDecimal couponBase,
            BigDecimal addonsTotal, List<BigDecimal> addonPrices, List<ItemCheckDTO> items) {
        List<String> codes = collectCouponCodes(request);
        if (codes.isEmpty()) {
            return null;
        }
        try {
            // 组合引擎统一校验+计算（主券/辅券分类由 mall 侧判定）；配送费券折扣不进入预览商品折扣
            return pointsMallService.previewCouponCombination(
                    userId, codes, couponBase, addonsTotal, addonPrices, items);
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "优惠券不可用";
            BusinessErrorCode code = message.contains("过期") ? BusinessErrorCode.COUPON_EXPIRED
                    : BusinessErrorCode.ITEM_CHANGED;
            throw new BusinessException(code, message);
        }
    }

    /** 合并主券 + 辅券为整组券码列表（分类交给组合引擎） */
    private List<String> collectCouponCodes(CartCheckRequest request) {
        List<String> codes = new ArrayList<>();
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            codes.add(request.getCouponCode().trim());
        }
        if (request.getAddonCouponCodes() != null) {
            for (String code : request.getAddonCouponCodes()) {
                if (code != null && !code.isBlank()) {
                    codes.add(code.trim());
                }
            }
        }
        return codes;
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
            BigDecimal unitPrice, BigDecimal addonAmount, String normalizedAddonsJson) {
        String canonical() {
            return product.getId() + ":" + product.getUpdatedAt() + ":" + unitPrice + ":" + quantity + ":"
                    + clean(item.getCupSize()) + ":" + clean(item.getSugarLevel()) + ":"
                    + clean(item.getTemperature()) + ":" + clean(item.getCoffeeStrength()) + ":"
                    + clean(item.getOptionsJson()) + ":" + normalizedAddonsJson + ":" + addonAmount;
        }

        private static String clean(String value) {
            return value == null ? "" : value.trim().toLowerCase();
        }
    }
}
