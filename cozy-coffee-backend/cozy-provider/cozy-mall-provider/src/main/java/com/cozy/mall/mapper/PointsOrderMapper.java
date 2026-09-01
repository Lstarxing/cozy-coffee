package com.cozy.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.mall.entity.PointsOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface PointsOrderMapper extends BaseMapper<PointsOrder> {

    @Select("SELECT IFNULL(SUM(quantity), 0) FROM points_orders WHERE user_id = #{userId} AND product_id = #{productId} AND created_at >= #{startDate} AND status != 'cancelled'")
    int countUserRedeemedQuantity(Long userId, Long productId, LocalDateTime startDate);

    /** 状态 CAS：仅 pending/processing 且用户匹配的行置为 cancelled，并发取消只有一个赢家 */
    @Update("UPDATE points_orders SET status = 'cancelled', updated_at = #{now} " +
            "WHERE id = #{id} AND user_id = #{userId} AND status IN ('pending', 'processing')")
    int cancelOrderIfPending(@Param("id") Long orderId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);
}
