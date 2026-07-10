package com.cozy.gateway.service;

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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端列表查询服务。
 * 将跨 Dubbo Provider 的多维度过滤/搜索/排序逻辑从 Controller 下沉到此层。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminListService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @DubboReference(check = false)
    private final UserService userService;

    @DubboReference(check = false)
    private final OrderService orderService;

    @DubboReference(check = false)
    private final PointsMallService pointsMallService;

    public List<UserDTO> listUsers(String keyword, String memberLevel, String startDate, String endDate) {
        List<UserDTO> users = userService.listAllUsers();
        if (users == null) return java.util.Collections.emptyList();

        return users.stream().filter(u -> {
            if (keyword != null && !keyword.isEmpty()) {
                String kw = keyword.toLowerCase();
                boolean nameMatch = u.getUsername() != null && u.getUsername().toLowerCase().contains(kw);
                boolean phoneMatch = u.getPhone() != null && u.getPhone().contains(kw);
                boolean emailMatch = u.getEmail() != null && u.getEmail().toLowerCase().contains(kw);
                if (!nameMatch && !phoneMatch && !emailMatch) return false;
            }
            if (memberLevel != null && !memberLevel.isEmpty() && !memberLevel.equals(u.getMemberLevel())) return false;
            if (startDate != null && !startDate.isEmpty()
                    && u.getCreatedAt() != null
                    && u.getCreatedAt().toLocalDate().isBefore(java.time.LocalDate.parse(startDate))) return false;
            if (endDate != null && !endDate.isEmpty()
                    && u.getCreatedAt() != null
                    && u.getCreatedAt().toLocalDate().isAfter(java.time.LocalDate.parse(endDate))) return false;
            return true;
        }).collect(Collectors.toList());
    }

    public List<ShopOrderDTO> listOrders(String status, String orderNo, String keyword, Long filterUserId,
                                          String startDate, String endDate, boolean noCache) {
        String cacheKey = AdminCacheUtil.buildKey(AdminCacheUtil.ORDERS_LIST_PREFIX, status, orderNo, keyword, filterUserId, startDate, endDate);
        if (!noCache) {
            List<ShopOrderDTO> cached = AdminCacheUtil.readCache(redisTemplate, objectMapper, cacheKey,
                    new TypeReference<List<ShopOrderDTO>>() {});
            if (cached != null) return cached;
        }

        List<ShopOrderDTO> orders = orderService.listAllOrders(status);

        Map<Long, UserDTO> userMap = null;
        if (keyword != null && !keyword.isEmpty()) {
            List<UserDTO> allUsers = userService.listAllUsers();
            if (allUsers != null)
                userMap = allUsers.stream().collect(Collectors.toMap(UserDTO::getId, u -> u, (k1, k2) -> k1));
        }
        final String kw = (keyword != null && !keyword.isEmpty()) ? keyword.toLowerCase() : null;
        final Map<Long, UserDTO> finalUserMap = userMap;

        if (filterUserId != null)
            orders = orders.stream().filter(o -> filterUserId.equals(o.getUserId())).collect(Collectors.toList());
        if (orderNo != null && !orderNo.isEmpty())
            orders = orders.stream().filter(o -> o.getOrderNo() != null && o.getOrderNo().contains(orderNo)).collect(Collectors.toList());
        if (kw != null) {
            orders = orders.stream().filter(o -> {
                if (o.getOrderNo() != null && o.getOrderNo().toLowerCase().contains(kw)) return true;
                if (o.getUserId() != null && finalUserMap != null) {
                    UserDTO u = finalUserMap.get(o.getUserId());
                    if (u != null) {
                        if (u.getPhone() != null && u.getPhone().contains(kw)) return true;
                        if (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw)) return true;
                        if (u.getNickname() != null && u.getNickname().toLowerCase().contains(kw)) return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());
        }
        if (startDate != null && !startDate.isEmpty()) {
            var start = java.time.LocalDate.parse(startDate);
            orders = orders.stream().filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().toLocalDate().isBefore(start))
                    .collect(Collectors.toList());
        }
        if (endDate != null && !endDate.isEmpty()) {
            var end = java.time.LocalDate.parse(endDate);
            orders = orders.stream().filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().toLocalDate().isAfter(end))
                    .collect(Collectors.toList());
        }

        orders.sort((a, b) -> {
            if (b.getCreatedAt() == null) return -1;
            if (a.getCreatedAt() == null) return 1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        if (!noCache)
            AdminCacheUtil.writeCache(redisTemplate, cacheKey, orders, AdminCacheUtil.ORDER_LIST_TTL_SECONDS);
        return orders;
    }

    public List<PointsOrderDTO> listRedemptions(String status, String keyword, Long filterUserId, String startDate, String endDate) {
        List<PointsOrderDTO> orders = pointsMallService.listAllOrders(status);

        Map<Long, UserDTO> userMap = null;
        if (keyword != null && !keyword.isEmpty()) {
            List<UserDTO> allUsers = userService.listAllUsers();
            if (allUsers != null)
                userMap = allUsers.stream().collect(Collectors.toMap(UserDTO::getId, u -> u, (k1, k2) -> k1));
        }
        final Map<Long, UserDTO> finalUserMap = userMap;

        if (orders != null) {
            orders = orders.stream().filter(o -> {
                if (filterUserId != null && !filterUserId.equals(o.getUserId())) return false;
                if (keyword != null && !keyword.isEmpty()) {
                    String kw = keyword.toLowerCase();
                    boolean matches = false;
                    if (o.getOrderNo() != null && o.getOrderNo().toLowerCase().contains(kw)) matches = true;
                    if (!matches && o.getProductName() != null && o.getProductName().toLowerCase().contains(kw)) matches = true;
                    if (!matches && o.getReceiverPhone() != null && o.getReceiverPhone().contains(kw)) matches = true;
                    if (!matches && o.getUserId() != null && finalUserMap != null) {
                        UserDTO u = finalUserMap.get(o.getUserId());
                        if (u != null && u.getPhone() != null && u.getPhone().contains(kw)) matches = true;
                    }
                    if (!matches) return false;
                }
                return true;
            }).filter(o -> {
                if (startDate != null && !startDate.isEmpty()
                        && o.getCreatedAt() != null
                        && o.getCreatedAt().toLocalDate().isBefore(java.time.LocalDate.parse(startDate))) return false;
                if (endDate != null && !endDate.isEmpty()
                        && o.getCreatedAt() != null
                        && o.getCreatedAt().toLocalDate().isAfter(java.time.LocalDate.parse(endDate))) return false;
                return true;
            }).collect(Collectors.toList());

            orders.sort((a, b) -> {
                if (b.getCreatedAt() == null) return -1;
                if (a.getCreatedAt() == null) return 1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
        }
        return orders;
    }
}
