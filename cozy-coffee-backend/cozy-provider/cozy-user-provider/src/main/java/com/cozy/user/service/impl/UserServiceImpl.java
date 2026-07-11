package com.cozy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.common.exception.BusinessException;
import com.cozy.common.util.JwtUtil;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.PointsMallService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.request.LoginRequest;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;
import com.cozy.user.entity.User;
import com.cozy.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @DubboReference(check = false, timeout = 60000)
    private MemberService memberService;

    // v5.0: 用于发放邀请奖励券
    @DubboReference(check = false, timeout = 60000)
    private PointsMallService pointsMallService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 参数验证
        if (request == null) {
            throw new BusinessException("注册信息不能为空");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("账号不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }

        // v4.2: 校验username格式必须是手机号或邮箱
        String username = request.getUsername().trim();
        if (!isPhone(username) && !isEmail(username)) {
            throw new BusinessException("账号格式不正确,请使用手机号或邮箱注册");
        }

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该账号已被注册，请换一个账号");
        }

        String memberCode = generateMemberCode();
        String inviteCode = generateInviteCode();

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMemberCode(memberCode);
        user.setInviteCode(inviteCode); // 生成用户专属邀请码
        user.setNickname(request.getNickname() != null && !request.getNickname().isEmpty()
                ? request.getNickname()
                : "COZY-" + memberCode);
        user.setAvatar("/images/default-avatar.png");

        if (isPhone(username)) {
            user.setPhone(username);
        } else if (isEmail(username)) {
            user.setEmail(username);
        }

        // 如果填写了邀请码，在插入用户前验证有效性
        User inviter = null;
        if (request.getInviterCode() != null && !request.getInviterCode().trim().isEmpty()) {
            String code = request.getInviterCode().trim().toUpperCase();

            // 验证格式
            if (code.length() != 8) {
                throw new BusinessException("邀请码格式错误（应为8位字符）");
            }

            // 查询邀请人
            LambdaQueryWrapper<User> queryInviter = new LambdaQueryWrapper<>();
            queryInviter.eq(User::getInviteCode, code);
            inviter = userMapper.selectOne(queryInviter);

            if (inviter == null) {
                throw new BusinessException("邀请码不存在，请核对或清空后注册");
            }
        }

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            String msg = e.getMessage();
            if (msg.contains("uk_phone")) {
                throw new BusinessException("该手机号已被其他账号绑定");
            } else if (msg.contains("uk_email")) {
                throw new BusinessException("该邮箱已被其他账号绑定");
            } else {
                throw new BusinessException("账号信息已存在");
            }
        }

        // v5.0: 处理注册时填写的邀请码（仅记录关系，不立即发放奖励）
        if (inviter != null) {
            final Long newUserId = user.getId();
            final Long inviterId = inviter.getId();

            // 更新当前用户的邀请人信息
            user.setInvitedBy(inviterId);
            user.setInvitedAt(LocalDateTime.now());
            user.setInviteRewardGranted(false); // 标记奖励待首单完成后发放
            userMapper.updateById(user);

            log.info("注册邀请关系绑定成功: inviter={}, invitee={}。奖励将在被邀请人首单完成时发放。", inviterId, newUserId);
        }
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());

        // 异步创建会员信息及发放新用户福利，不阻塞注册流程
        final Long userId = user.getId();
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 创建会员基础信息
                memberService.createMember(userId);
                log.info("会员信息创建成功: userId={}", userId);

                // 2. 发放新用户礼包：首单五折券（有效期7天，饮品专用）
                if (pointsMallService != null) {
                    pointsMallService.issueNewUserCoupon(userId);
                }
            } catch (Exception e) {
                log.error("执行注册后续逻辑失败: userId={}, error={}", userId, e.getMessage());
            }
        });
    }

    @Override
    public String login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new BusinessException("账号或密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername().trim());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("账号不存在");
        }

        // 检查用户状态
        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());
        try {
            // 强制单会话：先按 userId 清理历史会话键（兼容旧版本遗留键）
            clearUserSessions(user.getId());

            String userTokenKey = RedisKeyConstants.userCurrentTokenById(user.getId());
            String oldToken = stringRedisTemplate.opsForValue().get(userTokenKey);
            if (oldToken != null && !oldToken.isBlank()) {
                stringRedisTemplate.delete(RedisKeyConstants.userLoginSession(oldToken));
            }
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.userLoginSession(token),
                    String.valueOf(user.getId()),
                    JwtUtil.getExpirationTimeMillis(),
                    TimeUnit.MILLISECONDS);
            stringRedisTemplate.opsForValue().set(
                    userTokenKey,
                    token,
                    JwtUtil.getExpirationTimeMillis(),
                    TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("写入Redis登录会话失败: userId={}", user.getId(), e);
        }

        log.info("用户登录成功: userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        return token;
    }

    private void clearUserSessions(Long userId) {
        String targetUserId = String.valueOf(userId);
        ScanOptions options = ScanOptions.scanOptions().match("cozy:auth:session:*").count(500).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                try {
                    String cachedUserId = stringRedisTemplate.opsForValue().get(key);
                    if (targetUserId.equals(cachedUserId)) {
                        stringRedisTemplate.delete(key);
                    }
                } catch (Exception e) {
                    log.warn("清理历史登录会话失败: key={}", key, e);
                }
            }
        } catch (Exception e) {
            log.warn("扫描历史登录会话失败: userId={}", userId, e);
        }
    }

    @Override
    public void logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        try {
            String trimmedToken = token.trim();
            String sessionKey = RedisKeyConstants.userLoginSession(trimmedToken);
            Long userIdFromToken = null;
            try {
                userIdFromToken = JwtUtil.getUserIdFromToken(trimmedToken);
            } catch (Exception ignore) {
                // Token 解析失败时，继续走会话键删除兜底
            }
            String userId = stringRedisTemplate.opsForValue().get(sessionKey);
            stringRedisTemplate.delete(sessionKey);
            if (userId != null && !userId.isBlank()) {
                String userTokenKey = RedisKeyConstants.userCurrentTokenById(Long.parseLong(userId));
                String mappedToken = stringRedisTemplate.opsForValue().get(userTokenKey);
                if (trimmedToken.equals(mappedToken)) {
                    stringRedisTemplate.delete(userTokenKey);
                }
            }
            if (userIdFromToken != null) {
                clearUserSessions(userIdFromToken);
                stringRedisTemplate.delete(RedisKeyConstants.userCurrentTokenById(userIdFromToken));
            }
        } catch (Exception e) {
            log.warn("删除Redis登录会话失败", e);
        }
    }

    @Override
    public UserDTO getUserById(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        String cacheKey = RedisKeyConstants.userProfileById(userId);
        try {
            Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
            if (cachedObj != null) {
                if (cachedObj instanceof UserDTO) {
                    return (UserDTO) cachedObj;
                }
                if (cachedObj instanceof Map) {
                    return objectMapper.convertValue(cachedObj, UserDTO.class);
                }
                if (cachedObj instanceof String) {
                    return objectMapper.readValue((String) cachedObj, UserDTO.class);
                }
            }
        } catch (Exception e) {
            log.warn("读取Redis用户资料缓存失败: userId={}", userId, e);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            log.debug("getUserById未找到用户: userId={}", userId);
            return null;
        }
        UserDTO dto = toDTO(user);

        try {
            redisTemplate.opsForValue().set(cacheKey, dto, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入Redis用户资料缓存失败: userId={}", userId, e);
        }
        return dto;
    }

    @Override
    public List<UserDTO> getUsersByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getId, userIds);
        List<User> users = userMapper.selectList(wrapper);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return users.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username.trim());
        User user = userMapper.selectOne(wrapper);
        return user != null ? toDTO(user) : null;
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        if (request == null) {
            throw new BusinessException("更新信息不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        boolean hasUpdate = false;
        boolean isFirstPhone = false;
        boolean isFirstEmail = false;

        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            user.setNickname(request.getNickname().trim());
            hasUpdate = true;
        }
        if (request.getPhone() != null) {
            String newPhone = request.getPhone().trim();
            if (!newPhone.isEmpty()) {
                if (!isPhone(newPhone)) {
                    throw new BusinessException("手机号格式不正确");
                }
                // v4.2: 检查手机号唯一性（排除当前用户）
                if (!newPhone.equals(user.getPhone())) {
                    LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
                    phoneWrapper.eq(User::getPhone, newPhone)
                            .ne(User::getId, userId);
                    if (userMapper.selectCount(phoneWrapper) > 0) {
                        throw new BusinessException("该手机号已被其他账号绑定");
                    }
                }
            }
            // 检查是否首次填写手机号
            if ((user.getPhone() == null || user.getPhone().isEmpty()) && !newPhone.isEmpty()) {
                isFirstPhone = true;
            }
            user.setPhone(newPhone);
            hasUpdate = true;
        }
        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.isEmpty()) {
                if (!isEmail(newEmail)) {
                    throw new BusinessException("邮箱格式不正确");
                }
                // v4.2: 检查邮箱唯一性（排除当前用户）
                if (!newEmail.equals(user.getEmail())) {
                    LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
                    emailWrapper.eq(User::getEmail, newEmail)
                            .ne(User::getId, userId);
                    if (userMapper.selectCount(emailWrapper) > 0) {
                        throw new BusinessException("该邮箱已被其他账号绑定");
                    }
                }
            }
            // 检查是否首次填写邮箱
            if ((user.getEmail() == null || user.getEmail().isEmpty()) && !newEmail.isEmpty()) {
                isFirstEmail = true;
            }
            user.setEmail(newEmail);
            hasUpdate = true;
        }

        // v4.2 生日设置逻辑
        if (request.getBirthday() != null) {
            LocalDateTime now = LocalDateTime.now();

            // 检查是否允许修改
            if (user.getBirthdaySetAt() != null) {
                if (user.getNextBirthdayResetAt() != null && now.isBefore(user.getNextBirthdayResetAt())) {
                    throw new BusinessException("生日每年只能修改一次，下次可修改时间: " +
                            user.getNextBirthdayResetAt().toLocalDate());
                }
            }

            // 如果原来的生日是空的，不算修改，而是初始化
            if (user.getBirthday() == null) {
                // 首次设置
            }

            user.setBirthday(LocalDate.parse(request.getBirthday()));
            user.setBirthdaySetAt(now);
            user.setNextBirthdayResetAt(now.plusYears(1));
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new BusinessException("没有需要更新的内容");
        }

        try {
            userMapper.updateById(user);
            stringRedisTemplate.delete(RedisKeyConstants.userProfileById(userId));
        } catch (DuplicateKeyException e) {
            String msg = e.getMessage();
            if (msg.contains("uk_phone")) {
                throw new BusinessException("该手机号已被其他账号绑定");
            } else if (msg.contains("uk_email")) {
                throw new BusinessException("该邮箱已被其他账号绑定");
            } else {
                throw new BusinessException("更新失败，信息可能重复");
            }
        }
        log.info("用户资料更新成功: userId={}", userId);

        // 检查是否首次完成手机号+邮箱的完善（只有两者都填写才奖励50积分）
        // 更新后检查：如果之前手机号或邮箱任一为空，现在两者都有了，则发放奖励
        boolean profileNowComplete = user.getPhone() != null && !user.getPhone().isEmpty()
                && user.getEmail() != null && !user.getEmail().isEmpty();
        boolean shouldReward = (isFirstPhone || isFirstEmail) && profileNowComplete;

        if (shouldReward) {
            CompletableFuture.runAsync(() -> {
                try {
                    memberService.addPoints(userId, 20, "profile", "完善个人资料（手机号+邮箱）奖励");
                    log.info("完善资料奖励积分: userId={}", userId);
                } catch (Exception e) {
                    log.error("完善资料奖励积分失败: userId={}, error={}", userId, e.getMessage());
                }
            });
        }

        // v4.2: 设置生日后立即发放生日权益包
        if (request.getBirthday() != null) {
            final Long uid = userId;
            CompletableFuture.runAsync(() -> {
                try {
                    boolean granted = memberService.grantBirthdayReward(uid);
                    if (granted) {
                        log.info("生日权益包发放成功: userId={}", uid);
                    } else {
                        log.info("生日权益包已领取过: userId={}", uid);
                    }
                } catch (Exception e) {
                    log.error("生日权益包发放失败: userId={}, error={}", uid, e.getMessage());
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
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        // v4.2 生日权益
        dto.setBirthday(user.getBirthday() != null ? user.getBirthday().toString() : null);
        dto.setBirthdaySetAt(user.getBirthdaySetAt());
        return dto;
    }

    private String generateMemberCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = String.format("%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100000000));
            LambdaQueryWrapper<User> check = new LambdaQueryWrapper<>();
            check.eq(User::getMemberCode, code);
            if (userMapper.selectCount(check) == 0) {
                return code;
            }
            log.warn("Member code collision, retrying: attempt={}, code={}", attempt + 1, code);
        }
        throw new RuntimeException("生成会员码失败：多次碰撞，请重试");
    }

    /**
     * 生成8位字母数字混合邀请码（易读，排除容易混淆的字符）
     */
    private String generateInviteCode() {
        // 排除 0, O, 1, I, L 等容易混淆的字符
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        Random random = new Random();
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

    @Override
    @Transactional
    public void applyInviteCode(Long userId, String inviteCode) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new BusinessException("邀请码不能为空");
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
            throw new BusinessException("用户不存在");
        }

        // 2. 检查是否已填写过邀请码
        if (currentUser.getInvitedBy() != null) {
            throw new BusinessException("您已填写过邀请码，不可重复填写");
        }

        // 3. 检查是否填写自己的邀请码
        if (inviteCode.equals(currentUser.getInviteCode())) {
            throw new BusinessException("不能填写自己的邀请码");
        }

        // 4. 查找邀请人
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getInviteCode, inviteCode);
        User inviter = userMapper.selectOne(wrapper);

        if (inviter == null) {
            throw new BusinessException("邀请码无效，请检查后重新输入");
        }

        // 5. 更新当前用户的邀请人信息
        currentUser.setInvitedBy(inviter.getId());
        currentUser.setInvitedAt(LocalDateTime.now());
        currentUser.setInviteRewardGranted(false); // v5.0: 标记奖励未发放，等待首单触发
        userMapper.updateById(currentUser);

        log.info("用户 {} 填写邀请码成功，邀请人: {}。奖励将在被邀请人首单完成时发放。", userId, inviter.getId());

        // v5.0: 不再立即发放奖励，改为在被邀请人首单完成时发放
        // 详见 OrderServiceImpl.completeOrder() 中的 grantInviteRewardOnFirstOrder() 调用
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
    public List<UserDTO> listAllUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreatedAt);
        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(user -> {
            UserDTO dto = toDTO(user);
            // 获取会员信息
            try {
                MemberDTO memberInfo = memberService.getMemberByUserId(user.getId());
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

    @Override
    @Transactional
    public void updateUserStatus(Long userId, String status) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (!"active".equals(status) && !"disabled".equals(status)) {
            throw new BusinessException("无效的用户状态");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String oldStatus = user.getStatus();
        user.setStatus(status);

        // 如果是禁用操作，递增tokenVersion使所有Token失效
        if ("disabled".equals(status)) {
            user.setTokenVersion(user.getTokenVersion() + 1);
            log.info("用户 {} 被禁用，tokenVersion递增到 {}", userId, user.getTokenVersion());
        }

        userMapper.updateById(user);
        log.info("用户状态更新: userId={}, {} -> {}", userId, oldStatus, status);
    }

    @Override
    public UserDTO getUserDetail(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserDTO dto = toDTO(user);

        // 获取会员信息
        try {
            MemberDTO memberInfo = memberService.getMemberByUserId(userId);
            if (memberInfo != null) {
                dto.setMemberLevel(memberInfo.getMemberLevel());
                dto.setCurrentPoints(memberInfo.getCurrentPoints());
                dto.setTotalPoints(memberInfo.getTotalPoints());
            }
        } catch (Exception e) {
            dto.setMemberLevel("basic");
            dto.setCurrentPoints(0);
            dto.setTotalPoints(0);
        }

        return dto;
    }

    @Override
    public Integer getTokenVersion(Long userId) {
        if (userId == null) {
            return 0;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return 0;
        }
        return user.getTokenVersion() != null ? user.getTokenVersion() : 0;
    }

    @Override
    public List<Long> findUsersByBirthday(int month, int day) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 使用MySQL函数提取月日
        wrapper.apply("MONTH(birthday) = {0} AND DAY(birthday) = {1}", month, day);
        wrapper.select(User::getId);
        return userMapper.selectObjs(wrapper).stream()
                .map(obj -> (Long) obj)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public boolean grantInviteRewardOnFirstOrder(Long userId) {
        if (userId == null) {
            return false;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        // 检查是否有邀请人
        if (user.getInvitedBy() == null) {
            log.debug("用户 {} 没有邀请人，跳过首单奖励", userId);
            return false;
        }

        // 检查奖励是否已发放
        if (Boolean.TRUE.equals(user.getInviteRewardGranted())) {
            log.debug("用户 {} 的邀请奖励已发放过，跳过", userId);
            return false;
        }

        Long inviterId = user.getInvitedBy();
        log.info("用户 {} 完成首单，准备为邀请人 {} 发放买一送一券", userId, inviterId);

        try {
            // 发放买一送一券给邀请人
            if (pointsMallService != null) {
                String inviteKey = "invite_firstorder_" + userId + "_" + inviterId;
                pointsMallService.issueCouponToUser(inviterId, "BOGO",
                        inviteKey, 0, 40, 30); // 买一送一券，最高抵扣40元，有效期30天
                log.info("邀请人 {} 获得买一送一券奖励（被邀请人 {} 首单）", inviterId, userId);
            } else {
                log.error("券服务不可用，无法为邀请人 {} 发放首单奖励", inviterId);
                return false;
            }

            // 标记奖励已发放
            user.setInviteRewardGranted(true);
            userMapper.updateById(user);

            return true;
        } catch (Exception e) {
            log.error("发放首单邀请奖励失败: userId={}, inviterId={}, error={}",
                    userId, inviterId, e.getMessage());
            return false;
        }
    }

}
