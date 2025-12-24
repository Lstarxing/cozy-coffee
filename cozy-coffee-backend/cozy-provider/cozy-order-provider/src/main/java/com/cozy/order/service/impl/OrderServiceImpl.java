package com.cozy.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.member.api.MemberService;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.response.CoffeeProductDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ShopOrderMapper;
import com.cozy.order.service.PickupCodeService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 订单服务实现 - 独立微服务
 * 通过Dubbo远程调用MemberService发放积分
 */
@DubboService
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CoffeeProductMapper productMapper;
    private final ShopOrderMapper orderMapper;
    private final PickupCodeService pickupCodeService;

    // 跨服务调用：通过Dubbo RPC调用会员服务
    @DubboReference(check = false)
    private MemberService memberService;

    @Override
    public List<CoffeeProductDTO> listCoffeeProducts() {
        LambdaQueryWrapper<CoffeeProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoffeeProduct::getStatus, "active")
                .orderByAsc(CoffeeProduct::getSortOrder);
        return productMapper.selectList(wrapper).stream()
                .map(this::toProductDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CoffeeProductDTO getProduct(Long productId) {
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public ShopOrderDTO createOrder(Long userId, String memberLevel, CreateOrderRequest request) {
        // 参数验证
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (request == null || request.getProductId() == null) {
            throw new RuntimeException("请选择商品");
        }
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantity < 1 || quantity > 10) {
            throw new RuntimeException("购买数量需在1-10之间");
        }

        // 查询商品
        CoffeeProduct product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!"active".equals(product.getStatus())) {
            throw new RuntimeException("商品已下架");
        }

        // 根据会员等级计算积分倍率
        BigDecimal multiplier = getPointsMultiplier(memberLevel);

        // 计算订单金额和积分
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        int pointsEarned = totalAmount.multiply(multiplier).intValue();

        // 生成取餐码
        LocalDateTime now = LocalDateTime.now();
        String pickupCode = pickupCodeService.generatePickupCode(1L, now);
        java.time.LocalDate businessDate = pickupCodeService.calculateBusinessDate(now);

        // 创建订单
        ShopOrder order = new ShopOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setQuantity(quantity);
        order.setUnitPrice(product.getPrice());
        order.setTotalAmount(totalAmount);
        order.setPointsEarned(pointsEarned);
        order.setPointsMultiplier(multiplier);
        order.setStatus("pending"); // 改为待处理状态
        order.setRemark(request.getRemark());
        // 取餐码相关
        order.setStoreId(1L);
        order.setBusinessDate(businessDate);
        order.setPickupCode(pickupCode);
        order.setPickupCodeGeneratedAt(now);
        order.setCreatedAt(now); // 手动设置创建时间
        orderMapper.insert(order);

        // 跨服务调用：通过Dubbo RPC调用会员服务发放积分
        memberService.addPoints(userId, pointsEarned, "consume",
                "咖啡消费: " + product.getName() + " x" + quantity);

        return toOrderDTO(order);
    }

    @Override
    public List<ShopOrderDTO> listUserOrders(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getUserId, userId)
                .orderByDesc(ShopOrder::getCreatedAt);
        return orderMapper.selectList(wrapper).stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ShopOrderDTO getOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }
        return toOrderDTO(order);
    }

    // 根据会员等级获取积分倍率
    private BigDecimal getPointsMultiplier(String level) {
        if (level == null)
            level = "basic";
        return switch (level) {
            case "silver" -> new BigDecimal("1.2");
            case "gold" -> new BigDecimal("1.5");
            case "black" -> new BigDecimal("2.0");
            default -> new BigDecimal("1.0");
        };
    }

    // 生成订单编号
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(9000) + 1000;
        return "CF" + timestamp + random;
    }

    private CoffeeProductDTO toProductDTO(CoffeeProduct entity) {
        CoffeeProductDTO dto = new CoffeeProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());
        dto.setImageUrl(entity.getImageUrl());
        dto.setCategory(entity.getCategory());
        return dto;
    }

    private ShopOrderDTO toOrderDTO(ShopOrder entity) {
        ShopOrderDTO dto = new ShopOrderDTO();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setQuantity(entity.getQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setPointsEarned(entity.getPointsEarned());
        dto.setPointsMultiplier(entity.getPointsMultiplier());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        // 取餐码相关
        dto.setPickupCode(entity.getPickupCode());
        dto.setBusinessDate(entity.getBusinessDate());
        return dto;
    }

    // ==================== 管理端方法实现 ====================

    @Override
    public List<ShopOrderDTO> listAllOrders(String status) {
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ShopOrder::getStatus, status);
        }
        wrapper.orderByDesc(ShopOrder::getCreatedAt);
        return orderMapper.selectList(wrapper).stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShopOrderDTO updateOrderStatus(Long orderId, String status) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        order.setStatus(status);
        orderMapper.updateById(order);
        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public ShopOrderDTO acceptOrder(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new RuntimeException("只有待处理订单可以接单");
        }

        // 如果还没有取餐码，生成一个
        if (order.getPickupCode() == null || order.getPickupCode().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            String pickupCode = pickupCodeService.generatePickupCode(1L, now);
            java.time.LocalDate businessDate = pickupCodeService.calculateBusinessDate(now);
            order.setPickupCode(pickupCode);
            order.setBusinessDate(businessDate);
            order.setPickupCodeGeneratedAt(now);
            order.setStoreId(1L);
        }

        order.setStatus("preparing");
        orderMapper.updateById(order);
        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public ShopOrderDTO completeOrder(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!"preparing".equals(order.getStatus())) {
            throw new RuntimeException("只有制作中的订单可以完成");
        }
        order.setStatus("completed");
        orderMapper.updateById(order);
        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public ShopOrderDTO cancelOrder(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if ("completed".equals(order.getStatus()) || "cancelled".equals(order.getStatus())) {
            throw new RuntimeException("该订单无法取消");
        }
        order.setStatus("cancelled");
        orderMapper.updateById(order);
        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public ShopOrderDTO cancelUserOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        // 验证订单归属
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消该订单");
        }
        // 只有待处理的订单才能取消
        if (!"pending".equals(order.getStatus())) {
            throw new RuntimeException("只有待处理的订单才能取消");
        }
        order.setStatus("cancelled");
        orderMapper.updateById(order);
        return toOrderDTO(order);
    }

    // ==================== 商品管理 (管理端) ====================

    @Override
    public List<CoffeeProductDTO> listAllProducts() {
        LambdaQueryWrapper<CoffeeProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(CoffeeProduct::getSortOrder);
        return productMapper.selectList(wrapper).stream()
                .map(this::toProductDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public CoffeeProductDTO addProduct(CoffeeProductDTO dto) {
        CoffeeProduct product = new CoffeeProduct();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(dto.getCategory());
        product.setStatus("active");
        product.setSortOrder(0);
        product.setCreatedAt(java.time.LocalDateTime.now());
        productMapper.insert(product);
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public CoffeeProductDTO updateProduct(Long productId, CoffeeProductDTO dto) {
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (dto.getName() != null)
            product.setName(dto.getName());
        if (dto.getDescription() != null)
            product.setDescription(dto.getDescription());
        if (dto.getPrice() != null)
            product.setPrice(dto.getPrice());
        if (dto.getImageUrl() != null)
            product.setImageUrl(dto.getImageUrl());
        if (dto.getCategory() != null)
            product.setCategory(dto.getCategory());
        productMapper.updateById(product);
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        productMapper.deleteById(productId);
    }

    @Override
    @Transactional
    public CoffeeProductDTO toggleProductStatus(Long productId) {
        CoffeeProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setStatus("active".equals(product.getStatus()) ? "inactive" : "active");
        productMapper.updateById(product);
        return toProductDTO(product);
    }
}
