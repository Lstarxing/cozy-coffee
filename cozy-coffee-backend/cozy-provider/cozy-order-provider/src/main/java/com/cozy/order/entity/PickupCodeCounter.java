package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pickup_code_counter")
public class PickupCodeCounter {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;

    private LocalDate businessDate;

    private Integer lastSeq;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
