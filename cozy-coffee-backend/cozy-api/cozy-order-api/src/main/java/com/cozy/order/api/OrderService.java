package com.cozy.order.api;

import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderDTO;

import java.util.List;

/**
 * 咖啡订单服务接口 - 独立微服务
 */
public interface OrderService {

    /**
     * 获取咖啡商品列表
     */
    List<CoffeeProductDTO> listCoffeeProducts();

    /**
     * 获取单个商品详情
     */
    CoffeeProductDTO getProduct(Long productId);

    /**
     * 创建咖啡订单
     * 
     * @param userId      用户ID
     * @param memberLevel 会员等级（用于计算积分倍率）
     * @param request     下单请求
     * @return 订单信息（包含应发放的积分）
     */
    ShopOrderDTO createOrder(Long userId, String memberLevel, CreateOrderRequest request);

    /**
     * 获取用户订单列表
     */
    List<ShopOrderDTO> listUserOrders(Long userId);

    /**
     * 获取订单详情
     */
    ShopOrderDTO getOrder(Long orderId, Long userId);

    // ==================== 管理端方法 ====================

    /**
     * 获取订单详情（管理端，含完整信息）
     */
    ShopOrderDTO getOrderDetail(Long orderId);

    /**
     * 获取所有订单列表（管理端用）
     */
    List<ShopOrderDTO> listAllOrders(String status);

    /**
     * 更新订单状态（管理端用）
     * 
     * @param orderId 订单ID
     * @param status  新状态：pending/preparing/completed/cancelled
     * @return 更新后的订单
     */
    ShopOrderDTO updateOrderStatus(Long orderId, String status);

    /**
     * 获取各状态订单数量（管理端用）
     * 
     * @return status -> count
     */
    java.util.Map<String, Long> getOrderStatusCounts();

    /**
     * 接单（生成取餐码，状态改为preparing）
     */
    ShopOrderDTO acceptOrder(Long orderId);

    /**
     * 完成订单
     */
    ShopOrderDTO completeOrder(Long orderId);

    /**
     * 取消订单（管理端，无需验证用户）
     */
    ShopOrderDTO cancelOrder(Long orderId);

    // ==================== 用户端方法 ====================

    /**
     * 用户取消订单（需验证订单归属）
     * 
     * @param orderId 订单ID
     * @param userId  用户ID
     * @return 取消后的订单
     * @throws RuntimeException 如果订单不属于该用户
     */
    ShopOrderDTO cancelUserOrder(Long orderId, Long userId);

    // ==================== 商品管理 (管理端) ====================

    /**
     * 获取所有咖啡商品（包括下架）
     */
    List<CoffeeProductDTO> listAllProducts();

    /**
     * 添加咖啡商品
     */
    CoffeeProductDTO addProduct(CoffeeProductDTO product);

    /**
     * 更新咖啡商品
     */
    CoffeeProductDTO updateProduct(Long productId, CoffeeProductDTO product);

    /**
     * 删除咖啡商品
     */
    void deleteProduct(Long productId);

    /**
     * 切换商品上下架状态
     */
    CoffeeProductDTO toggleProductStatus(Long productId);

    /**
     * 获取用户月度订单统计数据（v5.0 任务用）
     */
    com.cozy.order.dto.response.MonthlyStatsDTO getMonthlyStats(Long userId);
}
