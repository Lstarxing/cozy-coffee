package com.cozy.mall.service;

import com.cozy.common.exception.BusinessException;
import com.cozy.mall.dto.response.PointsRefundDeadLetterDTO;
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
import java.util.List;

/** 积分兑换取消退款的本地 outbox；先随 mall 事务落库，再可靠重试 member RPC。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsRefundOutboxService {

    private static final int MAX_RETRIES = 10;
    private static final int LEASE_MINUTES = 2;

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

    @Scheduled(fixedDelayString = "${cozy.mall.refund-relay-delay-ms:5000}")
    public void relayPendingRefunds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusMinutes(LEASE_MINUTES);
        for (PointsRefundOutbox candidate : mapper.selectRelayCandidates(now, staleBefore, 100)) {
            if (mapper.claim(candidate.getId(), now, staleBefore) == 0) {
                continue;
            }
            processClaimed(candidate);
        }
    }

    public List<PointsRefundDeadLetterDTO> listDeadRefunds(Integer limit) {
        int safeLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        return mapper.selectDeadBatch(safeLimit).stream().map(this::toDeadLetterDTO).toList();
    }

    public long countDeadRefunds() {
        return mapper.countDead();
    }

    public void retryDeadRefund(Long id, Long operatorId) {
        if (id == null) {
            throw new BusinessException("DEAD 退款ID不能为空");
        }
        if (operatorId == null) {
            throw new BusinessException("人工重试操作人不能为空");
        }
        if (mapper.retryDead(id, operatorId, LocalDateTime.now()) == 0) {
            throw new BusinessException("退款任务不存在或已不处于 DEAD 状态");
        }
        log.warn("积分退款 outbox 已人工恢复为 PENDING: id={}, operatorId={}", id, operatorId);
    }

    /** 周期告警入口：日志平台应对该 ERROR 文案配置通知规则。 */
    @Scheduled(fixedDelayString = "${cozy.mall.refund-dead-alert-delay-ms:300000}")
    public void alertDeadRefunds() {
        long deadCount = mapper.countDead();
        if (deadCount > 0) {
            log.error("CONSISTENCY_ALERT points_refund_dead_count={}; 请在管理端检查并人工重试", deadCount);
        }
    }

    private void processClaimed(PointsRefundOutbox msg) {
        try {
            memberService.refundPointsByConsumption(msg.getUserId(), msg.getPoints(), msg.getConsumeType(),
                    msg.getOrderId(), msg.getDescription());
            mapper.markSent(msg.getId(), LocalDateTime.now());
            log.info("积分退款 outbox 完成: id={}, orderId={}", msg.getId(), msg.getOrderId());
        } catch (Exception e) {
            int retry = (msg.getRetryCount() == null ? 0 : msg.getRetryCount()) + 1;
            String status = retry >= MAX_RETRIES ? "DEAD" : "PENDING";
            LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(backoffSeconds(retry));
            String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (error.length() > 500) {
                error = error.substring(0, 500);
            }
            mapper.markFailed(msg.getId(), status, retry, nextRetryAt, error, LocalDateTime.now());
            log.error("积分退款 outbox 失败: id={}, orderId={}, retry={}, status={}",
                    msg.getId(), msg.getOrderId(), retry, status, e);
        }
    }

    private long backoffSeconds(int retry) {
        return Math.min(300L, 5L * (1L << Math.min(retry - 1, 6)));
    }

    private PointsRefundDeadLetterDTO toDeadLetterDTO(PointsRefundOutbox msg) {
        PointsRefundDeadLetterDTO dto = new PointsRefundDeadLetterDTO();
        dto.setId(msg.getId());
        dto.setOrderId(msg.getOrderId());
        dto.setUserId(msg.getUserId());
        dto.setPoints(msg.getPoints());
        dto.setConsumeType(msg.getConsumeType());
        dto.setDescription(msg.getDescription());
        dto.setStatus(msg.getStatus());
        dto.setRetryCount(msg.getRetryCount());
        dto.setManualRetryCount(msg.getManualRetryCount());
        dto.setLastError(msg.getLastError());
        dto.setNextRetryAt(msg.getNextRetryAt());
        dto.setLastManualRetryAt(msg.getLastManualRetryAt());
        dto.setLastManualRetryBy(msg.getLastManualRetryBy());
        dto.setCreatedAt(msg.getCreatedAt());
        dto.setUpdatedAt(msg.getUpdatedAt());
        return dto;
    }
}
