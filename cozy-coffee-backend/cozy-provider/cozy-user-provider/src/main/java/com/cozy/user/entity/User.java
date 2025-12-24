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
    private String phone;
    private String email;
    private String role = "user"; // 用户角色: user-普通用户, admin-管理员
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
