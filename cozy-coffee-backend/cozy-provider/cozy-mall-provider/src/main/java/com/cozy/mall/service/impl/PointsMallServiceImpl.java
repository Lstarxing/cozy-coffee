package com.cozy.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.CouponTemplateConfig;
import com.cozy.common.constant.RedemptionDiscountConfig;
import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.cozy.mall.coupon.CouponCalculator;
import com.cozy.mall.coupon.CouponCombinationService;
import com.cozy.mall.util.CouponRuleUtil;
import com.cozy.mall.service.PointsRefundOutboxService;
import com.cozy.mall.entity.PointsOrder;
import com.cozy.mall.entity.PointsOrderFulfillment;
import com.cozy.mall.entity.PointsProduct;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.mapper.PointsOrderFulfillmentMapper;
import com.cozy.mall.mapper.PointsOrderMapper;
import com.cozy.mall.mapper.PointsProductMapper;
import com.cozy.mall.mapper.UserCouponMapper;
import com.cozy.mall.mapper.MonthlyRedemptionMapper;
import com.cozy.mall.mapper.CouponRollbackInboxMapper;
import com.cozy.mall.entity.MonthlyRedemption;
import com.cozy.member.api.AddressService;
import com.cozy.member.api.MemberService;
import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.dto.request.RedeemRequest;
import com.cozy.member.dto.response.AddressDTO;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.mall.dto.response.CouponCombinationResult;
import com.cozy.mall.dto.response.PointsOrderDTO;
import com.cozy.mall.dto.response.PointsRefundDeadLetterDTO;
import com.cozy.mall.dto.response.PointsProductDTO;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.response.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import com.cozy.mall.dto.response.UserCouponDTO;
import com.cozy.mall.dto.response.CouponUsageResult;

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
    private final CouponRollbackInboxMapper couponRollbackInboxMapper;
    private final PointsRefundOutboxService pointsRefundOutboxService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 优惠券发券模板配置（单一事实源 @ConfigurationProperties，见 cozy.mall.coupon-template）
    private final CouponTemplateConfig couponTemplateConfig;

    // 优惠券抵扣策略（按 coupon_type 分发，Spring 注入 @Component(type) 实现）
    private final Map<String, CouponCalculator> couponCalculators;

    // 优惠券组合引擎（主券/辅券分类 + 组合校验 + 统一计算）
    private final CouponCombinationService couponCombinationService;

    // 跨服务调用：会员服务（获取积分、扣减积分）
    @DubboReference(check = false)
    private MemberService memberService;

    // 跨服务调用：地址服务（获取收货地址）
    @DubboReference(check = false)
    private AddressService addressService;
    
    // 跨服务调用：订单服务（查询咖啡商品信息）
    @DubboReference(check = false)
    private OrderService orderService;

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
            throw new BusinessException("商品ID不能为空");
        }
        PointsProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public PointsOrderDTO redeem(Long userId, RedeemRequest request) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        if (request == null || request.getProductId() == null) {
            throw new BusinessException("请选择商品");
        }
        log.info("用户 {} 发起兑换请求: productId={}", userId, request.getProductId());
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        // 服务层数量校验：Dubbo/内部调用不经 Web 校验，负数量会让库存反向增加
        if (quantity < 1 || quantity > 50) {
            throw new BusinessException("兑换数量不合法");
        }

        PointsProduct product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!"active".equals(product.getStatus())) {
            throw new BusinessException("商品已下架");
        }

        // 原子扣减库存：条件更新，并发下也不会超卖（stock >= qty 才扣减）
        int deducted = productMapper.deductStock(product.getId(), quantity);
        if (deducted == 0) {
            throw new BusinessException("库存不足");
        }
        product.setStock(product.getStock() - quantity);
        invalidateMallProductsCacheAfterCommit();

        // v4.2: 月度限购（条件自增；放在远程扣分之前，本地失败可整体回滚）
        if (product.getMonthlyLimit() != null && product.getMonthlyLimit() > 0) {
            String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            int limit = product.getMonthlyLimit();
            int incremented = monthlyRedemptionMapper.incrementIfWithinLimit(
                    userId, product.getId(), currentMonth, quantity, limit);
            if (incremented == 0) {
                MonthlyRedemption mr = monthlyRedemptionMapper.selectOne(new LambdaQueryWrapper<MonthlyRedemption>()
                        .eq(MonthlyRedemption::getUserId, userId)
                        .eq(MonthlyRedemption::getProductId, product.getId())
                        .eq(MonthlyRedemption::getMonth, currentMonth));
                if (mr == null) {
                    // 本月首兑：行不存在，校验不超限后插入
                    if (quantity > limit) {
                        throw new BusinessException("该商品每月限兑 " + limit + " 件");
                    }
                    MonthlyRedemption nr = new MonthlyRedemption();
                    nr.setUserId(userId);
                    nr.setProductId(product.getId());
                    nr.setMonth(currentMonth);
                    nr.setRedeemedCount(quantity);
                    try {
                        monthlyRedemptionMapper.insert(nr);
                    } catch (DuplicateKeyException e) {
                        // 并发首兑：另一请求已插入，重试条件自增
                        if (monthlyRedemptionMapper.incrementIfWithinLimit(
                                userId, product.getId(), currentMonth, quantity, limit) == 0) {
                            throw new BusinessException("该商品每月限兑 " + limit + " 件");
                        }
                    }
                } else {
                    throw new BusinessException("该商品每月限兑 " + limit + " 件，本月已兑换 " + mr.getRedeemedCount() + " 件");
                }
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
                    throw new BusinessException("收货地址不存在");
                }
            } else if (request.getReceiverName() == null || request.getReceiverPhone() == null
                    || request.getReceiverAddress() == null) {
                throw new BusinessException("快递订单必须提供收货信息");
            }
        }

        // 跨服务调用：获取会员信息
        MemberDTO memberDTO = memberService.getMemberByUserId(userId);
        if (memberDTO == null) {
            throw new BusinessException("会员信息不存在");
        }

        // 计算需要的积分
        int totalCost = calculateCost(product.getPointsPrice(), quantity, memberDTO.getMemberLevel());

        if (memberDTO.getCurrentPoints() < totalCost) {
            throw new BusinessException("积分不足，当前积分: " + memberDTO.getCurrentPoints() + "，需要: " + totalCost);
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

        // 7. 跨服务调用：FIFO 扣减积分（最后一步远程副作用；失败则本地整体回滚）
        boolean consumed = memberService.consumePointsFIFO(userId, totalCost, "redeem", order.getId());
        if (!consumed) {
            throw new BusinessException("积分扣减失败");
        }

        // 兜底补偿：远程扣分已提交后若本地事务回滚（如 DB 提交失败），退还积分（幂等：consumeId=orderId，见 Task A4）
        // 补偿走持久化退款 outbox，消除"远程扣分已提交、进程在补偿前崩溃"的丢失窗口；uk_refund_order 幂等
        // 守卫 isSynchronizationActive：纯单测（无活跃事务）时跳过
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            final Long consumeId = order.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        try {
                            pointsRefundOutboxService.enqueue(consumeId, userId, totalCost, "redeem",
                                    "兑换事务回滚补偿");
                        } catch (Exception e) {
                            log.error("兑换事务回滚补偿入队失败: orderId={}, userId={}", consumeId, userId, e);
                        }
                    }
                }
            });
        }

        log.info("兑换成功: orderNo={}, cost={}, type={}", order.getOrderNo(), totalCost, fulfillmentType);
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

    /**
     * 恢复商品库存：原子自增（与兑换侧 deductStock 同为单语句 UPDATE，DB 行锁串行，无丢失更新）
     */
    private void restoreProductStock(Long productId, int quantity) {
        if (productId == null || quantity <= 0) {
            return;
        }
        int affected = productMapper.addStock(productId, quantity);
        if (affected == 0) {
            log.warn("恢复库存失败（商品不存在）: productId={}", productId);
            return;
        }
        invalidateMallProductsCacheAfterCommit();
    }

    private void invalidateMallProductsCache() {
        try {
            redisTemplate.delete(RedisKeyConstants.MALL_PRODUCTS_ACTIVE);
        } catch (Exception e) {
            log.warn("清理Redis积分商城商品缓存失败", e);
        }
    }

    /** 库存变更后 afterCommit 才清缓存，避免提交前失效被并发请求用旧数据重建 */
    private void invalidateMallProductsCacheAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            invalidateMallProductsCache();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    invalidateMallProductsCache();
                }
            }
        });
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
            throw new BusinessException("用户未登录");
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
            throw new BusinessException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此订单");
        }
        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public PointsOrderDTO cancelOrder(Long orderId, Long userId) {
        log.info("用户 {} 取消订单: orderId={}", userId, orderId);

        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        // 状态 CAS：并发取消只有一个请求 affected==1，其余幂等返回，不重复恢复库存/退款
        LocalDateTime now = LocalDateTime.now();
        int affected = orderMapper.cancelOrderIfPending(orderId, userId, now);
        if (affected == 0) {
            PointsOrder current = orderMapper.selectById(orderId);
            if (current != null && "cancelled".equals(current.getStatus())) {
                return toOrderDTO(current); // 幂等：已被取消，直接返回
            }
            throw new BusinessException("订单状态不允许取消");
        }

        order.setStatus("cancelled");
        order.setUpdatedAt(now);

        // 原子恢复库存（与兑换侧原子扣减一致，不会丢失并发更新）
        restoreProductStock(order.getProductId(), order.getQuantity());

        // 与取消状态、库存恢复同事务写入退款 outbox；提交后由定时 relay 幂等调用 member，消除远程先提交窗口。
        pointsRefundOutboxService.enqueue(order.getId(), userId, order.getPointsCost(), "redeem",
                "订单取消退还: " + order.getProductName() + " x" + order.getQuantity());

        log.info("订单取消成功: orderNo={}, refund={}", order.getOrderNo(), order.getPointsCost());
        return toOrderDTO(order);
    }

    // ==================== 用户券包实现 ====================

    @Override
    public List<UserCouponDTO> getUserCoupons(Long userId, String status) {
        log.info("获取用户券包: userId={}, status={}", userId, status);
        if (userId == null) {
            throw new BusinessException("用户未登录");
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
            List<ItemCheckDTO> items) {
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
                    for (ItemCheckDTO item : items) {
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
                    for (ItemCheckDTO item : items) {
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
                        for (ItemCheckDTO item : items) {
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
                (List<ItemCheckDTO>) null);
        return result.getDiscountAmount();
    }

    @Override
    @Transactional
    public CouponUsageResult useCouponWithResult(Long userId, String couponCode, BigDecimal orderAmount,
            List<ItemCheckDTO> items) {
        log.info("使用券: userId={}, couponCode={}, orderAmount={}", userId, couponCode, orderAmount);

        if (userId == null || couponCode == null || couponCode.isEmpty()) {
            throw new BusinessException("参数不能为空");
        }

        // 查询券
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponCode, couponCode)
                .eq(UserCoupon::getStatus, "ISSUED");
        UserCoupon coupon = userCouponMapper.selectOne(wrapper);

        if (coupon == null) {
            throw new BusinessException("券不存在或已使用");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("券已过期");
        }

        // 计算折扣金额
        BigDecimal discountAmount = calculateCouponDiscount(coupon, orderAmount, items);

        // 更新券状态：订单创建（待支付）时冻结，支付/接单后转 USED，取消则回滚
        LocalDateTime now = LocalDateTime.now();
        coupon.setStatus("FROZEN");
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

    @Override
    @Transactional(readOnly = true)
    public CouponUsageResult previewCouponWithResult(Long userId, String couponCode, BigDecimal orderAmount,
            List<ItemCheckDTO> items) {
        if (userId == null || couponCode == null || couponCode.isBlank()) {
            throw new BusinessException("优惠券参数不能为空");
        }

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponCode, couponCode.trim())
                .eq(UserCoupon::getStatus, "ISSUED");
        UserCoupon coupon = userCouponMapper.selectOne(wrapper);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在或已使用");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("优惠券已过期");
        }

        BigDecimal discountAmount = calculateCouponDiscount(coupon, orderAmount, items);
        Long linkedProductId = parseLongValue(coupon.getRuleJson(), "linkedProductId");
        boolean exclusive = coupon.getRuleJson() != null && coupon.getRuleJson().contains("\"exclusive\":true");
        int freeAddonCount = parseValue(coupon.getRuleJson(), "freeAddon");
        return new CouponUsageResult(discountAmount, coupon.getCouponType(), coupon.getId(), linkedProductId,
                exclusive, freeAddonCount);
    }

    @Override
    public CouponCombinationResult previewCouponCombination(Long userId, List<String> couponCodes,
            BigDecimal couponBase, BigDecimal addonsTotal, List<BigDecimal> addonPrices,
            List<ItemCheckDTO> items) {
        return couponCombinationService.preview(userId, couponCodes, couponBase, addonsTotal, addonPrices, items);
    }

    @Override
    @Transactional
    public CouponCombinationResult useCouponCombination(Long userId, List<String> couponCodes,
            BigDecimal couponBase, BigDecimal addonsTotal, List<BigDecimal> addonPrices,
            List<ItemCheckDTO> items) {
        return couponCombinationService.use(userId, couponCodes, couponBase, addonsTotal, addonPrices, items);
    }

    private BigDecimal calculateCouponDiscount(UserCoupon coupon, BigDecimal orderAmount,
            List<ItemCheckDTO> items) {
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
    private boolean isBakery(String category) {
        return CouponRuleUtil.isBakery(category);
    }

    private boolean isDrink(String category) {
        return CouponRuleUtil.isDrink(category);
    }

    private int parseValue(String ruleJson, String key) {
        return CouponRuleUtil.parseValue(ruleJson, key);
    }

    private long parseLongValue(String ruleJson, String key) {
        return CouponRuleUtil.parseLongValue(ruleJson, key);
    }

    private int parseMinAmount(String ruleJson) {
        return parseValue(ruleJson, "minOrderAmount");
    }

    private double parseDoubleValue(String ruleJson, String key) {
        return CouponRuleUtil.parseDoubleValue(ruleJson, key);
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
        // 兑换折扣单一事实源：RedemptionDiscountConfig（black 0.85/diamond 0.90/gold 0.95/silver 0.98/basic 1.0）
        double discount = RedemptionDiscountConfig.getDiscount(memberLevel).doubleValue();
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
            throw new BusinessException("订单ID不能为空");
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
            throw new BusinessException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
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
            throw new BusinessException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
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
            throw new BusinessException("参数不能为空");
        }

        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        if (!"shipped".equals(order.getStatus())) {
            throw new BusinessException("只有已发货的订单可以确认收货");
        }

        PointsOrderFulfillment f = fulfillmentMapper.selectById(orderId);
        if (f == null || !"DELIVERY".equals(f.getType())) {
            throw new BusinessException("只有快递订单可以确认收货");
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
            throw new BusinessException("商品名称不能为空");
        }
        if (dto.getPointsPrice() == null || dto.getPointsPrice() <= 0) {
            throw new BusinessException("积分价格必须大于0");
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
            throw new BusinessException("商品不存在");
        }
        if (dto.getName() != null) {
            if (dto.getName().trim().isEmpty()) {
                throw new BusinessException("商品名称不能为空");
            }
            product.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null)
            product.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null)
            product.setImageUrl(dto.getImageUrl());
        if (dto.getPointsPrice() != null) {
            if (dto.getPointsPrice() <= 0) {
                throw new BusinessException("积分价格必须大于0");
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
            throw new BusinessException("商品不存在");
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
            throw new BusinessException("商品不存在");
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
            throw new BusinessException("订单不存在");
        }
        // 删除订单主表
        orderMapper.deleteById(orderId);
        // 删除履约子表
        fulfillmentMapper.deleteById(orderId);
        log.info("订单及其履约记录已删除: id={}", orderId);
    }

    @Override
    public List<PointsRefundDeadLetterDTO> listDeadPointRefunds(Integer limit) {
        return pointsRefundOutboxService.listDeadRefunds(limit);
    }

    @Override
    public void retryDeadPointRefund(Long id, Long operatorId) {
        pointsRefundOutboxService.retryDeadRefund(id, operatorId);
    }

    @Override
    public long countDeadPointRefunds() {
        return pointsRefundOutboxService.countDeadRefunds();
    }

    @Override
    @Transactional
    public void rollbackCoupon(Long couponId, Long userId) {
        rollbackCouponInternal(couponId, userId, false);
    }

    @Override
    @Transactional
    public void rollbackCoupons(String rollbackEventId, Long orderId, Long userId,
            Long mainCouponId, List<Long> addonCouponIds) {
        if (rollbackEventId == null || rollbackEventId.isBlank()) {
            throw new BusinessException("券回滚事件幂等键不能为空");
        }
        if (userId == null) {
            throw new BusinessException("券回滚事件用户不能为空");
        }
        if (couponRollbackInboxMapper.insertIfAbsent(rollbackEventId, LocalDateTime.now()) == 0) {
            log.info("券回滚事件已处理，幂等跳过: eventId={}, orderId={}", rollbackEventId, orderId);
            return;
        }
        List<Long> couponIds = new ArrayList<>();
        if (mainCouponId != null) {
            couponIds.add(mainCouponId);
        }
        if (addonCouponIds != null) {
            couponIds.addAll(addonCouponIds);
        }
        for (Long couponId : couponIds.stream().filter(Objects::nonNull).distinct().toList()) {
            rollbackCouponInternal(couponId, userId, true);
        }
    }

    private void rollbackCouponInternal(Long couponId, Long userId, boolean failOnOwnerMismatch) {
        log.info("回滚优惠券: couponId={}, userId={}", couponId, userId);
        if (couponId == null)
            return;

        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null) {
            log.warn("回滚优惠券失败，券不存在: id={}", couponId);
            return;
        }

        if (!Objects.equals(coupon.getUserId(), userId)) {
            if (failOnOwnerMismatch) {
                // 批量 MQ 回滚必须失败并回滚 inbox；否则错误事件会被永久标记为已处理，券却仍被冻结。
                throw new BusinessException("券回滚事件用户与券归属不一致");
            }
            log.warn("回滚优惠券失败，用户不匹配: couponUserId={}, requestUserId={}", coupon.getUserId(), userId);
            return;
        }

        if (!"USED".equals(coupon.getStatus()) && !"FROZEN".equals(coupon.getStatus())) {
            log.warn("针对非使用/冻结状态的券无需回滚: status={}", coupon.getStatus());
            return;
        }

        // 还原状态
        coupon.setStatus("ISSUED");
        coupon.setUsedAt(null);
        userCouponMapper.updateById(coupon);
        log.info("优惠券已归还为 ISSUED 状态: couponId={}", couponId);
    }

    @Override
    @Transactional
    public void confirmCoupon(Long couponId, Long userId) {
        confirmCoupons(couponId == null ? Collections.emptyList() : List.of(couponId), userId);
    }

    @Override
    @Transactional
    public void confirmCoupons(List<Long> couponIds, Long userId) {
        if (couponIds == null || couponIds.isEmpty()) {
            return;
        }
        for (Long couponId : couponIds.stream().filter(Objects::nonNull).distinct().toList()) {
            confirmCouponInternal(couponId, userId);
        }
    }

    private void confirmCouponInternal(Long couponId, Long userId) {
        log.info("确认优惠券(FROZEN→USED): couponId={}, userId={}", couponId, userId);

        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null) {
            log.warn("确认优惠券失败，券不存在: id={}", couponId);
            return;
        }
        if (userId != null && !coupon.getUserId().equals(userId)) {
            log.warn("确认优惠券失败，用户不匹配: couponUserId={}, requestUserId={}", coupon.getUserId(), userId);
            return;
        }

        if (!"FROZEN".equals(coupon.getStatus())) {
            log.warn("券状态不是 FROZEN，跳过确认: status={}", coupon.getStatus());
            return;
        }

        coupon.setStatus("USED");
        userCouponMapper.updateById(coupon);
        log.info("优惠券已确认核销: couponId={}", couponId);
    }

    @Override
    public List<UserCouponDTO> getCouponsByIds(List<Long> couponIds) {
        if (couponIds == null || couponIds.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(UserCoupon::getId, couponIds);
        return userCouponMapper.selectList(wrapper).stream()
                .map(this::toCouponDTO)
                .collect(Collectors.toList());
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

        // 券模板驱动（配置见 cozy.mall.coupon-template）；未命中模板走满减兜底
        CouponTemplateConfig.CouponTemplate template = couponTemplateConfig.match(couponType);
        if (template != null) {
            applyCouponTemplate(coupon, template, couponType, minAmount, discountAmount);
        } else {
            coupon.setCouponType("FULL_REDUCE");
            coupon.setRuleJson("{\"minOrderAmount\":" + (int) minAmount + ",\"value\":" + (int) discountAmount + "}");
            String title = (int) minAmount > 0 ? "满" + (int) minAmount + "减" + (int) discountAmount : (int) discountAmount + "元代金券";
            String subTitle = (int) minAmount > 0 ? "满" + (int) minAmount + "可用" : "无门槛";
            coupon.setDisplayTitle(title);
            coupon.setDisplaySubTitle(subTitle);
        }

        userCouponMapper.insert(coupon);
        log.info("优惠券发放成功: userId={}, type={}, ruleJson={}, minAmount={}, discount={}, validDays={}",
                userId, couponType, coupon.getRuleJson(), minAmount, discountAmount, validDays);
    }

    /**
     * 按券模板生成券属性（type/ruleJson/displayTitle/SubTitle）。
     * 动态部分：BOGO 生日标题、FREE_DRINK 生日排除、EXCHANGE_ 商品名、通用折扣券 rate 与折数标题。
     */
    private void applyCouponTemplate(UserCoupon coupon, CouponTemplateConfig.CouponTemplate t,
            String couponType, double minAmount, double discountAmount) {
        String type = t.getType();
        coupon.setCouponType(type);

        Map<String, Object> rule = new LinkedHashMap<>();
        if (t.getValue() != null) {
            rule.put("value", t.getValue());
        }
        if (t.getDiscountRate() != null) {
            rule.put("discountRate", t.getDiscountRate());
        }
        if (t.getMaxDiscountAmount() != null) {
            rule.put("maxDiscountAmount", t.getMaxDiscountAmount());
        }
        int maxDiscount = resolveMaxDiscount(t, discountAmount);
        if (maxDiscount > 0) {
            rule.put("maxDiscount", maxDiscount);
        }
        if (t.getScope() != null) {
            rule.put("scope", t.getScope());
        }
        if (t.getSkuLimit() != null) {
            rule.put("skuLimit", t.getSkuLimit());
        }
        if (t.getLimit() != null) {
            rule.put("limit", t.getLimit());
        }
        if (t.getCategoryBlocklist() != null && !t.getCategoryBlocklist().isEmpty()) {
            rule.put("categoryBlocklist", t.getCategoryBlocklist());
        }
        if (Boolean.TRUE.equals(t.getFreeAddon())) {
            rule.put("freeAddon", 1);
        }
        if (t.getStacking() != null && !t.getStacking().isBlank()) {
            rule.put("stacking", t.getStacking());
        }

        if (t.getDisplayTitle() != null) {
            coupon.setDisplayTitle(replacePlaceholders(t.getDisplayTitle(), minAmount, discountAmount));
        }
        if (t.getDisplaySubTitle() != null) {
            coupon.setDisplaySubTitle(replacePlaceholders(t.getDisplaySubTitle(), minAmount, discountAmount));
        }

        if (Boolean.TRUE.equals(t.getLinkedProductFromCode())) {
            // EXCHANGE_123：解析商品 ID 并查询名称
            String productIdStr = couponType.substring(couponType.indexOf("_") + 1);
            Long linkedProductId = Long.parseLong(productIdStr);
            rule.put("linkedProductId", linkedProductId);
            String productName = "商品";
            try {
                var coffeeProduct = orderService.getProduct(linkedProductId);
                if (coffeeProduct != null) {
                    productName = coffeeProduct.getName();
                }
            } catch (Exception e) {
                log.warn("查询关联商品失败: linkedProductId={}", linkedProductId, e);
            }
            coupon.setDisplayTitle(productName + "兑换券");
            if (coupon.getDisplaySubTitle() == null) {
                coupon.setDisplaySubTitle("限标准杯，升杯加料需补差价");
            }
        } else if ("BOGO".equals(type) && coupon.getDisplayTitle() == null) {
            coupon.setDisplayTitle(couponType.contains("BIRTHDAY") ? "生日买一赠一券" : "买一赠一券");
        } else if ("EXCHANGE".equals(type) && couponType.contains("FREE_DRINK")) {
            boolean birthday = couponType.contains("BIRTHDAY");
            coupon.setDisplayTitle(birthday ? "生日免单券" : "全场饮品通兑券");
            coupon.setDisplaySubTitle((birthday ? "排除精品咖啡 | " : "任选饮品 | ") + "封顶¥" + (int) discountAmount);
            if (birthday) {
                rule.put("categoryBlocklist", List.of("SPECIALTY"));
            }
        } else if ("DISCOUNT".equals(type) && t.getDiscountRate() == null && t.getValue() == null
                && coupon.getDisplayTitle() == null) {
            // 通用折扣券：rate 由 discountAmount 计算，折数标题动态
            double rate = couponType.contains("HALF_PRICE") ? 0.5 : discountAmount;
            double discount = rate >= 10 ? rate / 10 : (rate < 1 ? rate * 10 : rate);
            if (couponType.contains("DISCOUNT_SINGLE")) {
                coupon.setDisplayTitle(formatDiscount(discount) + "折券");
                coupon.setDisplaySubTitle("限单件商品");
                rule.put("limit", "SINGLE_ITEM");
                rule.put("scope", "DRINK_ONLY");
            } else {
                coupon.setDisplayTitle(formatDiscount(discount) + "折券");
                coupon.setDisplaySubTitle("全场饮品");
            }
            rule.put("discountRate", rate);
        }

        coupon.setRuleJson(toJson(rule));
    }

    private int resolveMaxDiscount(CouponTemplateConfig.CouponTemplate t, double discountAmount) {
        if (Boolean.TRUE.equals(t.getUseDiscountAmountAsMaxDiscount())) {
            return (int) discountAmount;
        }
        return t.getMaxDiscount() != null ? t.getMaxDiscount() : 0;
    }

    private String replacePlaceholders(String text, double minAmount, double discountAmount) {
        if (text == null) {
            return null;
        }
        return text.replace("{discountAmount}", String.valueOf((int) discountAmount))
                .replace("{minAmount}", String.valueOf((int) minAmount));
    }

    private String formatDiscount(double discount) {
        return discount == Math.floor(discount)
                ? String.format("%.0f", discount)
                : String.format("%.1f", discount);
    }

    private String toJson(Map<String, Object> rule) {
        try {
            return objectMapper.writeValueAsString(rule);
        } catch (Exception e) {
            log.warn("券规则JSON序列化失败: {}", e.getMessage());
            return "{}";
        }
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
