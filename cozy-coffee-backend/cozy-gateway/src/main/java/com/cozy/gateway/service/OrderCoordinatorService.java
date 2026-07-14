package com.cozy.gateway.service;

import com.cozy.common.mq.OrderCreatedEvent;
import com.cozy.common.exception.BusinessErrorCode;
import com.cozy.common.exception.BusinessException;
import com.cozy.gateway.mq.OrderEventProducer;
import com.cozy.member.api.MemberService;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.response.CartCheckResultDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单编排服务。
 * 将会员查询、下单、事件发布等跨 Provider 的编排逻辑从 Controller 下沉到此层。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCoordinatorService {

    @DubboReference(check = false)
    private OrderService orderService;

    @DubboReference(check = false)
    private MemberService memberService;

    private final OrderEventProducer orderEventProducer;

    public ShopOrderDTO createOrder(Long userId, String idempotencyKey, CreateOrderRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 64) {
            throw new BusinessException(BusinessErrorCode.IDEMPOTENCY_KEY_INVALID,
                    "Idempotency-Key 必须为 1-64 个字符");
        }
        log.info("创建订单: userId={}, itemsCount={}",
                userId, request.getItems() != null ? request.getItems().size() : 0);

        MemberContext member = getMemberContext(userId);
        ShopOrderDTO order = orderService.createOrder(userId, member.level(), idempotencyKey, request);

        if (!Boolean.TRUE.equals(order.getIdempotentReplay())) {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .userId(userId)
                    .username(member.nickname())
                    .payAmount(order.getPayAmount())
                    .itemCount(order.getTotalQuantity())
                    .occurredAt(LocalDateTime.now())
                    .build();
            orderEventProducer.publishOrderCreated(event);
        }

        return order;
    }

    public CartCheckResultDTO checkCart(Long userId, CartCheckRequest request) {
        MemberContext member = getMemberContext(userId);
        return orderService.checkCart(userId, member.level(), request);
    }

    private MemberContext getMemberContext(Long userId) {
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
        return new MemberContext(memberLevel, nickname);
    }

    private record MemberContext(String level, String nickname) {}
}
