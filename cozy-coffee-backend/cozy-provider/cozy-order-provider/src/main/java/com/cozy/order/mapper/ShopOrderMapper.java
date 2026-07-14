package com.cozy.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.order.entity.ShopOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShopOrderMapper extends BaseMapper<ShopOrder> {
    @Select("SELECT * FROM shop_orders WHERE user_id = #{userId} AND idempotency_key = #{idempotencyKey} LIMIT 1")
    ShopOrder selectByUserAndIdempotencyKey(@Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey);
}
