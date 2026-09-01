package com.cozy.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coupon_rollback_inbox")
public class CouponRollbackInbox {

    @TableId(type = IdType.INPUT)
    private String eventId;

    private LocalDateTime processedAt;
}
