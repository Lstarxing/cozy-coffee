package com.cozy.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.mall.entity.MonthlyRedemption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MonthlyRedemptionMapper extends BaseMapper<MonthlyRedemption> {

    /** 月度限购条件自增：redeemed_count + qty <= limit 才累加，防止并发超限 */
    @Update("UPDATE monthly_redemptions SET redeemed_count = redeemed_count + #{qty} " +
            "WHERE user_id = #{userId} AND product_id = #{productId} AND month = #{month} " +
            "AND redeemed_count + #{qty} <= #{limit}")
    int incrementIfWithinLimit(@Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("month") String month,
            @Param("qty") int qty,
            @Param("limit") int limit);
}
