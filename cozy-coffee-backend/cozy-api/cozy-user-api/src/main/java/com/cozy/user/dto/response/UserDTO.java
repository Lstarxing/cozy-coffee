package com.cozy.user.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String memberCode;
    private String phone;
    private String email;
    private String birthday;
    private java.time.LocalDateTime birthdaySetAt;
    private String inviteCode; // 用户专属邀请码（用于分享）
    private boolean hasAppliedInviteCode; // 是否已填写过邀请码
    private String role; // 用户角色 user/admin
    private String status; // 用户状态 active/disabled
    private LocalDateTime createdAt;

    // 会员信息（管理端用）
    private String memberLevel; // 会员等级
    private Integer currentPoints; // 当前积分
    private Integer totalPoints; // 累计积分
}
