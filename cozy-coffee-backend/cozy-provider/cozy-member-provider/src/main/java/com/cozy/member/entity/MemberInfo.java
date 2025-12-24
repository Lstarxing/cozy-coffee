package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("member_info")
public class MemberInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String memberLevel;
    private Integer totalPoints;
    private Integer currentPoints;
    private Integer consecutiveSignDays;
    private LocalDate lastSigninDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
