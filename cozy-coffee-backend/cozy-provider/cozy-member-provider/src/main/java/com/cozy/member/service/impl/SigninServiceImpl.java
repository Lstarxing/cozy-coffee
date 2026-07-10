package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.constant.RedisKeyConstants;
import com.cozy.member.api.PointsMallService;
import com.cozy.member.api.SigninService;
import com.cozy.member.dto.response.SigninResultDTO;
import com.cozy.member.entity.MemberInfo;
import com.cozy.member.entity.PointsLot;
import com.cozy.member.entity.SigninRecord;
import com.cozy.member.entity.PointsTransaction;
import com.cozy.member.mapper.MemberInfoMapper;
import com.cozy.member.mapper.PointsLotMapper;
import com.cozy.member.mapper.SigninRecordMapper;
import com.cozy.member.mapper.PointsTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 签到服务实现
 * v4.0: 月封顶 800 积分 + 积分批次
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class SigninServiceImpl implements SigninService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    private final MemberInfoMapper memberInfoMapper;
    private final SigninRecordMapper signinRecordMapper;
    private final PointsTransactionMapper transactionMapper;
    private final PointsLotMapper pointsLotMapper;
    private final StringRedisTemplate stringRedisTemplate;

    // v5.0: 通过 RPC 调用 PointsMallService 发放7日连签券
    @DubboReference(check = false, timeout = 2000, retries = 0)
    private PointsMallService pointsMallService;

    @Override
    @Transactional
    public SigninResultDTO signIn(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberInfo::getUserId, userId);
        MemberInfo member = memberInfoMapper.selectOne(wrapper);

        if (member == null) {
            throw new RuntimeException("会员信息不存在，请联系客服");
        }

        // 检查是否已签到
        if (member.getLastSigninDate() != null && member.getLastSigninDate().equals(today)) {
            throw new RuntimeException("今日已签到，明天再来吧");
        }

        // 计算连续签到天数
        int consecutiveDays = 1;
        if (member.getLastSigninDate() != null && member.getLastSigninDate().equals(today.minusDays(1))) {
            consecutiveDays = member.getConsecutiveSignDays() + 1;
        }

        // v5.0 白皮书: 每日固定 +2 积分
        int actualPoints = 2;

        // 更新会员信息
        member.setLastSigninDate(today);
        member.setConsecutiveSignDays(consecutiveDays);
        member.setCurrentPoints(member.getCurrentPoints() + actualPoints);
        member.setTotalPoints(member.getTotalPoints() + actualPoints);
        memberInfoMapper.updateById(member);

        // 插入签到记录
        SigninRecord record = new SigninRecord();
        record.setUserId(userId);
        record.setSigninDate(today);
        record.setPointsEarned(actualPoints);
        record.setConsecutiveDays(consecutiveDays);
        signinRecordMapper.insert(record);

        // 写入月度位图，支持签到日历/统计快速读取。
        syncSigninBitmap(userId, today);
        evictSigninMonthStatsCache(userId, today);

        // 创建积分批次（365天有效）
        PointsLot lot = new PointsLot();
        lot.setUserId(userId);
        lot.setInitialAmount(actualPoints);
        lot.setRemaining(actualPoints);
        lot.setSourceType("signin");
        lot.setSourceId(record.getId());
        lot.setExpiresAt(LocalDateTime.now().plusDays(365));
        lot.setCreatedAt(LocalDateTime.now());
        pointsLotMapper.insert(lot);

        // 记录积分流水
        PointsTransaction transaction = new PointsTransaction();
        transaction.setUserId(userId);
        transaction.setChangeAmount(actualPoints);
        transaction.setBalanceAfter(member.getCurrentPoints());
        transaction.setSourceType("signin");
        transaction.setDescription("每日签到" + (consecutiveDays > 1 ? "（连续" + consecutiveDays + "天）" : ""));
        transactionMapper.insert(transaction);

        // 构建返回结果
        SigninResultDTO result = new SigninResultDTO();
        result.setSuccess(true);

        String message = "签到成功！获得" + actualPoints + "积分";
        if (consecutiveDays == 7) {
            message += "，连续7天达成！赠送满35减10元优惠券";
        } else if (consecutiveDays > 1) {
            message += "（连续" + consecutiveDays + "天）";
        }

        result.setMessage(message);
        result.setPointsEarned(actualPoints);
        result.setConsecutiveDays(consecutiveDays);
        result.setCurrentPoints(member.getCurrentPoints());
        result.setTotalPoints(member.getTotalPoints());

        log.info("签到成功: userId={}, points={}, consecutiveDays={}", userId, actualPoints, consecutiveDays);

        // v5.0: 检查7日连签奖励 - 发放"满35减10"券
        if (consecutiveDays == 7) {
            // 奖励发券异步执行，避免下游波动拖慢签到主链路。
            CompletableFuture.runAsync(() -> grant7DayCoupon(userId, record.getId()));
        }

        return result;
    }

    @Override
    public Map<String, Object> getSigninCalendar(Long userId, String month) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        YearMonth targetMonth = parseMonth(month);
        List<Integer> signedDays = loadSignedDays(userId, targetMonth);

        Map<String, Object> result = new HashMap<>();
        result.put("month", targetMonth.format(MONTH_FMT));
        result.put("signedDays", signedDays);
        result.put("signedCount", signedDays.size());
        return result;
    }

    @Override
    public Map<String, Object> getSigninMonthStats(Long userId, String month) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        YearMonth targetMonth = parseMonth(month);
        String monthText = targetMonth.format(MONTH_FMT);
        String cacheKey = RedisKeyConstants.signinMonthStatsByUserAndMonth(userId, monthText);

        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                String[] parts = cached.split(",");
                if (parts.length == 3) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("month", monthText);
                    result.put("signedCount", Integer.parseInt(parts[0]));
                    result.put("maxConsecutiveDays", Integer.parseInt(parts[1]));
                    result.put("signedToday", "1".equals(parts[2]));
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("读取签到月统计缓存失败: userId={}, month={}", userId, monthText, e);
        }

        List<Integer> signedDays = loadSignedDays(userId, targetMonth);
        int maxConsecutiveDays = calcMaxConsecutiveInMonth(signedDays);
        boolean signedToday = false;
        if (YearMonth.now().equals(targetMonth)) {
            signedToday = signedDays.contains(LocalDate.now().getDayOfMonth());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("month", monthText);
        result.put("signedCount", signedDays.size());
        result.put("maxConsecutiveDays", maxConsecutiveDays);
        result.put("signedToday", signedToday);

        try {
            String value = signedDays.size() + "," + maxConsecutiveDays + "," + (signedToday ? "1" : "0");
            stringRedisTemplate.opsForValue().set(cacheKey, value, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入签到月统计缓存失败: userId={}, month={}", userId, monthText, e);
        }

        return result;
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month, MONTH_FMT);
        } catch (Exception e) {
            throw new RuntimeException("month 参数格式错误，应为 yyyyMM");
        }
    }

    private List<Integer> loadSignedDays(Long userId, YearMonth targetMonth) {
        String monthText = targetMonth.format(MONTH_FMT);
        String bitmapKey = RedisKeyConstants.signinBitmapByUserAndMonth(userId, monthText);

        tryBackfillBitmapFromDb(userId, targetMonth, bitmapKey);

        List<Integer> signedDays = new ArrayList<>();
        int daysInMonth = targetMonth.lengthOfMonth();
        for (int i = 0; i < daysInMonth; i++) {
            try {
                Boolean bit = stringRedisTemplate.opsForValue().getBit(bitmapKey, i);
                if (Boolean.TRUE.equals(bit)) {
                    signedDays.add(i + 1);
                }
            } catch (Exception e) {
                log.warn("读取签到位图失败: userId={}, month={}, day={}", userId, monthText, i + 1, e);
                break;
            }
        }

        return signedDays;
    }

    private int calcMaxConsecutiveInMonth(List<Integer> signedDays) {
        if (signedDays.isEmpty()) {
            return 0;
        }
        int max = 1;
        int cur = 1;
        for (int i = 1; i < signedDays.size(); i++) {
            if (signedDays.get(i) == signedDays.get(i - 1) + 1) {
                cur++;
                if (cur > max) {
                    max = cur;
                }
            } else {
                cur = 1;
            }
        }
        return max;
    }

    private void syncSigninBitmap(Long userId, LocalDate date) {
        String monthText = YearMonth.from(date).format(MONTH_FMT);
        String bitmapKey = RedisKeyConstants.signinBitmapByUserAndMonth(userId, monthText);
        int dayIndex = date.getDayOfMonth() - 1;
        try {
            stringRedisTemplate.opsForValue().setBit(bitmapKey, dayIndex, true);
            stringRedisTemplate.expire(bitmapKey, 90, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("写入签到位图失败: userId={}, date={}", userId, date, e);
        }
    }

    private void tryBackfillBitmapFromDb(Long userId, YearMonth targetMonth, String bitmapKey) {
        Boolean hasKey = stringRedisTemplate.hasKey(bitmapKey);
        if (Boolean.TRUE.equals(hasKey)) {
            return;
        }

        LocalDate start = targetMonth.atDay(1);
        LocalDate end = targetMonth.atEndOfMonth();

        LambdaQueryWrapper<SigninRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SigninRecord::getUserId, userId)
                .ge(SigninRecord::getSigninDate, start)
                .le(SigninRecord::getSigninDate, end);

        List<SigninRecord> records = signinRecordMapper.selectList(wrapper);
        if (records == null || records.isEmpty()) {
            return;
        }

        try {
            for (SigninRecord record : records) {
                int dayIndex = record.getSigninDate().getDayOfMonth() - 1;
                stringRedisTemplate.opsForValue().setBit(bitmapKey, dayIndex, true);
            }
            stringRedisTemplate.expire(bitmapKey, 90, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("回填签到位图失败: userId={}, month={}", userId, targetMonth.format(MONTH_FMT), e);
        }
    }

    private void evictSigninMonthStatsCache(Long userId, LocalDate date) {
        String monthText = YearMonth.from(date).format(MONTH_FMT);
        try {
            stringRedisTemplate.delete(RedisKeyConstants.signinMonthStatsByUserAndMonth(userId, monthText));
        } catch (Exception e) {
            log.warn("清理签到月统计缓存失败: userId={}, month={}", userId, monthText, e);
        }
    }

    /**
     * v5.0: 发放7日连签奖励 - "满35减10元优惠券"(有效期3天)
     * 通过 RPC 调用 PointsMallService 发放券
     * 
     * @param signinRecordId 触发奖励的签到记录ID（第7天）
     */
    private void grant7DayCoupon(Long userId, Long signinRecordId) {
        try {
            // 调用 PointsMallService 发放券
            // 券规则: 满35减10, 有效期3天
            if (pointsMallService != null) {
                pointsMallService.issueCouponToUser(
                        userId,
                        "SIGNIN_7DAY", // 券模板类型
                        "signin_7day_" + signinRecordId, // 唯一标识，防止重复发放
                        35.0, // 使用门槛
                        10.0, // 优惠金额
                        3 // 有效天数
                );
                log.info("7日连签券发放成功: userId={}, signinRecordId={}", userId, signinRecordId);
            } else {
                log.warn("PointsMallService 未注入, 无法发放7日券: userId={}", userId);
            }
        } catch (Exception e) {
            // 发放失败不影响签到主流程
            log.warn("7日连签券发放失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
