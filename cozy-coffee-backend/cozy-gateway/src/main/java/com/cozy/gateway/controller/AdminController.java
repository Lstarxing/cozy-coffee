package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.PointsMallService;
import com.cozy.member.dto.response.PointsOrderDTO;
import com.cozy.member.dto.response.PointsProductDTO;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.response.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.Cursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 管理端 API 控制器
 */
@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {

    @DubboReference(check = false)
    private UserService userService;

    @DubboReference(check = false)
    private MemberService memberService;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    @DubboReference(check = false)
    private OrderService orderService;

    @Autowired
    private com.cozy.gateway.mq.OrderEventProducer orderEventProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final long ADMIN_DASHBOARD_CACHE_TTL_SECONDS = 45;
    private static final long ADMIN_ANALYTICS_CACHE_TTL_SECONDS = 60;
    private static final long ADMIN_ORDER_LIST_CACHE_TTL_SECONDS = 30;
    private static final long ADMIN_ORDER_RECENT_CACHE_TTL_SECONDS = 20;
    private static final long ADMIN_CACHE_TTL_JITTER_MAX_SECONDS = 8;

    // 使用本地前缀，避免跨模块常量版本不一致导致运行时错误。
    private static final String ADMIN_DASHBOARD_STATS_PREFIX = "cozy:admin:dashboard:stats:";
    private static final String ADMIN_ANALYTICS_TREND_PREFIX = "cozy:admin:analytics:trend:";
    private static final String ADMIN_ANALYTICS_DISTRIBUTION_PREFIX = "cozy:admin:analytics:distribution:";
    private static final String ADMIN_ANALYTICS_RANK_PREFIX = "cozy:admin:analytics:rank:";
    private static final String ADMIN_ORDERS_LIST_PREFIX = "cozy:admin:orders:list:";
    private static final String ADMIN_ORDERS_RECENT_PREFIX = "cozy:admin:orders:recent:";

    // ==================== 控制台统计 ====================

    // ==================== 控制台统计 (Analytics) ====================

    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> stats = new HashMap<>();
        try {
            java.time.LocalDate start = startDate != null ? java.time.LocalDate.parse(startDate)
                    : java.time.LocalDate.now();
            java.time.LocalDate end = endDate != null ? java.time.LocalDate.parse(endDate) : java.time.LocalDate.now();

            String cacheKey = buildAdminCacheKey(
                ADMIN_DASHBOARD_STATS_PREFIX,
                start,
                end);
            Map<String, Object> cached = readCacheObject(cacheKey, new TypeReference<Map<String, Object>>() {
            });
            if (cached != null) {
                ensureLegacyDashboardFields(cached);
                return Result.success(cached);
            }

            // 1. Users (Total is crucial, maybe daily growth too?)
            List<UserDTO> users = userService.listAllUsers();
            stats.put("totalUsers", users != null ? users.size() : 0);

            // 2. Coffee Orders (in range)
            List<ShopOrderDTO> allOrders = orderService.listAllOrders(null);
            List<ShopOrderDTO> rangeOrders = filterOrdersByDate(allOrders, start, end);

            stats.put("coffeeOrders", rangeOrders.size());

            BigDecimal revenue = rangeOrders.stream()
                    .filter(o -> !"cancelled".equals(o.getStatus()))
                    .map(ShopOrderDTO::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("coffeeRevenue", revenue);

            // 3. Redemptions (in range)
            List<PointsOrderDTO> allRedemptions = pointsMallService.listAllOrders(null);
            List<PointsOrderDTO> rangeRedemptions = filterRedemptionsByDate(allRedemptions, start, end);
            stats.put("redemptionOrders", rangeRedemptions.size());

            int pointsSpent = rangeRedemptions.stream()
                    .filter(o -> !"cancelled".equals(o.getStatus()))
                    .mapToInt(PointsOrderDTO::getPointsCost)
                    .sum();
            stats.put("pointsSpent", pointsSpent);

                ensureLegacyDashboardFields(stats);

            writeCacheObject(cacheKey, stats, ADMIN_DASHBOARD_CACHE_TTL_SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计失败");
        }
        return Result.success(stats);
    }

    @GetMapping("/analytics/trend")
    public Result<List<Map<String, Object>>> getAnalyticsTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "day") String granularity) {
        try {
            java.time.LocalDate start = startDate != null ? java.time.LocalDate.parse(startDate)
                    : java.time.LocalDate.now().minusDays(7);
            java.time.LocalDate end = endDate != null ? java.time.LocalDate.parse(endDate) : java.time.LocalDate.now();

            String cacheKey = buildAdminCacheKey(
                ADMIN_ANALYTICS_TREND_PREFIX,
                start,
                end,
                granularity);
            List<Map<String, Object>> cached = readCacheObject(cacheKey, new TypeReference<List<Map<String, Object>>>() {
            });
            if (cached != null) {
            return Result.success(cached);
            }

            List<ShopOrderDTO> orders = filterOrdersByDate(orderService.listAllOrders(null), start, end);

            // Group by Time
            Map<String, Map<String, Object>> grouped = new java.util.TreeMap<>(); // Sort by time string

            for (ShopOrderDTO o : orders) {
                if (o.getCreatedAt() == null || "cancelled".equals(o.getStatus()))
                    continue;

                String key;
                if ("hour".equals(granularity)) {
                    key = o.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));
                } else {
                    key = o.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }

                grouped.putIfAbsent(key, new HashMap<>());
                Map<String, Object> data = grouped.get(key);
                data.put("time", key);
                data.put("coffeeOrders", (int) data.getOrDefault("coffeeOrders", 0) + 1);
                data.put("coffeeRevenue",
                        ((BigDecimal) data.getOrDefault("coffeeRevenue", BigDecimal.ZERO)).add(o.getTotalAmount()));
            }

            // Add Redemptions
            List<PointsOrderDTO> redemptions = filterRedemptionsByDate(pointsMallService.listAllOrders(null), start,
                    end);
            for (PointsOrderDTO r : redemptions) {
                if (r.getCreatedAt() == null || "cancelled".equals(r.getStatus()))
                    continue;

                String key;
                if ("hour".equals(granularity)) {
                    key = r.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));
                } else {
                    key = r.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }

                grouped.putIfAbsent(key, new HashMap<>());
                Map<String, Object> data = grouped.get(key);
                data.put("time", key);
                data.put("redemptionOrders", (int) data.getOrDefault("redemptionOrders", 0) + 1);
            }

            List<Map<String, Object>> result = new java.util.ArrayList<>(grouped.values());
            writeCacheObject(cacheKey, result, ADMIN_ANALYTICS_CACHE_TTL_SECONDS);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取趋势失败");
        }
    }

    @GetMapping("/analytics/distribution")
    public Result<List<Map<String, Object>>> getAnalyticsDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam String domain) { // coffee | redemption
        try {
            java.time.LocalDate start = startDate != null ? java.time.LocalDate.parse(startDate)
                    : java.time.LocalDate.now().minusDays(30);
            java.time.LocalDate end = endDate != null ? java.time.LocalDate.parse(endDate) : java.time.LocalDate.now();

            String cacheKey = buildAdminCacheKey(
                ADMIN_ANALYTICS_DISTRIBUTION_PREFIX,
                start,
                end,
                domain);
            List<Map<String, Object>> cached = readCacheObject(cacheKey, new TypeReference<List<Map<String, Object>>>() {
            });
            if (cached != null) {
            return Result.success(cached);
            }

            Map<String, Integer> counts = new HashMap<>();

            if ("coffee".equals(domain)) {
                List<ShopOrderDTO> list = filterOrdersByDate(orderService.listAllOrders(null), start, end);
                for (ShopOrderDTO o : list) {
                    counts.put(o.getStatus(), counts.getOrDefault(o.getStatus(), 0) + 1);
                }
            } else {
                List<PointsOrderDTO> list = filterRedemptionsByDate(pointsMallService.listAllOrders(null), start, end);
                for (PointsOrderDTO o : list) {
                    counts.put(o.getStatus(), counts.getOrDefault(o.getStatus(), 0) + 1);
                }
            }

            List<Map<String, Object>> result = new java.util.ArrayList<>();
            counts.forEach((k, v) -> {
                Map<String, Object> item = new HashMap<>();
                item.put("status", k);
                item.put("count", v);
                result.add(item);
            });

            writeCacheObject(cacheKey, result, ADMIN_ANALYTICS_CACHE_TTL_SECONDS);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取分布失败");
        }
    }

    @GetMapping("/analytics/rank")
    public Result<List<Map<String, Object>>> getAnalyticsRank(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam String domain,
            @RequestParam(defaultValue = "count") String metric,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            java.time.LocalDate start = startDate != null ? java.time.LocalDate.parse(startDate)
                    : java.time.LocalDate.now().minusDays(30);
            java.time.LocalDate end = endDate != null ? java.time.LocalDate.parse(endDate) : java.time.LocalDate.now();

            String cacheKey = buildAdminCacheKey(
                ADMIN_ANALYTICS_RANK_PREFIX,
                start,
                end,
                domain,
                metric,
                limit);
            List<Map<String, Object>> cached = readCacheObject(cacheKey, new TypeReference<List<Map<String, Object>>>() {
            });
            if (cached != null) {
            return Result.success(cached);
            }

            Map<String, Double> agg = new HashMap<>();

            if ("coffee".equals(domain)) {
                List<ShopOrderDTO> list = filterOrdersByDate(orderService.listAllOrders(null), start, end);
                for (ShopOrderDTO o : list) {
                    if ("cancelled".equals(o.getStatus()))
                        continue;
                    String name = o.getItemsSummary() != null ? o.getItemsSummary() : "未知商品";
                    if ("amount".equals(metric)) {
                        agg.put(name, agg.getOrDefault(name, 0.0) + o.getTotalAmount().doubleValue());
                    } else {
                        // 尝试从统计中获取数量，如果没有则默认为1
                        int qty = o.getTotalQuantity() != null ? o.getTotalQuantity() : 1;
                        agg.put(name, agg.getOrDefault(name, 0.0) + (double) qty);
                    }
                }
            } else {
                List<PointsOrderDTO> list = filterRedemptionsByDate(pointsMallService.listAllOrders(null), start, end);
                for (PointsOrderDTO o : list) {
                    if ("cancelled".equals(o.getStatus()))
                        continue;
                    String name = o.getProductName();
                    if ("points".equals(metric)) {
                        agg.put(name, agg.getOrDefault(name, 0.0) + o.getPointsCost());
                    } else { // count
                        agg.put(name, agg.getOrDefault(name, 0.0) + o.getQuantity());
                    }
                }
            }

            // Sort top N
            List<Map.Entry<String, Double>> sorted = new java.util.ArrayList<>(agg.entrySet());
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", sorted.get(i).getKey());
                item.put("value", sorted.get(i).getValue());
                result.add(item);
            }

            writeCacheObject(cacheKey, result, ADMIN_ANALYTICS_CACHE_TTL_SECONDS);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取排行失败");
        }
    }

    @GetMapping("/orders/recent")
    public Result<List<ShopOrderDTO>> getRecentOrders(@RequestParam(defaultValue = "10") int limit) {
        try {
            int safeLimit = Math.max(1, Math.min(limit, 100));
            String cacheKey = buildAdminCacheKey(ADMIN_ORDERS_RECENT_PREFIX, safeLimit);
            List<ShopOrderDTO> cached = readCacheObject(cacheKey, new TypeReference<List<ShopOrderDTO>>() {
            });
            if (cached != null) {
                return Result.success(cached);
            }

            List<ShopOrderDTO> all = orderService.listAllOrders(null);
            if (all == null)
                return Result.success(java.util.Collections.emptyList());

            // Sort desc
            all.sort((a, b) -> {
                if (b.getCreatedAt() == null)
                    return -1;
                if (a.getCreatedAt() == null)
                    return 1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });

            // 填充用户信息
            List<ShopOrderDTO> recent = all.stream().limit(safeLimit).collect(java.util.stream.Collectors.toList());
            for (ShopOrderDTO order : recent) {
                try {
                    if (order.getUserId() != null) {
                        UserDTO user = userService.getUserById(order.getUserId());
                        if (user != null) {
                            order.setUsername(user.getUsername());
                            order.setNickname(user.getNickname());
                        }
                    }
                } catch (Exception e) {
                    // 忽略获取用户失败的情况
                }
            }

            writeCacheObject(cacheKey, recent, ADMIN_ORDER_RECENT_CACHE_TTL_SECONDS);
            return Result.success(recent);
        } catch (Exception e) {
            return Result.error("获取最近订单失败");
        }
    }

    @GetMapping("/redemptions/recent")
    public Result<List<PointsOrderDTO>> getRecentRedemptions(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<PointsOrderDTO> all = pointsMallService.listAllOrders(null);
            if (all == null)
                return Result.success(java.util.Collections.emptyList());

            all.sort((a, b) -> {
                if (b.getCreatedAt() == null)
                    return -1;
                if (a.getCreatedAt() == null)
                    return 1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });

            return Result.success(all.stream().limit(limit).collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            return Result.error("获取最近兑换失败");
        }
    }

    // Helpers
    private List<ShopOrderDTO> filterOrdersByDate(List<ShopOrderDTO> list, java.time.LocalDate start,
            java.time.LocalDate end) {
        if (list == null)
            return new java.util.ArrayList<>();
        return list.stream().filter(o -> {
            if (o.getCreatedAt() == null)
                return false;
            java.time.LocalDate d = o.getCreatedAt().toLocalDate();
            return !d.isBefore(start) && !d.isAfter(end);
        }).collect(java.util.stream.Collectors.toList());
    }

    private List<PointsOrderDTO> filterRedemptionsByDate(List<PointsOrderDTO> list, java.time.LocalDate start,
            java.time.LocalDate end) {
        if (list == null)
            return new java.util.ArrayList<>();
        return list.stream().filter(o -> {
            if (o.getCreatedAt() == null)
                return false;
            java.time.LocalDate d = o.getCreatedAt().toLocalDate();
            return !d.isBefore(start) && !d.isAfter(end);
        }).collect(java.util.stream.Collectors.toList());
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<List<UserDTO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String memberLevel,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<UserDTO> users = userService.listAllUsers();

            if (users != null) {
                users = users.stream().filter(u -> {
                    boolean match = true;
                    if (keyword != null && !keyword.isEmpty()) {
                        String kw = keyword.toLowerCase();
                        boolean nameMatch = u.getUsername() != null && u.getUsername().toLowerCase().contains(kw);
                        boolean phoneMatch = u.getPhone() != null && u.getPhone().contains(kw);
                        boolean emailMatch = u.getEmail() != null && u.getEmail().toLowerCase().contains(kw);
                        if (!nameMatch && !phoneMatch && !emailMatch)
                            match = false;
                    }
                    if (match && memberLevel != null && !memberLevel.isEmpty()) {
                        if (!memberLevel.equals(u.getMemberLevel()))
                            match = false;
                    }
                    if (match && startDate != null && !startDate.isEmpty()) {
                        if (u.getCreatedAt() != null
                                && u.getCreatedAt().toLocalDate().isBefore(java.time.LocalDate.parse(startDate)))
                            match = false;
                    }
                    if (match && endDate != null && !endDate.isEmpty()) {
                        if (u.getCreatedAt() != null
                                && u.getCreatedAt().toLocalDate().isAfter(java.time.LocalDate.parse(endDate)))
                            match = false;
                    }
                    return match;
                }).collect(java.util.stream.Collectors.toList());
            }

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
            memberService.adminAdjustPoints(userId, amount, reason);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("调整积分失败: " + e.getMessage());
        }
    }

    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam String status) {
        try {
            userService.updateUserStatus(userId, status);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("更新用户状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/users/{userId}")
    public Result<UserDTO> getUserDetail(@PathVariable Long userId) {
        try {
            UserDTO user = userService.getUserDetail(userId);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("获取用户详情失败: " + e.getMessage());
        }
    }

    // ==================== 咖啡订单管理 ====================

    /**
     * 获取订单详情（含用户信息 + 脱敏手机号）
     */
    @GetMapping("/orders/{orderId}")
    public Result<ShopOrderDTO> getOrderDetail(@PathVariable Long orderId) {
        try {
            ShopOrderDTO order = orderService.getOrderDetail(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            // 关联用户信息
            if (order.getUserId() != null) {
                try {
                    UserDTO user = userService.getUserDetail(order.getUserId());
                    if (user != null) {
                        order.setUsername(user.getUsername());
                        order.setNickname(user.getNickname());
                        order.setPhoneMasked(maskPhone(user.getPhone()));
                    }
                } catch (Exception e) {
                    // 用户信息获取失败，不影响订单展示
                    order.setUsername(null);
                    order.setNickname(null);
                    order.setPhoneMasked("***");
                }
            }
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("获取订单详情失败");
        }
    }

    @GetMapping("/orders/counts")
    public Result<Map<String, Long>> getOrderCounts() {
        try {
            Map<String, Long> counts = orderService.getOrderStatusCounts();
            return Result.success(counts);
        } catch (Exception e) {
            return Result.error("获取订单统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/orders")
    public Result<List<ShopOrderDTO>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String keyword, // Added keyword param
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "false") boolean noCache) {
        try {
            String cacheKey = buildAdminCacheKey(
                ADMIN_ORDERS_LIST_PREFIX,
                    status,
                    orderNo,
                    keyword,
                    userId,
                    startDate,
                    endDate);
            if (!noCache) {
                List<ShopOrderDTO> cached = readCacheObject(cacheKey, new TypeReference<List<ShopOrderDTO>>() {
                });
                if (cached != null) {
                    return Result.success(cached);
                }
            }

            List<ShopOrderDTO> orders = orderService.listAllOrders(status);

            // Pre-fetch user map if keyword search is needed (for phone search)
            Map<Long, UserDTO> userMap = null;
            if (keyword != null && !keyword.isEmpty()) {
                List<UserDTO> allUsers = userService.listAllUsers();
                if (allUsers != null) {
                    userMap = allUsers.stream()
                            .collect(java.util.stream.Collectors.toMap(UserDTO::getId, u -> u, (k1, k2) -> k1));
                }
            }

            // Keyword final variable for lambda
            final String kw = (keyword != null && !keyword.isEmpty()) ? keyword.toLowerCase() : null;
            final Map<Long, UserDTO> finalUserMap = userMap;

            // userId 过滤
            if (userId != null) {
                orders = orders.stream()
                        .filter(o -> userId.equals(o.getUserId()))
                        .collect(java.util.stream.Collectors.toList());
            }
            // orderNo 过滤 (Legacy param, kept for compatibility if needed)
            if (orderNo != null && !orderNo.isEmpty()) {
                orders = orders.stream()
                        .filter(o -> o.getOrderNo() != null && o.getOrderNo().contains(orderNo))
                        .collect(java.util.stream.Collectors.toList());
            }
            // Keyword 过滤 (OrderNo OR Phone OR Username)
            if (kw != null) {
                orders = orders.stream().filter(o -> {
                    boolean match = false;
                    // 1. Order No
                    if (o.getOrderNo() != null && o.getOrderNo().toLowerCase().contains(kw))
                        match = true;
                    // 2. User Phone/Name (if userMap exists)
                    if (!match && o.getUserId() != null && finalUserMap != null) {
                        UserDTO u = finalUserMap.get(o.getUserId());
                        if (u != null) {
                            if (u.getPhone() != null && u.getPhone().contains(kw))
                                match = true;
                            if (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw))
                                match = true;
                            if (u.getNickname() != null && u.getNickname().toLowerCase().contains(kw))
                                match = true;
                        }
                    }
                    return match;
                }).collect(java.util.stream.Collectors.toList());
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

            // 默认按创建时间倒序
            orders.sort((a, b) -> {
                if (b.getCreatedAt() == null)
                    return -1;
                if (a.getCreatedAt() == null)
                    return 1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });

            if (!noCache) {
                writeCacheObject(cacheKey, orders, ADMIN_ORDER_LIST_CACHE_TTL_SECONDS);
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
            evictOrderAndAnalyticsCaches();
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("接单失败: " + e.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/complete")
    public Result<ShopOrderDTO> completeOrder(@PathVariable Long orderId) {
        try {
            ShopOrderDTO order = orderService.completeOrder(orderId);
            evictOrderAndAnalyticsCaches();

            // 派发订单完成事件：积分/EXP/首单奖励/月度任务走 MQ 异步链路
            com.cozy.common.mq.OrderCompletedEvent event = com.cozy.common.mq.OrderCompletedEvent.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .userId(order.getUserId())
                    .payAmount(order.getPayAmount())
                    .expEarned(order.getExpEarned())
                    .pointsEarned(order.getPointsEarned())
                    .isFirstOrder(order.getIsFirstOrder())
                    .hasNewProduct(order.getHasNewProduct())
                    .isDelivery("DELIVERY".equals(order.getDiningMethod()))
                    .occurredAt(java.time.LocalDateTime.now())
                    .build();
            orderEventProducer.publishOrderCompleted(event);

            return Result.success(order);
        } catch (Exception e) {
            return Result.error("完成订单失败: " + e.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/cancel")
    public Result<ShopOrderDTO> cancelOrder(@PathVariable Long orderId) {
        try {
            ShopOrderDTO order = orderService.cancelOrder(orderId);
            evictOrderAndAnalyticsCaches();
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
            List<PointsProductDTO> products = pointsMallService.listAllProducts();
            return Result.success(products);
        } catch (Exception e) {
            return Result.error("获取积分商品失败: " + e.getMessage());
        }
    }

    @PostMapping("/products/points")
    public Result<PointsProductDTO> addPointsProduct(@RequestBody PointsProductDTO product) {
        try {
            PointsProductDTO created = pointsMallService.addProduct(product);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error("添加积分商品失败: " + e.getMessage());
        }
    }

    @PutMapping("/products/points/{productId}")
    public Result<PointsProductDTO> updatePointsProduct(
            @PathVariable Long productId,
            @RequestBody PointsProductDTO product) {
        try {
            PointsProductDTO updated = pointsMallService.updateProduct(productId, product);
            return Result.success(updated);
        } catch (Exception e) {
            return Result.error("更新积分商品失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/products/points/{productId}")
    public Result<Void> deletePointsProduct(@PathVariable Long productId) {
        try {
            pointsMallService.deleteProduct(productId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除积分商品失败: " + e.getMessage());
        }
    }

    @PutMapping("/products/points/{productId}/status")
    public Result<PointsProductDTO> togglePointsProductStatus(@PathVariable Long productId) {
        try {
            PointsProductDTO product = pointsMallService.toggleProductStatus(productId);
            return Result.success(product);
        } catch (Exception e) {
            return Result.error("切换状态失败: " + e.getMessage());
        }
    }

    // ==================== 积分兑换订单管理 ====================

    @GetMapping("/redemptions")
    public Result<List<PointsOrderDTO>> listRedemptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<PointsOrderDTO> orders = pointsMallService.listAllOrders(status);

            // Pre-fetch user map if keyword search is needed
            Map<Long, UserDTO> userMap = null;
            if (keyword != null && !keyword.isEmpty()) {
                List<UserDTO> allUsers = userService.listAllUsers();
                if (allUsers != null) {
                    userMap = allUsers.stream()
                            .collect(java.util.stream.Collectors.toMap(UserDTO::getId, u -> u, (k1, k2) -> k1));
                }
            }
            final Map<Long, UserDTO> finalUserMap = userMap;

            if (orders != null) {
                orders = orders.stream().filter(o -> {
                    boolean match = true;
                    // userId 过滤
                    if (userId != null && !userId.equals(o.getUserId())) {
                        match = false;
                    }
                    // keyword 过滤 (OrderNo OR Product OR Phone)
                    if (match && keyword != null && !keyword.isEmpty()) {
                        String kw = keyword.toLowerCase();
                        boolean matches = false;

                        // 1. Order No
                        if (o.getOrderNo() != null && o.getOrderNo().toLowerCase().contains(kw))
                            matches = true;
                        // 2. Product Name
                        if (!matches && o.getProductName() != null && o.getProductName().toLowerCase().contains(kw))
                            matches = true;
                        // 3. Receiver Phone (in DTO)
                        if (!matches && o.getReceiverPhone() != null && o.getReceiverPhone().contains(kw))
                            matches = true;
                        // 4. User Phone (via map)
                        if (!matches && o.getUserId() != null && finalUserMap != null) {
                            UserDTO u = finalUserMap.get(o.getUserId());
                            if (u != null) {
                                if (u.getPhone() != null && u.getPhone().contains(kw))
                                    matches = true;
                            }
                        }

                        if (!matches)
                            match = false;
                    }
                    if (match && startDate != null && !startDate.isEmpty()) {
                        if (o.getCreatedAt() != null
                                && o.getCreatedAt().toLocalDate().isBefore(java.time.LocalDate.parse(startDate)))
                            match = false;
                    }
                    if (match && endDate != null && !endDate.isEmpty()) {
                        if (o.getCreatedAt() != null
                                && o.getCreatedAt().toLocalDate().isAfter(java.time.LocalDate.parse(endDate)))
                            match = false;
                    }
                    return match;
                }).collect(java.util.stream.Collectors.toList());

                // 默认按创建时间倒序
                orders.sort((a, b) -> {
                    if (b.getCreatedAt() == null)
                        return -1;
                    if (a.getCreatedAt() == null)
                        return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
            }
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取兑换订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取兑换订单详情（含用户信息 + 脱敏手机号）
     */
    @GetMapping("/redemptions/{orderId}")
    public Result<PointsOrderDTO> getRedemptionDetail(@PathVariable Long orderId) {
        try {
            PointsOrderDTO order = pointsMallService.getRedemptionDetail(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            // 关联用户信息
            if (order.getUserId() != null) {
                try {
                    UserDTO user = userService.getUserDetail(order.getUserId());
                    if (user != null) {
                        order.setUsername(user.getUsername());
                        order.setNickname(user.getNickname());
                        order.setPhoneMasked(maskPhone(user.getPhone()));
                    }
                } catch (Exception e) {
                    order.setPhoneMasked("***");
                }
            }
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("获取订单详情失败");
        }
    }

    @PostMapping("/redemptions/{orderId}/process")
    public Result<PointsOrderDTO> processRedemption(@PathVariable Long orderId) {
        try {
            PointsOrderDTO order = pointsMallService.updateOrderStatus(orderId, "processing");
            evictAnalyticsCaches();
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
            evictAnalyticsCaches();
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("发货失败: " + e.getMessage());
        }
    }

    @PostMapping("/redemptions/{orderId}/complete")
    public Result<PointsOrderDTO> completeRedemption(@PathVariable Long orderId) {
        try {
            PointsOrderDTO order = pointsMallService.updateOrderStatus(orderId, "completed");
            evictAnalyticsCaches();
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("完成订单失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/redemptions/{orderId}")
    public Result<Void> deleteRedemption(@PathVariable Long orderId) {
        try {
            pointsMallService.deleteOrder(orderId);
            evictAnalyticsCaches();
            return Result.success(null, "订单已删除");
        } catch (Exception e) {
            return Result.error("删除订单失败: " + e.getMessage());
        }
    }

    private String buildAdminCacheKey(String prefix, Object... parts) {
        StringBuilder key = new StringBuilder(prefix);
        if (parts == null || parts.length == 0) {
            return key.toString();
        }
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                key.append(':');
            }
            key.append(parts[i] == null ? "_" : String.valueOf(parts[i]).trim());
        }
        return key.toString();
    }

    private <T> T readCacheObject(String cacheKey, TypeReference<T> typeReference) {
        try {
            Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
            if (cachedObj == null) {
                return null;
            }
            return objectMapper.convertValue(cachedObj, typeReference);
        } catch (Exception e) {
            log.warn("读取管理端缓存失败: cacheKey={}", cacheKey, e);
            return null;
        }
    }

    private void writeCacheObject(String cacheKey, Object value, long ttlSeconds) {
        try {
            long jitter = ttlSeconds > 1
                    ? ThreadLocalRandom.current().nextLong(Math.min(ADMIN_CACHE_TTL_JITTER_MAX_SECONDS, ttlSeconds / 2) + 1)
                    : 0;
            redisTemplate.opsForValue().set(cacheKey, value, ttlSeconds + jitter, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入管理端缓存失败: cacheKey={}", cacheKey, e);
        }
    }

    private void ensureLegacyDashboardFields(Map<String, Object> stats) {
        if (stats == null) {
            return;
        }

        Object coffeeOrders = stats.get("coffeeOrders");
        Object coffeeRevenue = stats.get("coffeeRevenue");

        // 兼容老前端/测试脚本字段，避免页面初始化报错。
        stats.putIfAbsent("todayOrders", coffeeOrders != null ? coffeeOrders : 0);
        stats.putIfAbsent("todayRevenue", coffeeRevenue != null ? coffeeRevenue : BigDecimal.ZERO);
        stats.putIfAbsent("pendingOrders", 0);
    }

    private void evictOrderAndAnalyticsCaches() {
        evictByPrefix(ADMIN_ORDERS_LIST_PREFIX);
        evictByPrefix(ADMIN_ORDERS_RECENT_PREFIX);
        evictAnalyticsCaches();
    }

    private void evictAnalyticsCaches() {
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
            log.warn("按前缀清理缓存失败: prefix={}", prefix, e);
        }
    }

    // 手机号脱敏工具方法
    private String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "***";
        }
        phone = phone.trim();
        if (phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
