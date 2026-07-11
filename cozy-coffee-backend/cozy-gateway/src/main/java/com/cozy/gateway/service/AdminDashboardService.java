package com.cozy.gateway.service;

import com.cozy.common.result.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cozy.gateway.util.AdminCacheUtil;
import com.cozy.member.api.PointsMallService;
import com.cozy.member.dto.response.PointsOrderDTO;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.response.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端控制台/分析服务。
 * 将跨 Dubbo Provider 的数据聚合、缓存逻辑从 Controller 下沉到此层。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @DubboReference(check = false)
    private UserService userService;

    @DubboReference(check = false)
    private OrderService orderService;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    public Map<String, Object> getStats(String startDateName, String endDateName) {
        var start = startDateName != null ? java.time.LocalDate.parse(startDateName) : java.time.LocalDate.now();
        var end = endDateName != null ? java.time.LocalDate.parse(endDateName) : java.time.LocalDate.now();

        String cacheKey = AdminCacheUtil.buildKey(AdminCacheUtil.DASHBOARD_STATS_PREFIX, start, end);
        Map<String, Object> cached = AdminCacheUtil.readCache(redisTemplate, objectMapper, cacheKey,
                new TypeReference<Map<String, Object>>() {});
        if (cached != null) {
            ensureLegacy(cached);
            return cached;
        }

        Map<String, Object> stats = new HashMap<>();
        List<UserDTO> users = userService.listAllUsers();
        stats.put("totalUsers", users != null ? users.size() : 0);

        List<ShopOrderDTO> rangeOrders = filterOrders(orderService.listAllOrders(null), start, end);
        stats.put("coffeeOrders", rangeOrders.size());
        stats.put("coffeeRevenue", rangeOrders.stream()
                .filter(o -> !"cancelled".equals(o.getStatus()))
                .map(ShopOrderDTO::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        List<PointsOrderDTO> rangeRedemptions = filterRedemptions(pointsMallService.listAllOrders(null), start, end);
        stats.put("redemptionOrders", rangeRedemptions.size());
        stats.put("pointsSpent", rangeRedemptions.stream()
                .filter(o -> !"cancelled".equals(o.getStatus()))
                .mapToInt(PointsOrderDTO::getPointsCost).sum());

        ensureLegacy(stats);
        AdminCacheUtil.writeCache(redisTemplate, cacheKey, stats, AdminCacheUtil.DASHBOARD_TTL_SECONDS);
        return stats;
    }

    public List<Map<String, Object>> getTrend(String startDateName, String endDateName, String granularity) {
        var start = startDateName != null ? java.time.LocalDate.parse(startDateName) : java.time.LocalDate.now().minusDays(7);
        var end = endDateName != null ? java.time.LocalDate.parse(endDateName) : java.time.LocalDate.now();

        String cacheKey = AdminCacheUtil.buildKey(AdminCacheUtil.ANALYTICS_TREND_PREFIX, start, end, granularity);
        List<Map<String, Object>> cached = AdminCacheUtil.readCache(redisTemplate, objectMapper, cacheKey,
                new TypeReference<List<Map<String, Object>>>() {});
        if (cached != null) return cached;

        Map<String, Map<String, Object>> grouped = new java.util.TreeMap<>();
        for (ShopOrderDTO o : filterOrders(orderService.listAllOrders(null), start, end)) {
            if (o.getCreatedAt() == null || "cancelled".equals(o.getStatus())) continue;
            String key = "hour".equals(granularity)
                    ? o.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"))
                    : o.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            grouped.putIfAbsent(key, new HashMap<>());
            Map<String, Object> data = grouped.get(key);
            data.put("time", key);
            data.put("coffeeOrders", (int) data.getOrDefault("coffeeOrders", 0) + 1);
            data.put("coffeeRevenue", ((BigDecimal) data.getOrDefault("coffeeRevenue", BigDecimal.ZERO)).add(o.getTotalAmount()));
        }
        for (PointsOrderDTO r : filterRedemptions(pointsMallService.listAllOrders(null), start, end)) {
            if (r.getCreatedAt() == null || "cancelled".equals(r.getStatus())) continue;
            String key = "hour".equals(granularity)
                    ? r.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"))
                    : r.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            grouped.putIfAbsent(key, new HashMap<>());
            Map<String, Object> data = grouped.get(key);
            data.put("time", key);
            data.put("redemptionOrders", (int) data.getOrDefault("redemptionOrders", 0) + 1);
        }

        List<Map<String, Object>> result = new ArrayList<>(grouped.values());
        AdminCacheUtil.writeCache(redisTemplate, cacheKey, result, AdminCacheUtil.ANALYTICS_TTL_SECONDS);
        return result;
    }

    public List<Map<String, Object>> getDistribution(String startDateName, String endDateName, String domain) {
        var start = startDateName != null ? java.time.LocalDate.parse(startDateName) : java.time.LocalDate.now().minusDays(30);
        var end = endDateName != null ? java.time.LocalDate.parse(endDateName) : java.time.LocalDate.now();

        String cacheKey = AdminCacheUtil.buildKey(AdminCacheUtil.ANALYTICS_DISTRIBUTION_PREFIX, start, end, domain);
        List<Map<String, Object>> cached = AdminCacheUtil.readCache(redisTemplate, objectMapper, cacheKey,
                new TypeReference<List<Map<String, Object>>>() {});
        if (cached != null) return cached;

        Map<String, Integer> counts = new HashMap<>();
        if ("coffee".equals(domain)) {
            for (ShopOrderDTO o : filterOrders(orderService.listAllOrders(null), start, end))
                counts.put(o.getStatus(), counts.getOrDefault(o.getStatus(), 0) + 1);
        } else {
            for (PointsOrderDTO o : filterRedemptions(pointsMallService.listAllOrders(null), start, end))
                counts.put(o.getStatus(), counts.getOrDefault(o.getStatus(), 0) + 1);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        counts.forEach((k, v) -> { Map<String, Object> item = new HashMap<>(); item.put("status", k); item.put("count", v); result.add(item); });
        AdminCacheUtil.writeCache(redisTemplate, cacheKey, result, AdminCacheUtil.ANALYTICS_TTL_SECONDS);
        return result;
    }

    public List<Map<String, Object>> getRank(String startDateName, String endDateName, String domain, String metric, int limit) {
        var start = startDateName != null ? java.time.LocalDate.parse(startDateName) : java.time.LocalDate.now().minusDays(30);
        var end = endDateName != null ? java.time.LocalDate.parse(endDateName) : java.time.LocalDate.now();

        String cacheKey = AdminCacheUtil.buildKey(AdminCacheUtil.ANALYTICS_RANK_PREFIX, start, end, domain, metric, limit);
        List<Map<String, Object>> cached = AdminCacheUtil.readCache(redisTemplate, objectMapper, cacheKey,
                new TypeReference<List<Map<String, Object>>>() {});
        if (cached != null) return cached;

        Map<String, Double> agg = new HashMap<>();
        if ("coffee".equals(domain)) {
            for (ShopOrderDTO o : filterOrders(orderService.listAllOrders(null), start, end)) {
                if ("cancelled".equals(o.getStatus())) continue;
                String name = o.getItemsSummary() != null ? o.getItemsSummary() : "未知商品";
                agg.put(name, agg.getOrDefault(name, 0.0)
                        + ("amount".equals(metric) ? o.getTotalAmount().doubleValue() : (double) (o.getTotalQuantity() != null ? o.getTotalQuantity() : 1)));
            }
        } else {
            for (PointsOrderDTO o : filterRedemptions(pointsMallService.listAllOrders(null), start, end)) {
                if ("cancelled".equals(o.getStatus())) continue;
                agg.put(o.getProductName(), agg.getOrDefault(o.getProductName(), 0.0)
                        + ("points".equals(metric) ? o.getPointsCost() : o.getQuantity()));
            }
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(agg.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", sorted.get(i).getKey());
            item.put("value", sorted.get(i).getValue());
            result.add(item);
        }
        AdminCacheUtil.writeCache(redisTemplate, cacheKey, result, AdminCacheUtil.ANALYTICS_TTL_SECONDS);
        return result;
    }

    public List<ShopOrderDTO> getRecentOrders(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String cacheKey = AdminCacheUtil.buildKey(AdminCacheUtil.ORDERS_RECENT_PREFIX, safeLimit);
        List<ShopOrderDTO> cached = AdminCacheUtil.readCache(redisTemplate, objectMapper, cacheKey,
                new TypeReference<List<ShopOrderDTO>>() {});
        if (cached != null) return cached;

        List<ShopOrderDTO> all = orderService.listAllOrders(null);
        if (all == null) return java.util.Collections.emptyList();

        all.sort((a, b) -> {
            if (b.getCreatedAt() == null) return -1;
            if (a.getCreatedAt() == null) return 1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        List<ShopOrderDTO> recent = all.stream().limit(safeLimit).collect(java.util.stream.Collectors.toList());
        for (ShopOrderDTO o : recent) {
            try {
                if (o.getUserId() != null) {
                    UserDTO user = userService.getUserById(o.getUserId());
                    if (user != null) { o.setUsername(user.getUsername()); o.setNickname(user.getNickname()); }
                }
            } catch (Exception e) { /* 忽略 */ }
        }
        AdminCacheUtil.writeCache(redisTemplate, cacheKey, recent, AdminCacheUtil.ORDER_RECENT_TTL_SECONDS);
        return recent;
    }

    public List<PointsOrderDTO> getRecentRedemptions(int limit) {
        List<PointsOrderDTO> all = pointsMallService.listAllOrders(null);
        if (all == null) return java.util.Collections.emptyList();
        all.sort((a, b) -> {
            if (b.getCreatedAt() == null) return -1;
            if (a.getCreatedAt() == null) return 1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return all.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }

    // --- helpers ---

    private List<ShopOrderDTO> filterOrders(List<ShopOrderDTO> list, java.time.LocalDate start, java.time.LocalDate end) {
        if (list == null) return new ArrayList<>();
        return list.stream().filter(o -> o.getCreatedAt() != null
                && !o.getCreatedAt().toLocalDate().isBefore(start)
                && !o.getCreatedAt().toLocalDate().isAfter(end)).collect(java.util.stream.Collectors.toList());
    }

    private List<PointsOrderDTO> filterRedemptions(List<PointsOrderDTO> list, java.time.LocalDate start, java.time.LocalDate end) {
        if (list == null) return new ArrayList<>();
        return list.stream().filter(o -> o.getCreatedAt() != null
                && !o.getCreatedAt().toLocalDate().isBefore(start)
                && !o.getCreatedAt().toLocalDate().isAfter(end)).collect(java.util.stream.Collectors.toList());
    }

    private void ensureLegacy(Map<String, Object> stats) {
        if (stats == null) return;
        stats.putIfAbsent("todayOrders", stats.getOrDefault("coffeeOrders", 0));
        stats.putIfAbsent("todayRevenue", stats.getOrDefault("coffeeRevenue", BigDecimal.ZERO));
        stats.putIfAbsent("pendingOrders", 0);
    }
}
