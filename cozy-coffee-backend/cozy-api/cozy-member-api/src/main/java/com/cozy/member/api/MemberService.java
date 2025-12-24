package com.cozy.member.api;

import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.dto.response.PointsTransactionDTO;

import java.util.List;

public interface MemberService {
    MemberDTO getMemberByUserId(Long userId);

    void createMember(Long userId);

    /**
     * 为用户增加积分
     * 
     * @param userId      用户ID
     * @param points      积分数量
     * @param sourceType  来源类型
     * @param description 描述
     */
    void addPoints(Long userId, int points, String sourceType, String description);

    /**
     * 获取用户积分流水记录
     * 
     * @param userId 用户ID
     * @param limit  返回数量限制
     * @return 积分流水列表
     */
    List<PointsTransactionDTO> getPointsTransactions(Long userId, int limit);
}
