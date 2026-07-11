package com.cozy.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.entity.ShopOrderItem;
import com.cozy.order.mapper.ShopOrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单 DTO 装配服务。
 * 从 OrderServiceImpl 抽出，统一承担 ShopOrder entity -> ShopOrderDTO 的完整装配
 * （含会员信息查询、商品明细加载、过期信息填充）。
 *
 * 被 OrderQueryService / OrderCreationService / OrderCommandService 共享调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDtoEnricher {

    private final OrderDtoConverter dtoConverter;
    private final ShopOrderItemMapper orderItemMapper;

    @DubboReference(check = false)
    private MemberService memberService;

    @Value("${cozy.order.timeout-cancel.timeout-minutes:1}")
    private int orderTimeoutMinutes;

    /**
     * 装配订单 DTO（不预加载会员信息，内部按需查询）。
     */
    public ShopOrderDTO toOrderDTO(ShopOrder entity, List<ShopOrderItem> items) {
        return toOrderDTO(entity, items, null);
    }

    /**
     * 装配订单 DTO（支持预加载的会员信息，避免 N+1 查询）。
     */
    public ShopOrderDTO toOrderDTO(ShopOrder entity, List<ShopOrderItem> items,
            MemberDTO preLoadedMember) {
        ShopOrderDTO dto = new ShopOrderDTO();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setUserId(entity.getUserId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setStoreId(entity.getStoreId());
        dto.setBusinessDate(entity.getBusinessDate());
        dto.setPickupCode(entity.getPickupCode());
        dto.setPickupCodeGeneratedAt(entity.getPickupCodeGeneratedAt());
        dto.setDiningMethod(entity.getDiningMethod());
        dto.setRemark(entity.getRemark());

        // 金额信息
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setDiscountAmount(entity.getDiscountAmount());
        dto.setPayAmount(entity.getPayAmount());
        dto.setTotalQuantity(entity.getTotalQuantity());
        dto.setAppliedCouponId(entity.getAppliedCouponId());

        // 积分信息
        dto.setExpEarned(entity.getExpEarned());
        dto.setPointsEarned(entity.getPointsEarned());
        dto.setRewardsGranted(entity.getRewardsGranted());

        // 用户详细信息 (Nickname, Phone) - Fix for Admin List
        try {
            if (entity.getUserId() != null) {
                MemberDTO member = preLoadedMember;

                // Only fetch if not provided
                if (member == null) {
                    member = memberService.getMemberByUserId(entity.getUserId());
                }
                if (member != null) {
                    dto.setNickname(member.getNickname());
                    // 优先使用昵称，如果没有则使用用户名
                    dto.setUsername(member.getNickname() != null ? member.getNickname() : "User_" + member.getId());
                    String phone = member.getPhone();
                    if (phone != null && phone.length() >= 7) {
                        dto.setPhoneMasked(phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4));
                    } else {
                        dto.setPhoneMasked(phone);
                    }
                    // 传递会员等级，用于前端展示小图标
                    if (member.getLevel() != null) {
                        dto.setMemberLevel(member.getLevel());
                    }
                }
            }
        } catch (Exception e) {
            // ignore fetch error
        }

        // 商品明细
        if (items != null && !items.isEmpty()) {
            dto.setItems(dtoConverter.toItemDTOList(items));
            // 生成摘要
            String summary = items.stream()
                    .map(i -> i.getProductName() + " x" + i.getQuantity())
                    .collect(Collectors.joining(", "));
            dto.setItemsSummary(summary);
        } else {
            // 如果没有加载 items，尝试生成摘要
            List<ShopOrderItem> loadedItems = getOrderItemsByOrderId(entity.getId());
            if (!loadedItems.isEmpty()) {
                String summary = loadedItems.stream()
                        .map(i -> i.getProductName() + " x" + i.getQuantity())
                        .collect(Collectors.joining(", "));
                dto.setItemsSummary(summary);
                dto.setItems(dtoConverter.toItemDTOList(loadedItems));
            }
        }

        dto.setPointsMultiplier(entity.getPointsMultiplier());

        // v5.3: 配送费信息
        dto.setDeliveryFee(entity.getDeliveryFee());
        dto.setDeliveryFeeWaived(entity.getDeliveryFeeWaived());
        dto.setDeliveryFeeWaivedReason(entity.getDeliveryFeeWaivedReason());
        populateExpiryInfo(entity, dto);

        return dto;
    }

    /**
     * 转换订单DTO（支持预加载的会员信息，避免N+1查询）
     * 委托给 toOrderDTO(entity, items, member) -- 两个方法逻辑完全一致，
     * 仅 member 参数语义不同：null=自行查询, 非null=使用预加载值
     */
    public ShopOrderDTO toOrderDTOWithMember(ShopOrder entity, List<ShopOrderItem> items,
            MemberDTO member) {
        return toOrderDTO(entity, items, member);
    }

    public List<ShopOrderItem> getOrderItemsByOrderId(Long orderId) {
        LambdaQueryWrapper<ShopOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrderItem::getOrderId, orderId);
        return orderItemMapper.selectList(wrapper);
    }

    public void populateExpiryInfo(ShopOrder entity, ShopOrderDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        if (!"pending".equalsIgnoreCase(entity.getStatus()) || entity.getCreatedAt() == null) {
            return;
        }

        LocalDateTime expireAt = entity.getCreatedAt().plusMinutes(Math.max(orderTimeoutMinutes, 1));
        long remainingSeconds = Duration.between(LocalDateTime.now(), expireAt).getSeconds();
        if (remainingSeconds < 0) {
            remainingSeconds = 0;
        }

        dto.setExpireAt(expireAt);
        dto.setSecondsToExpire(remainingSeconds);
        dto.setAboutToExpire(remainingSeconds <= 30);
    }
}
