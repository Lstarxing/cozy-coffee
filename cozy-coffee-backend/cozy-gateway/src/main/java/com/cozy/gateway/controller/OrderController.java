package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.service.OrderCoordinatorService;
import com.cozy.gateway.util.AuthUtil;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.response.CartCheckResultDTO;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCoordinatorService orderCoordinatorService;

    @DubboReference(check = false)
    private OrderService orderService;

    @GetMapping("/products")
    public Result<List<CoffeeProductDTO>> listProducts() {
        return Result.success(orderService.listCoffeeProducts());
    }

    @GetMapping("/products/search")
    public Result<List<CoffeeProductDTO>> searchProducts(@RequestParam String q) {
        String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (keyword.isEmpty()) {
            return Result.success(List.of());
        }
        List<CoffeeProductDTO> matches = orderService.listCoffeeProducts().stream()
                .filter(product -> containsIgnoreCase(product.getName(), keyword)
                        || containsIgnoreCase(product.getDescription(), keyword)
                        || containsIgnoreCase(product.getCategory(), keyword))
                .limit(50)
                .toList();
        return Result.success(matches);
    }

    @GetMapping("/banners")
    public Result<List<Map<String, Object>>> listBanners() {
        return Result.success(List.of(
                Map.of("id", 1, "image", "/static/images/banner1.png", "title", "今日咖啡推荐", "target", "/pages/menu/menu"),
                Map.of("id", 2, "image", "/static/images/banner2.png", "title", "会员积分好礼", "target", "/pages/mall/index"),
                Map.of("id", 3, "image", "/static/images/banner3.png", "title", "每日签到", "target", "/pages/signin/index")
        ));
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    @GetMapping("/products/{id}")
    public Result<CoffeeProductDTO> getProduct(@PathVariable Long id) {
        return Result.success(orderService.getProduct(id));
    }

    @PostMapping("/create")
    public Result<ShopOrderDTO> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        ShopOrderDTO order = orderCoordinatorService.createOrder(AuthUtil.requireUserId(), idempotencyKey, request);
        return Result.success(order, "下单成功");
    }

    @PostMapping("/cart/check")
    public Result<CartCheckResultDTO> checkCart(@RequestBody CartCheckRequest request) {
        return Result.success(orderCoordinatorService.checkCart(AuthUtil.requireUserId(), request));
    }

    @GetMapping("/list")
    public Result<List<ShopOrderDTO>> listOrders() {
        return Result.success(orderService.listUserOrders(AuthUtil.requireUserId()));
    }

    @GetMapping("/{id}")
    public Result<ShopOrderDTO> getOrder(@PathVariable Long id) {
        return Result.success(orderService.getOrder(id, AuthUtil.requireUserId()));
    }

    @PostMapping("/{id}/cancel")
    public Result<ShopOrderDTO> cancelOrder(@PathVariable Long id) {
        return Result.success(orderService.cancelUserOrder(id, AuthUtil.requireUserId()), "订单已取消");
    }

    @PostMapping("/{id}/accept")
    public Result<ShopOrderDTO> acceptOrder(@PathVariable Long id) {
        return Result.success(orderService.acceptUserOrder(id, AuthUtil.requireUserId()), "已接单");
    }

    @PostMapping("/{id}/confirm")
    public Result<ShopOrderDTO> confirmOrder(@PathVariable Long id) {
        return Result.success(orderService.confirmUserOrder(id, AuthUtil.requireUserId()), "确认成功，奖励已发放");
    }
}
