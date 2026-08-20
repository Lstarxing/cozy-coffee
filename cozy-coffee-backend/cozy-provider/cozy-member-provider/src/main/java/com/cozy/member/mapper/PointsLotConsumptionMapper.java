package com.cozy.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.member.entity.PointsLotConsumption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointsLotConsumptionMapper extends BaseMapper<PointsLotConsumption> {

    /**
     * 查询某扣减记录中、批次已过期作废的应退总量（用于兜底新建批次）
     */
    @Select("SELECT COALESCE(SUM(c.consume_amount), 0) FROM points_lot_consumptions c " +
            "JOIN points_lots l ON c.lot_id = l.id " +
            "WHERE c.consume_type = #{consumeType} AND c.consume_id = #{consumeId} " +
            "  AND l.user_id = #{userId} AND l.expires_at <= NOW()")
    int selectExpiredRefundAmount(@Param("userId") Long userId,
            @Param("consumeType") String consumeType, @Param("consumeId") Long consumeId);
}
