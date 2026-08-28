package com.cozy.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.member.api.MemberService;
import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.dto.response.CouponCombinationResult;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.common.exception.BusinessException;
import com.cozy.common.exception.BusinessErrorCode;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.entity.ShopOrderItem;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ShopOrderMapper;
import com.cozy.order.mapper.ShopOrderItemMapper;
import com.cozy.order.service.PickupCodeService;
import com.cozy.order.service.OrderPreviewService;
import com.cozy.order.service.ProductPricingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final CoffeeProductMapper productMapper;
    private final ShopOrderMapper orderMapper;
    private final ShopOrderItemMapper orderItemMapper;
    private final PickupCodeService pickupCodeService;
    private final ObjectMapper objectMapper;
    private final OrderDtoConverter dtoConverter;
    private final OrderRewardService rewardService;
    private final OrderDtoEnricher orderDtoEnricher;
    private final OrderInfraService orderInfraService;
    private final TransactionTemplate transactionTemplate;
    private final OrderPreviewService orderPreviewService;
    private final ProductPricingService productPricingService;

    private final ConcurrentMap<String, Object> idempotencyLocks = new ConcurrentHashMap<>();

    @Value("${cozy.order.delivery.eta-minutes:55}")
    private int deliveryEtaMinutes;

    @DubboReference(check = false)
    private MemberService memberService;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    public ShopOrderDTO createOrder(Long userId, String memberLevel, String idempotencyKey,
            CreateOrderRequest request) {
        // Direct/Dubbo legacy callers may omit the key. The HTTP mobile contract enforces it in the gateway.
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return createOrderInternal(userId, memberLevel, null, request);
        }
        if (idempotencyKey.length() > 64) {
            throw new BusinessException(BusinessErrorCode.IDEMPOTENCY_KEY_INVALID,
                    "Idempotency-Key 必须为 1-64 个字符");
        }
        String normalizedKey = idempotencyKey.trim();
        String lockKey = userId + ":" + normalizedKey;
        Object lock = idempotencyLocks.computeIfAbsent(lockKey, ignored -> new Object());
        synchronized (lock) {
            try {
                return createOrderInternal(userId, memberLevel, normalizedKey, request);
            } finally {
                idempotencyLocks.remove(lockKey, lock);
            }
        }
    }

    private ShopOrderDTO createOrderInternal(Long userId, String memberLevel, String idempotencyKey,
            CreateOrderRequest request) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        if (request == null) {
            throw new BusinessException("请求参数不能为空");
        }

        ShopOrder existing = idempotencyKey != null
                ? orderMapper.selectByUserAndIdempotencyKey(userId, idempotencyKey)
                : null;
        if (existing != null) {
            log.info("命中幂等订单: userId={}, idempotencyKey={}, orderId={}",
                    userId, idempotencyKey, existing.getId());
            ShopOrderDTO replay = orderDtoEnricher.toOrderDTO(existing,
                    orderDtoEnricher.getOrderItemsByOrderId(existing.getId()));
            replay.setIdempotentReplay(true);
            return replay;
        }

        orderPreviewService.validateForCreate(userId, memberLevel, request);

        // 构建订单项列表
        List<OrderItemRequest> itemRequests = request.getItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new BusinessException("请选择商品");
        }

        // 验证商品并计算金额
        BigDecimal totalAmount = BigDecimal.ZERO; // 订单总金额（基础+加料）
        BigDecimal baseTotalAmount = BigDecimal.ZERO; // 基础商品总金额（基础价+杯型加价）
        BigDecimal addonsTotalAmount = BigDecimal.ZERO; // 加料总费用
        List<BigDecimal> addonPrices = new ArrayList<>(); // 原始加料价（尊享通兑券 freeAddon 免加料用）
        int totalQuantity = 0;
        List<ShopOrderItem> orderItems = new ArrayList<>();
        // v5.0: 用于券核销的商品检查列表
        List<ItemCheckDTO> itemChecks = new ArrayList<>();
        StringBuilder itemsSummary = new StringBuilder();

        for (OrderItemRequest itemReq : itemRequests) {
            if (itemReq.getProductId() == null) {
                throw new BusinessException("商品ID不能为空");
            }
            int qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            if (qty < 1 || qty > 10) {
                throw new BusinessException("单商品购买数量需在1-10之间");
            }

            CoffeeProduct product = productMapper.selectById(itemReq.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在: " + itemReq.getProductId());
            }
            if (!"active".equals(product.getStatus())) {
                throw new BusinessException("商品已下架: " + product.getName());
            }

            // V2 统一定价核心（P1E）：规格校验 + 基础价（按 size_type，无硬编码大杯加价）+ 加料权威价 + 成交快照
            ProductPricingService.PriceResult pr = productPricingService.price(
                    product, itemReq.getCupSize(), itemReq.getTemperature(),
                    itemReq.getSugarLevel(), itemReq.getAddonsJson());
            if (!pr.valid()) {
                throw new BusinessException(pr.error());
            }
            BigDecimal basePrice = pr.basePrice();

            BigDecimal itemBaseAmount = basePrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal itemAddonsAmount = pr.addonFee().multiply(BigDecimal.valueOf(qty));
            addonPrices.addAll(pr.addonPrices());
            if (itemAddonsAmount.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("商品加料费用: productId={}, addonsFee={}, quantity={}, addonsTotal={}",
                        product.getId(), pr.addonFee(), qty, itemAddonsAmount);
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
            item.setUnitPrice(pr.basePrice()); // 规格价快照（DEFAULT→price / MEDIUM_LARGE→medium·large）
            item.setQuantity(qty);
            item.setItemAmount(itemAmount);
            item.setCupSize(itemReq.getCupSize());
            item.setSugarLevel(itemReq.getSugarLevel());
            item.setTemperature(itemReq.getTemperature());
            item.setCoffeeStrength(itemReq.getCoffeeStrength());
            item.setOptionsJson(itemReq.getOptionsJson());
            item.setAddonsJson(pr.normalizedAddonsJson()); // 规范化成交快照（含默认项，price=price_delta 实际价）
            item.setCreatedAt(LocalDateTime.now());
            orderItems.add(item);

            // v5.3: 添加到检查列表，传入修饰符信息、杯型和新品标识
            // v5.3.2: 传入实际价格（含杯型加价），而非原始价格
            String modifiersJson = buildModifiersJson(itemReq);
            String cupSize = itemReq.getCupSize() != null ? itemReq.getCupSize() : "STANDARD";
            Boolean isNewProduct = product.getIsNewProduct() != null ? product.getIsNewProduct() : false;
            itemChecks.add(new ItemCheckDTO(
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

        // 券核销（组合引擎统一：主券/辅券分类、组合校验、金额计算、整组冻结）
        BigDecimal discountAmount = BigDecimal.ZERO;
        Long appliedCouponId = null;
        String mainCouponType = null; // 主券类型，用于免运费等判断
        BigDecimal addonDiscount = BigDecimal.ZERO; // 商品附加券折扣（如加浓缩券）
        BigDecimal deliveryFeeDiscount = BigDecimal.ZERO; // 配送费折扣（如配送费券）
        List<Long> addonCouponIds = new ArrayList<>();
        String couponDetailsJson = null; // 每张券抵扣明细快照（订单详情页逐条展示）
        List<String> couponCodes = collectCouponCodes(request);

        if (!couponCodes.isEmpty()) {
            try {
                CouponCombinationResult combo = pointsMallService.useCouponCombination(userId, couponCodes,
                        baseTotalAmount, addonsTotalAmount, addonPrices, itemChecks);
                discountAmount = combo.getMainDiscount();
                appliedCouponId = combo.getMainCouponId();
                mainCouponType = combo.getMainCouponType();
                addonDiscount = combo.getAddonDiscount();
                deliveryFeeDiscount = combo.getDeliveryFeeDiscount();
                addonCouponIds = combo.getAddonCouponIds();
                if (combo.getDetails() != null && !combo.getDetails().isEmpty()) {
                    try {
                        couponDetailsJson = objectMapper.writeValueAsString(combo.getDetails());
                    } catch (Exception e) {
                        log.warn("序列化优惠券抵扣明细失败: {}", e.getMessage());
                    }
                }
                log.info("整组券核销成功: userId={}, codes={}, mainDiscount={}, addonDiscount={}, deliveryFeeDiscount={}, exchange={}, exclusive={}",
                        userId, couponCodes, discountAmount, addonDiscount, deliveryFeeDiscount,
                        combo.isExchangeCoupon(), combo.isExclusive());
            } catch (Exception e) {
                log.warn("整组券核销失败: userId={}, codes={}, error={}", userId, couponCodes, e.getMessage());
                throw new BusinessException("优惠券使用失败: " + e.getMessage());
            }
        }

        // ============================================================
        // v5.3: 配送费与黑金会员无限免运费逻辑
        // ============================================================
        BigDecimal deliveryFee = BigDecimal.ZERO;
        boolean deliveryFeeWaived = false;
        String deliveryFeeWaivedReason = null;

        // 仅外卖订单有配送费
        if ("DELIVERY".equals(request.getDiningMethod())) {
            deliveryFee = new BigDecimal("3"); // 默认配送费 3 元

            // v5.3: 黑金会员自动免运费
            if ("black".equals(memberLevel)) {
                deliveryFeeWaived = true;
                deliveryFeeWaivedReason = "BLACK_GOLD_UNLIMITED";
                addonDiscount = addonDiscount.add(deliveryFee); // 免掉的配送费计入商品抵扣
                log.info("黑金会员无限免运费: userId={}, deliveryFee={}", userId, deliveryFee);
            }
            // v5.8: 主券为配送费券时标记免运费
            if ("DELIVERY_FEE".equals(mainCouponType)) {
                deliveryFeeWaived = true;
                deliveryFeeWaivedReason = "COUPON";
            }
        }

        // 配送费券仅限外卖订单（组合引擎已校验配送费券张数上限）
        if (deliveryFeeDiscount.compareTo(BigDecimal.ZERO) > 0 && !"DELIVERY".equals(request.getDiningMethod())) {
            throw new BusinessException("配送费券仅限外卖订单使用");
        }
        if (deliveryFeeDiscount.compareTo(deliveryFee) >= 0) {
            deliveryFeeWaived = true;
            deliveryFeeWaivedReason = "COUPON";
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

        // 取餐码在支付/接单时生成（待支付订单不生成取餐码）
        LocalDateTime now = LocalDateTime.now();
        LocalDate businessDate = pickupCodeService.calculateBusinessDate(now);

        // ============================================================
        // 计算预估奖励（复用 OrderRewardService 全等级分段口径）
        // - 奖励基数 = 实付 - 配送费（配送费不计入，与发放一致）
        // - 按 currentExp 跨越的等级阈值逐段计倍率，黑卡段走加速包
        // ============================================================
        int estimatedExp = 0;
        int estimatedPoints = 0;
        BigDecimal effectiveRate = BigDecimal.ONE; // 实际生效的积分倍率

        // 只要实付金额 > 0，就按奖励基数计算 EXP 和积分
        if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rewardBase = payAmount.subtract(deliveryFee).max(BigDecimal.ZERO);
            try {
                MemberDTO member = memberService.getMemberByUserId(userId);
                OrderRewardService.RewardEstimate est = rewardService.estimateRewards(rewardBase, member);
                estimatedExp = est.expEarned;
                estimatedPoints = est.pointsEarned;
                effectiveRate = est.effectiveRate;
            } catch (Exception e) {
                log.warn("计算预估奖励失败", e);
                estimatedExp = rewardBase.setScale(0, RoundingMode.HALF_UP).intValue();
                estimatedPoints = estimatedExp;
            }
        }

        // 创建订单主表
        ShopOrder order = new ShopOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setIdempotencyKey(idempotencyKey);
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
        order.setStatus("pending"); // 待支付：支付成功后由 accept 自动接单转 preparing
        order.setRemark(request.getRemark());
        order.setStoreId(1L);
        order.setBusinessDate(businessDate);
        order.setDiningMethod(request.getDiningMethod()); // v5.0: 用餐方式
        // v5.3: 配送费相关字段
        order.setDeliveryFee(deliveryFee);
        order.setDeliveryFeeWaived(deliveryFeeWaived);
        order.setDeliveryFeeWaivedReason(deliveryFeeWaivedReason);
        // v6.4: 外送预计送达时间（配送到点自动确认已完成）
        if ("DELIVERY".equals(request.getDiningMethod())) {
            order.setExpectedDeliveryAt(now.plusMinutes(deliveryEtaMinutes));
            // v6.5: 外送快照收货人信息（列表/详情展示配送地址）
            order.setReceiverName(request.getReceiverName());
            order.setReceiverAddress(request.getReceiverAddress());
        }
        // 预留电话：自提/外送都落库（详情页"预留电话"行展示；外送时 receiverPhone 也存收货人电话）
        order.setReceiverPhone(request.getReceiverPhone());
        // v5.0: 保存附加券ID列表用于取消时回滚
        if (!addonCouponIds.isEmpty()) {
            try {
                order.setAppliedAddonCouponIds(objectMapper
                        .writeValueAsString(addonCouponIds));
            } catch (Exception e) {
                log.warn("序列化附加券ID失败: {}", e.getMessage());
            }
        }
        // v5.8: 保存每张券抵扣明细快照（订单详情页逐条展示，与确认页 preview.couponDetails 同构）
        order.setCouponDetails(couponDetailsJson);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        // C1: DB 写入独立事务。useCouponWithResult 已在外层非 TX 执行。
        // 如果此处失败但券已核销，publishCouponRollbackEvent 通过 Outbox 异步回滚。
        try {
            doCreateOrderInTx(order, orderItems);
        } catch (DuplicateKeyException e) {
            ShopOrder duplicate = idempotencyKey != null
                    ? orderMapper.selectByUserAndIdempotencyKey(userId, idempotencyKey)
                    : null;
            if (duplicate != null) {
                log.info("幂等键并发冲突，返回已创建订单: userId={}, orderId={}", userId, duplicate.getId());
                ShopOrderDTO replay = orderDtoEnricher.toOrderDTO(duplicate,
                        orderDtoEnricher.getOrderItemsByOrderId(duplicate.getId()));
                replay.setIdempotentReplay(true);
                return replay;
            }
            throw new BusinessException(BusinessErrorCode.ORDER_CREATE_FAILED, "订单创建冲突，请稍后重试");
        } catch (Exception e) {
            if (appliedCouponId != null || !addonCouponIds.isEmpty()) {
                log.error("订单落库失败，触发券回滚: orderNo={}, couponId={}, addonIds={}",
                        order.getOrderNo(), appliedCouponId, addonCouponIds);
                orderInfraService.publishCouponRollbackEvent(order);
            }
            log.error("订单落库失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage(), e);
            throw new BusinessException(BusinessErrorCode.ORDER_CREATE_FAILED,
                    "订单创建失败: " + extractMessage(e));
        }

        log.info("订单创建成功: orderNo={}, userId={}, totalAmount={}, items={}",
                order.getOrderNo(), userId, totalAmount, itemsSummary);

        return orderDtoEnricher.toOrderDTO(order, orderItems);
    }

    private void doCreateOrderInTx(ShopOrder order, List<ShopOrderItem> orderItems) {
        transactionTemplate.executeWithoutResult(status -> {
            orderMapper.insert(order);
            orderInfraService.syncPendingTimeoutIndex(order);
            for (ShopOrderItem item : orderItems) {
                item.setOrderId(order.getId());
                orderItemMapper.insert(item);
            }
        });
    }

    // ==================== 订单查询 ====================

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

    /** 合并主券 + 辅券为整组券码列表（分类交给组合引擎，本层不再理解主/辅） */
    private List<String> collectCouponCodes(CreateOrderRequest request) {
        List<String> codes = new ArrayList<>();
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            codes.add(request.getCouponCode().trim());
        }
        if (request.getAddonCouponCodes() != null) {
            for (String code : request.getAddonCouponCodes()) {
                if (code != null && !code.trim().isEmpty()) {
                    codes.add(code.trim());
                }
            }
        }
        return codes;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "CF" + timestamp + random;
    }

    private String extractMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = e.getMessage();
        }
        return msg != null ? msg : "未知错误";
    }
}
