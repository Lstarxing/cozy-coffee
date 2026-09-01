package com.cozy.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.mall.entity.PointsProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PointsProductMapper extends BaseMapper<PointsProduct> {

    /** 原子扣减库存：stock >= qty 才扣，并发下不超卖 */
    @Update("UPDATE points_products SET stock = stock - #{qty} " +
            "WHERE id = #{id} AND stock >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);

    /** 原子恢复库存：与 deductStock 同为单语句 UPDATE，DB 行锁串行，无丢失更新 */
    @Update("UPDATE points_products SET stock = stock + #{qty} WHERE id = #{id}")
    int addStock(@Param("id") Long id, @Param("qty") int qty);
}
