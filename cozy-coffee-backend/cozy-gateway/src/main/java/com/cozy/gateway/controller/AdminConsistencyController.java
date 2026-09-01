package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.util.AuthUtil;
import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.response.PointsRefundDeadLetterDTO;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.response.OrderOutboxDeadLetterDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 一致性任务运维入口；由 /api/admin/** 的管理员鉴权统一保护。 */
@RestController
@RequestMapping("/api/admin/consistency")
public class AdminConsistencyController {

    @DubboReference(check = false)
    private OrderService orderService;

    @DubboReference(check = false)
    private PointsMallService pointsMallService;

    @GetMapping("/dead-counts")
    public Result<Map<String, Long>> deadCounts() {
        return Result.success(Map.of(
                "orderOutbox", orderService.countDeadOutboxMessages(),
                "pointsRefund", pointsMallService.countDeadPointRefunds()));
    }

    @GetMapping("/order-outbox/dead")
    public Result<List<OrderOutboxDeadLetterDTO>> listDeadOrderOutbox(
            @RequestParam(defaultValue = "100") Integer limit) {
        return Result.success(orderService.listDeadOutboxMessages(limit));
    }

    @PostMapping("/order-outbox/{id}/retry")
    public Result<Void> retryDeadOrderOutbox(@PathVariable Long id) {
        orderService.retryDeadOutboxMessage(id, AuthUtil.requireUserId());
        return Result.success(null, "订单消息已恢复为待重试");
    }

    @GetMapping("/points-refunds/dead")
    public Result<List<PointsRefundDeadLetterDTO>> listDeadPointRefunds(
            @RequestParam(defaultValue = "100") Integer limit) {
        return Result.success(pointsMallService.listDeadPointRefunds(limit));
    }

    @PostMapping("/points-refunds/{id}/retry")
    public Result<Void> retryDeadPointRefund(@PathVariable Long id) {
        pointsMallService.retryDeadPointRefund(id, AuthUtil.requireUserId());
        return Result.success(null, "积分退款已恢复为待重试");
    }
}
