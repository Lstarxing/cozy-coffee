package com.cozy.gateway.service;

import com.cozy.gateway.cache.AdminOrderCacheEvictor;
import com.cozy.common.exception.NotFoundException;
import com.cozy.gateway.util.AdminCacheUtil;
import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.response.PointsOrderDTO;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.ShopOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 管理端订单/兑换单命令服务。
 * 封装 Dubbo 调用 + 缓存驱逐 + 用户信息填充的编排逻辑。
 * 订单完成事件（ORDER_COMPLETED）已下沉到 order-provider 发奖时统一发布，此处不再发布。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderCommandService {

    @DubboReference(check = false)
    private OrderService orderService;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

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
        // 出餐仅履约：自提 → completed、外送 → delivering；奖励待用户确认/兜底 Job 发放（order-provider 统一发布事件）
        ShopOrderDTO order = orderService.completeOrder(orderId);
        AdminCacheUtil.evictOrderAndAnalytics(cacheEvictor);
        return order;
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
