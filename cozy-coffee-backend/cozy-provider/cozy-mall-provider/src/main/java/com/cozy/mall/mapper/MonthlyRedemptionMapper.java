package com.cozy.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.mall.entity.MonthlyRedemption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MonthlyRedemptionMapper extends BaseMapper<MonthlyRedemption> {

    @Update("INSERT INTO monthly_redemptions (user_id, product_id, month, redeemed_count, updated_at) " +
            "VALUES (#{userId}, #{productId}, #{month}, #{quantity}, NOW()) " +
            "ON DUPLICATE KEY UPDATE redeemed_count = redeemed_count + #{quantity}, updated_at = NOW()")
    int incrementRedemption(@Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("month") String month,
            @Param("quantity") Integer quantity);
}
