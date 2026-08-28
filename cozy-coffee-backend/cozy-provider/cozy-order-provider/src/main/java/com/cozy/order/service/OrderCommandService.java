package com.cozy.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.mall.api.PointsMallService;
import com.cozy.common.exception.BusinessException;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.dto.response.ShopOrderItemDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.entity.ShopOrderItem;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ShopOrderMapper;
import com.cozy.order.mapper.ShopOrderItemMapper;
import com.cozy.order.mq.OrderCompletedEventPublisher;
import com.cozy.order.service.PickupCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final ShopOrderMapper orderMapper;
    private final ShopOrderItemMapper orderItemMapper;
    private final CoffeeProductMapper productMapper;
    private final PickupCodeService pickupCodeService;
    private final OrderRewardService rewardService;
    private final OrderDtoEnricher orderDtoEnricher;
    private final OrderInfraService orderInfraService;
    private final TransactionTemplate transactionTemplate;
    private final OrderCompletedEventPublisher orderCompletedEventPublisher;

    @DubboReference(check = false)
    private MemberService memberService;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    public ShopOrderDTO updateOrderStatus(Long orderId, String status) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setStatus(status);
        orderMapper.updateById(order);
        orderInfraService.syncPendingTimeoutIndex(order);
        return orderDtoEnricher.toOrderDTO(order, null);
    }

    @Transactional
    public ShopOrderDTO acceptOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OrderStateMachine current = OrderStateMachine.from(order.getStatus());
        current.assertCanTransition(OrderStateMachine.PREPARING);

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

        order.setStatus(OrderStateMachine.PREPARING.value());
        orderMapper.updateById(order);
        orderInfraService.syncPendingTimeoutIndex(order);
        log.info("订单接单: orderId={}, orderNo={}", orderId, order.getOrderNo());
        confirmOrderCoupon(order);
        return orderDtoEnricher.toOrderDTO(order, null);
    }

    /**
     * 用户支付成功后自动接单：校验订单归属后复用接单逻辑。
     */
    @Transactional
    public ShopOrderDTO acceptUserOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        OrderStateMachine current = OrderStateMachine.from(order.getStatus());
        current.assertCanTransition(OrderStateMachine.PREPARING);

        if (order.getPickupCode() == null || order.getPickupCode().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            String pickupCode = pickupCodeService.generatePickupCode(1L, now);
            LocalDate businessDate = pickupCodeService.calculateBusinessDate(now);
            order.setPickupCode(pickupCode);
            order.setBusinessDate(businessDate);
            order.setPickupCodeGeneratedAt(now);
            order.setStoreId(1L);
        }

        order.setStatus(OrderStateMachine.PREPARING.value());
        orderMapper.updateById(order);
        orderInfraService.syncPendingTimeoutIndex(order);
        log.info("订单支付后自动接单: orderId={}, orderNo={}", orderId, order.getOrderNo());
        confirmOrderCoupon(order);
        return orderDtoEnricher.toOrderDTO(order, null);
    }

    /** 订单支付/接单成功后确认优惠券（FROZEN → USED），失败不阻塞接单。 */
    private void confirmOrderCoupon(ShopOrder order) {
        if (order == null || order.getAppliedCouponId() == null) return;
        try {
            pointsMallService.confirmCoupon(order.getAppliedCouponId(), order.getUserId());
        } catch (Exception e) {
            log.warn("确认优惠券失败(不影响接单): orderId={}, couponId={}, error={}",
                    order.getId(), order.getAppliedCouponId(), e.getMessage());
        }
    }

    /**
     * 出餐/完成订单（仅履约，不再发放奖励）。
     * - 自提：preparing → completed（写入 completedAt，奖励待用户「确认取餐」后发放）
     * - 外送：preparing → delivering（配送中），奖励由用户「确认收货」或兜底 Job 发放
     */
    public ShopOrderDTO completeOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OrderStateMachine current = OrderStateMachine.from(order.getStatus());

        // 外送出餐 → 配送中（等用户确认收货/兜底自动确认发放奖励）
        if ("DELIVERY".equalsIgnoreCase(order.getDiningMethod())) {
            current.assertCanTransition(OrderStateMachine.DELIVERING);
            order.setStatus(OrderStateMachine.DELIVERING.value());
            orderMapper.updateById(order);
            orderInfraService.syncPendingTimeoutIndex(order);
            log.info("外送出餐: orderId={}, orderNo={}, 进入配送中", orderId, order.getOrderNo());
            return orderDtoEnricher.toOrderDTO(order, null);
        }

        // 自提出餐 → 完成（奖励待用户确认取餐）
        current.assertCanTransition(OrderStateMachine.COMPLETED);
        order.setStatus(OrderStateMachine.COMPLETED.value());
        order.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        orderInfraService.syncPendingTimeoutIndex(order);
        log.info("自提出餐: orderId={}, orderNo={}, 已完成待确认取餐", orderId, order.getOrderNo());
        return orderDtoEnricher.toOrderDTO(order, null);
    }

    /**
     * 用户确认取餐（自提）/ 确认收货（外送）：校验订单归属后发放积分/EXP。
     * 发奖走 CAS，只有赢家发布 ORDER_COMPLETED。
     */
    public ShopOrderDTO confirmUserOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权确认该订单");
        }
        return grantRewards(orderId);
    }

    /**
     * 发放积分/EXP（自提/外送共用；由用户确认或兜底 Job 触发）。
     * CAS 条件更新（rewards_granted=0 且 status 在 completed/delivering），
     * 影响 1 行的赢家才发布 ORDER_COMPLETED，多入口并发不会重复发放。
     */
    public ShopOrderDTO grantRewards(Long orderId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (Boolean.TRUE.equals(order.getRewardsGranted())) {
            log.info("订单奖励已发放，跳过: orderId={}", orderId);
            return orderDtoEnricher.toOrderDTO(order, null);
        }
        OrderStateMachine current = OrderStateMachine.from(order.getStatus());
        if (current != OrderStateMachine.COMPLETED && current != OrderStateMachine.DELIVERING) {
            log.info("订单状态不可确认发奖: orderId={}, status={}", orderId, order.getStatus());
            return orderDtoEnricher.toOrderDTO(order, null);
        }

        // ===== 事务外：所有计算和远程调用 =====
        BigDecimal payAmount = order.getPayAmount() != null ? order.getPayAmount() : order.getTotalAmount();
        // 配送费不计入积分/成长值：奖励以商品实付金额为基数
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal rewardBase = payAmount.subtract(deliveryFee).max(BigDecimal.ZERO);
        int expEarned = rewardBase.setScale(0, RoundingMode.HALF_UP).intValue();

        // C2: Dubbo 远程调用移出 @Transactional
        String memberLevel = resolveMemberLevel(order.getUserId());

        int pointsEarned = resolvePointsEarned(order, rewardBase, memberLevel);
        boolean isFirstOrder = checkFirstOrder(order);
        boolean hasNewProduct = checkNewProduct(order);
        boolean isDelivery = "DELIVERY".equals(order.getDiningMethod());

        log.info("确认发奖(奖励已解耦到MQ): orderId={}, exp={}, points={}, isFirst={}, isDelivery={}, hasNew={}",
                order.getId(), expEarned, pointsEarned, isFirstOrder, isDelivery, hasNewProduct);

        // ===== CAS：赢家才发事件 =====
        if (tryGrantRewards(order.getId(), expEarned, pointsEarned)) {
            orderCompletedEventPublisher.publish(order, expEarned, pointsEarned, isFirstOrder, hasNewProduct, isDelivery);
        }
        return orderDtoEnricher.toOrderDTO(orderMapper.selectById(orderId), null);
    }

    private boolean tryGrantRewards(Long orderId, int expEarned, int pointsEarned) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            ShopOrder update = new ShopOrder();
            update.setStatus(OrderStateMachine.COMPLETED.value());
            update.setExpEarned(expEarned);
            update.setPointsEarned(pointsEarned);
            update.setRewardsGranted(true);
            update.setCompletedAt(LocalDateTime.now());
            update.setUpdatedAt(LocalDateTime.now());
            return orderMapper.update(update, new LambdaUpdateWrapper<ShopOrder>()
                    .eq(ShopOrder::getId, orderId)
                    .eq(ShopOrder::getRewardsGranted, false)
                    .in(ShopOrder::getStatus,
                            OrderStateMachine.COMPLETED.value(),
                            OrderStateMachine.DELIVERING.value())) == 1;
        }));
    }

    private String resolveMemberLevel(Long userId) {
        try {
            var memberInfo = memberService.getMemberInfo(userId);
            if (memberInfo != null && memberInfo.getMemberLevel() != null) {
                return memberInfo.getMemberLevel();
            }
        } catch (Exception e) {
            log.warn("获取会员信息失败，使用默认倍率: userId={}", userId, e);
        }
        return "basic";
    }

    private int resolvePointsEarned(ShopOrder order, BigDecimal payAmount, String memberLevel) {
        int pointsEarned = (order.getPointsEarned() != null) ? order.getPointsEarned() : 0;
        if (pointsEarned <= 0 && payAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = rewardService.getPointsRate(memberLevel);
            pointsEarned = payAmount.multiply(rate).setScale(0, RoundingMode.HALF_UP).intValue();
        }
        return pointsEarned;
    }

    private boolean checkFirstOrder(ShopOrder order) {
        try {
            LambdaQueryWrapper<ShopOrder> firstOrderCheck = new LambdaQueryWrapper<>();
            firstOrderCheck.eq(ShopOrder::getUserId, order.getUserId())
                    .eq(ShopOrder::getStatus, "completed")
                    .ne(ShopOrder::getId, order.getId());
            return orderMapper.selectCount(firstOrderCheck) == 0;
        } catch (Exception e) {
            log.warn("首单检测失败: orderId={}", order.getId(), e);
            return false;
        }
    }

    private boolean checkNewProduct(ShopOrder order) {
        try {
            LambdaQueryWrapper<ShopOrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(ShopOrderItem::getOrderId, order.getId());
            List<ShopOrderItem> items = orderItemMapper.selectList(itemWrapper);
            if (!items.isEmpty()) {
                List<Long> productIds = items.stream()
                        .map(ShopOrderItem::getProductId)
                        .distinct().collect(Collectors.toList());
                LambdaQueryWrapper<CoffeeProduct> productWrapper = new LambdaQueryWrapper<>();
                productWrapper.in(CoffeeProduct::getId, productIds)
                        .eq(CoffeeProduct::getIsNewProduct, true);
                return productMapper.selectCount(productWrapper) > 0;
            }
        } catch (Exception e) {
            log.warn("新品检测失败: orderId={}", order.getId(), e);
        }
        return false;
    }

    @Transactional
    public ShopOrderDTO cancelOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OrderStateMachine current = OrderStateMachine.from(order.getStatus());
        current.assertCanTransition(OrderStateMachine.CANCELLED);
        order.setStatus(OrderStateMachine.CANCELLED.value());
        orderMapper.updateById(order);
        orderInfraService.syncPendingTimeoutIndex(order);

        // v6.3: 券回滚走 Outbox 模式异步投递，跨库最终一致
        orderInfraService.publishCouponRollbackEvent(order);

        log.info("订单取消: orderId={}, orderNo={}", orderId, order.getOrderNo());
        return orderDtoEnricher.toOrderDTO(order, null);
    }

    @Transactional
    public ShopOrderDTO cancelUserOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权取消该订单");
        }
        OrderStateMachine current = OrderStateMachine.from(order.getStatus());
        current.assertCanTransition(OrderStateMachine.CANCELLED);
        order.setStatus(OrderStateMachine.CANCELLED.value());
        orderMapper.updateById(order);
        orderInfraService.syncPendingTimeoutIndex(order);

        // v6.3: 券回滚走 Outbox 模式异步投递
        orderInfraService.publishCouponRollbackEvent(order);

        return orderDtoEnricher.toOrderDTO(order, null);
    }

}
