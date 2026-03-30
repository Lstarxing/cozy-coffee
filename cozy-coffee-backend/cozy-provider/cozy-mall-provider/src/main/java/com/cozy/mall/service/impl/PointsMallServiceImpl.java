package com.cozy.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.RedisKeyConstants;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.cozy.mall.entity.PointsOrder;
import com.cozy.mall.entity.PointsOrderFulfillment;
import com.cozy.mall.entity.PointsProduct;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.mapper.PointsOrderFulfillmentMapper;
import com.cozy.mall.mapper.PointsOrderMapper;
import com.cozy.mall.mapper.PointsProductMapper;
import com.cozy.mall.mapper.UserCouponMapper;
import com.cozy.mall.mapper.MonthlyRedemptionMapper;
import com.cozy.mall.entity.MonthlyRedemption;
import com.cozy.member.api.AddressService;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.PointsMallService;
import com.cozy.member.dto.request.RedeemRequest;
import com.cozy.member.dto.response.AddressDTO;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.dto.response.PointsOrderDTO;
import com.cozy.member.dto.response.PointsProductDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.response.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import com.cozy.member.dto.response.UserCouponDTO;
import com.cozy.member.dto.response.CouponUsageResult;

/**
 * 积分商城服务实现 - 独立微服务 (mall-provider)
 * 实现 PointsMallService 接口，数据存储在 cozy_mall 数据库
 * 通过Dubbo RPC调用 member-provider 获取会员信息和收货地址
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class PointsMallServiceImpl implements PointsMallService {

    private static final String EMPTY_CACHE_MARKER = "__NULL__";
    private static final Semaphore MALL_DB_REBUILD_GUARD = new Semaphore(4);
    private static final LongAdder MALL_CACHE_HIT = new LongAdder();
    private static final LongAdder MALL_CACHE_MISS = new LongAdder();
    private static final LongAdder MALL_CACHE_EMPTY_HIT = new LongAdder();
    private static final LongAdder MALL_DEGRADE_FAST_FAIL = new LongAdder();
    private static final AtomicLong MALL_METRIC_SEQ = new AtomicLong();

    private final PointsProductMapper productMapper;
    private final PointsOrderMapper orderMapper;
    private final MonthlyRedemptionMapper monthlyRedemptionMapper;
    private final PointsOrderFulfillmentMapper fulfillmentMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 跨服务调用：会员服务（获取积分、扣减积分）
    @DubboReference(check = false)
    private MemberService memberService;

    // 跨服务调用：地址服务（获取收货地址）
    @DubboReference(check = false)
    private AddressService addressService;
    
    // 跨服务调用：订单服务（查询咖啡商品信息）
    @DubboReference(check = false)
    private com.cozy.order.api.OrderService orderService;

    // 跨服务调用：用户服务（获取用户信息）
    @DubboReference(check = false)
    private UserService userService;

    @Override
    public List<PointsProductDTO> listActiveProducts(Long userId) {
        log.info("获取积分商城商品列表, userId={}", userId);
        List<PointsProductDTO> dtoList = loadActiveProductsWithCache();

        // 如果用户已登录，填充月度限购进度
        if (userId != null && !dtoList.isEmpty()) {
            String currentMonth = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

            // 批量查询当前用户的月度兑换记录
            List<Long> productIds = dtoList.stream().map(PointsProductDTO::getId).collect(Collectors.toList());
            if (!productIds.isEmpty()) {
                LambdaQueryWrapper<MonthlyRedemption> query = new LambdaQueryWrapper<>();
                query.eq(MonthlyRedemption::getUserId, userId)
                        .eq(MonthlyRedemption::getMonth, currentMonth)
                        .in(MonthlyRedemption::getProductId, productIds);

                Map<Long, Integer> redemptionMap = new HashMap<>();
                try {
                    List<MonthlyRedemption> records = monthlyRedemptionMapper.selectList(query);
                    for (MonthlyRedemption r : records) {
                        redemptionMap.put(r.getProductId(), r.getRedeemedCount());
                    }
                } catch (Exception e) {
                    log.warn("查询用户月度兑换记录失败: userId={}", userId, e);
                }

                // 填充到 DTO
                for (PointsProductDTO dto : dtoList) {
                    dto.setCurrentUserMonthlyRedeemed(redemptionMap.getOrDefault(dto.getId(), 0));
                }
            }
        }

        return dtoList;
    }

    private List<PointsProductDTO> loadActiveProductsWithCache() {
        try {
            Object cachedValue = redisTemplate.opsForValue().get(RedisKeyConstants.MALL_PRODUCTS_ACTIVE);
            if (EMPTY_CACHE_MARKER.equals(cachedValue)) {
                MALL_CACHE_EMPTY_HIT.increment();
                logMallCacheMetricsMaybe();
                return Collections.emptyList();
            }
            List<PointsProductDTO> cachedProducts = convertToPointsProductList(cachedValue);
            if (cachedProducts != null) {
                MALL_CACHE_HIT.increment();
                logMallCacheMetricsMaybe();
                return cachedProducts;
            }
        } catch (Exception e) {
            log.warn("读取Redis积分商城商品缓存失败，回退数据库", e);
        }
        MALL_CACHE_MISS.increment();

        String lockToken = UUID.randomUUID().toString();
        boolean locked = tryAcquireRebuildLock(RedisKeyConstants.LOCK_MALL_PRODUCTS_REBUILD, lockToken, 8);
        if (!locked) {
            try {
                TimeUnit.MILLISECONDS.sleep(40L);
                Object retryCache = redisTemplate.opsForValue().get(RedisKeyConstants.MALL_PRODUCTS_ACTIVE);
                if (EMPTY_CACHE_MARKER.equals(retryCache)) {
                    MALL_CACHE_EMPTY_HIT.increment();
                    logMallCacheMetricsMaybe();
                    return Collections.emptyList();
                }
                List<PointsProductDTO> retryCachedProducts = convertToPointsProductList(retryCache);
                if (retryCachedProducts != null) {
                    MALL_CACHE_HIT.increment();
                    logMallCacheMetricsMaybe();
                    return retryCachedProducts;
                }
            } catch (Exception e) {
                log.warn("重建等待后读取Redis积分商城商品缓存失败", e);
            }
        }

        if (!acquireDbRebuildPermit()) {
            MALL_DEGRADE_FAST_FAIL.increment();
            logMallCacheMetricsMaybe();
            return Collections.emptyList();
        }

        try {
            LambdaQueryWrapper<PointsProduct> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PointsProduct::getStatus, "active")
                    .orderByAsc(PointsProduct::getPointsPrice);

            List<PointsProductDTO> dbResult = productMapper.selectList(wrapper).stream()
                    .map(this::toProductDTO)
                    .collect(Collectors.toList());

            try {
                if (dbResult.isEmpty()) {
                        redisTemplate.opsForValue().set(
                            RedisKeyConstants.MALL_PRODUCTS_ACTIVE,
                            EMPTY_CACHE_MARKER,
                            60,
                            TimeUnit.SECONDS);
                } else {
                    long ttlMinutes = 5L + ThreadLocalRandom.current().nextLong(3L);
                        redisTemplate.opsForValue().set(
                            RedisKeyConstants.MALL_PRODUCTS_ACTIVE,
                            dbResult,
                            ttlMinutes,
                            TimeUnit.MINUTES);
                }
            } catch (Exception e) {
                log.warn("写入Redis积分商城商品缓存失败", e);
            }
            logMallCacheMetricsMaybe();
            return dbResult;
        } finally {
            MALL_DB_REBUILD_GUARD.release();
            if (locked) {
                releaseLockSafely(RedisKeyConstants.LOCK_MALL_PRODUCTS_REBUILD, lockToken);
            }
        }
    }

    private boolean acquireDbRebuildPermit() {
        try {
            return MALL_DB_REBUILD_GUARD.tryAcquire(80, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void logMallCacheMetricsMaybe() {
        long seq = MALL_METRIC_SEQ.incrementAndGet();
        if (seq % 200 == 0) {
            log.info("mall-cache-metrics: hit={}, miss={}, emptyHit={}, fastFail={}",
                    MALL_CACHE_HIT.sum(),
                    MALL_CACHE_MISS.sum(),
                    MALL_CACHE_EMPTY_HIT.sum(),
                    MALL_DEGRADE_FAST_FAIL.sum());
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

    @Override
    public PointsProductDTO getProduct(Long id) {
        if (id == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        PointsProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public PointsOrderDTO redeem(Long userId, RedeemRequest request) {
        log.info("用户 {} 发起兑换请求: productId={}", userId, request.getProductId());

        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (request == null || request.getProductId() == null) {
            throw new RuntimeException("请选择商品");
        }
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;

        String lockKey = RedisKeyConstants.lockMallProductStock(request.getProductId());
        String lockToken = UUID.randomUUID().toString();
        Boolean lockOk = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, 8, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(lockOk)) {
            throw new RuntimeException("当前兑换请求较多，请稍后重试");
        }

        PointsProduct product;
        try {
            // 查询商品
            product = productMapper.selectById(request.getProductId());
            if (product == null) {
                throw new RuntimeException("商品不存在");
            }
            if (!"active".equals(product.getStatus())) {
                throw new RuntimeException("商品已下架");
            }
            if (product.getStock() < quantity) {
                throw new RuntimeException("库存不足");
            }

            // 扣减库存
            product.setStock(product.getStock() - quantity);
            productMapper.updateById(product);
            invalidateMallProductsCache();
        } finally {
            releaseLockSafely(lockKey, lockToken);
        }

        // v4.2: 检查月度限购 (基于计数表)
        if (product.getMonthlyLimit() != null && product.getMonthlyLimit() > 0) {
            String currentMonth = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            MonthlyRedemption mr = monthlyRedemptionMapper.selectOne(new LambdaQueryWrapper<MonthlyRedemption>()
                    .eq(MonthlyRedemption::getUserId, userId)
                    .eq(MonthlyRedemption::getProductId, product.getId())
                    .eq(MonthlyRedemption::getMonth, currentMonth));

            int usedCount = (mr != null) ? mr.getRedeemedCount() : 0;
            if (usedCount + quantity > product.getMonthlyLimit()) {
                throw new RuntimeException("该商品每月限兑 " + product.getMonthlyLimit() + " 件，本月已兑换 " + usedCount + " 件");
            }
        }

        // 1. 推断交付方式 (VIRTUAL/PICKUP/DELIVERY)
        String productType = product.getProductType() != null ? product.getProductType() : "PHYSICAL";
        String fulfillmentType = "PICKUP";

        // 优惠券分类商品强制为虚拟发放
        if ("VIRTUAL".equals(productType) || "coupon".equals(product.getCategory())) {
            fulfillmentType = "VIRTUAL";
            productType = "VIRTUAL"; // 同步更新 productType
        } else {
            // 实物商品：优先使用 fulfillmentType，兼容旧的 deliveryType
            String reqFType = request.getFulfillmentType();
            if (reqFType == null) {
                reqFType = "delivery".equals(request.getDeliveryType()) ? "DELIVERY" : "PICKUP";
            }
            fulfillmentType = reqFType.toUpperCase();
        }

        // 2. 校验并获取地址信息
        AddressDTO address = null;
        if ("DELIVERY".equals(fulfillmentType)) {
            if (request.getAddressId() != null) {
                address = addressService.getById(request.getAddressId());
                if (address == null || !address.getUserId().equals(userId)) {
                    throw new RuntimeException("收货地址不存在");
                }
            } else if (request.getReceiverName() == null || request.getReceiverPhone() == null
                    || request.getReceiverAddress() == null) {
                throw new RuntimeException("快递订单必须提供收货信息");
            }
        }

        // 跨服务调用：获取会员信息
        MemberDTO memberDTO = memberService.getMemberByUserId(userId);
        if (memberDTO == null) {
            throw new RuntimeException("会员信息不存在");
        }

        // 计算需要的积分
        int totalCost = calculateCost(product.getPointsPrice(), quantity, memberDTO.getMemberLevel());

        if (memberDTO.getCurrentPoints() < totalCost) {
            throw new RuntimeException("积分不足，当前积分: " + memberDTO.getCurrentPoints() + "，需要: " + totalCost);
        }

        // 3. 创建主订单
        LocalDateTime now = LocalDateTime.now();
        PointsOrder order = new PointsOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setProductImage(product.getImageUrl());
        order.setQuantity(quantity);
        order.setPointsCost(totalCost);
        order.setStatus("pending");
        order.setProductType(productType);
        order.setFulfillmentType(fulfillmentType);
        order.setRemark(request.getRemark());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        // 4. 创建交付子表
        PointsOrderFulfillment fulfillment = new PointsOrderFulfillment();
        fulfillment.setOrderId(order.getId());
        fulfillment.setType(fulfillmentType);
        fulfillment.setCreatedAt(now);
        fulfillment.setUpdatedAt(now);
        if ("DELIVERY".equals(fulfillmentType)) {
            fulfillment.setAddressId(request.getAddressId());
            if (address != null) {
                fulfillment.setReceiverName(address.getReceiverName());
                fulfillment.setReceiverPhone(address.getReceiverPhone());
                fulfillment.setReceiverAddress(buildFullAddress(address));
            } else {
                fulfillment.setReceiverName(request.getReceiverName());
                fulfillment.setReceiverPhone(request.getReceiverPhone());
                fulfillment.setReceiverAddress(request.getReceiverAddress());
            }
        } else if ("PICKUP".equals(fulfillmentType)) {
            fulfillment.setStoreId(request.getStoreId() != null ? request.getStoreId() : 1L);
            // 自提需要联系人信息（用于通知取货）
            if (request.getReceiverName() != null && !request.getReceiverName().isEmpty()) {
                fulfillment.setReceiverName(request.getReceiverName());
                fulfillment.setReceiverPhone(request.getReceiverPhone());
            } else {
                // 未传联系人信息时，从用户信息获取
                try {
                    UserDTO user = userService.getUserById(userId);
                    if (user != null) {
                        fulfillment
                                .setReceiverName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                        fulfillment.setReceiverPhone(user.getPhone());
                    }
                } catch (Exception e) {
                    log.warn("获取用户信息失败，自提订单联系人信息为空: userId={}", userId);
                }
            }
        }
        fulfillmentMapper.insert(fulfillment);

        // 5. 虚拟商品：自动发放并完成订单
        if ("VIRTUAL".equals(fulfillmentType)) {
            String virtualCode = generateVirtualCode();
            fulfillment.setVirtualCode(virtualCode);
            fulfillment.setIssuedAt(now);
            fulfillment.setUpdatedAt(now);
            fulfillmentMapper.updateById(fulfillment);

            order.setStatus("completed");
            order.setCompletedAt(now);
            order.setUpdatedAt(now);
            orderMapper.updateById(order);

            // 如果是券类商品，发放 UserCoupon
            boolean isCoupon = isCouponProduct(product);
            log.info("虚拟商品发放检查: productId={}, name={}, category={}, isCoupon={}", 
                    product.getId(), product.getName(), product.getCategory(), isCoupon);
            
            if (isCoupon) {
                log.info("开始发放优惠券: userId={}, orderId={}, productName={}", userId, order.getId(), product.getName());
                issueCouponToUser(userId, order.getId(), product, now);
                log.info("优惠券发放完成: userId={}, orderId={}", userId, order.getId());
            }

            log.info("虚拟商品自动发放完成: orderNo={}, virtualCode={}", order.getOrderNo(), virtualCode);
        }

        // 6. 自提商品：生成取货码
        if ("PICKUP".equals(fulfillmentType)) {
            String pickupCode = generatePickupCode(now.toLocalDate());
            fulfillment.setPickupCode(pickupCode);
            fulfillment.setUpdatedAt(now);
            fulfillmentMapper.updateById(fulfillment);
            log.info("自提订单取货码生成: orderNo={}, pickupCode={}", order.getOrderNo(), pickupCode);
        }

        // 跨服务调用：FIFO 扣减积分
        boolean consumed = memberService.consumePointsFIFO(userId, totalCost, "redeem", order.getId());
        if (!consumed) {
            throw new RuntimeException("积分扣减失败");
        }

        log.info("兑换成功: orderNo={}, cost={}, type={}", order.getOrderNo(), totalCost, fulfillmentType);

        // v4.2: 更新月度限购计数 (基于计数表)
        if (product.getMonthlyLimit() != null && product.getMonthlyLimit() > 0) {
            String currentMonth = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            monthlyRedemptionMapper.incrementRedemption(userId, product.getId(), currentMonth, quantity);
        }

        return toOrderDTO(order);
    }

    private void releaseLockSafely(String lockKey, String lockToken) {
        try {
            // 若切换到 Redisson，分布式锁可改为 RLock，自动续约、可重入和看门狗机制可进一步降低锁误释放风险。
            String releaseScript = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(releaseScript);
            redisScript.setResultType(Long.class);
            stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockToken);
        } catch (Exception e) {
            log.warn("释放Redis库存锁失败: key={}", lockKey, e);
        }
    }

    private void invalidateMallProductsCache() {
        try {
            redisTemplate.delete(RedisKeyConstants.MALL_PRODUCTS_ACTIVE);
        } catch (Exception e) {
            log.warn("清理Redis积分商城商品缓存失败", e);
        }
    }

    private List<PointsProductDTO> convertToPointsProductList(Object cachedValue) {
        if (!(cachedValue instanceof List<?> rawList)) {
            return null;
        }
        return rawList.stream()
                .map(item -> objectMapper.convertValue(item, PointsProductDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<PointsOrderDTO> listUserOrders(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        LambdaQueryWrapper<PointsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsOrder::getUserId, userId)
                .orderByDesc(PointsOrder::getCreatedAt);
        return orderMapper.selectList(wrapper).stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PointsOrderDTO getOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }
        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public PointsOrderDTO cancelOrder(Long orderId, Long userId) {
        log.info("用户 {} 取消订单: orderId={}", userId, orderId);

        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!"pending".equals(order.getStatus()) && !"processing".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许取消");
        }

        // 更新订单状态
        order.setStatus("cancelled");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 恢复库存
        PointsProduct product = productMapper.selectById(order.getProductId());
        if (product != null) {
            product.setStock(product.getStock() + order.getQuantity());
            productMapper.updateById(product);
            invalidateMallProductsCache();
        }

        // 跨服务调用：返还积分
        memberService.addPoints(userId, order.getPointsCost(), "refund",
                "订单取消退还: " + order.getProductName() + " x" + order.getQuantity());

        log.info("订单取消成功: orderNo={}, refund={}", order.getOrderNo(), order.getPointsCost());
        return toOrderDTO(order);
    }

    // ==================== 用户券包实现 ====================

    @Override
    public List<UserCouponDTO> getUserCoupons(Long userId, String status) {
        log.info("获取用户券包: userId={}, status={}", userId, status);
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);

        LocalDateTime now = LocalDateTime.now();

        if (status != null && !status.isEmpty()) {
            String s = status.toUpperCase();
            if ("ISSUED".equals(s)) {
                // ISSUED: must be ISSUED status AND not expired
                wrapper.eq(UserCoupon::getStatus, "ISSUED")
                        .gt(UserCoupon::getExpiresAt, now);
            } else if ("EXPIRED".equals(s)) {
                // EXPIRED: explicitly EXPIRED OR (ISSUED but expired time passed)
                wrapper.and(w -> w.eq(UserCoupon::getStatus, "EXPIRED")
                        .or(w2 -> w2.eq(UserCoupon::getStatus, "ISSUED")
                                .lt(UserCoupon::getExpiresAt, now)));
            } else {
                // USED and others
                wrapper.eq(UserCoupon::getStatus, s);
            }
        }
        wrapper.orderByDesc(UserCoupon::getCreatedAt);

        return userCouponMapper.selectList(wrapper).stream()
                .map(this::toCouponDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserCouponDTO> getAvailableCoupons(Long userId, BigDecimal orderAmount) {
        return getAvailableCoupons(userId, orderAmount, null);
    }

    @Override
    public List<UserCouponDTO> getAvailableCoupons(Long userId, BigDecimal orderAmount,
            List<com.cozy.member.dto.request.ItemCheckDTO> items) {
        log.info("获取可用券(含商品校验): userId={}, orderAmount={}, itemsCount={}",
                userId, orderAmount, items != null ? items.size() : 0);
        if (userId == null) {
            return new ArrayList<>();
        }

        LocalDateTime now = LocalDateTime.now();

        // 查询所有 ISSUED 状态且未过期的券
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, "ISSUED")
                .gt(UserCoupon::getExpiresAt, now)
                .orderByAsc(UserCoupon::getExpiresAt);

        List<UserCoupon> coupons = userCouponMapper.selectList(wrapper);
        List<UserCouponDTO> result = new ArrayList<>();

        for (UserCoupon coupon : coupons) {
            UserCouponDTO dto = toCouponDTO(coupon);
            String type = coupon.getCouponType();
            String ruleJson = coupon.getRuleJson();

            // 1. 检查门槛金额 (FULL_REDUCE, DISCOUNT 等可能都有门槛)
            int minAmount = parseMinAmount(ruleJson);
            if (orderAmount != null && orderAmount.intValue() < minAmount) {
                dto.setAvailable(false);
                dto.setUnavailableReason("订单金额不满" + minAmount + "元");
                result.add(dto);
                continue;
            }

            // 2. 检查商品范围 (DRINK_ONLY / CAKE_ONLY)
            if (ruleJson != null && ruleJson.contains("\"scope\":\"DRINK_ONLY\"")) {
                boolean hasDrink = false;
                if (items != null) {
                    for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                        if (isDrink(item.getCategory())) {
                            hasDrink = true;
                            break;
                        }
                    }
                }

                if (!hasDrink) {
                    dto.setAvailable(false);
                    dto.setUnavailableReason("仅限饮品使用");
                    result.add(dto);
                    continue;
                }
            }
            
            // v5.3.6: 检查商品范围 (CAKE_ONLY) - 烘焙甜品专用券
            if (ruleJson != null && ruleJson.contains("\"scope\":\"CAKE_ONLY\"")) {
                boolean hasBakery = false;
                if (items != null) {
                    for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                        if (isBakery(item.getCategory())) {
                            hasBakery = true;
                            break;
                        }
                    }
                }

                if (!hasBakery) {
                    dto.setAvailable(false);
                    dto.setUnavailableReason("仅限烘焙甜品使用");
                    result.add(dto);
                    continue;
                }
            }

            // 3. 检查特定商品兑换 (EXCHANGE)
            if ("EXCHANGE".equals(type)) {
                long linkedProductId = parseLongValue(ruleJson, "linkedProductId");
                if (linkedProductId > 0) {
                    boolean foundProduct = false;
                    if (items != null) {
                        for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                            if (item.getProductId() != null && item.getProductId() == linkedProductId) {
                                foundProduct = true;
                                break;
                            }
                        }
                    }
                    if (!foundProduct) {
                        dto.setAvailable(false);
                        dto.setUnavailableReason("订单中不含此券指定的商品");
                        result.add(dto);
                        continue;
                    }
                }
            }

            // 到这里说明可用
            dto.setAvailable(true);
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public BigDecimal useCoupon(Long userId, String couponCode, BigDecimal orderAmount) {
        // 对于旧调用，不传商品ID列表
        CouponUsageResult result = useCouponWithResult(userId, couponCode, orderAmount,
                (List<com.cozy.member.dto.request.ItemCheckDTO>) null);
        return result.getDiscountAmount();
    }

    @Override
    @Transactional
    public CouponUsageResult useCouponWithResult(Long userId, String couponCode, BigDecimal orderAmount,
            List<com.cozy.member.dto.request.ItemCheckDTO> items) {
        log.info("使用券: userId={}, couponCode={}, orderAmount={}", userId, couponCode, orderAmount);

        if (userId == null || couponCode == null || couponCode.isEmpty()) {
            throw new RuntimeException("参数不能为空");
        }

        // 查询券
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponCode, couponCode)
                .eq(UserCoupon::getStatus, "ISSUED");
        UserCoupon coupon = userCouponMapper.selectOne(wrapper);

        if (coupon == null) {
            throw new RuntimeException("券不存在或已使用");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("券已过期");
        }

        // 计算折扣金额
        BigDecimal discountAmount = calculateCouponDiscount(coupon, orderAmount, items);

        // 更新券状态
        LocalDateTime now = LocalDateTime.now();
        coupon.setStatus("USED");
        coupon.setUsedAt(now);
        userCouponMapper.updateById(coupon);

        Long linkedProductId = parseLongValue(coupon.getRuleJson(), "linkedProductId");
        boolean exclusive = coupon.getRuleJson() != null && coupon.getRuleJson().contains("\"exclusive\":true");
        
        // v5.7: 解析免费加料次数（尊享通兑券专属）
        int freeAddonCount = parseValue(coupon.getRuleJson(), "freeAddon");

        log.info("券核销成功: couponCode={}, type={}, discount={}, exclusive={}, freeAddon={}",
                couponCode, coupon.getCouponType(), discountAmount, exclusive, freeAddonCount);

        return new CouponUsageResult(discountAmount, coupon.getCouponType(), coupon.getId(), linkedProductId,
                exclusive, freeAddonCount);
    }

    private BigDecimal calculateCouponDiscount(UserCoupon coupon, BigDecimal orderAmount,
            List<com.cozy.member.dto.request.ItemCheckDTO> items) {
        if (orderAmount == null)
            orderAmount = BigDecimal.ZERO;

        String type = coupon.getCouponType();
        String ruleJson = coupon.getRuleJson();
        int value = parseValue(ruleJson, "value");
        int minOrderAmount = parseValue(ruleJson, "minOrderAmount");
        long linkedProductId = parseLongValue(ruleJson, "linkedProductId");

        if ("EXCHANGE".equals(type)) {
            // v5.7: 兑换券/免单券 - 支持 SKU 限制
            // rule_json 支持的字段:
            //   - maxDiscount: 最高抵扣金额（不设置=无上限）
            //   - value: 兼容旧字段，同 maxDiscount
            //   - skuLimit: "STANDARD_ONLY" | "ALL" (杯型限制)
            //   - categoryBlocklist: ["signature", "soe"] (品类黑名单)
            //   - linkedProductId: 指定商品ID (通兑券为0)
            
            // v5.7: 优先读取 maxDiscount，其次 value，都没有则无上限（9999）
            int maxDiscountFromRule = parseValue(ruleJson, "maxDiscount");
            if (maxDiscountFromRule <= 0) {
                maxDiscountFromRule = parseValue(ruleJson, "value");
            }
            // 无上限时使用极大值，而非硬编码40
            BigDecimal maxDiscount = maxDiscountFromRule > 0 ? new BigDecimal(maxDiscountFromRule) : new BigDecimal("9999");
        
            // v5.3: 解析 SKU 限制
            String cleanJson = ruleJson != null ? ruleJson.replace(" ", "").replace("\n", "").replace("\t", "") : "";
            boolean standardOnly = cleanJson.contains("\"skuLimit\":\"STANDARD_ONLY\"");
            
            // v5.3.6: 精确判断品类黑名单 - 只检查 categoryBlocklist 数组内容
            // 注意：不能用 contains("特调") 全文匹配，因为 description 字段可能包含"含特调"等描述文字
            boolean blockSoe = false;
            boolean blockSignature = false;
            if (cleanJson.contains("\"categoryBlocklist\"")) {
                // 提取 categoryBlocklist 数组部分进行精确匹配
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
                // 指定商品兑换券：仅限标准杯
                if (items != null) {
                    for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                        if (item.getProductId() != null && item.getProductId() == linkedProductId) {
                            // v6.1: 指定商品兑换券仅限标准杯
                            String cupSize = item.getCupSize() != null ? item.getCupSize().toUpperCase() : "STANDARD";
                            if (!cupSize.equals("STANDARD") && !cupSize.equals("MEDIUM")) {
                                throw new RuntimeException("此兑换券仅限标准杯使用，请调整杯型后再试");
                            }
                            
                            // v6.1: 兑换券只抵扣标准杯基础价格，升杯和加料费用由用户额外支付
                            try {
                                com.cozy.order.dto.response.CoffeeProductDTO product = orderService.getProduct(linkedProductId);
                                if (product != null && product.getPrice() != null) {
                                    BigDecimal standardPrice = product.getPrice(); // 标准杯基础价格
                                    log.info("指定商品兑换券：productId={}, 标准杯价格={}, 实际商品价格={}", 
                                            linkedProductId, standardPrice, item.getPrice());
                                    return standardPrice.min(maxDiscount);
                                }
                            } catch (Exception e) {
                                log.warn("查询商品标准价格失败，回退到使用商品实际价格: productId={}", linkedProductId, e);
                            }
                            
                            // 兜底：如果查询失败，使用商品实际价格
                            return item.getPrice().min(maxDiscount);
                        }
                    }
                    throw new RuntimeException("此兑换券仅限指定商品使用");
                }
                return BigDecimal.ZERO;
            } else {

                // 通兑券：自动匹配价格最高的符合条件商品
                if (items == null || items.isEmpty())
                    return BigDecimal.ZERO;

                BigDecimal maxPrice = BigDecimal.ZERO;
                String matchedProductInfo = null;
                
                // v5.3.6: 检测是否为烘焙甜品免单券
                String couponName = (coupon.getDisplayTitle() != null ? coupon.getDisplayTitle() : "").toLowerCase();
                boolean isCakeCoupon = cleanJson.contains("\"scope\":\"CAKE_ONLY\"") ||
                                       couponName.contains("烘培") ||
                                       couponName.contains("烘焙") ||
                                       couponName.contains("甜品") ||
                                       couponName.contains("蛋糕");
                
                for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                    if (isCakeCoupon) {
                        // 烘焙甜品免单券：仅匹配烘焙商品
                        if (!isBakery(item.getCategory())) {
                            continue;
                        }
                    } else {
                        // 饮品免单券：仅匹配饮品 (排除面包甜点)
                        if (!isDrink(item.getCategory())) {
                            continue;
                        }
                    }
                    
                    // v5.3.4: 品类黑名单检查
                    if (item.getCategory() != null) {
                        String cat = item.getCategory().toLowerCase();
                        
                        // 检查是否需要排除SOE/手冲（检查 category 字段）
                        if (blockSoe) {
                            boolean isSoe = cat.contains("soe") || cat.contains("手冲") || cat.contains("pour-over") || cat.contains("pour_over");
                            if (isSoe) {
                                log.info("免单券排除SOE/手冲品类: category={}", cat);
                                continue;
                            }
                        }
                        
                        // 检查是否需要排除特调（排除SOE后再检查特调）
                        if (blockSignature && (cat.contains("signature") || cat.contains("特调") || cat.contains("季节限定"))) {
                            log.info("免单券排除特调品类: category={}", cat);
                            continue;
                        }
                    }
                    
                    // v5.3: 杯型限制检查 (STANDARD_ONLY)
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
                        throw new RuntimeException("此免单券仅限标准杯饮品使用，请调整杯型后再试");
                    }
                    if (blockSoe) {
                        throw new RuntimeException("此免单券不适用于SOE/手冲类产品");
                    }
                    // v5.3.6: 根据券类型显示正确的错误信息
                    if (isCakeCoupon) {
                        throw new RuntimeException("此券仅限烘焙甜品使用");
                    }
                    throw new RuntimeException("通兑券仅限饮品使用");
                }
                
                log.info("免单券匹配最高价商品: {}, maxPrice={}, discount={}, isCakeCoupon={}", matchedProductInfo, maxPrice, maxPrice.min(maxDiscount), isCakeCoupon);
                return maxPrice.min(maxDiscount);
            }

        } else if ("DISCOUNT".equals(type)) {
            // v2.1: 按行业规范重构折扣计算

            // 1. 获取折扣百分比，兼容多种格式
            int discountPercent = value;
            if (discountPercent <= 0) {
                // 尝试解析浮点数 discountRate（如 0.5 表示 5折）
                double floatRate = parseDoubleValue(ruleJson, "discountRate");
                if (floatRate > 0 && floatRate < 1) {
                    discountPercent = (int) (floatRate * 100); // 0.5 -> 50
                } else if (floatRate >= 1 && floatRate <= 10) {
                    discountPercent = (int) (floatRate * 10); // 5 -> 50
                } else {
                    discountPercent = (int) floatRate;
                }
            }

            if (discountPercent <= 0) {
                log.warn("折扣券无效: discountPercent={}, ruleJson={}", discountPercent, ruleJson);
                return BigDecimal.ZERO;
            }

            // 2. 解析配置
            // 2. 解析配置 (v5.3: 增强 JSON 解析健壮性，移除空格后再匹配)
            String cleanJson = ruleJson != null ? ruleJson.replace(" ", "").replace("\n", "").replace("\t", "") : "";
            boolean isDrinkOnly = cleanJson.contains("\"scope\":\"DRINK_ONLY\"");
            boolean isCakeOnly = cleanJson.contains("\"scope\":\"CAKE_ONLY\""); // v5.3.4: 烘培甜品专用
            boolean isSingleItem = cleanJson.contains("\"limit\":\"SINGLE_ITEM\"");
            int maxDiscountAmount = parseValue(ruleJson, "maxDiscountAmount");

            // 3. 确定折扣基数
            BigDecimal baseAmount = orderAmount;

            if (isCakeOnly) {
                // v5.3.4: 蛋糕5折券 - 仅作用于单个最贵烘培甜品
                BigDecimal maxBakeryPrice = BigDecimal.ZERO;

                if (items != null) {
                    for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                        if (isBakery(item.getCategory())) {
                            if (item.getPrice().compareTo(maxBakeryPrice) > 0) {
                                maxBakeryPrice = item.getPrice();
                            }
                        }
                    }
                }

                if (maxBakeryPrice.equals(BigDecimal.ZERO)) {
                    throw new RuntimeException("此券仅限烘培甜品使用，订单中无烘培商品");
                }

                baseAmount = maxBakeryPrice;
                log.info("蛋糕5折券(CAKE_ONLY): 仅作用于最贵烘培甜品={}", maxBakeryPrice);

            } else if (isDrinkOnly || isSingleItem) {
                // 筛选饮品
                BigDecimal maxDrinkPrice = BigDecimal.ZERO;
                BigDecimal drinkTotal = BigDecimal.ZERO;

                if (items != null) {
                    for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                        if (isDrink(item.getCategory())) {
                            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                            drinkTotal = drinkTotal.add(item.getPrice().multiply(BigDecimal.valueOf(qty)));
                            if (item.getPrice().compareTo(maxDrinkPrice) > 0) {
                                maxDrinkPrice = item.getPrice();
                            }
                        }
                    }
                }

                if (maxDrinkPrice.equals(BigDecimal.ZERO)) {
                    throw new RuntimeException("此券仅限饮品使用，订单中无饮品");
                }

                if (isSingleItem) {
                    // 行业规范：大额折扣只作用于单杯最贵饮品
                    baseAmount = maxDrinkPrice;
                    log.info("折扣券(SINGLE_ITEM): 仅作用于最贵饮品={}", maxDrinkPrice);
                } else {
                    // 仅限饮品但可作用于多杯
                    baseAmount = drinkTotal;
                    log.info("折扣券(DRINK_ONLY): 作用于饮品总额={}", drinkTotal);
                }
            }

            // 4. 计算折扣金额
            BigDecimal rate = new BigDecimal(discountPercent).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            BigDecimal discount = baseAmount.multiply(BigDecimal.ONE.subtract(rate));
            discount = discount.setScale(2, RoundingMode.HALF_UP);

            // 5. 封顶控制
            if (maxDiscountAmount > 0) {
                BigDecimal maxCap = new BigDecimal(maxDiscountAmount);
                if (discount.compareTo(maxCap) > 0) {
                    log.info("折扣券封顶: 原折扣={}, 封顶={}", discount, maxCap);
                    discount = maxCap;
                }
            }

            return discount;

        } else if ("FULL_REDUCE".equals(type)) {
            // 满减券
            if (minOrderAmount > 0 && orderAmount.compareTo(new BigDecimal(minOrderAmount)) < 0) {
                throw new RuntimeException("订单金额未满 " + minOrderAmount + " 元");
            }
            return new BigDecimal(value);

        } else if ("BOGO".equals(type)) {
            // 买一送一：低价免单
            if (items == null || items.isEmpty()) {
                throw new RuntimeException("无法获取商品信息");
            }

            List<BigDecimal> drinkPrices = new ArrayList<>();
            for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                if (isDrink(item.getCategory())) {
                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                    for (int i = 0; i < qty; i++) {
                        drinkPrices.add(item.getPrice());
                    }
                }
            }

            if (drinkPrices.size() < 2) {
                throw new RuntimeException("买一送一券需要至少2杯饮品");
            }

            Collections.sort(drinkPrices); // 升序：p1 <= p2 <= p3 <= p4

            // v5.7: 从 ruleJson 读取封顶金额，默认40
            int maxDiscountFromRule = parseValue(ruleJson, "maxDiscount");
            BigDecimal maxPerCup = maxDiscountFromRule > 0 ? new BigDecimal(maxDiscountFromRule) : new BigDecimal("40");
            BigDecimal cheapestPrice = drinkPrices.get(0);
            BigDecimal discount = cheapestPrice.min(maxPerCup);
            
            log.info("BOGO券抵扣: 最便宜饮品={}，封顶={}, 实际抵扣={}", cheapestPrice, maxPerCup, discount);
            return discount;
        } else if ("SHOT".equals(type)) {
            // v5.3 加浓缩券：前置条件检查
            boolean hasExtraShot = false;

            if (items != null && !items.isEmpty()) {
                for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                    // 检查 modifiersJson 字段
                    String modifiers = item.getModifiersJson();
                    if (modifiers != null &&
                            (modifiers.contains("\"extraShot\":true") ||
                                    modifiers.toLowerCase().contains("extra_shot") ||
                                    modifiers.contains("加浓"))) {
                        hasExtraShot = true;
                        break;
                    }
                }
            }

            if (!hasExtraShot) {
                throw new RuntimeException("此券仅在点单时选择了【加浓缩】选项后可用");
            }

            int shotValue = parseValue(ruleJson, "value");
            if (shotValue == 0)
                shotValue = 5;
            return new BigDecimal(shotValue);

        } else if ("DELIVERY_FEE".equals(type)) {
            // v5.0 配送费抵扣券
            int maxFeeDiscount = parseValue(ruleJson, "value");
            if (maxFeeDiscount == 0)
                maxFeeDiscount = 3; // v5.3: 统一配送费3元
            return new BigDecimal(maxFeeDiscount);

        } else if ("NEW_PRODUCT_HALF".equals(type)) {
            // v5.3.4 新品半价券：自动选择价格最高的新品，最高封顶20元
            if (items == null || items.isEmpty()) {
                throw new RuntimeException("此券仅适用于新品饮品，请先添加新品商品");
            }
            
            // 查找订单中价格最高的新品（智能选择最优惠方案）
            com.cozy.member.dto.request.ItemCheckDTO highestNewProduct = null;
            BigDecimal highestPrice = BigDecimal.ZERO;
            
            for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                if (Boolean.TRUE.equals(item.getIsNewProduct())) {
                    BigDecimal itemPrice = item.getPrice();
                    if (highestNewProduct == null || itemPrice.compareTo(highestPrice) > 0) {
                        highestNewProduct = item;
                        highestPrice = itemPrice;
                    }
                }
            }
            
            if (highestNewProduct == null) {
                throw new RuntimeException("此券仅限新品饮品使用，当前订单中没有新品商品");
            }
            
            // 计算半价优惠，封顶20元
            BigDecimal halfPrice = highestPrice.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal maxDiscount = new BigDecimal("20");
            return halfPrice.compareTo(maxDiscount) > 0 ? maxDiscount : halfPrice;

        } else if ("NEW_PRODUCT_FREE".equals(type)) {
            // v5.3.4 新品免单券：自动选择价格最高的新品，最高封顶40元
            if (items == null || items.isEmpty()) {
                throw new RuntimeException("此券仅适用于新品饮品，请先添加新品商品");
            }
            
            // 查找订单中价格最高的新品（智能选择最优惠方案）
            com.cozy.member.dto.request.ItemCheckDTO highestNewProduct = null;
            BigDecimal highestPrice = BigDecimal.ZERO;
            
            for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                if (Boolean.TRUE.equals(item.getIsNewProduct())) {
                    BigDecimal itemPrice = item.getPrice();
                    if (highestNewProduct == null || itemPrice.compareTo(highestPrice) > 0) {
                        highestNewProduct = item;
                        highestPrice = itemPrice;
                    }
                }
            }
            
            if (highestNewProduct == null) {
                throw new RuntimeException("此券仅限新品饮品使用，当前订单中没有新品商品");
            }
            
            // 计算免单优惠，封顶40元
            BigDecimal maxDiscount = new BigDecimal("40");
            return highestPrice.compareTo(maxDiscount) > 0 ? maxDiscount : highestPrice;

        } else if ("CAKE_HALF".equals(type) || (ruleJson != null && ruleJson.contains("CAKE_ONLY"))) {
            // v5.3.4 蛋糕5折券：仅限烘培甜品，自动选择最高价商品，最高优惠¥50
            if (items == null || items.isEmpty()) {
                throw new RuntimeException("此券仅适用于烘培甜品，请先添加烘培商品");
            }
            
            // 查找订单中价格最高的烘培甜品（智能选择最优惠方案）
            com.cozy.member.dto.request.ItemCheckDTO highestBakeryProduct = null;
            BigDecimal highestPrice = BigDecimal.ZERO;
            
            for (com.cozy.member.dto.request.ItemCheckDTO item : items) {
                if (isBakery(item.getCategory())) {
                    BigDecimal itemPrice = item.getPrice();
                    if (highestBakeryProduct == null || itemPrice.compareTo(highestPrice) > 0) {
                        highestBakeryProduct = item;
                        highestPrice = itemPrice;
                    }
                }
            }
            
            if (highestBakeryProduct == null) {
                throw new RuntimeException("此券仅限烘培甜品使用，当前订单中没有烘培商品");
            }
            
            // 计算5折优惠（50% off），封顶50元
            BigDecimal halfPrice = highestPrice.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal maxDiscount = new BigDecimal("50");
            return halfPrice.compareTo(maxDiscount) > 0 ? maxDiscount : halfPrice;
        }

        return BigDecimal.ZERO;
    }

    private boolean isBakery(String category) {
        if (category == null) return false;
        String c = category.toLowerCase();
        return c.contains("bakery") || c.contains("dessert") || c.contains("cake") || c.contains("food");
    }

    private boolean isDrink(String category) {
        if (category == null)
            return true;
        String c = category.toLowerCase();
        return !c.contains("bakery") && !c.contains("dessert") && !c.contains("food");
    }

    /**
     * 解析规则 JSON 中的 value
     * 改进版：处理冒号后的空格
     */
    private int parseValue(String ruleJson, String key) {
        if (ruleJson == null || key == null)
            return 0;
        try {
            // 查找 "key": 或 "key" : （处理空格）
            String search = "\"" + key + "\"";
            int idx = ruleJson.indexOf(search);
            if (idx >= 0) {
                int colonPos = idx + search.length();
                // 跳过空格找到冒号
                while (colonPos < ruleJson.length() &&
                        (ruleJson.charAt(colonPos) == ' ' || ruleJson.charAt(colonPos) == '\t')) {
                    colonPos++;
                }
                // 确认是冒号
                if (colonPos < ruleJson.length() && ruleJson.charAt(colonPos) == ':') {
                    colonPos++; // 跳过冒号
                    // 跳过冒号后的空格
                    while (colonPos < ruleJson.length() &&
                            (ruleJson.charAt(colonPos) == ' ' || ruleJson.charAt(colonPos) == '\t')) {
                        colonPos++;
                    }
                    // 读取数字（包括小数点）
                    int start = colonPos;
                    int end = start;
                    while (end < ruleJson.length() &&
                            (Character.isDigit(ruleJson.charAt(end)) || ruleJson.charAt(end) == '.')) {
                        end++;
                    }
                    if (end > start) {
                        String valStr = ruleJson.substring(start, end);
                        // 如果包含小数点，转换为整数（如 8.5 -> 8）
                        double doubleVal = Double.parseDouble(valStr);
                        int result = (int) doubleVal;
                        log.debug("解析JSON成功: key={}, value={}", key, result);
                        return result;
                    }
                }
            }
            log.debug("JSON解析跳过（未找到key）: key={}", key);
        } catch (Exception e) {
            log.error("JSON解析异常: key={}, json={}", key, ruleJson, e);
        }
        return 0;
    }

    /**
     * 解析规则 JSON 中的 long 值（用于 linkedProductId）
     * 改进版：处理冒号后的空格
     */
    private long parseLongValue(String ruleJson, String key) {
        if (ruleJson == null || key == null)
            return 0;
        try {
            // 查找 "key": 或 "key" : （处理空格）
            String search = "\"" + key + "\"";
            int idx = ruleJson.indexOf(search);
            if (idx >= 0) {
                int colonPos = idx + search.length();
                // 跳过空格找到冒号
                while (colonPos < ruleJson.length() &&
                        (ruleJson.charAt(colonPos) == ' ' || ruleJson.charAt(colonPos) == '\t')) {
                    colonPos++;
                }
                // 确认是冒号
                if (colonPos < ruleJson.length() && ruleJson.charAt(colonPos) == ':') {
                    colonPos++; // 跳过冒号
                    // 跳过冒号后的空格
                    while (colonPos < ruleJson.length() &&
                            (ruleJson.charAt(colonPos) == ' ' || ruleJson.charAt(colonPos) == '\t')) {
                        colonPos++;
                    }
                    // 读取数字
                    int start = colonPos;
                    int end = start;
                    while (end < ruleJson.length() && Character.isDigit(ruleJson.charAt(end))) {
                        end++;
                    }
                    if (end > start) {
                        String valStr = ruleJson.substring(start, end);
                        long result = Long.parseLong(valStr);
                        log.debug("解析JSON成功: key={}, value={}", key, result);
                        return result;
                    }
                }
            }
            log.debug("JSON解析跳过（未找到key）: key={}", key);
        } catch (Exception e) {
            log.error("JSON解析异常: key={}, json={}", key, ruleJson, e);
            return 0;
        }
        return 0;
    }

    /**
     * 解析规则 JSON 中的 minOrderAmount（满减门槛）
     * 注意：这里必须与 issueCouponToUser 方法中写入的 key 一致
     */
    private int parseMinAmount(String ruleJson) {
        return parseValue(ruleJson, "minOrderAmount");
    }

    /**
     * v2.0: 解析浮点数值（用于 discountRate 等）
     */
    private double parseDoubleValue(String ruleJson, String key) {
        if (ruleJson == null || key == null)
            return 0.0;
        try {
            String searchKey = "\"" + key + "\":";
            int keyIndex = ruleJson.indexOf(searchKey);
            if (keyIndex >= 0) {
                int colonPos = keyIndex + searchKey.length();
                // 跳过空格
                while (colonPos < ruleJson.length() &&
                        (ruleJson.charAt(colonPos) == ' ' || ruleJson.charAt(colonPos) == '\t')) {
                    colonPos++;
                }
                // 读取数字
                int start = colonPos;
                int end = start;
                while (end < ruleJson.length() &&
                        (Character.isDigit(ruleJson.charAt(end)) || ruleJson.charAt(end) == '.'
                                || ruleJson.charAt(end) == '-')) {
                    end++;
                }
                if (end > start) {
                    return Double.parseDouble(ruleJson.substring(start, end));
                }
            }
        } catch (Exception e) {
            log.debug("解析浮点数失败: key={}, error={}", key, e.getMessage());
        }
        return 0.0;
    }

    /**
     * UserCoupon -> UserCouponDTO
     */
    private UserCouponDTO toCouponDTO(UserCoupon entity) {
        UserCouponDTO dto = new UserCouponDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setCouponCode(entity.getCouponCode());
        dto.setCouponType(entity.getCouponType());
        dto.setRuleJson(entity.getRuleJson());
        dto.setStatus(entity.getStatus());
        dto.setIssuedAt(entity.getIssuedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setUsedAt(entity.getUsedAt());

        // 解析规则 - v2.0: 透传原始数值，不做任何转换
        int val = parseValue(entity.getRuleJson(), "value");
        dto.setValue(val);
        dto.setMinAmount(parseMinAmount(entity.getRuleJson()));

        // 解析商品名
        String ruleJson = entity.getRuleJson();
        if (ruleJson != null && ruleJson.contains("\"productName\":\"")) {
            int start = ruleJson.indexOf("\"productName\":\"") + 15;
            int end = ruleJson.indexOf("\"", start);
            if (end > start) {
                dto.setProductName(ruleJson.substring(start, end));
                dto.setTitle(dto.getProductName());
            }
        }

        // v5.3.2: 优先使用数据库中存储的 displayTitle 和 displaySubTitle
        // 如果没有，则从 ruleJson 动态生成（兼容旧数据）
        String couponType = entity.getCouponType();
        String displayTitle = entity.getDisplayTitle();
        String displaySubTitle = entity.getDisplaySubTitle();
        String labelColor = "#8B4513"; // 默认咖啡色
        
        // 如果数据库中没有 displayTitle，则动态生成
        boolean needGenerate = (displayTitle == null || displayTitle.isEmpty());

        if (needGenerate && "DISCOUNT".equals(couponType)) {
            // 折扣券：value=50 表示 50% off (5折)
            int discountRate = val;
            if (discountRate <= 0) {
                // 尝试解析 discountRate 字段（可能是浮点数如 0.5）
                double floatRate = parseDoubleValue(ruleJson, "discountRate");
                if (floatRate > 0 && floatRate < 1) {
                    // 0.5 -> 50%
                    discountRate = (int) (floatRate * 100);
                } else if (floatRate >= 1 && floatRate <= 10) {
                    // 5 -> 50%（兼容旧格式：5折写成5）
                    discountRate = (int) (floatRate * 10);
                } else {
                    discountRate = (int) floatRate;
                }
            }
            // v5.3: 修复折扣显示 - 使用浮点除法避免丢失小数
            double displayDiscount = discountRate >= 10 ? discountRate / 10.0 : discountRate;
            // 格式化：去除末尾的.0 (5.0 -> 5, 8.8 -> 8.8)
            String discountStr = displayDiscount % 1 == 0
                    ? String.valueOf((int) displayDiscount)
                    : String.format("%.1f", displayDiscount);
            displayTitle = discountStr + "折";

            // 解析 scope 和 maxDiscountAmount
            boolean isDrinkOnly = ruleJson != null && ruleJson.contains("\"scope\":\"DRINK_ONLY\"");
            boolean isCakeOnly = ruleJson != null && ruleJson.contains("\"scope\":\"CAKE_ONLY\""); // v5.3.6: 烘焙甜品折扣券
            int maxDiscount = parseValue(ruleJson, "maxDiscountAmount");

            if (isCakeOnly) {
                displaySubTitle = "限烘焙甜品";
                if (maxDiscount > 0) {
                    displaySubTitle += " | 最高抵¥" + maxDiscount;
                }
                labelColor = "#F5A623"; // 橙黄色烘焙券
            } else if (isDrinkOnly) {
                displaySubTitle = "限饮品";
                if (maxDiscount > 0) {
                    displaySubTitle += " | 最高抵¥" + maxDiscount;
                }
                labelColor = "#FF6B35"; // 橙红色高亮新人券
            }

            // 检查是否为新用户券
            if (entity.getCouponCode() != null && entity.getCouponCode().contains("NEW_USER")) {
                labelColor = "#FF6B35";
            }

        } else if (needGenerate && "FULL_REDUCE".equals(couponType)) {
            // 满减券
            int threshold = dto.getMinAmount() != null ? dto.getMinAmount() : parseValue(ruleJson, "thresholdAmount");
            int reduce = val > 0 ? val : parseValue(ruleJson, "reduceAmount");
            displayTitle = "¥" + reduce;
            displaySubTitle = threshold > 0 ? "满" + threshold + "可用" : "无门槛";
            labelColor = "#52C41A"; // 绿色

        } else if (needGenerate && "EXCHANGE".equals(couponType)) {
            // 兑换券
            displayTitle = "免单";
            int maxDeduct = parseValue(ruleJson, "maxDeductAmount");
            if (maxDeduct == 0)
                maxDeduct = parseValue(ruleJson, "maxDiscount");
            if (maxDeduct == 0)
                maxDeduct = 40;
            displaySubTitle = "最高抵¥" + maxDeduct;
            if (dto.getProductName() != null && !dto.getProductName().isEmpty()) {
                displayTitle = dto.getProductName();
            }
            labelColor = "#722ED1"; // 紫色

        } else if (needGenerate && "BOGO".equals(couponType)) {
            // 买一送一
            displayTitle = "买一送一";
            displaySubTitle = "第二杯免费";
            labelColor = "#EB2F96"; // 粉色

        } else if (needGenerate && "DELIVERY_FEE".equals(couponType)) {
            // 配送费抵扣
            int maxFee = val > 0 ? val : 3;
            displayTitle = "免运费";
            displaySubTitle = "最高抵¥" + maxFee;
            labelColor = "#13C2C2"; // 青色

        } else if (needGenerate && "SHOT".equals(couponType)) {
            // 加浓缩券
            displayTitle = "+1 Shot";
            displaySubTitle = "额外浓缩";
            labelColor = "#FA8C16"; // 橙色
        }

        dto.setDisplayTitle(displayTitle);
        dto.setDisplaySubTitle(displaySubTitle);
        dto.setLabelColor(labelColor);

        // 兼容旧字段
        if (dto.getDesc() == null || dto.getDesc().isEmpty()) {
            dto.setDesc(displaySubTitle);
        }

        // 可用性判断
        if ("ISSUED".equals(entity.getStatus())) {
            if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
                dto.setAvailable(false);
                dto.setUnavailableReason("已过期");
            } else {
                dto.setAvailable(true);
            }
        } else {
            dto.setAvailable(false);
            dto.setUnavailableReason("USED".equals(entity.getStatus()) ? "已使用" : "已失效");
        }

        return dto;
    }

    /**
     * 根据会员等级计算积分消耗
     * v5.0: black 0.85, diamond 0.90, gold 0.95, silver 0.98
     */
    private int calculateCost(int basePrice, int quantity, String memberLevel) {
        double discount = 1.0;
        if ("black".equals(memberLevel)) {
            discount = 0.85;
        } else if ("diamond".equals(memberLevel)) {
            discount = 0.90;
        } else if ("gold".equals(memberLevel)) {
            discount = 0.95; // v5.0: 0.90→0.95
        } else if ("silver".equals(memberLevel)) {
            discount = 0.98; // v5.0: 0.95→0.98
        }
        return (int) Math.ceil(basePrice * quantity * discount);
    }

    private String buildFullAddress(AddressDTO address) {
        StringBuilder sb = new StringBuilder();
        if (address.getProvince() != null)
            sb.append(address.getProvince());
        if (address.getCity() != null)
            sb.append(address.getCity());
        if (address.getDistrict() != null)
            sb.append(address.getDistrict());
        if (address.getDetailAddress() != null)
            sb.append(address.getDetailAddress());
        return sb.toString();
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06X", new Random().nextInt(0xFFFFFF));
        return "PM" + timestamp + random;
    }

    /**
     * 生成虚拟券码（8位随机字母数字）
     */
    private String generateVirtualCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 生成自提码（日期前缀 + 3位递增序号）
     */
    private String generatePickupCode(LocalDate date) {
        // 格式：MMDD + 3位序号，例如 1225001
        String datePrefix = date.format(DateTimeFormatter.ofPattern("MMdd"));
        // 简化实现：使用当前毫秒的后3位作为序号
        int seq = (int) (System.currentTimeMillis() % 1000);
        return datePrefix + String.format("%03d", seq);
    }

    /**
     * 判断是否为券类商品（EXCHANGE/DISCOUNT/FULL_REDUCE）
     */
    /**
     * 判断是否为优惠券商品（v5.9 修正：使用 category 字段而非名称判断）
     */
    private boolean isCouponProduct(PointsProduct product) {
        // 优先使用 category 字段判断
        if ("coupon".equals(product.getCategory())) {
            return true;
        }
        // 降级兼容：通过名称判断
        String name = product.getName();
        if (name == null) {
            return false;
        }
        return name.contains("券") || name.contains("优惠") || name.contains("折扣") || name.contains("满减");
    }

    /**
     * 发放券类商品到用户券包
     * 从商品模板读取券配置，而非从名称推断
     */
    /**
     * v6.0 积分兑换券发放 - 复用系统发放逻辑
     * 直接调用公共方法 issueCouponToUser(userId, couponType, uniqueKey, ...) 确保格式统一
     */
    private void issueCouponToUser(Long userId, Long orderId, PointsProduct product, LocalDateTime now) {
        // 1. 从商品配置读取券类型
        String couponType = product.getCouponType();
        if (couponType == null || couponType.isEmpty()) {
            couponType = "EXCHANGE"; // 旧商品默认兑换券
        }
        
        // 2. 生成唯一键（用于幂等性）
        String uniqueKey = "REDEEM_" + orderId + "_" + System.currentTimeMillis();
        
        // 3. 获取有效期
        int validDays = (product.getValidDays() != null && product.getValidDays() > 0) 
            ? product.getValidDays() : 14;
        
        // 4. 根据商品的券类型映射到系统发放的 couponType 字符串
        // 并调用公共方法复用逻辑（保证 displayTitle, displaySubTitle, ruleJson 格式一致）
        String systemCouponType;
        double discountAmount = 0;
        double minAmount = 0;
        
        switch (couponType) {
            case "BOGO":
                // 买一赠一券 - T5_BOGO 逻辑
                systemCouponType = "BOGO";
                discountAmount = 40; // 赠品限额40元
                break;
                
            case "EXCHANGE":
                // 兑换券 - 区分单商品兑换券和全场通兑券
                if (product.getLinkedProductId() != null && product.getLinkedProductId() > 0) {
                    // 有关联商品ID：单商品兑换券（抵消标准杯价格，升杯加料需补差价）
                    systemCouponType = "EXCHANGE_" + product.getLinkedProductId(); // 带商品ID
                    discountAmount = 0; // 不设置最高抵扣金额
                } else {
                    // 无关联商品：全场通兑免单券（不限杯型，限额40元）
                    systemCouponType = "FREE_DRINK";
                    discountAmount = product.getFaceValue() != null ? product.getFaceValue() : 40;
                }
                break;
                
            case "DISCOUNT":
                // 折扣券（如8折、5折等）
                // couponValue 存储折扣率（如 80 表示8折，50 表示5折）
                int discountRate = product.getCouponValue() != null ? product.getCouponValue() : 85;
                
                // v6.0 折扣券限定逻辑：
                // 7折以下（<=70）：限单商品（SINGLE_ITEM）
                // 8折以上（>=80）：全场通用
                if (discountRate <= 70) {
                    // 低折扣券：限单商品
                    systemCouponType = "DISCOUNT_SINGLE";
                    discountAmount = discountRate;
                } else {
                    // 高折扣券：全场通用
                    systemCouponType = "DISCOUNT";
                    discountAmount = discountRate;
                }
                break;
                
            case "FULL_REDUCE":
                // 满减/代金券
                systemCouponType = "FULL_REDUCE";
                discountAmount = product.getCouponValue() != null ? product.getCouponValue() : 5;
                minAmount = product.getMinOrderAmount() != null ? product.getMinOrderAmount() : 0;
                break;
                
            case "DELIVERY_FEE":
                // 配送费抵扣券
                systemCouponType = "DELIVERY_FEE";
                discountAmount = 6;
                break;
                
            case "SHOT":
                // 加浓缩券
                systemCouponType = "SHOT";
                discountAmount = 5;
                break;
                
            default:
                // 其他未识别类型，默认为兑换券
                log.warn("未识别的券类型: {}, 默认使用 EXCHANGE", couponType);
                systemCouponType = "EXCHANGE";
                discountAmount = product.getFaceValue() != null ? product.getFaceValue() : 30;
                break;
        }
        
        log.info("积分兑换券映射: productType={} -> systemType={}, discountAmount={}, validDays={}", 
                couponType, systemCouponType, discountAmount, validDays);
        
        // 5. 调用公共方法发放券（复用系统发放逻辑）
        issueCouponToUser(userId, systemCouponType, uniqueKey, minAmount, discountAmount, validDays);
        
        // 6. 更新关联的积分订单ID（公共方法无法设置 sourcePointsOrderId）
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponCode, uniqueKey);
        UserCoupon issued = userCouponMapper.selectOne(wrapper);
        if (issued != null) {
            issued.setSourcePointsOrderId(orderId);
            userCouponMapper.updateById(issued);
            log.info("✅ 积分兑换券发放成功: userId={}, couponCode={}, type={}, orderId={}", 
                    userId, uniqueKey, systemCouponType, orderId);
        }
    }

    private PointsProductDTO toProductDTO(PointsProduct entity) {
        PointsProductDTO dto = new PointsProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImageUrl());
        dto.setPointsPrice(entity.getPointsPrice());
        dto.setOriginalPrice(entity.getOriginalPrice());
        dto.setStock(entity.getStock());
        dto.setStatus(entity.getStatus());
        dto.setCategory(entity.getCategory());
        dto.setProductType(entity.getProductType() != null ? entity.getProductType() : "PHYSICAL");
        // 优惠券配置字段
        dto.setCouponType(entity.getCouponType());
        dto.setCouponValue(entity.getCouponValue());
        dto.setFaceValue(entity.getFaceValue());
        dto.setMinOrderAmount(entity.getMinOrderAmount());
        dto.setLinkedProductId(entity.getLinkedProductId());
        // v4.2 新增
        dto.setMonthlyLimit(entity.getMonthlyLimit());
        dto.setValidDays(entity.getValidDays());
        // linkedProductName 需要查询咖啡商品表获取，暂时由前端处理
        return dto;
    }

    private PointsOrderDTO toOrderDTO(PointsOrder entity) {
        PointsOrderDTO dto = new PointsOrderDTO();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setUserId(entity.getUserId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCompletedAt(entity.getCompletedAt());

        // 商品信息
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setProductImage(entity.getProductImage());
        dto.setPointsCost(entity.getPointsCost());
        dto.setQuantity(entity.getQuantity());

        // 商品类型与交付方式
        dto.setProductType(entity.getProductType() != null ? entity.getProductType() : "PHYSICAL");
        dto.setFulfillmentType(entity.getFulfillmentType());

        // 备注
        dto.setRemark(entity.getRemark());

        // 交付详情从 fulfillment 子表获取（列表场景可选，详情场景必须）
        PointsOrderFulfillment fulfillment = fulfillmentMapper.selectById(entity.getId());
        if (fulfillment != null) {
            dto.setStoreId(fulfillment.getStoreId());
            dto.setReceiverName(fulfillment.getReceiverName());
            dto.setReceiverPhone(fulfillment.getReceiverPhone());
            dto.setReceiverAddress(fulfillment.getReceiverAddress());
            dto.setShippingCompany(fulfillment.getShippingCompany());
            dto.setTrackingNumber(fulfillment.getTrackingNumber());
            dto.setShippedAt(fulfillment.getShippedAt());
            dto.setVirtualCode(fulfillment.getVirtualCode());
            dto.setIssuedAt(fulfillment.getIssuedAt());
            dto.setPickupCode(fulfillment.getPickupCode());
        }

        // 填充用户信息 (昵称 + 脱敏手机号)
        try {
            UserDTO user = userService.getUserById(entity.getUserId());
            if (user != null) {
                dto.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                dto.setUsername(user.getUsername());
                // 脱敏手机号
                if (user.getPhone() != null && user.getPhone().length() >= 11) {
                    dto.setPhoneMasked(user.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
                }
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败: userId={}, error={}", entity.getUserId(), e.getMessage());
        }

        return dto;
    }

    /**
     * 转换订单DTO（支持预加载的用户信息，避免N+1查询）
     */
    private PointsOrderDTO toOrderDTOWithUser(PointsOrder entity, UserDTO user) {
        PointsOrderDTO dto = new PointsOrderDTO();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setUserId(entity.getUserId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCompletedAt(entity.getCompletedAt());

        // 商品信息
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setProductImage(entity.getProductImage());
        dto.setPointsCost(entity.getPointsCost());
        dto.setQuantity(entity.getQuantity());

        // 商品类型与交付方式
        dto.setProductType(entity.getProductType() != null ? entity.getProductType() : "PHYSICAL");
        dto.setFulfillmentType(entity.getFulfillmentType());

        // 备注
        dto.setRemark(entity.getRemark());

        // 交付详情
        PointsOrderFulfillment fulfillment = fulfillmentMapper.selectById(entity.getId());
        if (fulfillment != null) {
            dto.setStoreId(fulfillment.getStoreId());
            dto.setReceiverName(fulfillment.getReceiverName());
            dto.setReceiverPhone(fulfillment.getReceiverPhone());
            dto.setReceiverAddress(fulfillment.getReceiverAddress());
            dto.setShippingCompany(fulfillment.getShippingCompany());
            dto.setTrackingNumber(fulfillment.getTrackingNumber());
            dto.setShippedAt(fulfillment.getShippedAt());
            dto.setVirtualCode(fulfillment.getVirtualCode());
            dto.setIssuedAt(fulfillment.getIssuedAt());
            dto.setPickupCode(fulfillment.getPickupCode());
        }

        // 使用预加载的用户信息（避免N+1查询）
        if (user != null) {
            dto.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
            dto.setUsername(user.getUsername());
            if (user.getPhone() != null && user.getPhone().length() >= 11) {
                dto.setPhoneMasked(user.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
            }
        }

        return dto;
    }

    // ==================== 管理端方法 ====================

    @Override
    public List<PointsOrderDTO> listAllOrders(String status) {
        log.info("管理端获取兑换订单列表: status={}", status);
        LambdaQueryWrapper<PointsOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PointsOrder::getStatus, status);
        }
        wrapper.orderByDesc(PointsOrder::getCreatedAt);
        List<PointsOrder> orders = orderMapper.selectList(wrapper);

        // 优化：批量获取用户信息，避免 N+1 查询
        Set<Long> userIds = orders.stream()
                .map(PointsOrder::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, UserDTO> userMap = new HashMap<>();
        try {
            if (userService != null && !userIds.isEmpty()) {
                for (Long userId : userIds) {
                    try {
                        UserDTO user = userService.getUserById(userId);
                        if (user != null) {
                            userMap.put(userId, user);
                        }
                    } catch (Exception e) {
                        // ignore single user fetch failure
                    }
                }
            }
        } catch (Exception e) {
            log.warn("批量获取用户信息失败: {}", e.getMessage());
        }

        final Map<Long, UserDTO> finalUserMap = userMap;
        return orders.stream()
                .map(o -> toOrderDTOWithUser(o, finalUserMap.get(o.getUserId())))
                .collect(Collectors.toList());
    }

    @Override
    public PointsOrderDTO getRedemptionDetail(Long orderId) {
        log.info("管理端获取兑换订单详情: orderId={}", orderId);
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }

        PointsOrderDTO dto = toOrderDTO(order);

        // P1 阶段：尝试从 fulfillment 子表读取额外信息（如 storeId）并覆盖主表字段（切读预演）
        PointsOrderFulfillment f = fulfillmentMapper.selectById(orderId);
        if (f != null) {
            dto.setFulfillmentType(f.getType());
            dto.setStoreId(f.getStoreId());
            // 如果子表有数据，以子表为准（P2 切读逻辑预埋）
            if (f.getReceiverName() != null)
                dto.setReceiverName(f.getReceiverName());
            if (f.getReceiverPhone() != null)
                dto.setReceiverPhone(f.getReceiverPhone());
            if (f.getReceiverAddress() != null)
                dto.setReceiverAddress(f.getReceiverAddress());
            if (f.getShippingCompany() != null)
                dto.setShippingCompany(f.getShippingCompany());
            if (f.getTrackingNumber() != null)
                dto.setTrackingNumber(f.getTrackingNumber());
            if (f.getShippedAt() != null)
                dto.setShippedAt(f.getShippedAt());
            if (f.getVirtualCode() != null)
                dto.setVirtualCode(f.getVirtualCode());
            if (f.getIssuedAt() != null)
                dto.setIssuedAt(f.getIssuedAt());
            if (f.getPickupCode() != null)
                dto.setPickupCode(f.getPickupCode());
        }

        return dto;
    }

    @Override
    @Transactional
    public PointsOrderDTO updateOrderStatus(Long orderId, String status) {
        log.info("管理端更新订单状态: orderId={}, status={}", orderId, status);
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 更新状态与完成时间
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        if ("completed".equals(status)) {
            order.setCompletedAt(LocalDateTime.now());
        }
        orderMapper.updateById(order);

        // P1 双写同步：更新交付子表
        PointsOrderFulfillment f = fulfillmentMapper.selectById(orderId);
        if (f != null) {
            // VIRTUAL 类型发放即完成
            if ("VIRTUAL".equals(f.getType()) && "completed".equals(status)) {
                if (f.getIssuedAt() == null)
                    f.setIssuedAt(LocalDateTime.now());
            }
            fulfillmentMapper.updateById(f);
        }

        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public PointsOrderDTO updateShipping(Long orderId, String company, String trackingNo) {
        log.info("管理端更新物流信息: orderId={}, company={}, trackingNo={}", orderId, company, trackingNo);
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus("shipped");
        order.setUpdatedAt(now);
        orderMapper.updateById(order);

        // 更新交付子表的物流信息
        PointsOrderFulfillment f = fulfillmentMapper.selectById(orderId);
        if (f != null) {
            f.setShippingCompany(company);
            f.setTrackingNumber(trackingNo);
            f.setShippedAt(now);
            fulfillmentMapper.updateById(f);
        }

        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public PointsOrderDTO confirmReceipt(Long orderId, Long userId) {
        log.info("用户确认收货: orderId={}, userId={}", orderId, userId);
        if (orderId == null || userId == null) {
            throw new RuntimeException("参数不能为空");
        }

        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!"shipped".equals(order.getStatus())) {
            throw new RuntimeException("只有已发货的订单可以确认收货");
        }

        PointsOrderFulfillment f = fulfillmentMapper.selectById(orderId);
        if (f == null || !"DELIVERY".equals(f.getType())) {
            throw new RuntimeException("只有快递订单可以确认收货");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus("completed");
        order.setCompletedAt(now);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);

        log.info("用户确认收货成功: orderNo={}", order.getOrderNo());
        return toOrderDTO(order);
    }

    // ==================== 积分商品管理（管理端）====================

    @Override
    public List<PointsProductDTO> listAllProducts() {
        log.info("管理端获取所有积分商品");
        LambdaQueryWrapper<PointsProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PointsProduct::getId);
        return productMapper.selectList(wrapper).stream()
                .map(this::toProductDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PointsProductDTO addProduct(PointsProductDTO dto) {
        log.info("管理端添加积分商品: {}", dto.getName());
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (dto.getPointsPrice() == null || dto.getPointsPrice() <= 0) {
            throw new RuntimeException("积分价格必须大于0");
        }

        PointsProduct product = new PointsProduct();
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setImageUrl(dto.getImageUrl());
        product.setPointsPrice(dto.getPointsPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setStock(dto.getStock() != null ? dto.getStock() : 0);
        product.setCategory(dto.getCategory());
        product.setStatus("active");

        // 优惠券配置字段
        product.setCouponType(dto.getCouponType());
        product.setCouponValue(dto.getCouponValue());
        product.setFaceValue(dto.getFaceValue());
        product.setMinOrderAmount(dto.getMinOrderAmount());
        product.setLinkedProductId(dto.getLinkedProductId());

        // v4.2 通用配置字段
        product.setMonthlyLimit(dto.getMonthlyLimit());
        product.setValidDays(dto.getValidDays());

        // 手动设置时间戳（修复 MetaObjectHandler 可能未生效的问题）
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        productMapper.insert(product);
        invalidateMallProductsCache();
        log.info("积分商品添加成功: id={}, couponType={}, linkedProductId={}",
                product.getId(), product.getCouponType(), product.getLinkedProductId());
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public PointsProductDTO updateProduct(Long productId, PointsProductDTO dto) {
        log.info("管理端更新积分商品: id={}", productId);
        PointsProduct product = productMapper.selectById(productId);
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
        if (dto.getImageUrl() != null)
            product.setImageUrl(dto.getImageUrl());
        if (dto.getPointsPrice() != null) {
            if (dto.getPointsPrice() <= 0) {
                throw new RuntimeException("积分价格必须大于0");
            }
            product.setPointsPrice(dto.getPointsPrice());
        }
        if (dto.getOriginalPrice() != null)
            product.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getStock() != null)
            product.setStock(dto.getStock());
        if (dto.getCategory() != null)
            product.setCategory(dto.getCategory());

        // 优惠券配置字段（支持更新）
        if (dto.getCouponType() != null)
            product.setCouponType(dto.getCouponType());
        if (dto.getCouponValue() != null)
            product.setCouponValue(dto.getCouponValue());
        if (dto.getFaceValue() != null)
            product.setFaceValue(dto.getFaceValue());
        if (dto.getMinOrderAmount() != null)
            product.setMinOrderAmount(dto.getMinOrderAmount());
        if (dto.getLinkedProductId() != null)
            product.setLinkedProductId(dto.getLinkedProductId());

        // v4.2 通用配置字段
        if (dto.getMonthlyLimit() != null)
            product.setMonthlyLimit(dto.getMonthlyLimit());
        if (dto.getValidDays() != null)
            product.setValidDays(dto.getValidDays());

        // 手动设置更新时间戳
        product.setUpdatedAt(LocalDateTime.now());

        productMapper.updateById(product);
        invalidateMallProductsCache();
        log.info("积分商品更新成功: id={}, couponType={}, linkedProductId={}",
                productId, product.getCouponType(), product.getLinkedProductId());
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        log.info("管理端删除积分商品: id={}", productId);
        PointsProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        productMapper.deleteById(productId);
        invalidateMallProductsCache();
    }

    @Override
    @Transactional
    public PointsProductDTO toggleProductStatus(Long productId) {
        log.info("管理端切换积分商品状态: id={}", productId);
        PointsProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setStatus("active".equals(product.getStatus()) ? "inactive" : "active");
        productMapper.updateById(product);
        invalidateMallProductsCache();
        log.info("商品状态已切换: id={}, newStatus={}", productId, product.getStatus());
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        log.info("管理端删除兑换订单: id={}", orderId);
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        // 删除订单主表
        orderMapper.deleteById(orderId);
        // 删除履约子表
        fulfillmentMapper.deleteById(orderId);
        log.info("订单及其履约记录已删除: id={}", orderId);
    }

    @Override
    @Transactional
    public void rollbackCoupon(Long couponId, Long userId) {
        log.info("回滚优惠券: couponId={}, userId={}", couponId, userId);
        if (couponId == null)
            return;

        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null) {
            log.warn("回滚优惠券失败，券不存在: id={}", couponId);
            return;
        }

        if (!coupon.getUserId().equals(userId)) {
            log.warn("回滚优惠券失败，用户不匹配: couponUserId={}, requestUserId={}", coupon.getUserId(), userId);
            return;
        }

        if (!"USED".equals(coupon.getStatus())) {
            log.warn("针对非使用状态的券无需回滚: status={}", coupon.getStatus());
            return;
        }

        // 还原状态
        coupon.setStatus("ISSUED");
        coupon.setUsedAt(null);
        userCouponMapper.updateById(coupon);
        log.info("优惠券已归还为 ISSUED 状态: couponId={}", couponId);
    }

    /**
     * v5.0: 发放优惠券给用户（用于签到奖励、邀请奖励等场景）
     * 幂等性：通过 uniqueKey 检查是否已发放
     * 
     * 支持的券类型:
     * - FULL_REDUCE: 满减券 (需要 minAmount 和 discountAmount)
     * - BOGO: 买一送一券 (第二杯免费，最高抵扣 discountAmount)
     * - EXCHANGE: 兑换券/免单券 (最高抵扣 discountAmount)
     * - DISCOUNT: 折扣券 (discountAmount 表示折扣率，如 0.5 = 5折)
     */
    @Override
    @Transactional
    public void issueCouponToUser(Long userId, String couponType, String uniqueKey,
            double minAmount, double discountAmount, int validDays) {
        // 检查是否已发放（幂等性）
        LambdaQueryWrapper<UserCoupon> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponCode, uniqueKey);
        if (userCouponMapper.selectCount(checkWrapper) > 0) {
            log.info("券已发放过，跳过: userId={}, uniqueKey={}", userId, uniqueKey);
            return;
        }

        // 创建优惠券
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setCouponCode(uniqueKey);
        coupon.setStatus("ISSUED");
        coupon.setIssuedAt(LocalDateTime.now());
        coupon.setExpiresAt(LocalDateTime.now().plusDays(validDays));
        coupon.setCreatedAt(LocalDateTime.now());

        // 根据券类型设置不同的 couponType 和 ruleJson
        // v5.3: 统一添加 productName 字段供前端展示
        String actualType;
        String ruleJson;

        if (couponType != null && couponType.contains("BOGO")) {
            // 买一送一券：第二杯免费（最高抵扣 maxDiscount）
            actualType = "BOGO";
            String name = couponType.contains("BIRTHDAY") ? "生日买一赠一券" : "买一赠一券";
            ruleJson = "{\"maxDiscount\":" + (int) discountAmount + "}";
            coupon.setDisplayTitle(name);
            coupon.setDisplaySubTitle("买一送一 | 封顶¥" + (int) discountAmount);
        } else if (couponType != null && couponType.contains("SHOT")) {
            // v5.0 附加券：加浓缩券（可与主券叠加）
            actualType = "SHOT";
            ruleJson = "{\"value\":5}";
            coupon.setDisplayTitle("免费加浓缩券");
            coupon.setDisplaySubTitle("抵扣¥5");
        } else if (couponType != null && couponType.contains("DELIVERY_FEE")) {
            // v5.3 附加券：配送费抵扣券，统一3元
            actualType = "DELIVERY_FEE";
            ruleJson = "{\"value\":3}";
            coupon.setDisplayTitle("配送费抵扣券");
            coupon.setDisplaySubTitle("免运费");
        } else if (couponType != null && couponType.contains("NEW_PRODUCT_HALF")) {
            // v5.3 新品半价券
            actualType = "NEW_PRODUCT_HALF";
            ruleJson = "{\"maxDiscount\":20}";
            coupon.setDisplayTitle("新品5折券");
            coupon.setDisplaySubTitle("封顶¥20");
        } else if (couponType != null && couponType.contains("NEW_PRODUCT_FREE")) {
            // v5.3 新品免单券
            actualType = "NEW_PRODUCT_FREE";
            ruleJson = "{\"maxDiscount\":40}";
            coupon.setDisplayTitle("新品免单券");
            coupon.setDisplaySubTitle("封顶¥40");
        } else if (couponType != null && couponType.contains("FREE_DRINK")) {
            // 全场饮品通兑券（无关联商品限制）
            actualType = "EXCHANGE";
            String name = couponType.contains("BIRTHDAY") ? "生日免单券" : "全场饮品通兑券";
            String subTitle = couponType.contains("BIRTHDAY") ? "排除SOE | 封顶¥" + (int) discountAmount : "任选饮品 | 封顶¥" + (int) discountAmount;
            // v5.3.5: 生日免单券排除SOE/手冲
            String categoryBlocklist = couponType.contains("BIRTHDAY") ? ",\"categoryBlocklist\":[\"soe\",\"pour-over\"]" : "";
            ruleJson = "{\"maxDiscount\":" + (int) discountAmount + categoryBlocklist + "}";
            coupon.setDisplayTitle(name);
            coupon.setDisplaySubTitle(subTitle);
        } else if (couponType != null && couponType.contains("EXCHANGE_")) {
            // 单商品兑换券（关联了特定商品ID）- 抵消标准杯价格
            actualType = "EXCHANGE";
            // 从 couponType 中提取商品ID (格式: "EXCHANGE_123")
            String productIdStr = couponType.substring(couponType.indexOf("_") + 1);
            Long linkedProductId = Long.parseLong(productIdStr);
            
            // 查询商品名称
            String productName = "商品";
            try {
                var coffeeProduct = orderService.getProduct(linkedProductId);
                if (coffeeProduct != null) {
                    productName = coffeeProduct.getName();
                }
            } catch (Exception e) {
                log.warn("查询关联商品失败: linkedProductId={}", linkedProductId, e);
            }
            
            String name = productName + "兑换券";
            String subTitle = "限标准杯，升杯加料需补差价";
            ruleJson = "{\"linkedProductId\":" + linkedProductId + "}";
            coupon.setDisplayTitle(name);
            coupon.setDisplaySubTitle(subTitle);
        } else if (couponType != null && couponType.contains("FREE_CAKE")) {
            // v5.3.5 免费蛋糕券 (生日礼) - 黑金权益
            actualType = "EXCHANGE";
            ruleJson = "{\"scope\":\"CAKE_ONLY\",\"maxDiscount\":" + (int) discountAmount + "}";
            coupon.setDisplayTitle("烘培甜品免单券");
            coupon.setDisplaySubTitle("封顶¥" + (int) discountAmount);
        } else if (couponType != null && couponType.contains("CAKE_HALF")) {
            // v5.3 蛋糕5折券 (生日礼) - 钻石权益
            actualType = "DISCOUNT";
            ruleJson = "{\"value\":50,\"scope\":\"CAKE_ONLY\"}";
            coupon.setDisplayTitle("烘培甜品5折券");
            coupon.setDisplaySubTitle("限烘焙甜品");
        } else if (couponType != null && couponType.contains("UPGRADE_SILVER_DISCOUNT")) {
            // v5.3.2 白银升级礼：单饮品5折券，最高抵¥20
            actualType = "DISCOUNT";
            ruleJson = "{\"value\":50,\"limit\":\"SINGLE_ITEM\",\"scope\":\"DRINK_ONLY\",\"maxDiscountAmount\":20}";
            coupon.setDisplayTitle("晋升白银5折券");
            coupon.setDisplaySubTitle("限饮品 | 封顶¥20");
        } else if (couponType != null && couponType.contains("UPGRADE_GOLD_BOGO")) {
            // v5.3.2 黄金升级礼：BOGO券，赠品杯最高抵¥40
            actualType = "BOGO";
            ruleJson = "{\"maxDiscount\":" + (int) discountAmount + ",\"scope\":\"DRINK_ONLY\"}";
            coupon.setDisplayTitle("晋升黄金买一赠一券");
            coupon.setDisplaySubTitle("封顶¥" + (int) discountAmount);
        } else if (couponType != null && couponType.contains("UPGRADE_DIAMOND_STANDARD_FREE")) {
            // v5.3.4 钻石升级礼：优选饮品免单券，仅限标准杯，仅排除SOE（可用于特调）
            actualType = "EXCHANGE";
            ruleJson = "{\"maxDiscount\":" + (int) discountAmount + ",\"skuLimit\":\"STANDARD_ONLY\",\"categoryBlocklist\":[\"soe\",\"pour-over\"]}";
            coupon.setDisplayTitle("晋升钻石优选饮品免单券");
            coupon.setDisplaySubTitle("限标准杯 | 封顶¥" + (int) discountAmount);
        } else if (couponType != null && couponType.contains("UPGRADE_BLACK_PREMIUM")) {
            // v5.7 黑金尊享通兑券：不限杯型，含特调/SOE，无封顶
            actualType = "EXCHANGE";
            ruleJson = "{\"skuLimit\":\"ALL\",\"freeAddon\":1}";
            coupon.setDisplayTitle("黑金尊享通兑券");
            coupon.setDisplaySubTitle("不限杯型 | 含SOE | 无封顶");
        } else if (couponType != null && couponType.contains("MONTHLY_BLACK_FREE")) {
            // v5.6 T3_ALL_FREE: 黑金月度全通兑免单券(不限杯型，含特调，排除SOE)，封顶¥40
            actualType = "EXCHANGE";
            ruleJson = "{\"maxDiscount\":40,\"skuLimit\":\"ALL\",\"categoryBlocklist\":[\"soe\",\"pour-over\"]}";
            coupon.setDisplayTitle("黑金月度全通兑免单券");
            coupon.setDisplaySubTitle("不限杯型 | 封顶¥40");
        } else if (couponType != null && couponType.contains("MONTHLY_DIAMOND_FREE")) {
            // v5.6 T2_PRE_FREE: 钻石月度优选饮品免单券(限标准杯，含特调，排除SOE)，封顶¥40
            actualType = "EXCHANGE";
            ruleJson = "{\"maxDiscount\":40,\"skuLimit\":\"STANDARD_ONLY\",\"categoryBlocklist\":[\"soe\",\"pour-over\"]}";
            coupon.setDisplayTitle("钻石月度优选饮品免单券");
            coupon.setDisplaySubTitle("限标准杯 | 封顶¥40");
        } else if (couponType != null && couponType.contains("BIRTHDAY_BLACK_FREE")) {
            // v5.6 T3_ALL_FREE: 黑金生日全通兑免单券(不限杯型，含特调，排除SOE)，封顶¥40
            actualType = "EXCHANGE";
            ruleJson = "{\"maxDiscount\":40,\"skuLimit\":\"ALL\",\"categoryBlocklist\":[\"soe\",\"pour-over\"]}";
            coupon.setDisplayTitle("🎂黑金生日全通兑免单券");
            coupon.setDisplaySubTitle("不限杯型 | 封顶¥40");
        } else if (couponType != null && couponType.contains("BIRTHDAY_DIAMOND_FREE")) {
            // v5.6 T2_PRE_FREE: 钻石生日优选饮品免单券(限标准杯，含特调，排除SOE)，封顶¥40
            actualType = "EXCHANGE";
            ruleJson = "{\"maxDiscount\":" + (int) discountAmount + ",\"skuLimit\":\"STANDARD_ONLY\",\"categoryBlocklist\":[\"soe\",\"pour-over\"]}";
            coupon.setDisplayTitle("🎂钻石生日优选饮品免单券");
            coupon.setDisplaySubTitle("限标准杯 | 封顶¥" + (int) discountAmount);
        } else if (couponType != null && couponType.contains("BIRTHDAY_GOLD_FREE")) {
            // v6.1 黄金生日标准饮品免单券: T1_STD_FREE标准，限标准杯，排除特调&SOE，封顶¥40
            actualType = "EXCHANGE";
            ruleJson = "{\"maxDiscount\":" + (int) discountAmount + ",\"skuLimit\":\"STANDARD_ONLY\",\"categoryBlocklist\":[\"signature\",\"soe\",\"pour-over\"]}";
            coupon.setDisplayTitle("🎂黄金生日标准饮品免单券");
            coupon.setDisplaySubTitle("限标准杯/不含特调、SOE");
        } else if (couponType != null && couponType.contains("BIRTHDAY_SILVER_BOGO")) {
            // v5.6 T5_BOGO: 白银生日买一赠一券，赠品封顶¥40
            actualType = "BOGO";
            ruleJson = "{\"maxDiscount\":40,\"scope\":\"DRINK_ONLY\"}";
            coupon.setDisplayTitle("🎂白银生日买一赠一券");
            coupon.setDisplaySubTitle("封顶¥40");
        } else if (couponType != null && couponType.contains("BIRTHDAY_BASIC_DISCOUNT")) {
            // v5.6 T6_50_OFF Override: 基础会员生日5折券(限标准杯)，封顶¥20
            actualType = "DISCOUNT";
            ruleJson = "{\"discountRate\":0.5,\"limit\":\"SINGLE_ITEM\",\"skuLimit\":\"STANDARD_ONLY\",\"scope\":\"DRINK_ONLY\",\"maxDiscountAmount\":20}";
            coupon.setDisplayTitle("🎂基础会员生日5折券");
            coupon.setDisplaySubTitle("限标准杯 | 封顶¥20");
        } else if (couponType != null && couponType.contains("STANDARD_FREE")) {
            // v5.3.4 标准饮品免单券（通用，仅排除SOE/手冲，可用于特调） - 注意：此条件必须在 UPGRADE_DIAMOND_STANDARD_FREE 之后
            actualType = "EXCHANGE";
            ruleJson = "{\"maxDiscount\":" + (int) discountAmount + ",\"skuLimit\":\"STANDARD_ONLY\",\"categoryBlocklist\":[\"soe\",\"pour-over\"]}";
            coupon.setDisplayTitle("标准饮品免单券");
            coupon.setDisplaySubTitle("封顶¥" + (int) discountAmount);
        } else if (couponType != null && (couponType.contains("DISCOUNT") || couponType.contains("HALF_PRICE"))) {
            // 折扣券 (含生日5折)
            actualType = "DISCOUNT";
            // 如果是半价券，强制费率为 0.5
            double rate = couponType.contains("HALF_PRICE") ? 0.5 : discountAmount;
            String extraRules = "";
            String title;
            String subTitle;
            
            if (couponType.contains("BIRTHDAY")) {
                // v5.3.1: 基础会员生日5折券 - 限单饮品 + 标准杯 + 最高20元
                title = "🎂基础会员生日5折券";
                subTitle = "限标准杯 | 封顶¥20";
                extraRules = ",\"limit\":\"SINGLE_ITEM\",\"skuLimit\":\"STANDARD_ONLY\",\"scope\":\"DRINK_ONLY\",\"maxDiscountAmount\":20";
            } else if (couponType.contains("DISCOUNT_SINGLE")) {
                // v6.0 积分兑换低折扣券：限单商品（7折及以下）
                double discount = rate >= 10 ? rate / 10 : (rate < 1 ? rate * 10 : rate);
                String discountStr = discount == Math.floor(discount) 
                    ? String.format("%.0f", discount) 
                    : String.format("%.1f", discount);
                title = discountStr + "折券";
                subTitle = "限单件商品";
                extraRules = ",\"limit\":\"SINGLE_ITEM\",\"scope\":\"DRINK_ONLY\"";
            } else {
                // v5.3.2: 修正折扣显示逻辑（支持8.8折等小数折扣）
                double discount;
                if (rate >= 10) {
                    discount = rate / 10;
                } else if (rate < 1) {
                    discount = rate * 10;
                } else {
                    discount = rate;
                }
                
                // 格式化折扣显示
                String discountStr;
                if (discount == Math.floor(discount)) {
                    discountStr = String.format("%.0f", discount);
                } else {
                    discountStr = String.format("%.1f", discount);
                }
                
                title = discountStr + "折券";
                subTitle = "全场饮品";
            }
            
            ruleJson = "{\"discountRate\":" + rate + extraRules + "}";
            coupon.setDisplayTitle(title);
            coupon.setDisplaySubTitle(subTitle);
        } else {
            // 默认：满减券 (FULL_REDUCE)
            actualType = "FULL_REDUCE";
            String title = (int) minAmount > 0 ? "满" + (int) minAmount + "减" + (int) discountAmount : (int) discountAmount + "元代金券";
            String subTitle = (int) minAmount > 0 ? "满" + (int) minAmount + "可用" : "无门槛";
            ruleJson = "{\"minOrderAmount\":" + (int) minAmount + ",\"value\":" + (int) discountAmount + "}";
            coupon.setDisplayTitle(title);
            coupon.setDisplaySubTitle(subTitle);
        }

        coupon.setCouponType(actualType);
        coupon.setRuleJson(ruleJson);
        
        userCouponMapper.insert(coupon);
        log.info("优惠券发放成功: userId={}, type={}, actualType={}, minAmount={}, discount={}, validDays={}",
                userId, couponType, actualType, minAmount, discountAmount, validDays);
    }

    @Override
    @Transactional
    public void issueCouponWithSkuLimit(Long userId, String couponType, String uniqueKey,
            double minAmount, double discountAmount, int validDays, String extraRuleJson) {
        // 检查是否已发放（幂等性）
        LambdaQueryWrapper<UserCoupon> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponCode, uniqueKey);
        if (userCouponMapper.selectCount(checkWrapper) > 0) {
            log.info("券已发放过，跳过: userId={}, uniqueKey={}", userId, uniqueKey);
            return;
        }

        // 创建优惠券
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setCouponCode(uniqueKey);
        coupon.setStatus("ISSUED");
        coupon.setIssuedAt(LocalDateTime.now());
        coupon.setExpiresAt(LocalDateTime.now().plusDays(validDays));
        coupon.setCreatedAt(LocalDateTime.now());

        // v5.3: 合并基础规则和 SKU 限制规则，添加 productName
        String baseRule;
        String actualType;
        String productName;

        if (couponType != null && (couponType.contains("EXCHANGE") || couponType.contains("FREE_DRINK"))) {
            actualType = "EXCHANGE";
            // 根据 SKU 限制生成不同的名称和描述
            if (extraRuleJson != null && extraRuleJson.contains("STANDARD_ONLY")) {
                // 钻石会员月领权益：标准杯限制，排除SOE/手冲
                productName = "意式特调免单券";
                String description = "适用于【经典意式及特调】系列，最高抵扣¥" + (int) discountAmount;
                baseRule = "{\"type\":\"EXCHANGE\",\"value\":" + (int) discountAmount
                        + ",\"productName\":\"" + productName + "\",\"description\":\"" + description + "\"";
            } else {
                // 黑金会员：全场通用
                productName = "全场饮品通兑券";
                String description = "可兑换任意饮品一杯（不含特调/SOE），最高抵扣¥" + (int) discountAmount;
                baseRule = "{\"type\":\"EXCHANGE\",\"value\":" + (int) discountAmount
                        + ",\"productName\":\"" + productName + "\",\"description\":\"" + description + "\"";
            }
        } else {
            // 其他类型使用原有逻辑
            issueCouponToUser(userId, couponType, uniqueKey, minAmount, discountAmount, validDays);
            return;
        }

        // 合并 extraRuleJson（如 skuLimit, categoryBlocklist）
        String ruleJson;
        if (extraRuleJson != null && !extraRuleJson.isEmpty() && !extraRuleJson.equals("{}")) {
            // 移除两边的大括号后合并
            String extra = extraRuleJson.trim();
            if (extra.startsWith("{") && extra.endsWith("}")) {
                extra = extra.substring(1, extra.length() - 1);
            }
            ruleJson = baseRule + "," + extra + "}";
        } else {
            ruleJson = baseRule + "}";
        }

        coupon.setCouponType(actualType);
        coupon.setRuleJson(ruleJson);
        
        // v5.3.4: 设置前端显示字段（从ruleJson提取）
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(ruleJson);
            if (jsonNode.has("productName")) {
                coupon.setDisplayTitle(jsonNode.get("productName").asText());
            }
            if (jsonNode.has("description")) {
                coupon.setDisplaySubTitle(jsonNode.get("description").asText());
            }
        } catch (Exception e) {
            log.warn("解析券规则JSON失败: {}", e.getMessage());
        }

        userCouponMapper.insert(coupon);
        log.info("SKU限制优惠券发放成功: userId={}, type={}, validDays={}, ruleJson={}",
                userId, couponType, validDays, ruleJson);
    }

    @Override
    @Transactional
    public void issueNewUserCoupon(Long userId) {
        if (userId == null)
            return;

        // 检查是否已经领过新用户优惠券（幂等性）
        String uniqueKey = "NEW_USER_COUPON_" + userId;
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponCode, uniqueKey);
        if (userCouponMapper.selectCount(wrapper) > 0) {
            log.warn("用户 {} 已领过新用户优惠券，跳过", userId);
            return;
        }

        // v2.1: 复用通用发券逻辑，通过配置区分业务场景
        // 调用内部方法，传入标准化配置
        issueCouponByConfig(userId, uniqueKey, buildNewUserCouponConfig());
        log.info("新用户首单五折券发放成功: userId={}", userId);
    }

    /**
     * v2.1: 通用配置化发券方法
     * 
     * @param userId     用户ID
     * @param couponCode 券码（用于幂等性检查）
     * @param config     券配置 JSON
     */
    private void issueCouponByConfig(Long userId, String couponCode, CouponConfig config) {
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setCouponCode(couponCode);
        coupon.setCouponType(config.type);
        coupon.setStatus("ISSUED");

        LocalDateTime now = LocalDateTime.now();
        coupon.setCreatedAt(now);
        coupon.setIssuedAt(now);
        coupon.setExpiresAt(now.plusDays(config.validDays));
        coupon.setRuleJson(config.toRuleJson());
        
        // v5.3.2: 设置前端显示字段
        if (config.productName != null) {
            coupon.setDisplayTitle(config.productName);
        }
        if (config.description != null) {
            coupon.setDisplaySubTitle(config.description);
        }

        userCouponMapper.insert(coupon);
    }

    /**
     * v2.1: 券配置数据结构
     */
    private static class CouponConfig {
        String type; // DISCOUNT / FULL_REDUCE / EXCHANGE / BOGO
        int value; // 折扣百分比 (50=5折) 或 减免金额
        String scope; // DRINK_ONLY / ALL
        String limit; // SINGLE_ITEM / ALL
        int maxDiscountAmount;// 封顶金额
        String tag; // NEW_USER_GIFT / BIRTHDAY_GIFT / SIGNIN_REWARD
        String mutex; // L1_EXCLUSIVE / L2_STACKABLE
        String productName; // 显示名称
        String description; // 描述
        int validDays; // 有效天数

        String toRuleJson() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"value\":").append(value);
            if (scope != null)
                sb.append(",\"scope\":\"").append(scope).append("\"");
            if (limit != null)
                sb.append(",\"limit\":\"").append(limit).append("\"");
            if (maxDiscountAmount > 0)
                sb.append(",\"maxDiscountAmount\":").append(maxDiscountAmount);
            if (tag != null)
                sb.append(",\"tag\":\"").append(tag).append("\"");
            if (mutex != null)
                sb.append(",\"mutex\":\"").append(mutex).append("\"");
            if ("L1_EXCLUSIVE".equals(mutex))
                sb.append(",\"exclusive\":true");
            if (productName != null)
                sb.append(",\"productName\":\"").append(productName).append("\"");
            if (description != null)
                sb.append(",\"description\":\"").append(description).append("\"");
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * v2.1: 新用户首单5折券配置
     */
    private CouponConfig buildNewUserCouponConfig() {
        CouponConfig config = new CouponConfig();
        config.type = "DISCOUNT";
        config.value = 50; // 50% OFF (5折)
        config.scope = "DRINK_ONLY"; // 仅限饮品
        config.limit = "SINGLE_ITEM"; // 仅限单杯
        config.maxDiscountAmount = 20; // 封顶抵扣20元
        config.tag = "NEW_USER_GIFT"; // 业务标签
        config.mutex = "L1_EXCLUSIVE"; // 最高级互斥
        config.productName = "新用户首单5折";
        config.description = "单饮品5折（最高抵¥20）";
        config.validDays = 7; // 有效期7天
        return config;
    }

    /**
     * v2.1: 生日5折券配置（示例）
     */
    @SuppressWarnings("unused")
    private CouponConfig buildBirthdayCouponConfig() {
        CouponConfig config = new CouponConfig();
        config.type = "DISCOUNT";
        config.value = 50;
        config.scope = "DRINK_ONLY";
        config.limit = "SINGLE_ITEM";
        config.maxDiscountAmount = 25; // 生日券稍微高一点
        config.tag = "BIRTHDAY_GIFT";
        config.mutex = "L1_EXCLUSIVE";
        config.productName = "生日特享5折";
        config.description = "生日礼：限单杯饮品，最高抵¥25";
        config.validDays = 30; // 有效期30天
        return config;
    }
}
