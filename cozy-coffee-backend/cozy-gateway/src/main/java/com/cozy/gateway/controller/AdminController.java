package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.PointsMallService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.dto.response.PointsOrderDTO;
import com.cozy.member.dto.response.PointsProductDTO;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.response.UserDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端 API 控制器
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @DubboReference(check = false)
    private UserService userService;

    @DubboReference(check = false)
    private MemberService memberService;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    @DubboReference(check = false)
    private OrderService orderService;

    // ==================== 控制台统计 ====================

    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // 获取用户总数
            List<UserDTO> users = userService.listAllUsers();
            stats.put("totalUsers", users != null ? users.size() : 0);

            // 获取今日订单
            List<ShopOrderDTO> allOrders = orderService.listAllOrders(null);
            long todayOrders = allOrders != null ? allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null &&
                            o.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()))
                    .count() : 0;
            stats.put("todayOrders", todayOrders);

            // 今日营收
            BigDecimal todayRevenue = allOrders != null ? allOrders.stream()
                    .filter(o -> o.getCreatedAt() != null &&
                            o.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()) &&
                            !"cancelled".equals(o.getStatus()))
                    .map(ShopOrderDTO::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
            stats.put("todayRevenue", todayRevenue);

            // 待处理订单数
            long pendingOrders = allOrders != null ? allOrders.stream()
                    .filter(o -> "pending".equals(o.getStatus()))
                    .count() : 0;
            stats.put("pendingOrders", pendingOrders);

            // 待处理兑换数（暂时模拟）
            stats.put("pendingRedemptions", 0);

        } catch (Exception e) {
            stats.put("totalUsers", 0);
            stats.put("todayOrders", 0);
            stats.put("todayRevenue", 0);
            stats.put("pendingOrders", 0);
            stats.put("pendingRedemptions", 0);
        }
        return Result.success(stats);
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<List<UserDTO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        try {
            List<UserDTO> users = userService.listAllUsers();
            return Result.success(users);
        } catch (Exception e) {
            return Result.error("获取用户列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/points")
    public Result<Void> adjustUserPoints(
            @PathVariable Long userId,
            @RequestParam int amount,
            @RequestParam String reason) {
        try {
            if (amount == 0) {
                return Result.error("积分调整数量不能为0");
            }
            memberService.addPoints(userId, amount, "admin_adjust", reason);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("调整积分失败: " + e.getMessage());
        }
    }

    // ==================== 咖啡订单管理 ====================

    @GetMapping("/orders")
    public Result<List<ShopOrderDTO>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<ShopOrderDTO> orders = orderService.listAllOrders(status);

            // 后端过滤（如果Service不支持多条件，在这里过滤）
            if (orderNo != null && !orderNo.isEmpty()) {
                orders = orders.stream()
                        .filter(o -> o.getOrderNo() != null && o.getOrderNo().contains(orderNo))
                        .collect(java.util.stream.Collectors.toList());
            }
            if (startDate != null && !startDate.isEmpty()) {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                orders = orders.stream()
                        .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().toLocalDate().isBefore(start))
                        .collect(java.util.stream.Collectors.toList());
            }
            if (endDate != null && !endDate.isEmpty()) {
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                orders = orders.stream()
                        .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().toLocalDate().isAfter(end))
                        .collect(java.util.stream.Collectors.toList());
            }

            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/accept")
    public Result<ShopOrderDTO> acceptOrder(@PathVariable Long orderId) {
        try {
            ShopOrderDTO order = orderService.acceptOrder(orderId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("接单失败: " + e.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/complete")
    public Result<ShopOrderDTO> completeOrder(@PathVariable Long orderId) {
        try {
            ShopOrderDTO order = orderService.completeOrder(orderId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("完成订单失败: " + e.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/cancel")
    public Result<ShopOrderDTO> cancelOrder(@PathVariable Long orderId) {
        try {
            ShopOrderDTO order = orderService.cancelOrder(orderId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("取消订单失败: " + e.getMessage());
        }
    }

    // ==================== 咖啡商品管理 ====================

    @GetMapping("/products/coffee")
    public Result<List<CoffeeProductDTO>> listCoffeeProducts() {
        try {
            List<CoffeeProductDTO> products = orderService.listAllProducts();
            return Result.success(products);
        } catch (Exception e) {
            return Result.error("获取咖啡商品失败: " + e.getMessage());
        }
    }

    @PostMapping("/products/coffee")
    public Result<CoffeeProductDTO> addCoffeeProduct(@RequestBody CoffeeProductDTO product) {
        try {
            CoffeeProductDTO created = orderService.addProduct(product);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error("添加商品失败: " + e.getMessage());
        }
    }

    @PutMapping("/products/coffee/{productId}")
    public Result<CoffeeProductDTO> updateCoffeeProduct(
            @PathVariable Long productId,
            @RequestBody CoffeeProductDTO product) {
        try {
            CoffeeProductDTO updated = orderService.updateProduct(productId, product);
            return Result.success(updated);
        } catch (Exception e) {
            return Result.error("更新商品失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/products/coffee/{productId}")
    public Result<Void> deleteCoffeeProduct(@PathVariable Long productId) {
        try {
            orderService.deleteProduct(productId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除商品失败: " + e.getMessage());
        }
    }

    @PutMapping("/products/coffee/{productId}/status")
    public Result<CoffeeProductDTO> toggleCoffeeProductStatus(@PathVariable Long productId) {
        try {
            CoffeeProductDTO product = orderService.toggleProductStatus(productId);
            return Result.success(product);
        } catch (Exception e) {
            return Result.error("切换状态失败: " + e.getMessage());
        }
    }

    // ==================== 积分商品管理 ====================

    @GetMapping("/products/points")
    public Result<List<PointsProductDTO>> listPointsProducts() {
        try {
            List<PointsProductDTO> products = pointsMallService.listActiveProducts();
            return Result.success(products);
        } catch (Exception e) {
            return Result.error("获取积分商品失败: " + e.getMessage());
        }
    }

    // ==================== 积分兑换订单管理 ====================

    @GetMapping("/redemptions")
    public Result<List<PointsOrderDTO>> listRedemptions(
            @RequestParam(required = false) String status) {
        try {
            List<PointsOrderDTO> orders = pointsMallService.listAllOrders(status);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取兑换订单失败: " + e.getMessage());
        }
    }

    @PostMapping("/redemptions/{orderId}/process")
    public Result<PointsOrderDTO> processRedemption(@PathVariable Long orderId) {
        try {
            PointsOrderDTO order = pointsMallService.updateOrderStatus(orderId, "processing");
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("备货失败: " + e.getMessage());
        }
    }

    @PostMapping("/redemptions/{orderId}/ship")
    public Result<PointsOrderDTO> shipRedemption(
            @PathVariable Long orderId,
            @RequestParam String company,
            @RequestParam String trackingNo) {
        try {
            PointsOrderDTO order = pointsMallService.updateShipping(orderId, company, trackingNo);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("发货失败: " + e.getMessage());
        }
    }

    @PostMapping("/redemptions/{orderId}/complete")
    public Result<PointsOrderDTO> completeRedemption(@PathVariable Long orderId) {
        try {
            PointsOrderDTO order = pointsMallService.updateOrderStatus(orderId, "completed");
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("完成订单失败: " + e.getMessage());
        }
    }
}
