package com.cozy.member.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.member.entity.PointsExpiryNotification;
import com.cozy.member.entity.PointsLot;
import com.cozy.member.mapper.PointsExpiryNotificationMapper;
import com.cozy.member.mapper.PointsLotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分到期提醒定时任务
 * 每天凌晨2点执行，扫描即将到期的积分并生成通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpiryJob {

    private final PointsLotMapper pointsLotMapper;
    private final PointsExpiryNotificationMapper notificationMapper;

    // 提醒天数：30天、7天、1天
    private static final int[] REMIND_DAYS = { 30, 7, 1 };

    /**
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scanExpiringPoints() {
        log.info("开始扫描即将到期的积分...");

        for (int days : REMIND_DAYS) {
            processExpiryReminder(days);
        }

        log.info("积分到期扫描完成");
    }

    /**
     * 处理指定天数的到期提醒
     */
    private void processExpiryReminder(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetStart = now.plusDays(days).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime targetEnd = targetStart.plusDays(1);

        // 查询即将到期且有剩余的积分批次
        LambdaQueryWrapper<PointsLot> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(PointsLot::getRemaining, 0)
                .ge(PointsLot::getExpiresAt, targetStart)
                .lt(PointsLot::getExpiresAt, targetEnd);

        List<PointsLot> expiringLots = pointsLotMapper.selectList(wrapper);

        log.info("{}天后到期的积分批次数: {}", days, expiringLots.size());

        for (PointsLot lot : expiringLots) {
            try {
                // 检查是否已经提醒过
                LambdaQueryWrapper<PointsExpiryNotification> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(PointsExpiryNotification::getLotId, lot.getId())
                        .eq(PointsExpiryNotification::getRemindDays, days);

                if (notificationMapper.selectCount(checkWrapper) > 0) {
                    continue; // 已提醒过，跳过
                }

                // 创建提醒记录（仅包含表中存在的字段）
                PointsExpiryNotification notification = new PointsExpiryNotification();
                notification.setUserId(lot.getUserId());
                notification.setLotId(lot.getId());
                notification.setRemindDays(days);
                notification.setSentAt(now);

                notificationMapper.insert(notification);

                log.info("生成积分到期提醒: userId={}, lotId={}, days={}, amount={}",
                        lot.getUserId(), lot.getId(), days, lot.getRemaining());

            } catch (Exception e) {
                // 唯一约束冲突说明已处理过
                log.debug("积分到期提醒已存在或处理失败: lotId={}, days={}", lot.getId(), days);
            }
        }
    }

    /**
     * 手动触发扫描（用于测试）
     */
    public void manualTrigger() {
        log.info("手动触发积分到期扫描");
        scanExpiringPoints();
    }
}
