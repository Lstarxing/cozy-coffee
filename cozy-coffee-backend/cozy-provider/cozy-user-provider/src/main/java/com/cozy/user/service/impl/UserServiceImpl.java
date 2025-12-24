package com.cozy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.util.JwtUtil;
import com.cozy.member.api.MemberService;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.request.LoginRequest;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;
import com.cozy.user.entity.User;
import com.cozy.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @DubboReference(check = false, timeout = 60000)
    private MemberService memberService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 参数验证
        if (request == null) {
            throw new RuntimeException("注册信息不能为空");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("账号不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername().trim());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("该账号已被注册，请换一个账号");
        }

        String memberCode = generateMemberCode();
        String inviteCode = generateInviteCode();

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMemberCode(memberCode);
        user.setInviteCode(inviteCode); // 生成用户专属邀请码
        user.setNickname(request.getNickname() != null && !request.getNickname().isEmpty()
                ? request.getNickname()
                : "COZY-" + memberCode);
        user.setAvatar("/images/default-avatar.png");

        if (isPhone(request.getUsername())) {
            user.setPhone(request.getUsername());
        } else if (isEmail(request.getUsername())) {
            user.setEmail(request.getUsername());
        }

        // 如果填写了邀请码，在插入用户前验证有效性
        User inviter = null;
        if (request.getInviterCode() != null && !request.getInviterCode().trim().isEmpty()) {
            String code = request.getInviterCode().trim().toUpperCase();

            // 验证格式
            if (code.length() != 8) {
                throw new RuntimeException("邀请码格式错误（应为8位字符）");
            }

            // 查询邀请人
            LambdaQueryWrapper<User> queryInviter = new LambdaQueryWrapper<>();
            queryInviter.eq(User::getInviteCode, code);
            inviter = userMapper.selectOne(queryInviter);

            if (inviter == null) {
                throw new RuntimeException("邀请码不存在，请核对或清空后注册");
            }
        }

        userMapper.insert(user);

        // 处理注册时填写的邀请码奖励（如果存在）
        if (inviter != null) {
            final Long newUserId = user.getId();
            final Long inviterId = inviter.getId();

            // 更新当前用户的邀请人信息
            user.setInvitedBy(inviterId);
            user.setInvitedAt(java.time.LocalDateTime.now());
            userMapper.updateById(user);

            // 异步发放积分奖励
            CompletableFuture.runAsync(() -> {
                try {
                    // 邀请人获得奖励
                    memberService.addPoints(inviterId, INVITER_REWARD_POINTS,
                            "invite", "邀请好友注册奖励");

                    // 被邀请人获得奖励
                    memberService.addPoints(newUserId, INVITEE_REWARD_POINTS,
                            "invited", "填写好友邀请码奖励");

                    log.info("注册邀请奖励发放成功: inviter={}, invitee={}", inviterId, newUserId);
                } catch (Exception e) {
                    log.error("注册邀请奖励发放失败: {}", e.getMessage());
                }
            });
        }
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());

        // 异步创建会员信息，不阻塞注册流程
        final Long userId = user.getId();
        CompletableFuture.runAsync(() -> {
            try {
                memberService.createMember(userId);
                log.info("会员信息创建成功: userId={}", userId);
            } catch (Exception e) {
                log.error("创建会员信息失败: userId={}, error={}", userId, e.getMessage());
                // 会员信息创建失败不影响用户注册，可以后续补偿
            }
        });
    }

    @Override
    public String login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new RuntimeException("账号或密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername().trim());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("账号不存在");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        log.info("用户登录成功: userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        return JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    public UserDTO getUserById(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return toDTO(user);
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username.trim());
        User user = userMapper.selectOne(wrapper);
        return user != null ? toDTO(user) : null;
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (request == null) {
            throw new RuntimeException("更新信息不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        boolean hasUpdate = false;
        boolean isFirstPhone = false;
        boolean isFirstEmail = false;

        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            user.setNickname(request.getNickname().trim());
            hasUpdate = true;
        }
        if (request.getPhone() != null) {
            if (!request.getPhone().isEmpty() && !isPhone(request.getPhone())) {
                throw new RuntimeException("手机号格式不正确");
            }
            // 检查是否首次填写手机号
            if ((user.getPhone() == null || user.getPhone().isEmpty()) && !request.getPhone().isEmpty()) {
                isFirstPhone = true;
            }
            user.setPhone(request.getPhone());
            hasUpdate = true;
        }
        if (request.getEmail() != null) {
            if (!request.getEmail().isEmpty() && !isEmail(request.getEmail())) {
                throw new RuntimeException("邮箱格式不正确");
            }
            // 检查是否首次填写邮箱
            if ((user.getEmail() == null || user.getEmail().isEmpty()) && !request.getEmail().isEmpty()) {
                isFirstEmail = true;
            }
            user.setEmail(request.getEmail());
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new RuntimeException("没有需要更新的内容");
        }

        userMapper.updateById(user);
        log.info("用户资料更新成功: userId={}", userId);

        // 检查是否首次完成手机号+邮箱的完善（只有两者都填写才奖励50积分）
        // 更新后检查：如果之前手机号或邮箱任一为空，现在两者都有了，则发放奖励
        boolean profileNowComplete = user.getPhone() != null && !user.getPhone().isEmpty()
                && user.getEmail() != null && !user.getEmail().isEmpty();
        boolean shouldReward = (isFirstPhone || isFirstEmail) && profileNowComplete;

        if (shouldReward) {
            CompletableFuture.runAsync(() -> {
                try {
                    memberService.addPoints(userId, 50, "profile", "完善个人资料（手机号+邮箱）奖励");
                    log.info("完善资料奖励积分: userId={}", userId);
                } catch (Exception e) {
                    log.error("完善资料奖励积分失败: userId={}, error={}", userId, e.getMessage());
                }
            });
        }
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setMemberCode(user.getMemberCode());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setInviteCode(user.getInviteCode()); // 用户的邀请码（用于分享）
        dto.setHasAppliedInviteCode(user.getInvitedBy() != null); // 是否已填写过邀请码
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    private String generateMemberCode() {
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++)
            sb.append(random.nextInt(10));
        return sb.toString();
    }

    /**
     * 生成8位字母数字混合邀请码（易读，排除容易混淆的字符）
     */
    private String generateInviteCode() {
        // 排除 0, O, 1, I, L 等容易混淆的字符
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private boolean isPhone(String str) {
        return str != null && str.matches("^1[3-9]\\d{9}$");
    }

    private boolean isEmail(String str) {
        return str != null && str.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$");
    }

    // ========== 邀请码功能实现 ==========

    // 邀请积分配置
    private static final int INVITER_REWARD_POINTS = 150; // 邀请人奖励
    private static final int INVITEE_REWARD_POINTS = 80; // 被邀请人奖励

    @Override
    @Transactional
    public void applyInviteCode(Long userId, String inviteCode) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new RuntimeException("邀请码不能为空");
        }

        processInviteReward(userId, inviteCode.trim().toUpperCase());
    }

    /**
     * 处理邀请奖励（注册时和登录后填写都调用此方法）
     */
    private void processInviteReward(Long userId, String inviteCode) {
        // 1. 查询当前用户
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 检查是否已填写过邀请码
        if (currentUser.getInvitedBy() != null) {
            throw new RuntimeException("您已填写过邀请码，不可重复填写");
        }

        // 3. 检查是否填写自己的邀请码
        if (inviteCode.equals(currentUser.getInviteCode())) {
            throw new RuntimeException("不能填写自己的邀请码");
        }

        // 4. 查找邀请人
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getInviteCode, inviteCode);
        User inviter = userMapper.selectOne(wrapper);

        if (inviter == null) {
            throw new RuntimeException("邀请码无效，请检查后重新输入");
        }

        // 5. 更新当前用户的邀请人信息
        currentUser.setInvitedBy(inviter.getId());
        currentUser.setInvitedAt(java.time.LocalDateTime.now());
        userMapper.updateById(currentUser);

        log.info("用户 {} 填写邀请码成功，邀请人: {}", userId, inviter.getId());

        // 6. 发放邀请奖励积分（异步，避免影响主流程）
        CompletableFuture.runAsync(() -> {
            try {
                // 邀请人获得奖励
                memberService.addPoints(inviter.getId(), INVITER_REWARD_POINTS,
                        "invite", "邀请好友加入奖励");
                log.info("邀请人 {} 获得 {} 积分奖励", inviter.getId(), INVITER_REWARD_POINTS);

                // 被邀请人获得奖励
                memberService.addPoints(userId, INVITEE_REWARD_POINTS,
                        "invited", "填写有效邀请码奖励");
                log.info("被邀请人 {} 获得 {} 积分奖励", userId, INVITEE_REWARD_POINTS);
            } catch (Exception e) {
                log.error("发放邀请积分失败: {}", e.getMessage());
            }
        });
    }

    @Override
    public UserDTO getUserByInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getInviteCode, inviteCode.trim().toUpperCase());
        User user = userMapper.selectOne(wrapper);
        return user != null ? toDTO(user) : null;
    }

    @Override
    public java.util.List<UserDTO> listAllUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreatedAt);
        java.util.List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(user -> {
            UserDTO dto = toDTO(user);
            // 获取会员信息
            try {
                com.cozy.member.dto.response.MemberDTO memberInfo = memberService.getMemberByUserId(user.getId());
                if (memberInfo != null) {
                    dto.setMemberLevel(memberInfo.getMemberLevel());
                    dto.setCurrentPoints(memberInfo.getCurrentPoints());
                    dto.setTotalPoints(memberInfo.getTotalPoints());
                }
            } catch (Exception e) {
                // 如果获取会员信息失败，使用默认值
                dto.setMemberLevel("basic");
                dto.setCurrentPoints(0);
                dto.setTotalPoints(0);
            }
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }
}
