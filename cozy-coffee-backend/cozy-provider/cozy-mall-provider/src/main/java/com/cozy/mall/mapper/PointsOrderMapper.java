package com.cozy.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.mall.entity.PointsOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface PointsOrderMapper extends BaseMapper<PointsOrder> {

    @Select("SELECT IFNULL(SUM(quantity), 0) FROM points_orders WHERE user_id = #{userId} AND product_id = #{productId} AND created_at >= #{startDate} AND status != 'cancelled'")
    int countUserRedeemedQuantity(Long userId, Long productId, LocalDateTime startDate);
}
