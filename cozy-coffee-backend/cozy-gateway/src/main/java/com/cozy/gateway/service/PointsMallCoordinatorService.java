package com.cozy.gateway.service;

import com.cozy.gateway.sse.SseEventPublisher;
import com.cozy.member.api.PointsMallService;
import com.cozy.member.dto.request.RedeemRequest;
import com.cozy.member.dto.response.PointsOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 积分商城编排服务 — 兑换 + SSE 通知等跨切面逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsMallCoordinatorService {

    @DubboReference(check = false)
    private final PointsMallService pointsMallService;

    private final SseEventPublisher sseEventPublisher;

    public PointsOrderDTO redeem(Long userId, RedeemRequest request) {
        PointsOrderDTO order = pointsMallService.redeem(userId, request);
        try {
            sseEventPublisher.publishNewRedemption(order.getId());
        } catch (Exception e) {
            log.warn("发布新兑换订单 SSE 事件失败", e);
        }
        return order;
    }
}
