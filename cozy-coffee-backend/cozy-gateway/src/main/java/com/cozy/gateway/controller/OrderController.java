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

    @GetMapping("/products/{id}")
    public Result<CoffeeProductDTO> getProduct(@PathVariable Long id) {
        return Result.success(orderService.getProduct(id));
    }

    @PostMapping("/create")
    public Result<ShopOrderDTO> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        ShopOrderDTO order = orderCoordinatorService.createOrder(AuthUtil.requireUserId(), idempotencyKey, request);
        return Result.success(order, "下单成功！获得 " + order.getPointsEarned() + " 积分");
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
}
