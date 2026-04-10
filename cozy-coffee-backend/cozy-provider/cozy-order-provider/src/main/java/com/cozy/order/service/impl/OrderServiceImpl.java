package com.cozy.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.RedisKeyConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cozy.member.api.MemberService;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.dto.response.ShopOrderItemDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.entity.ShopOrderItem;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ShopOrderMapper;
import com.cozy.order.mapper.ShopOrderItemMapper;
import com.cozy.order.service.PickupCodeService;
import com.cozy.order.service.ProductSkuValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * 订单服务实现 - 独立微服务
 * v4.0: 支持一单多商品、券核销、幂等发放 EXP/POINT
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String EMPTY_CACHE_MARKER = "__NULL__";
    private static final Semaphore MENU_DB_REBUILD_GUARD = new Semaphore(4);
    private static final LongAdder MENU_CACHE_HIT = new LongAdder();
    private static final LongAdder MENU_CACHE_MISS = new LongAdder();
    private static final LongAdder MENU_CACHE_EMPTY_HIT = new LongAdder();
    private static final LongAdder MENU_DEGRADE_FAST_FAIL = new LongAdder();
    private static final AtomicLong MENU_METRIC_SEQ = new AtomicLong();

    private final CoffeeProductMapper productMapper;
    private final ShopOrderMapper orderMapper;
    private final ShopOrderItemMapper orderItemMapper;
    private final PickupCodeService pickupCodeService;
    private final ProductSkuValidationService skuValidationService; // v5.3: SKU 验证服务
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @DubboReference(check = false)
    private MemberService memberService;

    @DubboReference(check = false)
    private com.cozy.member.api.PointsMallService pointsMallService;

    @DubboReference(check = false)
    private com.cozy.member.api.MonthlyTaskService monthlyTaskService;

    @DubboReference(check = false)
    private com.cozy.user.api.UserService userService; // v5.0: 用于首单邀请奖励发放

    // 菜单缓存 (Local Cache)
    private volatile List<CoffeeProductDTO> cachedMenu = null;

    private void invalidateMenuCache() {
        this.cachedMenu = null;
        try {
            redisTemplate.delete(RedisKeyConstants.ORDER_MENU_ACTIVE);
        } catch (Exception e) {
            log.warn("清理Redis菜单缓存失败", e);
        }
        log.info("菜单缓存已清除");
    }

    // ==================== 商品查询 ====================

    @Override
    public List<CoffeeProductDTO> listCoffeeProducts() {
        // L1: in-process local cache
        List<CoffeeProductDTO> cache = this.cachedMenu;
        if (cache != null) {
            backfillMenuRedisIfMissing(cache);
            MENU_CACHE_HIT.increment();
            logMenuCacheMetricsMaybe();
            return cache;
        }

        // L2: Redis cache (shared across instances)
        try {
            Object cachedValue = redisTemplate.opsForValue().get(RedisKeyConstants.ORDER_MENU_ACTIVE);
            if (EMPTY_CACHE_MARKER.equals(cachedValue)) {
                this.cachedMenu = Collections.emptyList();
                MENU_CACHE_EMPTY_HIT.increment();
                logMenuCacheMetricsMaybe();
                return this.cachedMenu;
            }
            List<CoffeeProductDTO> redisCached = convertToCoffeeProductList(cachedValue);
            if (redisCached != null) {
                this.cachedMenu = redisCached;
                MENU_CACHE_HIT.increment();
                logMenuCacheMetricsMaybe();
                return redisCached;
            }
        } catch (Exception e) {
            log.warn("读取Redis菜单缓存失败，回退数据库查询", e);
        }
        MENU_CACHE_MISS.increment();

        String lockToken = UUID.randomUUID().toString();
        boolean locked = tryAcquireRebuildLock(RedisKeyConstants.LOCK_ORDER_MENU_REBUILD, lockToken, 8);
        if (!locked) {
            try {
                TimeUnit.MILLISECONDS.sleep(40L);
                Object retryCache = redisTemplate.opsForValue().get(RedisKeyConstants.ORDER_MENU_ACTIVE);
                if (EMPTY_CACHE_MARKER.equals(retryCache)) {
                    this.cachedMenu = Collections.emptyList();
                    MENU_CACHE_EMPTY_HIT.increment();
                    logMenuCacheMetricsMaybe();
                    return this.cachedMenu;
                }
                List<CoffeeProductDTO> redisCached = convertToCoffeeProductList(retryCache);
                if (redisCached != null) {
                    this.cachedMenu = redisCached;
                    MENU_CACHE_HIT.increment();
                    logMenuCacheMetricsMaybe();
                    return redisCached;
                }
            } catch (Exception e) {
                log.warn("重建等待后读取Redis菜单缓存失败", e);
            }
        }

        if (!acquireDbRebuildPermit()) {
            MENU_DEGRADE_FAST_FAIL.increment();
            logMenuCacheMetricsMaybe();
            return Collections.emptyList();
        }

        try {
            synchronized (this) {
                if (this.cachedMenu != null)
                    return this.cachedMenu;

                LambdaQueryWrapper<CoffeeProduct> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CoffeeProduct::getStatus, "active")
                        .orderByAsc(CoffeeProduct::getSortOrder);
                List<CoffeeProductDTO> result = productMapper.selectList(wrapper).stream()
                        .map(this::toProductDTO)
                        .collect(Collectors.toList());

                this.cachedMenu = result;
                try {
                    if (result.isEmpty()) {
                        redisTemplate.opsForValue().set(
                                RedisKeyConstants.ORDER_MENU_ACTIVE,
                                EMPTY_CACHE_MARKER,
                                60,
                                TimeUnit.SECONDS);
                    } else {
                        long ttlMinutes = 5L + ThreadLocalRandom.current().nextLong(3L);
                        redisTemplate.opsForValue().set(
                                RedisKeyConstants.ORDER_MENU_ACTIVE,
                            result,
                                ttlMinutes,
                                TimeUnit.MINUTES);
                    }
                } catch (Exception e) {
                    log.warn("写入Redis菜单缓存失败", e);
                }
                log.info("菜单缓存已更新，共 {} 个商品", result.size());
                logMenuCacheMetricsMaybe();
                return result;
            }
        } finally {
            MENU_DB_REBUILD_GUARD.release();
            if (locked) {
                releaseLockSafely(RedisKeyConstants.LOCK_ORDER_MENU_REBUILD, lockToken);
            }
        }
    }

    private boolean acquireDbRebuildPermit() {
        try {
            return MENU_DB_REBUILD_GUARD.tryAcquire(80, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void backfillMenuRedisIfMissing(List<CoffeeProductDTO> cache) {
        try {
            Boolean hasKey = redisTemplate.hasKey(RedisKeyConstants.ORDER_MENU_ACTIVE);
            if (Boolean.TRUE.equals(hasKey)) {
                return;
            }
            if (cache.isEmpty()) {
                redisTemplate.opsForValue().set(
                        RedisKeyConstants.ORDER_MENU_ACTIVE,
                        EMPTY_CACHE_MARKER,
                        60,
                        TimeUnit.SECONDS);
            } else {
                long ttlMinutes = 5L + ThreadLocalRandom.current().nextLong(3L);
                redisTemplate.opsForValue().set(
                        RedisKeyConstants.ORDER_MENU_ACTIVE,
                        cache,
                        ttlMinutes,
                        TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.warn("L1命中时回填Redis菜单缓存失败", e);
        }
    }

    private void logMenuCacheMetricsMaybe() {
        long seq = MENU_METRIC_SEQ.incrementAndGet();
        if (seq % 200 == 0) {
            log.info("menu-cache-metrics: hit={}, miss={}, emptyHit={}, fastFail={}",
                    MENU_CACHE_HIT.sum(),
                    MENU_CACHE_MISS.sum(),
                    MENU_CACHE_EMPTY_HIT.sum(),
                    MENU_DEGRADE_FAST_FAIL.sum());
        }
    }

    private boolean tryAcquireRebuildLock(String lockKey, String lockToken, int ttlSeconds) {
        try {
            Boolean lockOk = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, ttlSeconds,
                    TimeUnit.SECONDS);
            return Boolean.TRUE.equals(lockOk);
        } catch (Exception e) {
            log.warn("获取Redis重建锁失败: key={}", lockKey, e);
            return false;
        }
    }

    private void releaseLockSafely(String lockKey, String lockToken) {
        try {
            // 若切换到 Redisson，可替换为 RLock.tryLock/unlock，省去Lua解锁与token校验逻辑。
            String releaseScript = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end";
            org.springframework.data.redis.core.script.DefaultRedisScript<Long> redisScript = new org.springframework.data.redis.core.script.DefaultRedisScript<>();
            redisScript.setScriptText(releaseScript);
            redisScript.setResultType(Long.class);
            stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockToken);
        } catch (Exception e) {
            log.warn("释放Redis重建锁失败: key={}", lockKey, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<CoffeeProductDTO> convertToCoffeeProductList(Object cachedValue) {
        if (!(cachedValue instanceof List<?> rawList)) {
            return null;
        }
        return rawList.stream()
                .map(item -> objectMapper.convertValue(item, CoffeeProductDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public CoffeeProductDTO getProduct(Long productId) {
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        return toProductDTO(product);
    }

    // ==================== 订单创建（多商品 + 券核销）====================

    @Override
    @Transactional
    public ShopOrderDTO createOrder(Long userId, String memberLevel, CreateOrderRequest request) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }

        // 构建订单项列表
        List<OrderItemRequest> itemRequests = request.getItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new RuntimeException("请选择商品");
        }

        // 验证商品并计算金额
        BigDecimal totalAmount = BigDecimal.ZERO; // 订单总金额（基础+加料）
        BigDecimal baseTotalAmount = BigDecimal.ZERO; // 基础商品总金额（基础价+杯型加价）
        BigDecimal addonsTotalAmount = BigDecimal.ZERO; // 加料总费用
        int totalQuantity = 0;
        List<ShopOrderItem> orderItems = new ArrayList<>();
        // v5.0: 用于券核销的商品检查列表
        List<com.cozy.member.dto.request.ItemCheckDTO> itemChecks = new ArrayList<>();
        StringBuilder itemsSummary = new StringBuilder();

        for (OrderItemRequest itemReq : itemRequests) {
            if (itemReq.getProductId() == null) {
                throw new RuntimeException("商品ID不能为空");
            }
            int qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            if (qty < 1 || qty > 10) {
                throw new RuntimeException("单商品购买数量需在1-10之间");
            }

            CoffeeProduct product = productMapper.selectById(itemReq.getProductId());
            if (product == null) {
                throw new RuntimeException("商品不存在: " + itemReq.getProductId());
            }
            if (!"active".equals(product.getStatus())) {
                throw new RuntimeException("商品已下架: " + product.getName());
            }

            // v5.3: SKU 配置验证 - 检查杯型/甜度/温度选择是否符合产品规则
            String skuError = skuValidationService.validateSkuOptions(
                    product,
                    itemReq.getCupSize(),
                    itemReq.getSugarLevel(),
                    itemReq.getTemperature());
            if (skuError != null) {
                throw new RuntimeException(skuError);
            }

            // v5.3.2: 计算商品金额（基础价格 + 杯型加价），加料费用单独计算
            BigDecimal basePrice = product.getPrice(); // 中杯基础价格

            // 1. 杯型加价（LARGE大杯 +3元）
            if ("LARGE".equals(itemReq.getCupSize())) {
                basePrice = basePrice.add(new BigDecimal("3"));
                log.info("大杯加价: productId={}, originalPrice={}, finalPrice={}",
                        product.getId(), product.getPrice(), basePrice);
            }

            BigDecimal itemBaseAmount = basePrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal itemAddonsAmount = BigDecimal.ZERO;

            // 2. 加料费用（如加浓缩 +5元）- 单独计算，不参与主券折扣
            if (itemReq.getAddonsJson() != null && !itemReq.getAddonsJson().trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode addons = objectMapper.readTree(itemReq.getAddonsJson());

                    BigDecimal addonsFee = BigDecimal.ZERO;
                    if (addons.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode addon : addons) {
                            if (addon.has("price")) {
                                BigDecimal addonPrice = addon.get("price").decimalValue();
                                addonsFee = addonsFee.add(addonPrice);
                            }
                        }
                    }

                    if (addonsFee.compareTo(BigDecimal.ZERO) > 0) {
                        itemAddonsAmount = addonsFee.multiply(BigDecimal.valueOf(qty));
                        log.info("商品加料费用: productId={}, addonsFee={}, quantity={}, addonsTotal={}",
                                product.getId(), addonsFee, qty, itemAddonsAmount);
                    }
                } catch (Exception e) {
                    log.warn("解析加料信息失败: {}", e.getMessage());
                }
            }

            BigDecimal itemAmount = itemBaseAmount.add(itemAddonsAmount);
            baseTotalAmount = baseTotalAmount.add(itemBaseAmount); // 累加基础商品金额
            addonsTotalAmount = addonsTotalAmount.add(itemAddonsAmount); // 累加加料费用
            totalAmount = totalAmount.add(itemAmount); // 累加总金额
            totalQuantity += qty;

            // 构建订单项
            ShopOrderItem item = new ShopOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(qty);
            item.setItemAmount(itemAmount);
            item.setCupSize(itemReq.getCupSize());
            item.setSugarLevel(itemReq.getSugarLevel());
            item.setTemperature(itemReq.getTemperature());
            item.setCoffeeStrength(itemReq.getCoffeeStrength());
            item.setOptionsJson(itemReq.getOptionsJson());
            item.setAddonsJson(itemReq.getAddonsJson()); // v5.3: 加料信息
            item.setCreatedAt(LocalDateTime.now());
            orderItems.add(item);

            // v5.3: 添加到检查列表，传入修饰符信息、杯型和新品标识
            // v5.3.2: 传入实际价格（含杯型加价），而非原始价格
            String modifiersJson = buildModifiersJson(itemReq);
            String cupSize = itemReq.getCupSize() != null ? itemReq.getCupSize() : "STANDARD";
            Boolean isNewProduct = product.getIsNewProduct() != null ? product.getIsNewProduct() : false;
            itemChecks.add(new com.cozy.member.dto.request.ItemCheckDTO(
                    product.getId(), basePrice, product.getCategory(), qty, modifiersJson, cupSize, isNewProduct));

            // 摘要
            if (itemsSummary.length() > 0)
                itemsSummary.append(", ");
            itemsSummary.append(product.getName()).append(" x").append(qty);
        }

        // ============================================================
        // v5.8: 黑金会员 SOE 8.5折优惠（15% off）
        // 说明：黑金会员购买 soe 类产品（手冲精品）时自动享受 8.5折
        // 应用时机：在优惠券折扣之前，先对商品原价进行会员折扣
        // ============================================================
        BigDecimal memberDiscount = BigDecimal.ZERO;
        boolean hasBlackGoldSoeDiscount = false;

        if ("black".equals(memberLevel)) {
            for (ShopOrderItem item : orderItems) {
                CoffeeProduct product = productMapper.selectById(item.getProductId());
                // 判断是否为 soe 类产品（手冲精品）
                if (product != null && "soe".equals(product.getCategory())) {
                    // 黑金会员享受 8.5折，即 15% 折扣
                    // 折扣只应用于商品基础价格（含杯型加价），不包含加料费用
                    BigDecimal itemBasePrice = item.getUnitPrice();
                    if ("LARGE".equals(item.getCupSize())) {
                        itemBasePrice = itemBasePrice.add(new BigDecimal("3"));
                    }
                    BigDecimal itemBaseAmount = itemBasePrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal itemDiscount = itemBaseAmount.multiply(new BigDecimal("0.15"))
                            .setScale(2, RoundingMode.HALF_UP);

                    memberDiscount = memberDiscount.add(itemDiscount);
                    hasBlackGoldSoeDiscount = true;

                    log.info("黑金会员SOE折扣: productId={}, productName={}, baseAmount={}, discount={}",
                            item.getProductId(), item.getProductName(), itemBaseAmount, itemDiscount);
                }
            }

            if (hasBlackGoldSoeDiscount) {
                // 从基础商品金额中扣除会员折扣
                baseTotalAmount = baseTotalAmount.subtract(memberDiscount);
                totalAmount = totalAmount.subtract(memberDiscount);
                log.info("黑金会员SOE总折扣: memberDiscount={}, 折后baseTotalAmount={}, 折后totalAmount={}",
                        memberDiscount, baseTotalAmount, totalAmount);
            }
        }

        // 券核销
        BigDecimal discountAmount = BigDecimal.ZERO;
        Long appliedCouponId = null;
        String couponCode = request.getCouponCode();
        boolean isExchangeCoupon = false; // 标记是否使用兑换券
        boolean isExclusiveCoupon = false; // v5.2: 标记是否为互斥券（不可叠加其他优惠）

        if (couponCode != null && !couponCode.trim().isEmpty()) {
            try {
                // 收集商品ID列表用于验证兑换券
                List<Long> productIds = orderItems.stream()
                        .map(ShopOrderItem::getProductId)
                        .collect(Collectors.toList());

                log.info(
                        "准备核销优惠券: couponCode={}, baseTotalAmount={}, addonsTotalAmount={}, totalAmount={}, productIds={}",
                        couponCode.trim(), baseTotalAmount, addonsTotalAmount, totalAmount, productIds);

                // v5.3.2: 主券只对基础商品金额（含杯型加价）打折，不包含加料费用
                // 使用新方法获取券类型信息（传入 itemChecks 以支持精准核销）
                com.cozy.member.dto.response.CouponUsageResult couponResult = pointsMallService
                        .useCouponWithResult(userId, couponCode.trim(), baseTotalAmount, itemChecks);

                discountAmount = couponResult.getDiscountAmount();
                appliedCouponId = couponResult.getCouponId();
                isExchangeCoupon = couponResult.isExchangeCoupon();
                isExclusiveCoupon = couponResult.isExclusive();

                // v5.7: 尊享通兑券免费加料逻辑（免除金额最高的 N 个加料）
                int freeAddonCount = couponResult.getFreeAddonCount();
                if (freeAddonCount > 0 && addonsTotalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // 收集所有加料价格，找出最高的 N 个免除
                    java.util.List<BigDecimal> addonPrices = new java.util.ArrayList<>();
                    for (ShopOrderItem item : orderItems) {
                        if (item.getAddonsJson() != null && !item.getAddonsJson().trim().isEmpty()) {
                            try {
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                com.fasterxml.jackson.databind.JsonNode addons = mapper.readTree(item.getAddonsJson());
                                if (addons.isArray()) {
                                    for (com.fasterxml.jackson.databind.JsonNode addon : addons) {
                                        if (addon.has("price")) {
                                            addonPrices.add(addon.get("price").decimalValue());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("解析加料信息失败", e);
                            }
                        }
                    }
                    // 按金额从高到低排序，取前 freeAddonCount 个
                    addonPrices.sort((a, b) -> b.compareTo(a));
                    BigDecimal addonDiscount = BigDecimal.ZERO;
                    for (int i = 0; i < Math.min(freeAddonCount, addonPrices.size()); i++) {
                        addonDiscount = addonDiscount.add(addonPrices.get(i));
                    }
                    discountAmount = discountAmount.add(addonDiscount);
                    log.info("尊享通兑券免费加料: freeAddonCount={}, addonPrices={}, addonDiscount={}, totalDiscount={}",
                            freeAddonCount, addonPrices, addonDiscount, discountAmount);
                }

                // v5.0: 修正后的折扣逻辑已在 PointsMallService 中计算，此处无需再次修正
                // 仅记录日志
                if (isExchangeCoupon) {
                    log.info("兑换券核销(来自MallService): discount={}, linkedProductId={}",
                            discountAmount, couponResult.getLinkedProductId());
                }

                log.info("券核销成功: couponCode={}, type={}, discount={}, isExchange={}, exclusive={}, freeAddon={}",
                        couponCode, couponResult.getCouponType(), discountAmount, isExchangeCoupon, isExclusiveCoupon,
                        freeAddonCount);
            } catch (Exception e) {
                log.warn("券核销失败: couponCode={}, error={}", couponCode, e.getMessage());
                throw new RuntimeException("优惠券使用失败: " + e.getMessage());
            }
        }

        // ============================================================
        // v5.3: 配送费与黑金会员无限免运费逻辑
        // ============================================================
        BigDecimal deliveryFee = BigDecimal.ZERO;
        boolean deliveryFeeWaived = false;
        String deliveryFeeWaivedReason = null;
        BigDecimal addonDiscount = BigDecimal.ZERO; // 商品附加券折扣（如加浓缩券）
        BigDecimal deliveryFeeDiscount = BigDecimal.ZERO; // 配送费折扣（如配送费券）
        List<Long> addonCouponIds = new java.util.ArrayList<>();

        // 仅外卖订单有配送费
        if ("DELIVERY".equals(request.getDiningMethod())) {
            deliveryFee = new BigDecimal("3"); // 默认配送费 3 元

            // v5.3: 黑金会员自动免运费
            if ("black".equals(memberLevel)) {
                deliveryFeeWaived = true;
                deliveryFeeWaivedReason = "BLACK_GOLD_UNLIMITED";
                addonDiscount = deliveryFee; // 直接免除全部配送费
                log.info("黑金会员无限免运费: userId={}, deliveryFee={}", userId, deliveryFee);
            }
        }

        // ============================================================
        // v5.3: 附加券核销（独立于配送方式，所有订单类型都可用）
        // ============================================================
        List<String> addonCouponCodes = request.getAddonCouponCodes();

        // 互斥检查
        if (isExclusiveCoupon && addonCouponCodes != null && !addonCouponCodes.isEmpty()) {
            throw new RuntimeException("当前优惠券不可与其他优惠（如免运费券）叠加使用");
        }

        // v5.3: 校验配送费券数量（一单只能用一张）
        if (addonCouponCodes != null && !addonCouponCodes.isEmpty()) {
            long deliveryFeeCouponCount = addonCouponCodes.stream()
                    .filter(code -> code != null && code.contains("DELIVERY_FEE"))
                    .count();
            if (deliveryFeeCouponCount > 1) {
                throw new RuntimeException("配送费券一单只能使用一张");
            }
        }

        if (addonCouponCodes != null && !addonCouponCodes.isEmpty()) {
            for (String addonCode : addonCouponCodes) {
                if (addonCode == null || addonCode.trim().isEmpty())
                    continue;

                try {
                    log.info("核销附加券: userId={}, addonCode={}, diningMethod={}, itemChecks.size={}",
                            userId, addonCode.trim(), request.getDiningMethod(), itemChecks.size());

                    // v5.3: 统一传入商品总额和商品列表，由 PointsMallService 根据券类型自动判断
                    com.cozy.member.dto.response.CouponUsageResult addonResult = pointsMallService
                            .useCouponWithResult(userId, addonCode.trim(), totalAmount, itemChecks);

                    if (addonResult != null && addonResult.getDiscountAmount() != null) {
                        String couponType = addonResult.getCouponType();

                        // v5.3.2: 区分商品附加券和配送费券
                        if ("DELIVERY_FEE".equals(couponType)) {
                            // 配送费券只在外卖订单时生效
                            if (!"DELIVERY".equals(request.getDiningMethod())) {
                                throw new RuntimeException("配送费券仅限外卖订单使用");
                            }
                            // 累加配送费折扣
                            deliveryFeeDiscount = deliveryFeeDiscount.add(addonResult.getDiscountAmount());
                            // 更新配送费减免状态
                            if (deliveryFeeDiscount.compareTo(deliveryFee) >= 0) {
                                deliveryFeeWaived = true;
                                deliveryFeeWaivedReason = "COUPON";
                            }
                        } else {
                            // 其他附加券（如加浓缩券SHOT）从商品金额中扣除
                            addonDiscount = addonDiscount.add(addonResult.getDiscountAmount());
                        }

                        if (addonResult.getCouponId() != null) {
                            addonCouponIds.add(addonResult.getCouponId());
                        }

                        log.info("附加券核销成功: addonCode={}, type={}, discount={}",
                                addonCode, couponType, addonResult.getDiscountAmount());
                    }
                } catch (Exception e) {
                    log.error("附加券核销失败: addonCode={}, userId={}, error={}",
                            addonCode, userId, e.getMessage(), e);
                    // v5.3: 附加券核销失败时必须抛出异常，避免用户以为用了券但实际没核销
                    throw new RuntimeException("使用优惠券失败: " + e.getMessage());
                }
            }
        }

        // ============================================================
        // v5.3.2: 实付金额计算（修复附加券抵扣逻辑）
        // 公式：实付 = (商品总额 - 主券折扣 - 商品附加券折扣) + (配送费 - 配送费券折扣)
        // 说明：
        // - 商品总额：包含商品基础价格 + 加料费用（如加浓缩）
        // - 主券折扣：主要优惠券（如半折券、满减券）
        // - 商品附加券：SHOT券（加浓缩券）等，从商品总额中扣除
        // - 配送费券：DELIVERY_FEE券，从配送费中扣除
        // ============================================================
        BigDecimal actualDeliveryFee = deliveryFee.subtract(deliveryFeeDiscount);
        if (actualDeliveryFee.compareTo(BigDecimal.ZERO) < 0) {
            actualDeliveryFee = BigDecimal.ZERO;
        }

        BigDecimal payAmount = totalAmount
                .subtract(discountAmount) // 扣除主券折扣
                .subtract(addonDiscount) // 扣除商品附加券折扣（如加浓缩券）
                .add(actualDeliveryFee); // 加上实际配送费（已扣除配送费券）

        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }

        log.info("订单金额计算: 基础商品金额={}, 加料费用={}, 商品总额={}, 会员折扣={}, 主券折扣={}, 商品附加券折扣={}, 配送费={}, 配送费券折扣={}, 最终实付={}",
                baseTotalAmount, addonsTotalAmount, totalAmount, memberDiscount, discountAmount, addonDiscount,
                deliveryFee, deliveryFeeDiscount, payAmount);

        // 生成取餐码 (外卖订单也要生成)
        LocalDateTime now = LocalDateTime.now();
        String pickupCode = pickupCodeService.generatePickupCode(1L, now);
        LocalDate businessDate = pickupCodeService.calculateBusinessDate(now);

        // ============================================================
        // 计算预估奖励
        // 【核心业务逻辑 v4.2 修正】
        // - 所有类型的优惠券都按实付金额计算 EXP 和 Points
        // - 兑换券抵扣的金额已经从 payAmount 中扣除，无需特殊处理
        // ============================================================
        int estimatedExp = 0;
        int estimatedPoints = 0;
        BigDecimal effectiveRate = BigDecimal.ONE; // 实际生效的积分倍率

        // 只要实付金额 > 0，就按实付金额计算 EXP 和积分
        if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
            estimatedExp = payAmount.setScale(0, RoundingMode.HALF_UP).intValue();
            try {
                // 获取会员详细信息（包含 EXP 和月度消费统计）
                com.cozy.member.dto.response.MemberDTO member = memberService.getMemberByUserId(userId);
                int currentExp = (member != null && member.getExpTotal() != null) ? member.getExpTotal() : 0;
                // v5.3.5: 修复黑卡门槛，与 MemberServiceImpl 保持一致 (9000 EXP)
                final int BLACK_THRESHOLD = 9000;

                if (currentExp >= BLACK_THRESHOLD) {
                    // 1. 已经是黑卡：直接按加速包逻辑计算
                    BigDecimal monthlySpent = BigDecimal.ZERO;
                    String currentMonth = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")
                            .format(java.time.LocalDate.now());

                    // 获取月度加速包剩余额度
                    BigDecimal accelerateRemaining = member.getMonthlyAccelerateRemaining() != null
                            ? member.getMonthlyAccelerateRemaining()
                            : new BigDecimal("300");

                    if (member != null && currentMonth.equals(member.getMonthlySpentMonth())) {
                        monthlySpent = member.getMonthlySpent() != null ? member.getMonthlySpent() : BigDecimal.ZERO;
                    }

                    log.info(
                            "黑卡加速包计算参数: userId={}, currentExp={}, monthlySpentMonth={}, currentMonth={}, monthlySpent={}, accelerateRemaining={}",
                            userId, currentExp, member.getMonthlySpentMonth(), currentMonth, monthlySpent,
                            accelerateRemaining);

                    // 使用加速包剩余额度计算积分（而非 monthlySpent）
                    estimatedPoints = calculateBlackCardPoints(payAmount, accelerateRemaining);

                    effectiveRate = payAmount.compareTo(BigDecimal.ZERO) > 0
                            ? new BigDecimal(estimatedPoints).divide(payAmount, 2, RoundingMode.HALF_UP)
                            : getPointsRate("black");

                    log.info("黑卡加速包预估: userId={}, payAmount={}, points={}, effectiveRate={}", userId, payAmount,
                            estimatedPoints, effectiveRate);

                } else if (currentExp + estimatedExp > BLACK_THRESHOLD) {
                    // 2. 本单触发升级：分段计算
                    int toUpgrade = BLACK_THRESHOLD - currentExp; // 升级所需金额
                    BigDecimal preUpgradeAmount = new BigDecimal(Math.min(toUpgrade, payAmount.intValue()));
                    BigDecimal postUpgradeAmount = payAmount.subtract(preUpgradeAmount);

                    // 升级前的部分（按当前等级倍率计算）
                    int prePoints = preUpgradeAmount.multiply(getPointsRate(memberLevel))
                            .setScale(0, RoundingMode.HALF_UP).intValue();
                    // 升级后的部分（按黑卡 1.70x 加速逻辑计算，此时加速包从满额 300 开始计）
                    int postPoints = calculateBlackCardPoints(postUpgradeAmount, new BigDecimal("300"));

                    estimatedPoints = prePoints + postPoints;
                    effectiveRate = payAmount.compareTo(BigDecimal.ZERO) > 0
                            ? new BigDecimal(estimatedPoints).divide(payAmount, 2, RoundingMode.HALF_UP)
                            : getPointsRate(memberLevel);

                    log.info("跨级订单积分预估: userId={}, pre={}@{}, post={}@加速包, total={}",
                            userId, preUpgradeAmount, getPointsRate(memberLevel), postUpgradeAmount, estimatedPoints);
                } else {
                    // 3. 普通等级：简单倍率
                    BigDecimal baseRate = getPointsRate(memberLevel);
                    estimatedPoints = payAmount.multiply(baseRate).setScale(0, RoundingMode.HALF_UP).intValue();
                    effectiveRate = baseRate;
                }
            } catch (Exception e) {
                log.warn("计算预估奖励失败", e);
            }
        }

        // 创建订单主表
        ShopOrder order = new ShopOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQuantity);
        // v5.8: discountAmount 包含会员折扣 + 优惠券折扣 + 附加券折扣
        order.setDiscountAmount(memberDiscount.add(discountAmount).add(addonDiscount));
        order.setPayAmount(payAmount);
        order.setAppliedCouponId(appliedCouponId);
        order.setExpEarned(estimatedExp); // 【预估】EXP（实际发放在completeOrder）
        order.setPointsEarned(estimatedPoints); // 【预估】积分（实际发放在completeOrder）
        order.setPointsMultiplier(effectiveRate); // 记录实际生效的积分倍率
        order.setRewardsGranted(false); // 标记：奖励尚未发放
        order.setStatus("pending");
        order.setRemark(request.getRemark());
        order.setStoreId(1L);
        order.setBusinessDate(businessDate);
        order.setPickupCode(pickupCode);
        order.setPickupCodeGeneratedAt(now);
        order.setDiningMethod(request.getDiningMethod()); // v5.0: 用餐方式
        // v5.3: 配送费相关字段
        order.setDeliveryFee(deliveryFee);
        order.setDeliveryFeeWaived(deliveryFeeWaived);
        order.setDeliveryFeeWaivedReason(deliveryFeeWaivedReason);
        // v5.0: 保存附加券ID列表用于取消时回滚
        if (!addonCouponIds.isEmpty()) {
            try {
                order.setAppliedAddonCouponIds(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(addonCouponIds));
            } catch (Exception e) {
                log.warn("序列化附加券ID失败: {}", e.getMessage());
            }
        }
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        // 创建订单项
        for (ShopOrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        log.info("订单创建成功: orderNo={}, userId={}, totalAmount={}, items={}",
                order.getOrderNo(), userId, totalAmount, itemsSummary);

        return toOrderDTO(order, orderItems);
    }

    // ==================== 订单查询 ====================

    @Override
    public List<ShopOrderDTO> listUserOrders(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getUserId, userId)
                .orderByDesc(ShopOrder::getCreatedAt);
        List<ShopOrder> orders = orderMapper.selectList(wrapper);

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        // Optimization 1: Batch fetch items
        List<Long> orderIds = orders.stream().map(ShopOrder::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ShopOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(ShopOrderItem::getOrderId, orderIds);
        List<ShopOrderItem> allItems = orderItemMapper.selectList(itemWrapper);

        Map<Long, List<ShopOrderItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(ShopOrderItem::getOrderId));

        // Optimization 2: Pre-fetch member info (once)
        com.cozy.member.dto.response.MemberDTO memberInfo = null;
        try {
            memberInfo = memberService.getMemberByUserId(userId);
        } catch (Exception e) {
            // ignore
        }
        final com.cozy.member.dto.response.MemberDTO finalMember = memberInfo;

        return orders.stream()
                .map(o -> toOrderDTO(o, itemsMap.get(o.getId()), finalMember))
                .collect(Collectors.toList());
    }

    @Override
    public ShopOrderDTO getOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }
        List<ShopOrderItem> items = getOrderItemsByOrderId(orderId);
        return toOrderDTO(order, items);
    }

    @Override
    public ShopOrderDTO getOrderDetail(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }
        List<ShopOrderItem> items = getOrderItemsByOrderId(orderId);
        return toOrderDTO(order, items);
    }

    @Override
    public List<ShopOrderDTO> listAllOrders(String status) {
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ShopOrder::getStatus, status);
        }
        wrapper.orderByDesc(ShopOrder::getCreatedAt);
        List<ShopOrder> orders = orderMapper.selectList(wrapper);

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        // 优化 1：批量获取所有订单项，避免 N+1 查询
        List<Long> orderIds = orders.stream().map(ShopOrder::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ShopOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(ShopOrderItem::getOrderId, orderIds);
        List<ShopOrderItem> allItems = orderItemMapper.selectList(itemWrapper);

        Map<Long, List<ShopOrderItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(ShopOrderItem::getOrderId));

        // 优化 2：批量获取会员信息，避免 N+1 查询
        Set<Long> userIds = orders.stream()
                .map(ShopOrder::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, com.cozy.member.dto.response.MemberDTO> memberMap = new java.util.HashMap<>();
        try {
            if (!userIds.isEmpty()) {
                memberMap = memberService.getMembersByUserIds(userIds);
            }
        } catch (Exception e) {
            log.warn("批量获取会员信息失败，回退到单条查询: {}", e.getMessage());
        }

        // 使用 final 引用供 lambda 使用
        final Map<Long, com.cozy.member.dto.response.MemberDTO> finalMemberMap = memberMap;

        return orders.stream()
                .map(o -> toOrderDTOWithMember(o, itemsMap.get(o.getId()), finalMemberMap.get(o.getUserId())))
                .collect(Collectors.toList());
    }

    // ==================== 订单状态变更 ====================

    @Override
    @Transactional
    public ShopOrderDTO updateOrderStatus(Long orderId, String status) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        order.setStatus(status);
        orderMapper.updateById(order);
        return toOrderDTO(order, null);
    }

    @Override
    public Map<String, Long> getOrderStatusCounts() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ShopOrder> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.select("status", "count(*) as cnt").groupBy("status");
        List<Map<String, Object>> list = orderMapper.selectMaps(wrapper);

        Map<String, Long> result = new java.util.HashMap<>();
        for (Map<String, Object> map : list) {
            String status = (String) map.get("status");
            Number cnt = (Number) map.get("cnt");
            if (status != null && cnt != null) {
                result.put(status, cnt.longValue());
            }
        }
        return result;
    }

    @Override
    @Transactional
    public ShopOrderDTO acceptOrder(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new RuntimeException("只有待处理订单可以接单");
        }

        // 如果还没有取餐码，生成一个
        if (order.getPickupCode() == null || order.getPickupCode().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            String pickupCode = pickupCodeService.generatePickupCode(1L, now);
            LocalDate businessDate = pickupCodeService.calculateBusinessDate(now);
            order.setPickupCode(pickupCode);
            order.setBusinessDate(businessDate);
            order.setPickupCodeGeneratedAt(now);
            order.setStoreId(1L);
        }

        order.setStatus("preparing");
        orderMapper.updateById(order);
        log.info("订单接单: orderId={}, orderNo={}", orderId, order.getOrderNo());
        return toOrderDTO(order, null);
    }

    /**
     * 完成订单 - 幂等发放 EXP/POINT
     */
    @Override
    @Transactional
    public ShopOrderDTO completeOrder(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!"preparing".equals(order.getStatus())) {
            throw new RuntimeException("只有制作中的订单可以完成");
        }

        // 幂等检查：如果已发放奖励，直接返回
        if (Boolean.TRUE.equals(order.getRewardsGranted())) {
            log.info("订单奖励已发放，跳过: orderId={}", orderId);
            order.setStatus("completed");
            orderMapper.updateById(order);
            return toOrderDTO(order, null);
        }

        // 计算 EXP（实付金额，四舍五入取整）
        BigDecimal payAmount = order.getPayAmount() != null ? order.getPayAmount() : order.getTotalAmount();
        int expEarned = payAmount.setScale(0, RoundingMode.HALF_UP).intValue();

        // 获取用户会员等级
        String memberLevel = "basic";
        try {
            var memberInfo = memberService.getMemberInfo(order.getUserId());
            if (memberInfo != null && memberInfo.getMemberLevel() != null) {
                memberLevel = memberInfo.getMemberLevel();
            }
        } catch (Exception e) {
            log.warn("获取会员信息失败，使用默认倍率: userId={}", order.getUserId(), e);
        }

        // 直接使用订单创建时预计算的 POINT（包含精准的跨级和加速逻辑）
        int pointsEarned = (order.getPointsEarned() != null) ? order.getPointsEarned() : 0;

        // v4.2 修正：如果预计算积分为 0 但实付金额 > 0，重新计算积分
        // 这处理了历史订单或者旧版本创建的使用兑换券的订单
        if (pointsEarned <= 0 && payAmount.compareTo(BigDecimal.ZERO) > 0) {
            log.info("订单预计算积分为 0 但有实付金额，重新计算: orderId={}, payAmount={}", orderId, payAmount);
            BigDecimal rate = getPointsRate(memberLevel);
            pointsEarned = payAmount.multiply(rate).setScale(0, RoundingMode.HALF_UP).intValue();
            log.info("重新计算后的积分: orderId={}, points={}, rate={}", orderId, pointsEarned, rate);
        }

        if (pointsEarned > 0) {
            log.info("订单积分结算: orderId={}, points={}", orderId, pointsEarned);
        } else {
            log.info("订单无积分奖励（实付金额为 0）: orderId={}", orderId);
        }
        // 调用会员服务发放奖励
        try {
            // 发放积分（会自动创建 lot）
            memberService.addPointsWithLot(order.getUserId(), pointsEarned, "order_completed",
                    order.getId(), "咖啡订单完成: " + order.getOrderNo());

            // 发放 EXP
            memberService.addExp(order.getUserId(), expEarned, order.getId());

            log.info("订单奖励发放成功: orderId={}, exp={}, points={}, level={}",
                    orderId, expEarned, pointsEarned, memberLevel);
        } catch (Exception e) {
            log.error("订单奖励发放失败: orderId={}", orderId, e);
            // 不抛异常，继续完成订单
        }

        // v4.2: 检测首单并发放奖励 (+300积分)
        // 注意：此时当前订单还未更新为completed，检测历史已完成订单数
        try {
            LambdaQueryWrapper<ShopOrder> firstOrderCheck = new LambdaQueryWrapper<>();
            firstOrderCheck.eq(ShopOrder::getUserId, order.getUserId())
                    .eq(ShopOrder::getStatus, "completed")
                    .ne(ShopOrder::getId, order.getId()); // 排除当前订单
            long historyCompletedCount = orderMapper.selectCount(firstOrderCheck);

            if (historyCompletedCount == 0) {
                // 没有历史已完成订单，当前是第一单
                // v5.3: 新用户首单奖励 (+200积分)
                log.info("新用户完成首单，发放200积分奖励: userId={}", order.getUserId());
                memberService.addPointsWithLot(order.getUserId(), 200, "first_order_bonus",
                        order.getId(), "新用户首单奖励");

                // v5.0: 触发邀请奖励（给邀请人发放买一送一券）
                try {
                    if (userService != null) {
                        boolean granted = userService.grantInviteRewardOnFirstOrder(order.getUserId());
                        if (granted) {
                            log.info("首单邀请奖励发放成功: userId={}", order.getUserId());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("首单邀请奖励发放失败: userId={}, error={}",
                            order.getUserId(), ex.getMessage());
                }
            } else {
                log.debug("非首单,跳过首单奖励: userId={}, historyCount={}",
                        order.getUserId(), historyCompletedCount);
            }
        } catch (Exception e) {
            log.warn("首单检测失败: orderId={}", orderId, e);
        }

        // v4.2: 触发月度任务更新
        // v6.0 修复: 必须先更新订单状态为 completed，再触发月度任务更新
        // 原因: getMonthlyStats 只统计 completed 状态的订单
        // 如果先调用 updateMonthlySpent，此时当前订单尚未 completed，不会被计入统计

        // 1. 先更新订单状态
        order.setStatus("completed");
        order.setExpEarned(expEarned);
        order.setPointsEarned(pointsEarned);
        order.setRewardsGranted(true);
        orderMapper.updateById(order);

        // 2. 再触发月度任务更新（此时当前订单已是 completed 状态）
        // v6.0: 传入当前订单属性，用于精确补偿事务隔离问题
        try {
            if (monthlyTaskService != null && order.getPayAmount() != null
                    && order.getPayAmount().compareTo(BigDecimal.ZERO) > 0) {

                // 判断是否外卖订单
                boolean isDelivery = "DELIVERY".equals(order.getDiningMethod());

                // 判断是否包含新品 - 查询订单项是否包含新品商品
                boolean hasNewProduct = false;
                try {
                    LambdaQueryWrapper<ShopOrderItem> itemWrapper = new LambdaQueryWrapper<>();
                    itemWrapper.eq(ShopOrderItem::getOrderId, order.getId());
                    List<ShopOrderItem> items = orderItemMapper.selectList(itemWrapper);

                    if (!items.isEmpty()) {
                        List<Long> productIds = items.stream()
                                .map(ShopOrderItem::getProductId)
                                .distinct()
                                .collect(Collectors.toList());

                        LambdaQueryWrapper<CoffeeProduct> productWrapper = new LambdaQueryWrapper<>();
                        productWrapper.in(CoffeeProduct::getId, productIds)
                                .eq(CoffeeProduct::getIsNewProduct, true);
                        hasNewProduct = productMapper.selectCount(productWrapper) > 0;
                    }
                } catch (Exception e) {
                    log.warn("检测新品订单失败: orderId={}", orderId, e);
                }

                monthlyTaskService.updateMonthlySpentWithDetails(order.getUserId(), order.getId(),
                        order.getPayAmount(), isDelivery, hasNewProduct);
                log.info("月度任务更新成功: userId={}, orderId={}, amount={}, isDelivery={}, hasNewProduct={}",
                        order.getUserId(), order.getId(), order.getPayAmount(), isDelivery, hasNewProduct);
            }
        } catch (Exception e) {
            log.warn("月度任务更新失败: orderId={}", orderId, e);
        }

        return

        toOrderDTO(order, null);
    }

    @Override
    @Transactional
    public ShopOrderDTO cancelOrder(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if ("completed".equals(order.getStatus()) || "cancelled".equals(order.getStatus())) {
            throw new RuntimeException("该订单无法取消");
        }
        order.setStatus("cancelled");
        orderMapper.updateById(order);

        // 如果使用了优惠券，核销回滚
        if (order.getAppliedCouponId() != null) {
            log.info("订单取消，准备回滚优惠券: orderId={}, couponId={}, userId={}",
                    orderId, order.getAppliedCouponId(), order.getUserId());
            try {
                pointsMallService.rollbackCoupon(order.getAppliedCouponId(), order.getUserId());
                log.info("优惠券回滚成功: orderId={}, couponId={}", orderId, order.getAppliedCouponId());
            } catch (Exception e) {
                log.error("取消订单回滚优惠券失败: orderId={}, couponId={}, error={}",
                        orderId, order.getAppliedCouponId(), e.getMessage(), e);
            }
        } else {
            log.info("订单未使用优惠券，无需回滚: orderId={}", orderId);
        }

        // v5.0: 附加券回滚
        if (order.getAppliedAddonCouponIds() != null && !order.getAppliedAddonCouponIds().isEmpty()) {
            log.info("订单取消，准备回滚附加券: orderId={}, addonCouponIds={}", orderId, order.getAppliedAddonCouponIds());
            try {
                java.util.List<Long> addonCouponIds = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(order.getAppliedAddonCouponIds(),
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Long>>() {
                                });
                for (Long addonCouponId : addonCouponIds) {
                    try {
                        pointsMallService.rollbackCoupon(addonCouponId, order.getUserId());
                        log.info("附加券回滚成功: orderId={}, addonCouponId={}", orderId, addonCouponId);
                    } catch (Exception e) {
                        log.error("附加券回滚失败: orderId={}, addonCouponId={}, error={}",
                                orderId, addonCouponId, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("解析附加券ID失败: orderId={}, error={}", orderId, e.getMessage());
            }
        }

        log.info("订单取消: orderId={}, orderNo={}", orderId, order.getOrderNo());
        return toOrderDTO(order, null);
    }

    @Override
    @Transactional
    public ShopOrderDTO cancelUserOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消该订单");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new RuntimeException("只有待处理的订单才能取消");
        }
        order.setStatus("cancelled");
        orderMapper.updateById(order);

        // 如果使用了优惠券，核销回滚
        if (order.getAppliedCouponId() != null) {
            log.info("用户取消订单，准备回滚优惠券: orderId={}, couponId={}, userId={}",
                    orderId, order.getAppliedCouponId(), userId);
            try {
                pointsMallService.rollbackCoupon(order.getAppliedCouponId(), userId);
                log.info("优惠券回滚成功: orderId={}, couponId={}", orderId, order.getAppliedCouponId());
            } catch (Exception e) {
                log.error("用户取消订单回滚优惠券失败: orderId={}, couponId={}, error={}",
                        orderId, order.getAppliedCouponId(), e.getMessage(), e);
            }
        } else {
            log.info("订单未使用优惠券，无需回滚: orderId={}", orderId);
        }

        // v5.0: 附加券回滚
        if (order.getAppliedAddonCouponIds() != null && !order.getAppliedAddonCouponIds().isEmpty()) {
            log.info("用户取消订单，准备回滚附加券: orderId={}, addonCouponIds={}", orderId, order.getAppliedAddonCouponIds());
            try {
                java.util.List<Long> addonCouponIds = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(order.getAppliedAddonCouponIds(),
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Long>>() {
                                });
                for (Long addonCouponId : addonCouponIds) {
                    try {
                        pointsMallService.rollbackCoupon(addonCouponId, userId);
                        log.info("附加券回滚成功: orderId={}, addonCouponId={}", orderId, addonCouponId);
                    } catch (Exception e) {
                        log.error("附加券回滚失败: orderId={}, addonCouponId={}, error={}",
                                orderId, addonCouponId, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("解析附加券ID失败: orderId={}, error={}", orderId, e.getMessage());
            }
        }

        return toOrderDTO(order, null);
    }

    // ==================== 商品管理 ====================

    @Override
    public List<CoffeeProductDTO> listAllProducts() {
        LambdaQueryWrapper<CoffeeProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(CoffeeProduct::getSortOrder);
        return productMapper.selectList(wrapper).stream()
                .map(this::toProductDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CoffeeProductDTO addProduct(CoffeeProductDTO dto) {
        if (dto == null) {
            throw new RuntimeException("商品信息不能为空");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("商品价格不能为负数");
        }
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new RuntimeException("商品分类不能为空");
        }

        CoffeeProduct product = new CoffeeProduct();
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setPriceMedium(dto.getPriceMedium()); // v5.0
        product.setPriceLarge(dto.getPriceLarge()); // v5.0
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(dto.getCategory().trim());
        product.setStatus("active");
        product.setSortOrder(0);
        product.setIsNewProduct(dto.getIsNewProduct() != null ? dto.getIsNewProduct() : false); // v5.0

        // v5.2: SKU 配置字段
        product.setSizeType(dto.getSizeType() != null ? dto.getSizeType() : "MEDIUM_LARGE");
        product.setSugarType(dto.getSugarType() != null ? dto.getSugarType() : "FREE_CHOICE");
        product.setTempType(dto.getTempType() != null ? dto.getTempType() : "ALL_OK");

        // 手动设置时间戳（修复 MetaObjectHandler 可能未扫码到的问题）
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        productMapper.insert(product);
        invalidateMenuCache();
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public CoffeeProductDTO updateProduct(Long productId, CoffeeProductDTO dto) {
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (dto.getName() != null) {
            if (dto.getName().trim().isEmpty()) {
                throw new RuntimeException("商品名称不能为空");
            }
            product.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null)
            product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) {
            if (dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("商品价格不能为负数");
            }
            product.setPrice(dto.getPrice());
        }
        // v5.0: 中/大杯价格更新（始终设置，允许清除）
        product.setPriceMedium(dto.getPriceMedium());
        product.setPriceLarge(dto.getPriceLarge());
        if (dto.getIsNewProduct() != null)
            product.setIsNewProduct(dto.getIsNewProduct());

        // v5.2: SKU 配置字段
        if (dto.getSizeType() != null)
            product.setSizeType(dto.getSizeType());
        if (dto.getSugarType() != null)
            product.setSugarType(dto.getSugarType());
        if (dto.getTempType() != null)
            product.setTempType(dto.getTempType());

        if (dto.getImageUrl() != null)
            product.setImageUrl(dto.getImageUrl());
        if (dto.getCategory() != null) {
            if (dto.getCategory().trim().isEmpty()) {
                throw new RuntimeException("商品分类不能为空");
            }
            product.setCategory(dto.getCategory().trim());
        }

        // 手动更新时间戳
        product.setUpdatedAt(LocalDateTime.now());

        productMapper.updateById(product);
        invalidateMenuCache();
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        productMapper.deleteById(productId);
        invalidateMenuCache();
    }

    @Override
    @Transactional
    public CoffeeProductDTO toggleProductStatus(Long productId) {
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setStatus("active".equals(product.getStatus()) ? "inactive" : "active");
        productMapper.updateById(product);
        return toProductDTO(product);
    }

    // ==================== 辅助方法 ====================

    private List<ShopOrderItem> getOrderItemsByOrderId(Long orderId) {
        LambdaQueryWrapper<ShopOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrderItem::getOrderId, orderId);
        return orderItemMapper.selectList(wrapper);
    }

    /**
     * 根据会员等级获取积分倍率（从配置）
     * v5.0: basic=1.0, silver=1.1, gold=1.2, diamond=1.3, black=1.5
     * v6.1 会员日: 周五倍率 +0.5x
     */
    private BigDecimal getPointsRate(String level) {
        if (level == null)
            level = "basic";
        BigDecimal baseRate = switch (level) {
            case "silver" -> new BigDecimal("1.1");
            case "gold" -> new BigDecimal("1.2");
            case "diamond" -> new BigDecimal("1.3");
            case "black" -> new BigDecimal("1.5"); // v5.0: 1.35→1.5
            default -> BigDecimal.ONE;
        };

        // v6.1 会员日: 周五积分翻倍 (+0.5x)
        if (isCozyDay()) {
            baseRate = baseRate.add(new BigDecimal("0.5"));
            log.debug("会员日加成生效: 原倍率+0.5, 当前等级={}", level);
        }
        return baseRate;
    }

    /**
     * 判断今天是否为会员日 (Cozy Day)
     * v6.1: 每周五
     */
    private boolean isCozyDay() {
        return java.time.LocalDate.now().getDayOfWeek() == java.time.DayOfWeek.FRIDAY;
    }

    /**
     * 黑卡加速包：加速包剩余额度内 1.70 倍积分，超出部分 1.5 倍（v5.0）
     * v4.2修复：直接使用 accelerateRemaining（加速包剩余额度）而不是 monthlySpent
     * 
     * @param payAmount           本次支付金额
     * @param accelerateRemaining 加速包剩余额度（由 MemberService 维护，每月重置为300）
     */
    private int calculateBlackCardPoints(BigDecimal payAmount, BigDecimal accelerateRemaining) {
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        final BigDecimal ACCELERATE_RATE = new BigDecimal("1.70");
        final BigDecimal NORMAL_RATE = new BigDecimal("1.5"); // v5.0: 1.35→1.5

        // 加速包剩余额度（传入值已由 MemberService 计算好）
        BigDecimal remainingCap = accelerateRemaining != null ? accelerateRemaining.max(BigDecimal.ZERO)
                : new BigDecimal("300");

        // 分段计算
        BigDecimal acceleratedAmount = payAmount.min(remainingCap);
        BigDecimal normalAmount = payAmount.subtract(acceleratedAmount);

        int acceleratedPoints = acceleratedAmount.multiply(ACCELERATE_RATE)
                .setScale(0, RoundingMode.HALF_UP).intValue();
        int normalPoints = normalAmount.multiply(NORMAL_RATE)
                .setScale(0, RoundingMode.HALF_UP).intValue();

        int totalPoints = acceleratedPoints + normalPoints;

        log.info("黑卡加速包计算明细: payAmount={}, accelerateRemaining={}, accelerated={}@{}, normal={}@{}, total={}",
                payAmount, accelerateRemaining, acceleratedAmount, ACCELERATE_RATE, normalAmount, NORMAL_RATE,
                totalPoints);

        return totalPoints;
    }

    /**
     * v5.3: 构建修饰符 JSON (用于 SHOT 券校验)
     * 优先级: addonsJson 中的 EXTRA_SHOT > coffeeStrength=STRONG
     */
    private String buildModifiersJson(OrderItemRequest itemReq) {
        if (itemReq == null) {
            return "{\"extraShot\":false}";
        }

        boolean hasExtraShot = false;

        // 优先解析 addonsJson 中的 EXTRA_SHOT 加料项
        String addonsJson = itemReq.getAddonsJson();
        if (addonsJson != null && !addonsJson.isEmpty()) {
            String lower = addonsJson.toLowerCase();
            // 检查多种可能的标识
            if (lower.contains("extra_shot") ||
                    lower.contains("\"code\":\"extra_shot\"") ||
                    lower.contains("额外浓缩") ||
                    lower.contains("加浓缩") ||
                    lower.contains("加浓")) {
                hasExtraShot = true;
            }
        }

        // 兑容旧逻辑：检查 coffeeStrength 字段
        if (!hasExtraShot && "STRONG".equals(itemReq.getCoffeeStrength())) {
            hasExtraShot = true;
        }

        return "{\"extraShot\":" + hasExtraShot + "}";
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(9000) + 1000;
        return "CF" + timestamp + random;
    }

    private CoffeeProductDTO toProductDTO(CoffeeProduct entity) {
        CoffeeProductDTO dto = new CoffeeProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());
        dto.setPriceMedium(entity.getPriceMedium()); // v5.0
        dto.setPriceLarge(entity.getPriceLarge()); // v5.0
        dto.setImageUrl(entity.getImageUrl());
        dto.setCategory(entity.getCategory());
        dto.setStatus(entity.getStatus());
        dto.setIsNewProduct(entity.getIsNewProduct()); // v5.0

        // v5.2: SKU 配置字段（已启用）
        dto.setSizeType(entity.getSizeType());
        dto.setSugarType(entity.getSugarType());
        dto.setTempType(entity.getTempType());

        return dto;
    }

    private ShopOrderItemDTO toItemDTO(ShopOrderItem entity) {
        ShopOrderItemDTO dto = new ShopOrderItemDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrderId());
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setQuantity(entity.getQuantity());
        dto.setItemAmount(entity.getItemAmount());
        dto.setCupSize(entity.getCupSize());
        dto.setSugarLevel(entity.getSugarLevel());
        dto.setTemperature(entity.getTemperature());
        dto.setCoffeeStrength(entity.getCoffeeStrength());
        dto.setOptionsJson(entity.getOptionsJson());

        // 获取商品图片
        try {
            CoffeeProduct product = productMapper.selectById(entity.getProductId());
            if (product != null) {
                dto.setProductImage(product.getImageUrl());
            }
        } catch (Exception e) {
            log.warn("获取订单项商品图片失败: productId={}", entity.getProductId());
        }

        return dto;
    }

    private ShopOrderDTO toOrderDTO(ShopOrder entity, List<ShopOrderItem> items) {
        return toOrderDTO(entity, items, null);
    }

    private ShopOrderDTO toOrderDTO(ShopOrder entity, List<ShopOrderItem> items,
            com.cozy.member.dto.response.MemberDTO preLoadedMember) {
        ShopOrderDTO dto = new ShopOrderDTO();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setUserId(entity.getUserId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setStoreId(entity.getStoreId());
        dto.setBusinessDate(entity.getBusinessDate());
        dto.setPickupCode(entity.getPickupCode());
        dto.setPickupCodeGeneratedAt(entity.getPickupCodeGeneratedAt());
        dto.setDiningMethod(entity.getDiningMethod());
        dto.setRemark(entity.getRemark());

        // 金额信息
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setDiscountAmount(entity.getDiscountAmount());
        dto.setPayAmount(entity.getPayAmount());
        dto.setTotalQuantity(entity.getTotalQuantity());
        dto.setAppliedCouponId(entity.getAppliedCouponId());

        // 积分信息
        dto.setExpEarned(entity.getExpEarned());
        dto.setPointsEarned(entity.getPointsEarned());
        dto.setRewardsGranted(entity.getRewardsGranted());

        // 用户详细信息 (Nickname, Phone) - Fix for Admin List
        try {
            if (entity.getUserId() != null) {
                com.cozy.member.dto.response.MemberDTO member = preLoadedMember;

                // Only fetch if not provided
                if (member == null) {
                    member = memberService.getMemberByUserId(entity.getUserId());
                }
                if (member != null) {
                    dto.setNickname(member.getNickname());
                    // 优先使用昵称，如果没有则使用用户名
                    dto.setUsername(member.getNickname() != null ? member.getNickname() : "User_" + member.getId());
                    String phone = member.getPhone();
                    if (phone != null && phone.length() >= 7) {
                        dto.setPhoneMasked(phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4));
                    } else {
                        dto.setPhoneMasked(phone);
                    }
                    // 传递会员等级，用于前端展示小图标
                    if (member.getLevel() != null) {
                        // 我们暂用 remark 字段或者新增字段传递 level，这里为了不动 DTO 结构，
                        // 可以考虑加到 DTO, 但目前最快是加到 nickname 后面或者专门字段
                        // 既然要加图标，最好加字段。
                        // 这里假设 ShopOrderDTO 有 memberLevel 字段
                        dto.setMemberLevel(member.getLevel());
                    }
                }
            }
        } catch (Exception e) {
            // ignore fetch error
        }

        // 商品明细
        if (items != null && !items.isEmpty()) {
            dto.setItems(items.stream().map(this::toItemDTO).collect(Collectors.toList()));
            // 生成摘要
            String summary = items.stream()
                    .map(i -> i.getProductName() + " x" + i.getQuantity())
                    .collect(Collectors.joining(", "));
            dto.setItemsSummary(summary);
        } else {
            // 如果没有加载 items，尝试生成摘要
            List<ShopOrderItem> loadedItems = getOrderItemsByOrderId(entity.getId());
            if (!loadedItems.isEmpty()) {
                String summary = loadedItems.stream()
                        .map(i -> i.getProductName() + " x" + i.getQuantity())
                        .collect(Collectors.joining(", "));
                dto.setItemsSummary(summary);
                dto.setItems(loadedItems.stream().map(this::toItemDTO).collect(Collectors.toList()));
            }
        }

        dto.setPointsMultiplier(entity.getPointsMultiplier());

        // v5.3: 配送费信息
        dto.setDeliveryFee(entity.getDeliveryFee());
        dto.setDeliveryFeeWaived(entity.getDeliveryFeeWaived());
        dto.setDeliveryFeeWaivedReason(entity.getDeliveryFeeWaivedReason());

        return dto;
    }

    /**
     * 转换订单DTO（支持预加载的会员信息，避免N+1查询）
     */
    private ShopOrderDTO toOrderDTOWithMember(ShopOrder entity, List<ShopOrderItem> items,
            com.cozy.member.dto.response.MemberDTO member) {
        ShopOrderDTO dto = new ShopOrderDTO();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setUserId(entity.getUserId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setStoreId(entity.getStoreId());
        dto.setBusinessDate(entity.getBusinessDate());
        dto.setPickupCode(entity.getPickupCode());
        dto.setPickupCodeGeneratedAt(entity.getPickupCodeGeneratedAt());
        dto.setDiningMethod(entity.getDiningMethod());
        dto.setRemark(entity.getRemark());

        // 金额信息
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setDiscountAmount(entity.getDiscountAmount());
        dto.setPayAmount(entity.getPayAmount());
        dto.setTotalQuantity(entity.getTotalQuantity());
        dto.setAppliedCouponId(entity.getAppliedCouponId());

        // 积分信息
        dto.setExpEarned(entity.getExpEarned());
        dto.setPointsEarned(entity.getPointsEarned());
        dto.setRewardsGranted(entity.getRewardsGranted());

        // 使用预加载的会员信息（避免N+1查询）
        if (member != null) {
            dto.setNickname(member.getNickname());
            dto.setUsername(member.getNickname() != null ? member.getNickname() : "User_" + member.getId());
            String phone = member.getPhone();
            if (phone != null && phone.length() >= 7) {
                dto.setPhoneMasked(phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4));
            } else {
                dto.setPhoneMasked(phone);
            }
            if (member.getLevel() != null) {
                dto.setMemberLevel(member.getLevel());
            }
        }

        // 商品明细
        if (items != null && !items.isEmpty()) {
            dto.setItems(items.stream().map(this::toItemDTO).collect(Collectors.toList()));
            String summary = items.stream()
                    .map(i -> i.getProductName() + " x" + i.getQuantity())
                    .collect(Collectors.joining(", "));
            dto.setItemsSummary(summary);
        } else {
            List<ShopOrderItem> loadedItems = getOrderItemsByOrderId(entity.getId());
            if (!loadedItems.isEmpty()) {
                String summary = loadedItems.stream()
                        .map(i -> i.getProductName() + " x" + i.getQuantity())
                        .collect(Collectors.joining(", "));
                dto.setItemsSummary(summary);
                dto.setItems(loadedItems.stream().map(this::toItemDTO).collect(Collectors.toList()));
            }
        }

        dto.setPointsMultiplier(entity.getPointsMultiplier());

        // v5.3: 配送费信息
        dto.setDeliveryFee(entity.getDeliveryFee());
        dto.setDeliveryFeeWaived(entity.getDeliveryFeeWaived());
        dto.setDeliveryFeeWaivedReason(entity.getDeliveryFeeWaivedReason());

        return dto;
    }

    @Override
    public com.cozy.order.dto.response.MonthlyStatsDTO getMonthlyStats(Long userId) {
        com.cozy.order.dto.response.MonthlyStatsDTO stats = new com.cozy.order.dto.response.MonthlyStatsDTO();
        if (userId == null)
            return stats;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59)
                .withSecond(59);

        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getUserId, userId)
                .ge(ShopOrder::getCreatedAt, start)
                .le(ShopOrder::getCreatedAt, end);

        List<ShopOrder> orders = orderMapper.selectList(wrapper);
        // v5.3.1 修复: 月度挑战任务统计仅包含已完成订单（排除pending/preparing/cancelled）
        orders = orders.stream().filter(o -> "completed".equals(o.getStatus())).collect(Collectors.toList());

        stats.setOrderCount(orders.size());
        // 晨间唤醒任务：10点前下单的订单（status=completed 已过滤）
        stats.setMorningOrderCount((int) orders.stream()
                .filter(o -> o.getCreatedAt().getHour() < 10)
                .count());

        // v5.0: 统计外卖订单 (diningMethod = DELIVERY)
        stats.setDeliveryOrderCount((int) orders.stream()
                .filter(o -> "DELIVERY".equals(o.getDiningMethod()))
                .count());

        // v5.0: 新品统计 - 查询订单项中包含新品的订单数量
        int newProductOrders = 0;
        if (!orders.isEmpty()) {
            try {
                // 获取所有新品商品ID
                LambdaQueryWrapper<CoffeeProduct> productWrapper = new LambdaQueryWrapper<>();
                productWrapper.eq(CoffeeProduct::getIsNewProduct, true);
                List<Long> newProductIds = productMapper.selectList(productWrapper).stream()
                        .map(CoffeeProduct::getId)
                        .collect(Collectors.toList());

                if (!newProductIds.isEmpty()) {
                    // 获取本月所有订单ID
                    List<Long> orderIds = orders.stream().map(ShopOrder::getId).collect(Collectors.toList());

                    // 查询包含新品的订单项
                    LambdaQueryWrapper<ShopOrderItem> itemWrapper = new LambdaQueryWrapper<>();
                    itemWrapper.in(ShopOrderItem::getOrderId, orderIds)
                            .in(ShopOrderItem::getProductId, newProductIds);
                    List<ShopOrderItem> newProductItems = orderItemMapper.selectList(itemWrapper);

                    // 统计包含新品的订单数（去重）
                    newProductOrders = (int) newProductItems.stream()
                            .map(ShopOrderItem::getOrderId)
                            .distinct()
                            .count();
                }
            } catch (Exception e) {
                log.warn("新品统计失败: {}", e.getMessage());
            }
        }
        stats.setNewProductCount(newProductOrders);

        return stats;
    }
}
