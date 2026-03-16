package com.cozy.user.api;

import com.cozy.user.dto.request.LoginRequest;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;

/**
 * 用户服务 Dubbo 接口
 */
public interface UserService {

    void register(RegisterRequest request);

    String login(LoginRequest request);

    UserDTO getUserById(Long userId);

    UserDTO getUserByUsername(String username);

    void updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 填写邀请码获取积分
     * 
     * @param userId     当前用户ID
     * @param inviteCode 邀请人的邀请码
     */
    void applyInviteCode(Long userId, String inviteCode);

    /**
     * 根据邀请码查找用户
     */
    UserDTO getUserByInviteCode(String inviteCode);

    /**
     * 获取所有用户列表（管理端用）
     */
    java.util.List<UserDTO> listAllUsers();

    /**
     * 更新用户状态（管理端用）
     * 
     * @param userId 用户ID
     * @param status 新状态 active/disabled
     */
    void updateUserStatus(Long userId, String status);

    /**
     * 获取用户详情（含会员信息）
     */
    UserDTO getUserDetail(Long userId);

    /**
     * 获取用户token版本号（用于校验Token是否失效）
     */
    Integer getTokenVersion(Long userId);

    /**
     * 获取指定月日生日的用户ID列表
     */
    java.util.List<Long> findUsersByBirthday(int month, int day);

    /**
     * v5.0: 被邀请人首单完成时触发邀请奖励发放
     * 
     * @param userId 被邀请人的用户ID
     * @return 是否成功发放奖励（如果已发放过则返回false）
     */
    boolean grantInviteRewardOnFirstOrder(Long userId);
}
