package com.cozy.user.api;

import com.cozy.common.exception.BusinessException;
import com.cozy.user.dto.request.LoginRequest;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;

import java.util.List;
import java.util.Set;

/**
 * 用户服务 Dubbo 接口
 */
public interface UserService {

    void register(RegisterRequest request) throws BusinessException;

    /**
     * 必须在签名里声明 BusinessException：Dubbo ExceptionFilter 只对「签名声明过」或「与接口同 jar」的异常原样回传，
     * 否则会把它包成 RuntimeException(StringUtils.toString(e)) —— 客户端拿到的是堆栈字符串且丢失 errorCode。
     */
    String login(LoginRequest request) throws BusinessException;

    String loginWechatDev(String deviceId) throws BusinessException;

    /**
     * 微信登录：按 openid 查找或创建用户并签发 token。
     */
    String loginWechat(String openid) throws BusinessException;

    void resetPasswordDev(String username, String newPassword) throws BusinessException;

    /**
     * 修改登录密码：校验原密码后更新为新密码，并使既有会话失效。
     */
    void changePassword(Long userId, String oldPassword, String newPassword) throws BusinessException;

    /**
     * 登出并使当前 token 会话失效。
     */
    void logout(String token) throws BusinessException;

    UserDTO getUserById(Long userId) throws BusinessException;

    /**
     * 批量获取用户信息
     *
     * @param userIds 用户ID集合
     * @return 用户DTO列表
     */
    List<UserDTO> getUsersByIds(Set<Long> userIds) throws BusinessException;

    UserDTO getUserByUsername(String username) throws BusinessException;

    void updateProfile(Long userId, UpdateProfileRequest request) throws BusinessException;

    /**
     * 填写邀请码获取积分
     * 
     * @param userId     当前用户ID
     * @param inviteCode 邀请人的邀请码
     */
    void applyInviteCode(Long userId, String inviteCode) throws BusinessException;

    /**
     * 根据邀请码查找用户
     */
    UserDTO getUserByInviteCode(String inviteCode) throws BusinessException;

    /**
     * 获取所有用户列表（管理端用）
     */
    java.util.List<UserDTO> listAllUsers() throws BusinessException;

    /**
     * 更新用户状态（管理端用）
     * 
     * @param userId 用户ID
     * @param status 新状态 active/disabled
     */
    void updateUserStatus(Long userId, String status) throws BusinessException;

    /**
     * 获取用户详情（含会员信息）
     */
    UserDTO getUserDetail(Long userId) throws BusinessException;

    /**
     * 获取用户token版本号（用于校验Token是否失效）
     */
    Integer getTokenVersion(Long userId) throws BusinessException;

    /**
     * 获取指定月日生日的用户ID列表
     */
    java.util.List<Long> findUsersByBirthday(int month, int day) throws BusinessException;

    /**
     * v5.0: 被邀请人首单完成时触发邀请奖励发放。
     *
     * <p>返回 true 表示「**邀请奖励资格已被本调用认领，且发券事件已可靠入队**」
     * （资格标记与 outbox 入队同事务），**不**表示券已在 mall 落库 —— 发券由 mall 侧消费者异步完成
     * （见 docs/adr/0001 C5）。已认领过、或该用户没有邀请人时返回 false。
     *
     * @param userId 被邀请人的用户ID
     */
    boolean grantInviteRewardOnFirstOrder(Long userId) throws BusinessException;
}
