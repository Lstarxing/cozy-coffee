package com.cozy.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.mall.entity.PointsOrder;
import com.cozy.mall.entity.PointsProduct;
import com.cozy.mall.mapper.PointsOrderMapper;
import com.cozy.mall.mapper.PointsProductMapper;
import com.cozy.member.api.AddressService;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.PointsMallService;
import com.cozy.member.dto.request.RedeemRequest;
import com.cozy.member.dto.response.AddressDTO;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.dto.response.PointsOrderDTO;
import com.cozy.member.dto.response.PointsProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 积分商城服务实现 - 独立微服务 (mall-provider)
 * 实现 PointsMallService 接口，数据存储在 cozy_mall 数据库
 * 通过Dubbo RPC调用 member-provider 获取会员信息和收货地址
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class PointsMallServiceImpl implements PointsMallService {

    private final PointsProductMapper productMapper;
    private final PointsOrderMapper orderMapper;

    // 跨服务调用：会员服务（获取积分、扣减积分）
    @DubboReference(check = false)
    private MemberService memberService;

    // 跨服务调用：地址服务（获取收货地址）
    @DubboReference(check = false)
    private AddressService addressService;

    @Override
    public List<PointsProductDTO> listActiveProducts() {
        log.info("获取积分商城商品列表");
        LambdaQueryWrapper<PointsProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsProduct::getStatus, "active")
                .orderByAsc(PointsProduct::getPointsPrice);
        return productMapper.selectList(wrapper).stream()
                .map(this::toProductDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PointsProductDTO getProduct(Long id) {
        if (id == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        PointsProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        return toProductDTO(product);
    }

    @Override
    @Transactional
    public PointsOrderDTO redeem(Long userId, RedeemRequest request) {
        log.info("用户 {} 发起兑换请求: productId={}", userId, request.getProductId());

        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (request == null || request.getProductId() == null) {
            throw new RuntimeException("请选择商品");
        }
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;

        // 查询商品
        PointsProduct product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!"active".equals(product.getStatus())) {
            throw new RuntimeException("商品已下架");
        }
        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }

        // 跨服务调用：获取会员信息
        MemberDTO memberDTO = memberService.getMemberByUserId(userId);
        if (memberDTO == null) {
            throw new RuntimeException("会员信息不存在");
        }

        // 计算需要的积分（根据会员等级可能有折扣）
        int totalCost = calculateCost(product.getPointsPrice(), quantity, memberDTO.getMemberLevel());

        if (memberDTO.getCurrentPoints() < totalCost) {
            throw new RuntimeException("积分不足，当前积分: " + memberDTO.getCurrentPoints() + "，需要: " + totalCost);
        }

        // 跨服务调用：获取用户收货地址
        AddressDTO address = addressService.getById(request.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new RuntimeException("收货地址不存在");
        }

        // 扣减库存
        product.setStock(product.getStock() - quantity);
        productMapper.updateById(product);

        // 创建订单
        PointsOrder order = new PointsOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setProductImage(product.getImageUrl());
        order.setQuantity(quantity);
        order.setPointsCost(totalCost);
        order.setStatus("pending");
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(buildFullAddress(address));
        orderMapper.insert(order);

        // 跨服务调用：扣减积分
        memberService.addPoints(userId, -totalCost, "redeem",
                "兑换商品: " + product.getName() + " x" + quantity);

        log.info("兑换成功: orderNo={}, cost={}", order.getOrderNo(), totalCost);
        return toOrderDTO(order);
    }

    @Override
    public List<PointsOrderDTO> listUserOrders(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        LambdaQueryWrapper<PointsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsOrder::getUserId, userId)
                .orderByDesc(PointsOrder::getCreatedAt);
        return orderMapper.selectList(wrapper).stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PointsOrderDTO getOrder(Long orderId, Long userId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }
        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public PointsOrderDTO cancelOrder(Long orderId, Long userId) {
        log.info("用户 {} 取消订单: orderId={}", userId, orderId);

        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!"pending".equals(order.getStatus()) && !"processing".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许取消");
        }

        // 更新订单状态
        order.setStatus("cancelled");
        orderMapper.updateById(order);

        // 恢复库存
        PointsProduct product = productMapper.selectById(order.getProductId());
        if (product != null) {
            product.setStock(product.getStock() + order.getQuantity());
            productMapper.updateById(product);
        }

        // 跨服务调用：返还积分
        memberService.addPoints(userId, order.getPointsCost(), "refund",
                "订单取消退还: " + order.getProductName() + " x" + order.getQuantity());

        log.info("订单取消成功: orderNo={}, refund={}", order.getOrderNo(), order.getPointsCost());
        return toOrderDTO(order);
    }

    /**
     * 根据会员等级计算积分消耗
     * 黑卡9折，金卡95折
     */
    private int calculateCost(int basePrice, int quantity, String memberLevel) {
        double discount = 1.0;
        if ("black".equals(memberLevel)) {
            discount = 0.9;
        } else if ("gold".equals(memberLevel)) {
            discount = 0.95;
        }
        return (int) Math.ceil(basePrice * quantity * discount);
    }

    private String buildFullAddress(AddressDTO address) {
        StringBuilder sb = new StringBuilder();
        if (address.getProvince() != null)
            sb.append(address.getProvince());
        if (address.getCity() != null)
            sb.append(address.getCity());
        if (address.getDistrict() != null)
            sb.append(address.getDistrict());
        if (address.getDetailAddress() != null)
            sb.append(address.getDetailAddress());
        return sb.toString();
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06X", new Random().nextInt(0xFFFFFF));
        return "PM" + timestamp + random;
    }

    private PointsProductDTO toProductDTO(PointsProduct entity) {
        PointsProductDTO dto = new PointsProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImageUrl());
        dto.setPointsPrice(entity.getPointsPrice());
        dto.setOriginalPrice(entity.getOriginalPrice());
        dto.setStock(entity.getStock());
        dto.setStatus(entity.getStatus());
        dto.setCategory(entity.getCategory());
        return dto;
    }

    private PointsOrderDTO toOrderDTO(PointsOrder entity) {
        PointsOrderDTO dto = new PointsOrderDTO();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setUserId(entity.getUserId());
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setProductImage(entity.getProductImage());
        dto.setPointsCost(entity.getPointsCost());
        dto.setQuantity(entity.getQuantity());
        dto.setReceiverName(entity.getReceiverName());
        dto.setReceiverPhone(entity.getReceiverPhone());
        dto.setReceiverAddress(entity.getReceiverAddress());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        // 物流信息
        dto.setShippingCompany(entity.getShippingCompany());
        dto.setTrackingNumber(entity.getTrackingNumber());
        dto.setShippedAt(entity.getShippedAt());
        // 配送方式（如果有收货地址则是快递，否则是自提）
        dto.setDeliveryType(entity.getReceiverAddress() != null ? "express" : "pickup");
        return dto;
    }

    // ==================== 管理端方法 ====================

    @Override
    public List<PointsOrderDTO> listAllOrders(String status) {
        log.info("管理端获取兑换订单列表: status={}", status);
        LambdaQueryWrapper<PointsOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PointsOrder::getStatus, status);
        }
        wrapper.orderByDesc(PointsOrder::getCreatedAt);
        return orderMapper.selectList(wrapper).stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PointsOrderDTO updateOrderStatus(Long orderId, String status) {
        log.info("管理端更新订单状态: orderId={}, status={}", orderId, status);
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        order.setStatus(status);
        orderMapper.updateById(order);
        return toOrderDTO(order);
    }

    @Override
    @Transactional
    public PointsOrderDTO updateShipping(Long orderId, String company, String trackingNo) {
        log.info("管理端更新物流信息: orderId={}, company={}, trackingNo={}", orderId, company, trackingNo);
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        PointsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        order.setShippingCompany(company);
        order.setTrackingNumber(trackingNo);
        order.setStatus("shipped");
        order.setShippedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return toOrderDTO(order);
    }
}
