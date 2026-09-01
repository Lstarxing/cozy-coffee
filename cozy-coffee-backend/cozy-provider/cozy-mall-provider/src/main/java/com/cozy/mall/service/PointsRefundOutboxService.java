package com.cozy.mall.service;

import com.cozy.mall.entity.PointsRefundOutbox;
import com.cozy.mall.mapper.PointsRefundOutboxMapper;
import com.cozy.member.api.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 积分兑换取消退款的本地 outbox；先随 mall 事务落库，再可靠重试 member RPC。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsRefundOutboxService {

    private static final int MAX_RETRIES = 10;

    private final PointsRefundOutboxMapper mapper;

    @DubboReference(check = false, timeout = 3000, retries = 0)
    private MemberService memberService;

    public void enqueue(Long orderId, Long userId, int points, String consumeType, String description) {
        LocalDateTime now = LocalDateTime.now();
        PointsRefundOutbox msg = new PointsRefundOutbox();
        msg.setOrderId(orderId);
        msg.setUserId(userId);
        msg.setPoints(points);
        msg.setConsumeType(consumeType);
        msg.setDescription(description);
        msg.setStatus("PENDING");
        msg.setRetryCount(0);
        msg.setNextRetryAt(now);
        msg.setCreatedAt(now);
        msg.setUpdatedAt(now);
        try {
            mapper.insert(msg);
        } catch (DuplicateKeyException e) {
            log.info("积分退款 outbox 已存在，幂等跳过: orderId={}, consumeType={}", orderId, consumeType);
        }
    }

    /** 单实例轮询：member 退款本身按 consumeId 幂等，无需 claim/lease 也能安全重试 */
    @Scheduled(fixedDelayString = "${cozy.mall.refund-relay-delay-ms:5000}")
    public void relayPendingRefunds() {
        for (PointsRefundOutbox msg : mapper.selectPendingDue(LocalDateTime.now(), 100)) {
            process(msg);
        }
    }

    private void process(PointsRefundOutbox msg) {
        try {
            memberService.refundPointsByConsumption(msg.getUserId(), msg.getPoints(), msg.getConsumeType(),
                    msg.getOrderId(), msg.getDescription());
            mapper.markSent(msg.getId(), LocalDateTime.now());
            log.info("积分退款 outbox 完成: id={}, orderId={}", msg.getId(), msg.getOrderId());
        } catch (Exception e) {
            int retry = (msg.getRetryCount() == null ? 0 : msg.getRetryCount()) + 1;
            String status = retry >= MAX_RETRIES ? "DEAD" : "PENDING";
            LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(backoffSeconds(retry));
            mapper.markFailed(msg.getId(), status, retry, nextRetryAt, LocalDateTime.now());
            log.error("积分退款 outbox 失败: id={}, orderId={}, retry={}, status={}",
                    msg.getId(), msg.getOrderId(), retry, status, e);
        }
    }

    private long backoffSeconds(int retry) {
        return Math.min(300L, 5L * (1L << Math.min(retry - 1, 6)));
    }
}
