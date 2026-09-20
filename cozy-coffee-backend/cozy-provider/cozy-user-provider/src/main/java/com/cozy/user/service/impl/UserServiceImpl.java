package com.cozy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.common.exception.BusinessException;
import com.cozy.common.mq.BirthdaySetEvent;
import com.cozy.common.mq.InviteRewardEarnedEvent;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.ProfileCompletedEvent;
import com.cozy.common.mq.UserEventKeys;
import com.cozy.common.mq.WelcomeGiftEligibleEvent;
import com.cozy.common.tx.AfterCommit;
import com.cozy.common.util.JwtUtil;
import com.cozy.member.api.MemberService;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.request.LoginRequest;
import com.cozy.user.dto.request.RegisterRequest;
import com.cozy.user.dto.request.UpdateProfileRequest;
import com.cozy.user.dto.response.UserDTO;
import com.cozy.user.entity.User;
import com.cozy.user.mapper.UserMapper;
import com.cozy.user.mq.UserEventOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 用户生命周期事实事件的本地 outbox（见 docs/adr/0001 §3）
    private final UserEventOutboxService userEventOutboxService;

    @DubboReference(check = false, timeout = 60000)
    private MemberService memberService;

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

        // 新人礼改为事件驱动（ADR 0001 §8 第 6 步）：在【注册事务内】落 outbox 行，与 users 行同生共死，
        // 提交后由 relay 投递，mall 侧的 WelcomeGiftEligibleConsumer 复用 issueNewUserCoupon 发券。
        // 换成 outbox 解决的是原写法（提交后异步 RPC）的两处缺口：投递失败没有任何记录、券永久丢失；
        // 以及"券已经发出去、注册事务却回滚了"只能靠 AfterCommit 事后兜。
        // 幂等键沿用既有业务键 NEW_USER_COUPON_{userId}（ADR C2），与券表的 coupon_code 同一把锁。
        final Long userId = user.getId();
        userEventOutboxService.publish(MqTags.WELCOME_GIFT_ELIGIBLE, userId,
                WelcomeGiftEligibleEvent.builder()
                        .userId(userId)
                        .uniqueKey(UserEventKeys.welcomeGift(userId))
                        .occurredAt(LocalDateTime.now())
                        .build());

        // 会员信息仍走同步 RPC（USER_CREATED 的切换排在 §8 第 6 步最后一项）。
        // 同样必须等本事务提交后再派发：runAsync 会立即启动，若注册事务随后回滚（如唯一键冲突），
        // 会员已经建出且无法撤销。会员侧另有自愈（getMemberByUserId 查不到会补建）。
        AfterCommit.run(() -> CompletableFuture.runAsync(() -> {
            try {
                memberService.createMember(userId);
                log.info("会员信息创建成功: userId={}", userId);
            } catch (Exception e) {
                log.error("执行注册后续逻辑失败: userId={}, error={}", userId, e.getMessage());
            }
        }));
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

        return issueToken(user);
    }

    @Override
    @Transactional
    public String loginWechatDev(String deviceId) {
        if (deviceId == null || !deviceId.matches("[A-Za-z0-9_-]{8,64}")) {
            throw new BusinessException("开发设备标识无效");
        }
        String username = "wxdev_" + deviceId;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(deviceId + "-cozy-dev"));
            user.setMemberCode(generateMemberCode());
            user.setInviteCode(generateInviteCode());
            user.setNickname("微信开发用户");
            user.setAvatar("/images/default-avatar.png");
            userMapper.insert(user);
            try {
                memberService.createMember(user.getId());
            } catch (Exception e) {
                log.warn("创建微信开发用户会员信息失败: userId={}", user.getId(), e);
            }
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        return issueToken(user);
    }

    @Override
    @Transactional
    public String loginWechat(String openid) {
        if (openid == null || openid.isBlank()) {
            throw new BusinessException("微信登录 openid 无效");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getOpenid, openid);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            user = new User();
            user.setUsername("wx_" + openid);
            user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            user.setMemberCode(generateMemberCode());
            user.setInviteCode(generateInviteCode());
            user.setNickname("微信用户");
            user.setAvatar("/images/default-avatar.png");
            user.setOpenid(openid);
            userMapper.insert(user);
            try {
                memberService.createMember(user.getId());
            } catch (Exception e) {
                log.warn("创建微信用户会员信息失败: userId={}", user.getId(), e);
            }
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        return issueToken(user);
    }

    @Override
    @Transactional
    public void resetPasswordDev(String username, String newPassword) {
        if (username == null || username.isBlank() || newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("账号或新密码格式不正确");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username.trim());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
        // 重置密码的语义就是"把旧会话全部踢掉"：必须成功，否则会出现"报错但密码已变"。
        // 所以配 @Transactional —— 撤销失败时密码更新一起回滚。
        revokeAllSessions(user.getId());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new BusinessException("原密码不能为空");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException("新密码长度需在 6-20 位之间");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
        // 改密后旧会话必须失效（含其他设备）：强制撤销，失败让异常逃出 → 密码更新随事务回滚
        revokeAllSessions(user.getId());
    }

    private String issueToken(User user) {
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

    /**
     * 强制撤销某用户的全部登录会话（禁用等"必须成功"的场景）。
     *
     * <p>与 {@link #clearUserSessions} 的两处不同都是刻意的：
     * ① 连 {@code cozy:auth:user:token:{id}} 一起删 —— 那个指针只在下次登录时被 {@code issueToken} 覆盖，
     * 否则会一直留着；
     * ② <b>不吞异常</b> —— 失败必须抛出去让调用方的事务回滚，不能"接口报禁用成功、会话其实还在"。
     * 所以它不复用上面那个"逐键 try/catch + 整体 try/catch"的尽力而为版本。
     *
     * <p>鉴权侧只看 Redis 里 session 键是否存在（不比 tokenVersion、也不查用户状态），
     * 删掉这些键就等于立刻踢下线。
     */
    private void revokeAllSessions(Long userId) {
        stringRedisTemplate.delete(RedisKeyConstants.userCurrentTokenById(userId));
        String targetUserId = String.valueOf(userId);
        ScanOptions options = ScanOptions.scanOptions().match("cozy:auth:session:*").count(500).build();
        // 故意不写 catch：这里只可能抛 RuntimeException（如 RedisConnectionFailureException），
        // 让它一路逃出 updateUserStatus 的 @Transactional，状态变更随之回滚
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (targetUserId.equals(stringRedisTemplate.opsForValue().get(key))) {
                    stringRedisTemplate.delete(key);
                }
            }
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
        // 单行资料直读 DB，不做 Redis 缓存（per-user 廉价查询 + 频繁变更，缓存收益低且易引入 stale）
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.debug("getUserById未找到用户: userId={}", userId);
            return null;
        }
        return toDTO(user);
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
    @Transactional
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
        if (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) {
            String avatar = request.getAvatar().trim();
            if (!avatar.startsWith("/uploads/")
                    && !avatar.startsWith("/images/")
                    && !avatar.startsWith("http://")
                    && !avatar.startsWith("https://")) {
                throw new BusinessException("头像地址格式不正确");
            }
            user.setAvatar(avatar);
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
            // 完善资料奖励改由事件驱动（ADR 0001 §8 第 6 步）：事务内落 outbox，与资料行同生共死，
            // 提交后由 relay 投递，member 侧的 ProfileCompletedConsumer 调 addPointsWithLot 发放。
            // 事件**不携带积分数/文案** —— 奖励规则归消费端持有（member 侧的 cozy.user.profile），
            // 所以 user 侧不再需要 ProfileRewardConfig（已随之删除）。
            // 键映射见 C7：事件键 profile_completed_{userId} ↔ 落库 source_type=profile + source_id={userId}。
            userEventOutboxService.publish(MqTags.PROFILE_COMPLETED, userId,
                    ProfileCompletedEvent.builder()
                            .userId(userId)
                            .uniqueKey(UserEventKeys.profileCompleted(userId))
                            .occurredAt(LocalDateTime.now())
                            .build());
            log.info("完善资料奖励事件已入队: userId={}", userId);
        }

        // 生日权益改由事件驱动（ADR 0001 §8 第 6 步）：在【本事务内】落 outbox，与资料行同生共死，
        // 提交后由 relay 投递，member 侧的 BirthdaySetConsumer 调 grantBirthdayReward(userId, benefitYear) 发放。
        // benefitYear 必须由【生产者】盖章（C8）：消费者若用 now() 重算，跨年重投会算出下一年度的键，
        // 同一笔权益会跨年各发一次。键沿用既有业务键 birthday_{userId}_{year}（C2）。
        if (request.getBirthday() != null) {
            int benefitYear = LocalDate.now().getYear();
            userEventOutboxService.publish(MqTags.BIRTHDAY_SET, userId,
                    BirthdaySetEvent.builder()
                            .userId(userId)
                            .benefitYear(benefitYear)
                            .uniqueKey(UserEventKeys.birthday(userId, benefitYear))
                            .occurredAt(LocalDateTime.now())
                            .build());
            log.info("生日权益事件已入队: userId={}, benefitYear={}", userId, benefitYear);
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
        // 实际调用方是 member-provider 的 FirstOrderConsumer（消费 ORDER_COMPLETED 后回调本方法）
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
        // 只返回 user 域字段。会员等级/积分由网关 AdminUserProfileCoordinator 一次批量补齐——
        // 原先在这里逐用户反查 member 是 N+1，且 member 不可用时静默降级成 basic/0/0 的假数据（无日志）。
        return userMapper.selectList(wrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
            // null 按 0 处理：本列是 NOT NULL DEFAULT 0，但实体可能被手工构造，
            // 原先的 getTokenVersion() + 1 会 NPE
            int nextVersion = (user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1;
            user.setTokenVersion(nextVersion);
            log.info("用户 {} 被禁用，tokenVersion递增到 {}", userId, nextVersion);
        }

        userMapper.updateById(user);
        log.info("用户状态更新: userId={}, {} -> {}", userId, oldStatus, status);

        if ("disabled".equals(status)) {
            // 鉴权只认 Redis 里的 session 键（JwtAuthInterceptor 不比 tokenVersion、也不查用户状态），
            // 所以光改状态是不够的 —— 必须同时掐掉会话，否则用户手上的 token 在会话 TTL 内仍然可用。
            // 放在 updateById 之后：撤销失败时异常逃出方法 → 状态变更随事务回滚（Redis 没有回滚，
            // 可能出现"状态没变但会话已被踢掉"，这个方向可接受：宁可多踢一次，也不能反过来）。
            revokeAllSessions(userId);
            log.info("用户 {} 的登录会话已全部撤销（禁用）", userId);
        }
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

        // 会员等级/积分由网关 AdminUserProfileCoordinator 组合填充，user 域不再反查 member。
        return toDTO(user);
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

        Long inviterId = user.getInvitedBy();
        if (inviterId == null) {
            log.debug("用户 {} 没有邀请人，跳过首单奖励", userId);
            return false;
        }

        // 条件更新认领资格：并发/重投下只有一个调用能把 0 改成 1（docs/adr/0001 C4）。
        // 刻意不用上面 selectById 的 inviteRewardGranted 判断 —— 那种"先查后写"有 TOCTOU 窗口。
        if (userMapper.claimInviteReward(userId) == 0) {
            log.debug("用户 {} 的邀请奖励资格已被认领过，跳过", userId);
            return false;
        }

        // 与上面那次条件更新同属一个事务：入队失败会一并回滚，资格不会被"白白认领"。
        // 幂等键与 mall 消费者校验的完全一致（UserEventKeys 是唯一来源）；
        // 事件不带券的面额/门槛/有效期 —— 那些由 mall 的消费者自己读配置。
        String uniqueKey = UserEventKeys.inviteReward(userId, inviterId);
        userEventOutboxService.publish(MqTags.INVITE_REWARD_EARNED, userId,
                InviteRewardEarnedEvent.builder()
                        .inviteeUserId(userId)
                        .inviterId(inviterId)
                        .uniqueKey(uniqueKey)
                        .occurredAt(LocalDateTime.now())
                        .build());

        log.info("被邀请人 {} 首单完成，邀请券已可靠入队: inviterId={}, uniqueKey={}",
                userId, inviterId, uniqueKey);
        return true;
    }

}
