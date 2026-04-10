package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.common.context.UserContext;
import com.cozy.gateway.sse.SseEventPublisher;
import com.cozy.member.api.MemberService;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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

    private final SseEventPublisher sseEventPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ADMIN_DASHBOARD_STATS_PREFIX = "cozy:admin:dashboard:stats:";
    private static final String ADMIN_ANALYTICS_TREND_PREFIX = "cozy:admin:analytics:trend:";
    private static final String ADMIN_ANALYTICS_DISTRIBUTION_PREFIX = "cozy:admin:analytics:distribution:";
    private static final String ADMIN_ANALYTICS_RANK_PREFIX = "cozy:admin:analytics:rank:";
    private static final String ADMIN_ORDERS_LIST_PREFIX = "cozy:admin:orders:list:";

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
            try {
                var memberInfo = memberService.getMemberByUserId(userId);
                if (memberInfo != null) {
                    memberLevel = memberInfo.getMemberLevel();
                }
            } catch (Exception e) {
                log.warn("获取会员等级失败，使用默认等级", e);
            }
            ShopOrderDTO order = orderService.createOrder(userId, memberLevel, request);

            // 新订单创建后立即清理管理端缓存，确保订单页和看板可实时看到数据。
            evictAdminOrderAndAnalyticsCaches();

            // 发布 SSE 事件通知管理端
            try {
                sseEventPublisher.publishNewOrder(order.getId(), order.getOrderNo());
            } catch (Exception e) {
                log.warn("发布新订单 SSE 事件失败", e);
            }

            return Result.success(order, "下单成功！获得 " + order.getPointsEarned() + " 积分");
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return Result.fail(e.getMessage());
        }
    }

    private void evictAdminOrderAndAnalyticsCaches() {
        evictByPrefix(ADMIN_ORDERS_LIST_PREFIX);
        evictByPrefix(ADMIN_DASHBOARD_STATS_PREFIX);
        evictByPrefix(ADMIN_ANALYTICS_TREND_PREFIX);
        evictByPrefix(ADMIN_ANALYTICS_DISTRIBUTION_PREFIX);
        evictByPrefix(ADMIN_ANALYTICS_RANK_PREFIX);
    }

    private void evictByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return;
        }
        String pattern = prefix + "*";
        List<String> batch = new ArrayList<>(200);
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
            try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= 200) {
                        stringRedisTemplate.delete(batch);
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) {
                stringRedisTemplate.delete(batch);
            }
        } catch (Exception e) {
            log.warn("下单后清理管理端缓存失败: prefix={}", prefix, e);
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
