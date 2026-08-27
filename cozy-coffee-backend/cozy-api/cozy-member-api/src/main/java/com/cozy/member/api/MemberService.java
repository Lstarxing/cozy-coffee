package com.cozy.member.api;

import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.dto.response.MemberOverviewDTO;
import com.cozy.member.dto.response.PointsTransactionDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MemberService {

    /**
     * 获取会员信息
     */
    MemberDTO getMemberByUserId(Long userId);

    /**
     * 获取会员信息（别名）
     */
    default MemberDTO getMemberInfo(Long userId) {
        return getMemberByUserId(userId);
    }

    /**
     * 批量获取会员信息（用于订单列表优化，避免 N+1 查询）
     * 
     * @param userIds 用户ID集合
     * @return userId -> MemberDTO 映射
     */
    Map<Long, MemberDTO> getMembersByUserIds(Set<Long> userIds);

    /**
     * 创建会员
     */
    void createMember(Long userId);

    /**
     * 为用户增加积分（旧方法，保留兼容）
     */
    void addPoints(Long userId, int points, String sourceType, String description);

    /**
     * 为用户增加积分并创建积分批次（FIFO）
     * 
     * @param userId      用户ID
     * @param points      积分数量
     * @param sourceType  来源类型：order_completed/signin/register/admin
     * @param sourceId    来源ID（如订单ID）
     * @param description 描述
     */
    void addPointsWithLot(Long userId, int points, String sourceType, Long sourceId, String description);

    /**
     * 为用户增加 EXP（成长值）
     * 
     * @param userId  用户ID
     * @param exp     EXP 数量
     * @param orderId 关联订单ID
     */
    void addExp(Long userId, int exp, Long orderId);

    /**
     * FIFO 扣减积分
     * 
     * @param userId      用户ID
     * @param points      扣减积分数
     * @param consumeType 消耗类型：redeem
     * @param consumeId   消耗关联ID（如兑换订单ID）
     * @return 是否扣减成功
     */
    boolean consumePointsFIFO(Long userId, int points, String consumeType, Long consumeId);

    /**
     * 按消费明细退款：回补原积分批次（保持原到期时间，维持 FIFO 语义）
     * <p>
     * 先按 points_lot_consumptions 明细批量回补原批次；个别批次若已过期作废
     * 则差额新建 365 天批次兜底。幂等：refund 流水 (userId, sourceType, sourceId) 唯一。
     *
     * @param userId      用户ID
     * @param points      应退积分总额
     * @param consumeType 扣减类型（redeem）
     * @param consumeId   扣减关联ID（如兑换订单ID）
     * @param description 描述
     */
    void refundPointsByConsumption(Long userId, int points, String consumeType, Long consumeId, String description);

    /**
     * 管理员人工调整积分（支持正负，强一致性：同步更新批次 lot）
     * 
     * @param userId 用户ID
     * @param delta  变动值（正数为加，负数为扣）
     * @param reason 调整原因
     */
    void adminAdjustPoints(Long userId, int delta, String reason);

    /**
     * 修复用户积分一致性故障（方案 B：补齐或扣减 Lot 使其 SUM 等于 current_points）
     *
     * @param userId 用户ID（若为 null 则修复所有不一致用户）
     */
    void fixPointsConsistency(Long userId);

    /**
     * 获取用户即将到期的积分（近30天）
     */
    int getExpiringPoints(Long userId, int days);

    /**
     * 获取用户积分流水记录
     */
    List<PointsTransactionDTO> getPointsTransactions(Long userId, int limit);

    /**
     * 检查并升级会员等级
     */
    void checkAndUpgradeLevel(Long userId);

    /**
     * 手动触发生日福利发放（测试用）
     */
    void processBirthdayRewards();

    /**
     * 设置生日时立即发放权益包
     * 
     * @return true 发放成功, false 已领取过
     */
    boolean grantBirthdayReward(Long userId);

    /**
     * v5.5: 获取本月权益领取状态
     *
     * @return Map containing: claimed(bool), canClaim(bool), benefitName(String)
     */
    Map<String, Object> getMonthlyBenefitStatus(Long userId);

    /**
     * v5.5: 领取本月等级权益
     */
    void receiveMonthlyBenefit(Long userId);

    /**
     * v6.0: 会员权益面板聚合视图（权益页单一数据源）
     *
     * @return 当前等级身份 + 当前可享权益 + 升级预告 + 全部等级对比
     */
    MemberOverviewDTO getMemberOverview(Long userId);
}
