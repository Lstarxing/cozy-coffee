package com.cozy.gateway.service;

import com.cozy.common.mq.OrderCreatedEvent;
import com.cozy.common.mq.OrderPaidEvent;
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

    /**
     * 用户支付成功后自动接单，接单成功后派发 ORDER_PAID 通知商家。
     * <p>
     * 事件在 Dubbo 返回后发 —— 此时 provider 的 {@code @Transactional acceptUserOrder} 已提交。
     * 刻意**不**放进 provider 的方法体内：那是提交前，一旦事务回滚就会通知商家"已付款"。
     * 失败会抛异常，天然走不到下面的派发（"成功才发、失败不发"）。
     */
    public ShopOrderDTO acceptUserOrder(Long userId, Long orderId) {
        ShopOrderDTO order = orderService.acceptUserOrder(orderId, userId);

        MemberContext member = getMemberContext(userId);
        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .username(member.nickname())
                .payAmount(order.getPayAmount())
                .itemCount(order.getTotalQuantity())
                .build();
        orderEventProducer.publishOrderPaid(event);

        return order;
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
