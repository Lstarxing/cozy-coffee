package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.dto.response.PointsTransactionDTO;
import com.cozy.member.entity.MemberInfo;
import com.cozy.member.entity.PointsTransaction;
import com.cozy.member.mapper.MemberInfoMapper;
import com.cozy.member.mapper.PointsTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@DubboService
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberInfoMapper memberInfoMapper;
    private final PointsTransactionMapper transactionMapper;

    @Override
    public MemberDTO getMemberByUserId(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberInfo::getUserId, userId);
        MemberInfo info = memberInfoMapper.selectOne(wrapper);

        if (info == null) {
            throw new RuntimeException("会员信息不存在，请联系客服");
        }

        MemberDTO dto = new MemberDTO();
        dto.setId(info.getId());
        dto.setUserId(info.getUserId());
        dto.setCurrentPoints(info.getCurrentPoints());
        dto.setTotalPoints(info.getTotalPoints());
        dto.setMemberLevel(info.getMemberLevel());
        dto.setLastSigninDate(info.getLastSigninDate());
        dto.setConsecutiveSignDays(info.getConsecutiveSignDays());
        return dto;
    }

    @Override
    public void createMember(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        // 检查是否已存在
        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberInfo::getUserId, userId);
        if (memberInfoMapper.selectCount(wrapper) > 0) {
            return; // 已存在则不重复创建
        }

        // 新用户注册奖励积分
        int registerBonus = 100;

        MemberInfo info = new MemberInfo();
        info.setUserId(userId);
        info.setMemberLevel("basic");
        info.setTotalPoints(registerBonus); // 注册奖励计入总积分
        info.setCurrentPoints(registerBonus); // 注册奖励计入可用积分
        info.setConsecutiveSignDays(0);
        memberInfoMapper.insert(info);

        // 记录注册奖励积分流水
        PointsTransaction transaction = new PointsTransaction();
        transaction.setUserId(userId);
        transaction.setChangeAmount(registerBonus);
        transaction.setBalanceAfter(registerBonus);
        transaction.setSourceType("register");
        transaction.setDescription("新用户注册奖励");
        transactionMapper.insert(transaction);
    }

    @Override
    @Transactional
    public void addPoints(Long userId, int points, String sourceType, String description) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (points == 0) {
            throw new RuntimeException("积分变动数量不能为0");
        }

        // 查询会员信息
        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberInfo::getUserId, userId);
        MemberInfo member = memberInfoMapper.selectOne(wrapper);

        if (member == null) {
            throw new RuntimeException("会员信息不存在");
        }

        // 扣减积分时检查余额
        if (points < 0 && member.getCurrentPoints() < Math.abs(points)) {
            throw new RuntimeException("积分不足，当前积分: " + member.getCurrentPoints());
        }

        // 更新积分
        member.setCurrentPoints(member.getCurrentPoints() + points);

        // 只有增加积分时才计入总积分（用于等级判定）
        if (points > 0) {
            member.setTotalPoints(member.getTotalPoints() + points);
            // 自动升级会员等级
            updateMemberLevel(member);
        }

        memberInfoMapper.updateById(member);

        // 记录积分流水
        PointsTransaction transaction = new PointsTransaction();
        transaction.setUserId(userId);
        transaction.setChangeAmount(points);
        transaction.setBalanceAfter(member.getCurrentPoints());
        transaction.setSourceType(sourceType);
        transaction.setDescription(description);
        transactionMapper.insert(transaction);
    }

    /**
     * 根据总积分自动升级会员等级
     */
    private void updateMemberLevel(MemberInfo member) {
        int totalPoints = member.getTotalPoints();
        String newLevel;

        if (totalPoints >= 8000) {
            newLevel = "black";
        } else if (totalPoints >= 3000) {
            newLevel = "gold";
        } else if (totalPoints >= 1000) {
            newLevel = "silver";
        } else {
            newLevel = "basic";
        }

        if (!newLevel.equals(member.getMemberLevel())) {
            member.setMemberLevel(newLevel);
            memberInfoMapper.updateById(member);
        }
    }

    @Override
    public List<PointsTransactionDTO> getPointsTransactions(Long userId, int limit) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        LambdaQueryWrapper<PointsTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsTransaction::getUserId, userId)
                .orderByDesc(PointsTransaction::getCreatedAt)
                .last("LIMIT " + Math.min(limit, 100));

        return transactionMapper.selectList(wrapper).stream()
                .map(this::toTransactionDTO)
                .collect(Collectors.toList());
    }

    private PointsTransactionDTO toTransactionDTO(PointsTransaction entity) {
        PointsTransactionDTO dto = new PointsTransactionDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setChangeAmount(entity.getChangeAmount());
        dto.setBalanceAfter(entity.getBalanceAfter());
        dto.setSourceType(entity.getSourceType());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
