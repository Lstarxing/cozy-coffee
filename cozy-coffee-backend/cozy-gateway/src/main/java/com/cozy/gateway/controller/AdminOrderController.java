package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.service.AdminListService;
import com.cozy.gateway.service.AdminOrderCommandService;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.AddonGroupRequest;
import com.cozy.order.dto.response.CoffeeBeanDTO;
import com.cozy.order.dto.response.CoffeeBlendDTO;
import com.cozy.order.dto.response.CoffeeOriginDTO;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ProductAddonDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import jakarta.validation.Valid;
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
    public Result<CoffeeProductDTO> addCoffeeProduct(@Valid @RequestBody CoffeeProductDTO product) { return Result.success(orderService.addProduct(product)); }

    @PutMapping("/products/coffee/{productId}")
    public Result<CoffeeProductDTO> updateCoffeeProduct(@PathVariable Long productId, @Valid @RequestBody CoffeeProductDTO product) { return Result.success(orderService.updateProduct(productId, product)); }

    @DeleteMapping("/products/coffee/{productId}")
    public Result<Void> deleteCoffeeProduct(@PathVariable Long productId) { orderService.deleteProduct(productId); return Result.success(null); }

    @PutMapping("/products/coffee/{productId}/status")
    public Result<CoffeeProductDTO> toggleCoffeeProductStatus(@PathVariable Long productId) { return Result.success(orderService.toggleProductStatus(productId)); }

    @GetMapping("/products/addon-catalog")
    public Result<List<ProductAddonDTO>> listAddonCatalog() { return Result.success(orderService.listAddonCatalog()); }

    @PostMapping("/products/coffee/{productId}/addon-groups")
    public Result<Void> saveAddonGroups(@PathVariable Long productId, @RequestBody List<AddonGroupRequest> groups) {
        orderService.saveAddonGroups(productId, groups);
        return Result.success();
    }

    // ==================== 内容档案（产区/单品豆/拼配豆） ====================

    @GetMapping("/content/origins")
    public Result<List<CoffeeOriginDTO>> listOrigins() { return Result.success(orderService.listOrigins()); }

    @PostMapping("/content/origins")
    public Result<CoffeeOriginDTO> saveOrigin(@RequestBody CoffeeOriginDTO origin) { return Result.success(orderService.saveOrigin(origin)); }

    @DeleteMapping("/content/origins/{id}")
    public Result<Void> deleteOrigin(@PathVariable Long id) { orderService.deleteOrigin(id); return Result.success(null); }

    @GetMapping("/content/beans")
    public Result<List<CoffeeBeanDTO>> listBeans() { return Result.success(orderService.listBeans()); }

    @PostMapping("/content/beans")
    public Result<CoffeeBeanDTO> saveBean(@RequestBody CoffeeBeanDTO bean) { return Result.success(orderService.saveBean(bean)); }

    @DeleteMapping("/content/beans/{id}")
    public Result<Void> deleteBean(@PathVariable Long id) { orderService.deleteBean(id); return Result.success(null); }

    @GetMapping("/content/blends")
    public Result<List<CoffeeBlendDTO>> listBlends() { return Result.success(orderService.listBlends()); }

    @PostMapping("/content/blends")
    public Result<CoffeeBlendDTO> saveBlend(@RequestBody CoffeeBlendDTO blend) { return Result.success(orderService.saveBlend(blend)); }

    @DeleteMapping("/content/blends/{id}")
    public Result<Void> deleteBlend(@PathVariable Long id) { orderService.deleteBlend(id); return Result.success(null); }
}
