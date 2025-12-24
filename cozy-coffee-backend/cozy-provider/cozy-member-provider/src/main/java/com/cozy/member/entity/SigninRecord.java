package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("signin_records")
public class SigninRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate signinDate;
    private Integer pointsEarned;
    private Integer consecutiveDays;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
