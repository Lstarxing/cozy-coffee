package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 积分到期提醒去重实体 (v4.2)
 * 表结构: id, user_id, lot_id, remind_days, sent_at
 */
@Data
@TableName("points_expiry_notifications")
public class PointsExpiryNotification {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long lotId;

    private Integer remindDays; // 30/7/1

    private LocalDateTime sentAt;
}
