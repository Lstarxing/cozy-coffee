package com.cozy.order.service.impl;
import com.cozy.order.service.converter.OrderDtoEnricher;

import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.AddonGroupRequest;
import com.cozy.order.dto.response.CartCheckResultDTO;
import com.cozy.order.dto.response.CoffeeBeanDTO;
import com.cozy.order.dto.response.CoffeeBlendDTO;
import com.cozy.order.dto.response.CoffeeOriginDTO;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.MonthlyStatsDTO;
import com.cozy.order.dto.response.ProductAddonDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.service.order.OrderPreviewer;
import com.cozy.order.service.order.OrderQueryService;
import com.cozy.order.service.order.OrderCreator;
import com.cozy.order.service.order.OrderCommandService;
import com.cozy.order.service.product.CoffeeContentAdminService;
import com.cozy.order.service.product.ProductAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;
import java.util.Map;

/**
 * 订单服务 Dubbo 入口 - thin delegator。
 * 全部业务逻辑委托给子 Service：
 * OrderQueryService / OrderCreator / OrderCommandService / ProductAdminService
 * CoffeeContentAdminService / OrderPreviewer
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderQueryService queryService;
    private final OrderCreator creator;
    private final OrderCommandService commandService;
    private final ProductAdminService productAdminService;
    private final CoffeeContentAdminService contentAdminService;
    private final OrderPreviewer previewer;

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
        return previewer.preview(userId, memberLevel, request);
    }

    @Override
    public ShopOrderDTO createOrder(Long userId, String memberLevel, String idempotencyKey, CreateOrderRequest request) {
        return creator.createOrder(userId, memberLevel, idempotencyKey, request);
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
    public ShopOrderDTO confirmUserOrder(Long orderId, Long userId) {
        return commandService.confirmUserOrder(orderId, userId);
    }

    @Override
    public ShopOrderDTO grantRewards(Long orderId) {
        return commandService.grantRewards(orderId);
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

    @Override
    public List<ProductAddonDTO> listAddonCatalog() {
        return productAdminService.listAddonCatalog();
    }

    @Override
    public void saveAddonGroups(Long productId, List<AddonGroupRequest> groups) {
        productAdminService.saveAddonGroups(productId, groups);
    }

    // ==================== 内容档案（管理端：产区/单品豆/拼配豆） ====================

    @Override
    public List<CoffeeOriginDTO> listOrigins() {
        return contentAdminService.listOrigins();
    }

    @Override
    public CoffeeOriginDTO saveOrigin(CoffeeOriginDTO origin) {
        return contentAdminService.saveOrigin(origin);
    }

    @Override
    public void deleteOrigin(Long id) {
        contentAdminService.deleteOrigin(id);
    }

    @Override
    public List<CoffeeBeanDTO> listBeans() {
        return contentAdminService.listBeans();
    }

    @Override
    public CoffeeBeanDTO saveBean(CoffeeBeanDTO bean) {
        return contentAdminService.saveBean(bean);
    }

    @Override
    public void deleteBean(Long id) {
        contentAdminService.deleteBean(id);
    }

    @Override
    public List<CoffeeBlendDTO> listBlends() {
        return contentAdminService.listBlends();
    }

    @Override
    public CoffeeBlendDTO saveBlend(CoffeeBlendDTO blend) {
        return contentAdminService.saveBlend(blend);
    }

    @Override
    public void deleteBlend(Long id) {
        contentAdminService.deleteBlend(id);
    }
}
