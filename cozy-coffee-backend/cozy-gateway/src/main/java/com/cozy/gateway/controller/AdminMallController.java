package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.service.AdminListService;
import com.cozy.gateway.service.AdminOrderCommandService;
import com.cozy.member.api.PointsMallService;
import com.cozy.member.dto.response.PointsOrderDTO;
import com.cozy.member.dto.response.PointsProductDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMallController {

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    private final AdminListService listService;
    private final AdminOrderCommandService commandService;

    @GetMapping("/products/points")
    public Result<List<PointsProductDTO>> listPointsProducts() { return Result.success(pointsMallService.listAllProducts()); }

    @PostMapping("/products/points")
    public Result<PointsProductDTO> addPointsProduct(@Valid @RequestBody PointsProductDTO product) { return Result.success(pointsMallService.addProduct(product)); }

    @PutMapping("/products/points/{productId}")
    public Result<PointsProductDTO> updatePointsProduct(@PathVariable Long productId, @Valid @RequestBody PointsProductDTO product) { return Result.success(pointsMallService.updateProduct(productId, product)); }

    @DeleteMapping("/products/points/{productId}")
    public Result<Void> deletePointsProduct(@PathVariable Long productId) { pointsMallService.deleteProduct(productId); return Result.success(null); }

    @PutMapping("/products/points/{productId}/status")
    public Result<PointsProductDTO> togglePointsProductStatus(@PathVariable Long productId) { return Result.success(pointsMallService.toggleProductStatus(productId)); }

    @GetMapping("/redemptions")
    public Result<List<PointsOrderDTO>> listRedemptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(listService.listRedemptions(status, keyword, userId, startDate, endDate));
    }

    @GetMapping("/redemptions/{orderId}")
    public Result<PointsOrderDTO> getRedemptionDetail(@PathVariable Long orderId) {
        return Result.success(commandService.getRedemptionDetail(orderId));
    }

    @PostMapping("/redemptions/{orderId}/process")
    public Result<PointsOrderDTO> processRedemption(@PathVariable Long orderId) {
        return Result.success(commandService.processRedemption(orderId));
    }

    @PostMapping("/redemptions/{orderId}/ship")
    public Result<PointsOrderDTO> shipRedemption(@PathVariable Long orderId, @RequestParam String company, @RequestParam String trackingNo) {
        return Result.success(commandService.shipRedemption(orderId, company, trackingNo));
    }

    @PostMapping("/redemptions/{orderId}/complete")
    public Result<PointsOrderDTO> completeRedemption(@PathVariable Long orderId) {
        return Result.success(commandService.completeRedemption(orderId));
    }

    @DeleteMapping("/redemptions/{orderId}")
    public Result<Void> deleteRedemption(@PathVariable Long orderId) {
        commandService.deleteRedemption(orderId);
        return Result.success(null, "订单已删除");
    }
}
