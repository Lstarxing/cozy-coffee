package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.service.AdminListService;
import com.cozy.gateway.service.AdminOrderCommandService;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    @DubboReference(check = false)
    private OrderService orderService;

    private final AdminListService listService;
    private final AdminOrderCommandService commandService;

    @GetMapping("/orders/{orderId}")
    public Result<ShopOrderDTO> getOrderDetail(@PathVariable Long orderId) {
        return Result.success(commandService.getOrderDetail(orderId));
    }

    @GetMapping("/orders/counts")
    public Result<Map<String, Long>> getOrderCounts() {
        return Result.success(orderService.getOrderStatusCounts());
    }

    @GetMapping("/orders")
    public Result<List<ShopOrderDTO>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "false") boolean noCache) {
        return Result.success(listService.listOrders(status, orderNo, keyword, userId, startDate, endDate, noCache));
    }

    @PostMapping("/orders/{orderId}/accept")
    public Result<ShopOrderDTO> acceptOrder(@PathVariable Long orderId) {
        return Result.success(commandService.acceptOrder(orderId));
    }

    @PostMapping("/orders/{orderId}/complete")
    public Result<ShopOrderDTO> completeOrder(@PathVariable Long orderId) {
        return Result.success(commandService.completeOrder(orderId));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public Result<ShopOrderDTO> cancelOrder(@PathVariable Long orderId) {
        return Result.success(commandService.cancelOrder(orderId));
    }

    @GetMapping("/products/coffee")
    public Result<List<CoffeeProductDTO>> listCoffeeProducts() { return Result.success(orderService.listAllProducts()); }

    @PostMapping("/products/coffee")
    public Result<CoffeeProductDTO> addCoffeeProduct(@RequestBody CoffeeProductDTO product) { return Result.success(orderService.addProduct(product)); }

    @PutMapping("/products/coffee/{productId}")
    public Result<CoffeeProductDTO> updateCoffeeProduct(@PathVariable Long productId, @RequestBody CoffeeProductDTO product) { return Result.success(orderService.updateProduct(productId, product)); }

    @DeleteMapping("/products/coffee/{productId}")
    public Result<Void> deleteCoffeeProduct(@PathVariable Long productId) { orderService.deleteProduct(productId); return Result.success(null); }

    @PutMapping("/products/coffee/{productId}/status")
    public Result<CoffeeProductDTO> toggleCoffeeProductStatus(@PathVariable Long productId) { return Result.success(orderService.toggleProductStatus(productId)); }
}
