package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 签到服务实现
 * v4.0: 月封顶 800 积分 + 积分批次
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class SigninServiceImpl implements SigninService {

    private final MemberInfoMapper memberInfoMapper;
    private final SigninRecordMapper signinRecordMapper;
    private final PointsTransactionMapper transactionMapper;
    private final PointsLotMapper pointsLotMapper;

    // v5.0: 通过 RPC 调用 PointsMallService 发放7日连签券
    @org.apache.dubbo.config.annotation.DubboReference(check = false)
    private com.cozy.member.api.PointsMallService pointsMallService;

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
            grant7DayCoupon(userId, record.getId());
        }

        return result;
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
