package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("points_transactions")
public class PointsTransaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer changeAmount;
    private Integer balanceAfter;
    private String sourceType;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
