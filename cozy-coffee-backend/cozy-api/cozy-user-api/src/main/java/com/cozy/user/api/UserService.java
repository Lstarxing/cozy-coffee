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
}
