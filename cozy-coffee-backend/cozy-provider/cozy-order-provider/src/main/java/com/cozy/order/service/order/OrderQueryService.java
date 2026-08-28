package com.cozy.order.service.order;
import com.cozy.order.service.converter.OrderDtoConverter;
import com.cozy.order.service.converter.OrderDtoEnricher;
import com.cozy.order.service.product.MenuCacheService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.common.exception.BusinessException;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.MonthlyStatsDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.dto.response.ShopOrderItemDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.entity.ShopOrderItem;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ShopOrderMapper;
import com.cozy.order.mapper.ShopOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final CoffeeProductMapper productMapper;
    private final ShopOrderMapper orderMapper;
    private final ShopOrderItemMapper orderItemMapper;
    private final MenuCacheService menuCacheService;
    private final OrderDtoConverter dtoConverter;
    private final OrderDtoEnricher orderDtoEnricher;

    @DubboReference(check = false)
    private MemberService memberService;

    public List<CoffeeProductDTO> listCoffeeProducts() {
        return menuCacheService.getMenu();
    }

    public CoffeeProductDTO getProduct(Long productId) {
        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return dtoConverter.toProductDTO(product);
    }

    public List<CoffeeProductDTO> listAllProducts() {
        List<CoffeeProduct> products = productMapper.selectList(null);
        return products.stream()
                .map(dtoConverter::toProductDTO)
                .collect(Collectors.toList());
    }

    public List<ShopOrderDTO> listUserOrders(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getUserId, userId)
                .orderByDesc(ShopOrder::getCreatedAt);
        List<ShopOrder> orders = orderMapper.selectList(wrapper);

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = orders.stream().map(ShopOrder::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ShopOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(ShopOrderItem::getOrderId, orderIds);
        List<ShopOrderItem> allItems = orderItemMapper.selectList(itemWrapper);

        Map<Long, List<ShopOrderItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(ShopOrderItem::getOrderId));

        MemberDTO memberInfo = null;
        try {
            memberInfo = memberService.getMemberByUserId(userId);
        } catch (Exception e) {
            // ignore
        }
        final MemberDTO finalMember = memberInfo;

        return orders.stream()
                .map(o -> orderDtoEnricher.toOrderDTO(o, itemsMap.get(o.getId()), finalMember))
                .collect(Collectors.toList());
    }

    public ShopOrderDTO getOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此订单");
        }
        List<ShopOrderItem> items = orderDtoEnricher.getOrderItemsByOrderId(orderId);
        return orderDtoEnricher.toOrderDTO(order, items);
    }

    public ShopOrderDTO getOrderDetail(Long orderId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }
        List<ShopOrderItem> items = orderDtoEnricher.getOrderItemsByOrderId(orderId);
        return orderDtoEnricher.toOrderDTO(order, items);
    }

    public List<ShopOrderDTO> listAllOrders(String status) {
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ShopOrder::getStatus, status);
        }
        wrapper.orderByDesc(ShopOrder::getCreatedAt);
        List<ShopOrder> orders = orderMapper.selectList(wrapper);

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = orders.stream().map(ShopOrder::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ShopOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(ShopOrderItem::getOrderId, orderIds);
        List<ShopOrderItem> allItems = orderItemMapper.selectList(itemWrapper);

        Map<Long, List<ShopOrderItem>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(ShopOrderItem::getOrderId));

        Set<Long> userIds = orders.stream()
                .map(ShopOrder::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, MemberDTO> memberMap = new HashMap<>();
        try {
            if (!userIds.isEmpty()) {
                memberMap = memberService.getMembersByUserIds(userIds);
            }
        } catch (Exception e) {
            log.warn("批量获取会员信息失败，回退到单条查询: {}", e.getMessage());
        }

        final Map<Long, MemberDTO> finalMemberMap = memberMap;

        return orders.stream()
                .map(o -> orderDtoEnricher.toOrderDTOWithMember(o, itemsMap.get(o.getId()), finalMemberMap.get(o.getUserId())))
                .collect(Collectors.toList());
    }

    public Map<String, Long> getOrderStatusCounts() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ShopOrder> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.select("status", "count(*) as cnt").groupBy("status");
        List<Map<String, Object>> list = orderMapper.selectMaps(wrapper);

        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> map : list) {
            String status = (String) map.get("status");
            Number cnt = (Number) map.get("cnt");
            if (status != null && cnt != null) {
                result.put(status, cnt.longValue());
            }
        }
        return result;
    }

    public MonthlyStatsDTO getMonthlyStats(Long userId) {
        MonthlyStatsDTO stats = new MonthlyStatsDTO();
        if (userId == null)
            return stats;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59)
                .withSecond(59);

        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getUserId, userId)
                .ge(ShopOrder::getCreatedAt, start)
                .le(ShopOrder::getCreatedAt, end);

        List<ShopOrder> orders = orderMapper.selectList(wrapper);
        orders = orders.stream().filter(o -> "completed".equals(o.getStatus())).collect(Collectors.toList());

        stats.setOrderCount(orders.size());
        stats.setMorningOrderCount((int) orders.stream()
                .filter(o -> o.getCreatedAt().getHour() < 10)
                .count());

        stats.setDeliveryOrderCount((int) orders.stream()
                .filter(o -> "DELIVERY".equals(o.getDiningMethod()))
                .count());

        int newProductOrders = 0;
        if (!orders.isEmpty()) {
            try {
                LambdaQueryWrapper<CoffeeProduct> productWrapper = new LambdaQueryWrapper<>();
                productWrapper.eq(CoffeeProduct::getIsNewProduct, true);
                List<Long> newProductIds = productMapper.selectList(productWrapper).stream()
                        .map(CoffeeProduct::getId)
                        .collect(Collectors.toList());

                if (!newProductIds.isEmpty()) {
                    List<Long> orderIds = orders.stream().map(ShopOrder::getId).collect(Collectors.toList());

                    LambdaQueryWrapper<ShopOrderItem> itemWrapper = new LambdaQueryWrapper<>();
                    itemWrapper.in(ShopOrderItem::getOrderId, orderIds)
                            .in(ShopOrderItem::getProductId, newProductIds);
                    List<ShopOrderItem> newProductItems = orderItemMapper.selectList(itemWrapper);

                    newProductOrders = (int) newProductItems.stream()
                            .map(ShopOrderItem::getOrderId)
                            .distinct()
                            .count();
                }
            } catch (Exception e) {
                log.warn("新品统计失败: {}", e.getMessage());
            }
        }
        stats.setNewProductCount(newProductOrders);

        return stats;
    }
}
