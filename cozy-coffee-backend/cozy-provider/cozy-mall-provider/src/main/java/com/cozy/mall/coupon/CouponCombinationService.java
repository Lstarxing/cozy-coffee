package com.cozy.mall.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.CouponStackingConfig;
import com.cozy.common.exception.BusinessException;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.dto.response.CouponCombinationResult;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.mapper.UserCouponMapper;
import com.cozy.mall.util.CouponRuleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 优惠券组合引擎：主券/辅券/独占分类、组合校验、统一计算的唯一入口。
 * <p>
 * - preview 只校验+计算不消费；use 校验+计算+整组冻结（ISSUED→FROZEN），两者共用 combine()。
 * - 分类判定优先级：ruleJson.stacking → CouponStackingConfig.categories[type] → 默认 MAIN；
 *   ruleJson.exclusive=true 视为 EXCLUSIVE（兼容历史券）。
 * - 金额基数由 order 层传入：主券 couponBase（商品小计−会员折扣，不含加料），辅券 couponBase+addonsTotal；
 *   addonPrices 供尊享通兑券 freeAddon 免加料取最高 N 个（与旧 create/preview 语义一致）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponCombinationService {

    private static final String CATEGORY_MAIN = "MAIN";
    private static final String CATEGORY_ADDON = "ADDON";
    private static final String CATEGORY_EXCLUSIVE = "EXCLUSIVE";

    private final UserCouponMapper userCouponMapper;
    private final CouponStackingConfig stackingConfig;
    private final Map<String, CouponCalculator> couponCalculators;

    public CouponCombinationResult preview(Long userId, List<String> couponCodes, BigDecimal couponBase,
            BigDecimal addonsTotal, List<BigDecimal> addonPrices, List<ItemCheckDTO> items) {
        return combine(userId, couponCodes, couponBase, addonsTotal, addonPrices, items, false);
    }

    @Transactional
    public CouponCombinationResult use(Long userId, List<String> couponCodes, BigDecimal couponBase,
            BigDecimal addonsTotal, List<BigDecimal> addonPrices, List<ItemCheckDTO> items) {
        return combine(userId, couponCodes, couponBase, addonsTotal, addonPrices, items, true);
    }

    private CouponCombinationResult combine(Long userId, List<String> couponCodes, BigDecimal couponBase,
            BigDecimal addonsTotal, List<BigDecimal> addonPrices, List<ItemCheckDTO> items, boolean consume) {
        List<UserCoupon> coupons = loadCoupons(userId, couponCodes);
        validateCombination(coupons, items);
        CouponCombinationResult result = compute(coupons, couponBase, addonsTotal, addonPrices, items);
        if (consume) {
            LocalDateTime now = LocalDateTime.now();
            for (UserCoupon coupon : coupons) {
                coupon.setStatus("FROZEN");
                coupon.setUsedAt(now);
                userCouponMapper.updateById(coupon);
            }
            log.info("整组券冻结: userId={}, count={}", userId, coupons.size());
        }
        return result;
    }

    private List<UserCoupon> loadCoupons(Long userId, List<String> couponCodes) {
        if (userId == null || couponCodes == null || couponCodes.isEmpty()) {
            throw new BusinessException("优惠券参数不能为空");
        }
        Set<String> seen = new HashSet<>();
        List<UserCoupon> result = new ArrayList<>();
        for (String code : couponCodes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            if (!seen.add(code)) {
                throw new BusinessException("优惠券不能重复选择");
            }
            LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponCode, code)
                    .eq(UserCoupon::getStatus, "ISSUED");
            UserCoupon coupon = userCouponMapper.selectOne(wrapper);
            if (coupon == null) {
                throw new BusinessException("券不存在或已使用: " + code);
            }
            if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException("券已过期: " + code);
            }
            result.add(coupon);
        }
        return result;
    }

    private void validateCombination(List<UserCoupon> coupons, List<ItemCheckDTO> items) {
        if (coupons.isEmpty()) {
            return;
        }
        int mainCount = 0;
        int deliveryFeeCount = 0;
        int shotCount = 0;
        for (UserCoupon coupon : coupons) {
            String category = classify(coupon);
            if (CATEGORY_EXCLUSIVE.equals(category)) {
                if (coupons.size() > 1) {
                    throw new BusinessException("此券不可与其他券叠加");
                }
                return;
            }
            if (CATEGORY_MAIN.equals(category)) {
                mainCount++;
            } else {
                if ("DELIVERY_FEE".equals(coupon.getCouponType())) {
                    deliveryFeeCount++;
                }
                if ("SHOT".equals(coupon.getCouponType())) {
                    shotCount++;
                }
            }
        }
        if (mainCount > 1) {
            throw new BusinessException("主券最多使用一张");
        }
        int maxDeliveryFee = stackingConfig.getMaxDeliveryFee() != null ? stackingConfig.getMaxDeliveryFee() : 1;
        if (deliveryFeeCount > maxDeliveryFee) {
            throw new BusinessException("配送费券最多使用 " + maxDeliveryFee + " 张");
        }
        int extraShotCount = countExtraShot(items);
        if (shotCount > extraShotCount) {
            throw new BusinessException("加浓缩券数量超过订单可用数量");
        }
    }

    private CouponCombinationResult compute(List<UserCoupon> coupons, BigDecimal couponBase,
            BigDecimal addonsTotal, List<BigDecimal> addonPrices, List<ItemCheckDTO> items) {
        CouponCombinationResult result = new CouponCombinationResult();
        BigDecimal mainDiscount = BigDecimal.ZERO;
        BigDecimal addonDiscount = BigDecimal.ZERO;
        BigDecimal deliveryFeeDiscount = BigDecimal.ZERO;
        BigDecimal mainBase = nullSafe(couponBase);
        BigDecimal addonBase = mainBase.add(nullSafe(addonsTotal));
        List<Long> addonCouponIds = new ArrayList<>();
        UserCoupon mainCoupon = null;

        for (UserCoupon coupon : coupons) {
            // MAIN / EXCLUSIVE 均按主券口径计算（EXCLUSIVE 只是不可叠加，折扣仍是主券型）
            if (CATEGORY_MAIN.equals(classify(coupon)) || CATEGORY_EXCLUSIVE.equals(classify(coupon))) {
                if (mainCoupon == null) {
                    mainCoupon = coupon;
                }
            } else {
                BigDecimal discount = calculate(coupon, addonBase, items);
                if ("DELIVERY_FEE".equals(coupon.getCouponType())) {
                    deliveryFeeDiscount = deliveryFeeDiscount.add(discount);
                } else {
                    addonDiscount = addonDiscount.add(discount);
                }
                if (coupon.getId() != null) {
                    addonCouponIds.add(coupon.getId());
                }
            }
        }

        if (mainCoupon != null) {
            mainDiscount = calculate(mainCoupon, mainBase, items);
            int freeAddonCount = CouponRuleUtil.parseValue(mainCoupon.getRuleJson(), "freeAddon");
            if (freeAddonCount > 0 && nullSafe(addonsTotal).signum() > 0
                    && addonPrices != null && !addonPrices.isEmpty()) {
                BigDecimal freeAddonDiscount = freeAddonDiscount(addonPrices, freeAddonCount);
                mainDiscount = mainDiscount.add(freeAddonDiscount);
                log.info("尊享通兑券免费加料: freeAddonCount={}, discount={}", freeAddonCount, freeAddonDiscount);
            }
            result.setMainCouponId(mainCoupon.getId());
            result.setMainCouponType(mainCoupon.getCouponType());
            result.setExchangeCoupon("EXCHANGE".equals(mainCoupon.getCouponType()));
        }

        result.setMainDiscount(mainDiscount);
        result.setAddonDiscount(addonDiscount);
        result.setDeliveryFeeDiscount(deliveryFeeDiscount);
        result.setAddonCouponIds(addonCouponIds);
        result.setExclusive(coupons.stream().anyMatch(c -> CATEGORY_EXCLUSIVE.equals(classify(c))));
        return result;
    }

    private BigDecimal calculate(UserCoupon coupon, BigDecimal orderAmount, List<ItemCheckDTO> items) {
        if (orderAmount == null) {
            orderAmount = BigDecimal.ZERO;
        }
        CouponCalculator calculator = couponCalculators.get(coupon.getCouponType());
        if (calculator == null) {
            log.warn("未知券类型，不抵扣: type={}, ruleJson={}", coupon.getCouponType(), coupon.getRuleJson());
            return BigDecimal.ZERO;
        }
        return calculator.calculate(coupon, orderAmount, items);
    }

    private BigDecimal freeAddonDiscount(List<BigDecimal> addonPrices, int count) {
        List<BigDecimal> sorted = new ArrayList<>(addonPrices);
        sorted.sort(Comparator.reverseOrder());
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < Math.min(count, sorted.size()); i++) {
            total = total.add(sorted.get(i));
        }
        return total;
    }

    private int countExtraShot(List<ItemCheckDTO> items) {
        if (items == null) {
            return 0;
        }
        int count = 0;
        for (ItemCheckDTO item : items) {
            String modifiers = item.getModifiersJson();
            if (modifiers != null
                    && (modifiers.contains("\"extraShot\":true")
                            || modifiers.toLowerCase().contains("extra_shot")
                            || modifiers.contains("加浓"))) {
                count += item.getQuantity() != null ? item.getQuantity() : 1;
            }
        }
        return count;
    }

    /** 分类判定：ruleJson.exclusive=true → EXCLUSIVE；ruleJson.stacking → 类型映射 → 默认 MAIN */
    private String classify(UserCoupon coupon) {
        String ruleJson = coupon.getRuleJson();
        if (ruleJson != null && ruleJson.contains("\"exclusive\":true")) {
            return CATEGORY_EXCLUSIVE;
        }
        String stacking = CouponRuleUtil.parseStringValue(ruleJson, "stacking");
        if (stacking != null && !stacking.isBlank()) {
            String upper = stacking.trim().toUpperCase();
            if (CATEGORY_MAIN.equals(upper) || CATEGORY_ADDON.equals(upper) || CATEGORY_EXCLUSIVE.equals(upper)) {
                return upper;
            }
        }
        return stackingConfig.categoryOf(coupon.getCouponType());
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
