package com.cozy.gateway.controller;

import com.cozy.common.context.UserContext;
import com.cozy.common.mq.OrderCreatedEvent;
import com.cozy.common.result.Result;
import com.cozy.gateway.mq.OrderEventProducer;
import com.cozy.member.api.MemberService;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 咖啡订单控制器
 * 连接独立的订单微服务(order-provider)
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderEventProducer orderEventProducer;

    @DubboReference(check = false)
    private OrderService orderService;

    @DubboReference(check = false)
    private MemberService memberService;

    /**
     * 获取咖啡商品列表
     */
    @GetMapping("/products")
    public Result<List<CoffeeProductDTO>> listProducts() {
        try {
            List<CoffeeProductDTO> products = orderService.listCoffeeProducts();
            return Result.success(products);
        } catch (Exception e) {
            log.error("获取商品列表失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/products/{id}")
    public Result<CoffeeProductDTO> getProduct(@PathVariable Long id) {
        try {
            CoffeeProductDTO product = orderService.getProduct(id);
            return Result.success(product);
        } catch (Exception e) {
            log.error("获取商品详情失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 创建咖啡订单
     */
    @PostMapping("/create")
    public Result<ShopOrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("网关收到创建订单请求: userId={}, itemsCount={}",
                UserContext.getUserIdOrNull(), request.getItems() != null ? request.getItems().size() : 0);
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            // 获取会员等级传递给订单服务
            String memberLevel = "basic";
            String nickname = null;
            try {
                var memberInfo = memberService.getMemberByUserId(userId);
                if (memberInfo != null) {
                    memberLevel = memberInfo.getMemberLevel();
                    nickname = memberInfo.getNickname();
                }
            } catch (Exception e) {
                log.warn("获取会员等级失败，使用默认等级", e);
            }
            ShopOrderDTO order = orderService.createOrder(userId, memberLevel, request);

            // 派发下单成功事件：SSE 广播 + 管理端缓存清理走异步链路，避免拖慢下单接口
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .userId(userId)
                    .username(nickname)
                    .payAmount(order.getPayAmount())
                    .itemCount(order.getTotalQuantity())
                    .occurredAt(LocalDateTime.now())
                    .build();
            orderEventProducer.publishOrderCreated(event);

            return Result.success(order, "下单成功！获得 " + order.getPointsEarned() + " 积分");
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取用户订单列表
     */
    @GetMapping("/list")
    public Result<List<ShopOrderDTO>> listOrders() {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            List<ShopOrderDTO> orders = orderService.listUserOrders(userId);
            return Result.success(orders);
        } catch (Exception e) {
            log.error("获取订单列表失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<ShopOrderDTO> getOrder(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            ShopOrderDTO order = orderService.getOrder(id, userId);
            return Result.success(order);
        } catch (Exception e) {
            log.error("获取订单详情失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public Result<ShopOrderDTO> cancelOrder(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            ShopOrderDTO order = orderService.cancelUserOrder(id, userId);
            return Result.success(order, "订单已取消");
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return Result.fail(e.getMessage());
        }
    }
}
