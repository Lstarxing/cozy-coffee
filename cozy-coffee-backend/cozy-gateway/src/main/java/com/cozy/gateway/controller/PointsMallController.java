package com.cozy.gateway.controller;

import com.cozy.common.context.UserContext;
import com.cozy.common.result.Result;
import com.cozy.member.api.PointsMallService;
import com.cozy.member.dto.request.RedeemRequest;
import com.cozy.member.dto.response.PointsOrderDTO;
import com.cozy.member.dto.response.PointsProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/member/mall")
@CrossOrigin(origins = "*")
public class PointsMallController {

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    @GetMapping("/products")
    public Result<List<PointsProductDTO>> listProducts() {
        try {
            List<PointsProductDTO> products = pointsMallService.listActiveProducts();
            return Result.success(products);
        } catch (Exception e) {
            log.error("获取商品列表失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/products/{id}")
    public Result<PointsProductDTO> getProduct(@PathVariable Long id) {
        try {
            PointsProductDTO product = pointsMallService.getProduct(id);
            return Result.success(product);
        } catch (Exception e) {
            log.error("获取商品详情失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/redeem")
    public Result<PointsOrderDTO> redeem(@Valid @RequestBody RedeemRequest request) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            PointsOrderDTO order = pointsMallService.redeem(userId, request);
            return Result.success(order, "兑换成功！订单号：" + order.getOrderNo());
        } catch (Exception e) {
            log.error("兑换失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/orders")
    public Result<List<PointsOrderDTO>> listOrders() {
        try {
            Long userId = UserContext.getUserIdOrNull();
            List<PointsOrderDTO> orders = pointsMallService.listUserOrders(userId);
            return Result.success(orders);
        } catch (Exception e) {
            log.error("获取订单列表失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/orders/{id}")
    public Result<PointsOrderDTO> getOrder(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            PointsOrderDTO order = pointsMallService.getOrder(id, userId);
            return Result.success(order);
        } catch (Exception e) {
            log.error("获取订单详情失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/orders/{id}/cancel")
    public Result<PointsOrderDTO> cancelOrder(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            PointsOrderDTO order = pointsMallService.cancelOrder(id, userId);
            return Result.success(order, "订单已取消，积分已返还");
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return Result.fail(e.getMessage());
        }
    }
}
