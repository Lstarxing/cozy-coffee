package com.cozy.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.order.entity.PickupCodeCounter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

@Mapper
public interface PickupCodeCounterMapper extends BaseMapper<PickupCodeCounter> {

    /**
     * 获取并锁定计数器行（FOR UPDATE）
     */
    @Select("SELECT * FROM pickup_code_counter WHERE store_id = #{storeId} AND business_date = #{businessDate} FOR UPDATE")
    PickupCodeCounter selectForUpdate(@Param("storeId") Long storeId, @Param("businessDate") LocalDate businessDate);

    /**
     * 原子递增序号
     */
    @Update("UPDATE pickup_code_counter SET last_seq = last_seq + 1, updated_at = NOW() WHERE store_id = #{storeId} AND business_date = #{businessDate}")
    int incrementSeq(@Param("storeId") Long storeId, @Param("businessDate") LocalDate businessDate);
}
