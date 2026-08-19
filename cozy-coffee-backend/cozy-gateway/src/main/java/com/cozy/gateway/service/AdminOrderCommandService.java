package com.cozy.gateway.service;

import com.cozy.common.mq.OrderCompletedEvent;
import com.cozy.gateway.cache.AdminOrderCacheEvictor;
import com.cozy.common.exception.NotFoundException;
import com.cozy.gateway.mq.OrderEventProducer;
import com.cozy.gateway.util.AdminCacheUtil;
import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.response.PointsOrderDTO;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.ShopOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理端订单/兑换单命令服务。
 * 封装 Dubbo 调用 + 缓存驱逐 + 事件发布 + 用户信息填充的编排逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderCommandService {

    @DubboReference(check = false)
    private OrderService orderService;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    private final OrderEventProducer orderEventProducer;
    private final AdminOrderCacheEvictor cacheEvictor;
    private final AdminUserEnrichService userEnrichService;

    public ShopOrderDTO getOrderDetail(Long orderId) {
        ShopOrderDTO order = orderService.getOrderDetail(orderId);
        if (order == null) throw new NotFoundException("订单不存在");
        userEnrichService.enrichOrder(order);
        return order;
    }

    public PointsOrderDTO getRedemptionDetail(Long orderId) {
        PointsOrderDTO order = pointsMallService.getRedemptionDetail(orderId);
        if (order == null) throw new NotFoundException("订单不存在");
        userEnrichService.enrichRedemption(order);
        return order;
    }

    public ShopOrderDTO acceptOrder(Long orderId) {
        ShopOrderDTO order = orderService.acceptOrder(orderId);
        AdminCacheUtil.evictOrderAndAnalytics(cacheEvictor);
        return order;
    }

    public ShopOrderDTO completeOrder(Long orderId) {
        ShopOrderDTO order = orderService.completeOrder(orderId);
        AdminCacheUtil.evictOrderAndAnalytics(cacheEvictor);
        // 外送出餐仅进入配送中（delivering），到点自动完成后才发奖励事件
        if ("completed".equalsIgnoreCase(order.getStatus())) {
            publishCompleted(order);
        }
        return order;
    }

    /**
     * 配送中订单到点自动完成（DeliveryAutoCompleteJob 触发），发放积分/EXP 事件。
     */
    public ShopOrderDTO completeDeliveredOrder(Long orderId) {
        ShopOrderDTO order = orderService.completeDeliveredOrder(orderId);
        AdminCacheUtil.evictOrderAndAnalytics(cacheEvictor);
        publishCompleted(order);
        return order;
    }

    private void publishCompleted(ShopOrderDTO order) {
        orderEventProducer.publishOrderCompleted(OrderCompletedEvent.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .payAmount(order.getPayAmount())
                .expEarned(order.getExpEarned())
                .pointsEarned(order.getPointsEarned())
                .isFirstOrder(order.getIsFirstOrder())
                .hasNewProduct(order.getHasNewProduct())
                .isDelivery("DELIVERY".equals(order.getDiningMethod()))
                .occurredAt(LocalDateTime.now())
                .build());
    }

    public ShopOrderDTO cancelOrder(Long orderId) {
        ShopOrderDTO order = orderService.cancelOrder(orderId);
        AdminCacheUtil.evictOrderAndAnalytics(cacheEvictor);
        return order;
    }

    public PointsOrderDTO processRedemption(Long orderId) {
        PointsOrderDTO order = pointsMallService.updateOrderStatus(orderId, "processing");
        AdminCacheUtil.evictAnalytics(cacheEvictor);
        return order;
    }

    public PointsOrderDTO shipRedemption(Long orderId, String company, String trackingNo) {
        PointsOrderDTO order = pointsMallService.updateShipping(orderId, company, trackingNo);
        AdminCacheUtil.evictAnalytics(cacheEvictor);
        return order;
    }

    public PointsOrderDTO completeRedemption(Long orderId) {
        PointsOrderDTO order = pointsMallService.updateOrderStatus(orderId, "completed");
        AdminCacheUtil.evictAnalytics(cacheEvictor);
        return order;
    }

    public void deleteRedemption(Long orderId) {
        pointsMallService.deleteOrder(orderId);
        AdminCacheUtil.evictAnalytics(cacheEvictor);
    }
}
