package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.service.PointsMallCoordinatorService;
import com.cozy.gateway.util.AuthUtil;
import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.request.AvailableCouponRequest;
import com.cozy.mall.dto.request.RedeemRequest;
import com.cozy.mall.dto.response.PointsOrderDTO;
import com.cozy.mall.dto.response.PointsProductDTO;
import com.cozy.mall.dto.response.UserCouponDTO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/member/mall")
@RequiredArgsConstructor
public class PointsMallController {

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    private final PointsMallCoordinatorService mallCoordinatorService;

    @GetMapping("/products")
    public Result<List<PointsProductDTO>> listProducts() {
        return Result.success(pointsMallService.listActiveProducts(AuthUtil.requireUserId()));
    }

    @GetMapping("/products/{id}")
    public Result<PointsProductDTO> getProduct(@PathVariable Long id) {
        return Result.success(pointsMallService.getProduct(id));
    }

    @PostMapping("/redeem")
    public Result<PointsOrderDTO> redeem(@Valid @RequestBody RedeemRequest request) {
        PointsOrderDTO order = mallCoordinatorService.redeem(AuthUtil.requireUserId(), request);
        return Result.success(order, "兑换成功！订单号：" + order.getOrderNo());
    }

    @GetMapping("/orders")
    public Result<List<PointsOrderDTO>> listOrders() {
        return Result.success(pointsMallService.listUserOrders(AuthUtil.requireUserId()));
    }

    @GetMapping("/orders/{id}")
    public Result<PointsOrderDTO> getOrder(@PathVariable Long id) {
        return Result.success(pointsMallService.getOrder(id, AuthUtil.requireUserId()));
    }

    @PostMapping("/orders/{id}/cancel")
    public Result<PointsOrderDTO> cancelOrder(@PathVariable Long id) {
        return Result.success(pointsMallService.cancelOrder(id, AuthUtil.requireUserId()), "订单已取消，积分已返还");
    }

    @PostMapping("/orders/{id}/confirm-receipt")
    public Result<PointsOrderDTO> confirmReceipt(@PathVariable Long id) {
        return Result.success(pointsMallService.confirmReceipt(id, AuthUtil.requireUserId()), "已确认收货");
    }

    @GetMapping("/coupons")
    public Result<List<UserCouponDTO>> listCoupons(@RequestParam(required = false) String status) {
        return Result.success(pointsMallService.getUserCoupons(AuthUtil.requireUserId(), status));
    }

    @PostMapping("/coupons/available")
    public Result<List<UserCouponDTO>> listAvailableCoupons(@Valid @RequestBody AvailableCouponRequest request) {
        return Result.success(pointsMallService.getAvailableCoupons(AuthUtil.requireUserId(), request.getOrderAmount(), request.getItems()));
    }
}
