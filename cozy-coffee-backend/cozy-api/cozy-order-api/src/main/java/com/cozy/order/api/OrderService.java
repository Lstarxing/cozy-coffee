package com.cozy.order.api;

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

import java.util.List;
import java.util.Map;

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
     * @param idempotencyKey 幂等键
     * @param request     下单请求
     * @return 订单信息（包含应发放的积分）
     */
    CartCheckResultDTO checkCart(Long userId, String memberLevel, CartCheckRequest request);

    ShopOrderDTO createOrder(Long userId, String memberLevel, String idempotencyKey, CreateOrderRequest request);

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
    Map<String, Long> getOrderStatusCounts();

    /**
     * 接单（生成取餐码，状态改为preparing）
     */
    ShopOrderDTO acceptOrder(Long orderId);

    /**
     * 用户支付成功后自动接单（校验订单归属，状态改为preparing）
     */
    ShopOrderDTO acceptUserOrder(Long orderId, Long userId);

    /**
     * 完成订单（出餐）：自提 → completed、外送 → delivering，仅履约不发奖
     */
    ShopOrderDTO completeOrder(Long orderId);

    /**
     * 用户确认取餐（自提）/ 确认收货（外送）：校验订单归属后发放积分/EXP
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     * @return 发放后的订单
     * @throws RuntimeException 如果订单不属于该用户
     */
    ShopOrderDTO confirmUserOrder(Long orderId, Long userId);

    /**
     * 发放积分/EXP（用户确认或兜底 Job 触发）。
     * CAS 条件更新（rewards_granted=0 且 status 在 completed/delivering），
     * 赢家才发布 ORDER_COMPLETED，多入口并发不会重复发放。
     */
    ShopOrderDTO grantRewards(Long orderId);

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
     * 获取加料主数据目录（管理端，绑定加料组时选择）
     */
    List<ProductAddonDTO> listAddonCatalog();

    /**
     * 保存商品加料组（管理端，全量替换）
     */
    void saveAddonGroups(Long productId, List<AddonGroupRequest> groups);

    // ==================== 内容档案（管理端：产区/单品豆/拼配豆） ====================

    /**
     * 获取产区列表（管理端）
     */
    List<CoffeeOriginDTO> listOrigins();

    /**
     * 新增/更新产区（管理端）
     */
    CoffeeOriginDTO saveOrigin(CoffeeOriginDTO origin);

    /**
     * 删除产区（管理端，被豆档案引用时拒绝）
     */
    void deleteOrigin(Long id);

    /**
     * 获取单品豆列表（管理端）
     */
    List<CoffeeBeanDTO> listBeans();

    /**
     * 新增/更新单品豆（管理端）
     */
    CoffeeBeanDTO saveBean(CoffeeBeanDTO bean);

    /**
     * 删除单品豆（管理端，被 active 商品/拼配引用时拒绝）
     */
    void deleteBean(Long id);

    /**
     * 获取拼配豆列表（管理端）
     */
    List<CoffeeBlendDTO> listBlends();

    /**
     * 新增/更新拼配豆（管理端，composition Σratio=100）
     */
    CoffeeBlendDTO saveBlend(CoffeeBlendDTO blend);

    /**
     * 删除拼配豆（管理端，被 active 商品引用时拒绝）
     */
    void deleteBlend(Long id);

    /**
     * 获取用户月度订单统计数据（v5.0 任务用）
     */
    MonthlyStatsDTO getMonthlyStats(Long userId);
}
