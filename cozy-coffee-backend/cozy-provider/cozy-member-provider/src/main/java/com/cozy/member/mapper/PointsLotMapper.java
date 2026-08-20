package com.cozy.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.member.entity.PointsLot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface PointsLotMapper extends BaseMapper<PointsLot> {

    /**
     * 【FIFO排序】获取用户可用批次，严格按 expires_at ASC, id ASC 排序
     * 使用 FOR UPDATE 加行锁，确保并发安全
     */
    @Select("SELECT * FROM points_lots WHERE user_id = #{userId} AND remaining > 0 " +
            "AND expires_at > NOW() ORDER BY expires_at ASC, id ASC FOR UPDATE")
    List<PointsLot> selectAvailableLotsForUpdate(Long userId);

    /**
     * 【CAS并发保护】原子扣减积分批次
     * 使用 remaining >= deductAmount 条件确保不会超扣
     * 
     * @return 影响行数，0表示扣减失败（已被其他线程抢占）
     */
    @Update("UPDATE points_lots SET remaining = remaining - #{deductAmount} " +
            "WHERE id = #{lotId} AND remaining >= #{deductAmount}")
    int casDeductRemaining(@Param("lotId") Long lotId, @Param("deductAmount") int deductAmount);

    /**
     * 【批量回补】按消费明细一次性回补原积分批次（保持原到期时间，FIFO 语义）
     * 单条 SQL 内联聚合，与批次条数无关；跳过已过期作废的批次（由调用方兜底新建）
     *
     * @return 受影响行数
     */
    @Update("UPDATE points_lots pl " +
            "JOIN (SELECT lot_id, SUM(consume_amount) amt FROM points_lot_consumptions " +
            "      WHERE consume_type = #{consumeType} AND consume_id = #{consumeId} GROUP BY lot_id) c " +
            "  ON pl.id = c.lot_id " +
            "SET pl.remaining = pl.remaining + c.amt " +
            "WHERE pl.user_id = #{userId} AND pl.expires_at > NOW()")
    int batchRestoreByConsumption(@Param("userId") Long userId,
            @Param("consumeType") String consumeType, @Param("consumeId") Long consumeId);
}
