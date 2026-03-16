package com.cozy.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String memberCode;
    private String inviteCode; // 用户专属邀请码
    private Long invitedBy; // 邀请人用户ID
    private LocalDateTime invitedAt; // 填写邀请码时间
    private Boolean inviteRewardGranted; // v5.0: 邀请奖励是否已发放（被邀请人首单后触发）
    private String phone;
    private String email;
    private String role = "user"; // 用户角色: user-普通用户, admin-管理员
    private String status = "active"; // 用户状态: active-正常, disabled-禁用
    private Integer tokenVersion = 0; // Token版本号，用于禁用时使Token失效

    // v4.2 生日权益
    private java.time.LocalDate birthday;
    private java.time.LocalDateTime birthdaySetAt; // 生日设置时间
    private java.time.LocalDateTime nextBirthdayResetAt; // 下次可修改时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
