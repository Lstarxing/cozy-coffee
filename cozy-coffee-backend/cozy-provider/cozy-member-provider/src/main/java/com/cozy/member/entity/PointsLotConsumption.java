package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 积分扣减明细实体
 */
@Data
@TableName("points_lot_consumptions")
public class PointsLotConsumption {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long lotId;
    private Integer consumeAmount;
    private String consumeType; // redeem
    private Long consumeId;
    private LocalDateTime createdAt;
}
