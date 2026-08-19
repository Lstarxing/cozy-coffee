package com.cozy.order.service.impl;

import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.response.CartCheckResultDTO;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.MonthlyStatsDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.service.OrderPreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;
import java.util.Map;

/**
 * 订单服务 Dubbo 入口 - thin delegator。
 * 全部业务逻辑委托给 6 个子 Service：
 * OrderQueryService / OrderCreationService / OrderCommandService / ProductAdminService
 * OrderDtoEnricher / OrderInfraService
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderQueryService queryService;
    private final OrderCreationService creationService;
    private final OrderCommandService commandService;
    private final ProductAdminService productAdminService;
    private final OrderPreviewService previewService;

    // ==================== 商品查询 ====================

    @Override
    public List<CoffeeProductDTO> listCoffeeProducts() {
        return queryService.listCoffeeProducts();
    }

    @Override
    public CoffeeProductDTO getProduct(Long productId) {
        return queryService.getProduct(productId);
    }

    @Override
    public List<CoffeeProductDTO> listAllProducts() {
        return queryService.listAllProducts();
    }

    // ==================== 订单查询 ====================

    @Override
    public List<ShopOrderDTO> listUserOrders(Long userId) {
        return queryService.listUserOrders(userId);
    }

    @Override
    public ShopOrderDTO getOrder(Long orderId, Long userId) {
        return queryService.getOrder(orderId, userId);
    }

    @Override
    public ShopOrderDTO getOrderDetail(Long orderId) {
        return queryService.getOrderDetail(orderId);
    }

    @Override
    public List<ShopOrderDTO> listAllOrders(String status) {
        return queryService.listAllOrders(status);
    }

    @Override
    public Map<String, Long> getOrderStatusCounts() {
        return queryService.getOrderStatusCounts();
    }

    @Override
    public MonthlyStatsDTO getMonthlyStats(Long userId) {
        return queryService.getMonthlyStats(userId);
    }

    // ==================== 订单创建 ====================

    @Override
    public CartCheckResultDTO checkCart(Long userId, String memberLevel, CartCheckRequest request) {
        return previewService.preview(userId, memberLevel, request);
    }

    @Override
    public ShopOrderDTO createOrder(Long userId, String memberLevel, String idempotencyKey, CreateOrderRequest request) {
        return creationService.createOrder(userId, memberLevel, idempotencyKey, request);
    }

    // ==================== 订单状态变更 ====================

    @Override
    public ShopOrderDTO updateOrderStatus(Long orderId, String status) {
        return commandService.updateOrderStatus(orderId, status);
    }

    @Override
    public ShopOrderDTO acceptOrder(Long orderId) {
        return commandService.acceptOrder(orderId);
    }

    @Override
    public ShopOrderDTO acceptUserOrder(Long orderId, Long userId) {
        return commandService.acceptUserOrder(orderId, userId);
    }

    @Override
    public ShopOrderDTO completeOrder(Long orderId) {
        return commandService.completeOrder(orderId);
    }

    @Override
    public ShopOrderDTO completeDeliveredOrder(Long orderId) {
        return commandService.completeDeliveredOrder(orderId);
    }

    @Override
    public ShopOrderDTO cancelOrder(Long orderId) {
        return commandService.cancelOrder(orderId);
    }

    @Override
    public ShopOrderDTO cancelUserOrder(Long orderId, Long userId) {
        return commandService.cancelUserOrder(orderId, userId);
    }

    // ==================== 商品管理（管理端）====================

    @Override
    public CoffeeProductDTO addProduct(CoffeeProductDTO dto) {
        return productAdminService.addProduct(dto);
    }

    @Override
    public CoffeeProductDTO updateProduct(Long productId, CoffeeProductDTO dto) {
        return productAdminService.updateProduct(productId, dto);
    }

    @Override
    public void deleteProduct(Long productId) {
        productAdminService.deleteProduct(productId);
    }

    @Override
    public CoffeeProductDTO toggleProductStatus(Long productId) {
        return productAdminService.toggleProductStatus(productId);
    }
}
