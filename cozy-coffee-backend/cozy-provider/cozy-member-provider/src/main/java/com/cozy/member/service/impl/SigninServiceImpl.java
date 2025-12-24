package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.member.api.SigninService;
import com.cozy.member.dto.response.SigninResultDTO;
import com.cozy.member.entity.MemberInfo;
import com.cozy.member.entity.SigninRecord;
import com.cozy.member.entity.PointsTransaction;
import com.cozy.member.mapper.MemberInfoMapper;
import com.cozy.member.mapper.SigninRecordMapper;
import com.cozy.member.mapper.PointsTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@DubboService
@RequiredArgsConstructor
public class SigninServiceImpl implements SigninService {

    private final MemberInfoMapper memberInfoMapper;
    private final SigninRecordMapper signinRecordMapper;
    private final PointsTransactionMapper transactionMapper;

    @Override
    @Transactional
    public SigninResultDTO signIn(Long userId) {
        // 参数验证
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

        // 根据会员等级计算基础签到积分
        int basePoints = getBaseSigninPointsByLevel(member.getMemberLevel());

        // 计算积分：等级基础分 + 连续天数奖励（最多7天额外14分）
        int points = basePoints + Math.min(consecutiveDays - 1, 7) * 2;

        // 更新会员信息
        member.setLastSigninDate(today);
        member.setConsecutiveSignDays(consecutiveDays);
        member.setCurrentPoints(member.getCurrentPoints() + points);
        member.setTotalPoints(member.getTotalPoints() + points);
        memberInfoMapper.updateById(member);

        // 插入签到记录
        SigninRecord record = new SigninRecord();
        record.setUserId(userId);
        record.setSigninDate(today);
        record.setPointsEarned(points);
        record.setConsecutiveDays(consecutiveDays);
        signinRecordMapper.insert(record);

        // 记录积分流水
        PointsTransaction transaction = new PointsTransaction();
        transaction.setUserId(userId);
        transaction.setChangeAmount(points);
        transaction.setBalanceAfter(member.getCurrentPoints());
        transaction.setSourceType("signin");
        transaction.setDescription("每日签到" + (consecutiveDays > 1 ? "（连续" + consecutiveDays + "天）" : ""));
        transactionMapper.insert(transaction);

        // 构建返回结果
        SigninResultDTO result = new SigninResultDTO();
        result.setSuccess(true);
        result.setMessage("签到成功！连续签到" + consecutiveDays + "天，获得" + points + "积分");
        result.setPointsEarned(points);
        result.setConsecutiveDays(consecutiveDays);
        result.setCurrentPoints(member.getCurrentPoints());
        result.setTotalPoints(member.getTotalPoints());
        return result;
    }

    /**
     * 根据会员等级获取基础签到积分
     * 基础会员：10分
     * 白银会员：15分
     * 黄金会员：20分
     * 黑金会员：30分
     */
    private int getBaseSigninPointsByLevel(String memberLevel) {
        if (memberLevel == null) {
            return 10;
        }
        return switch (memberLevel.toLowerCase()) {
            case "silver" -> 15;
            case "gold" -> 20;
            case "black" -> 30;
            default -> 10;
        };
    }
}
