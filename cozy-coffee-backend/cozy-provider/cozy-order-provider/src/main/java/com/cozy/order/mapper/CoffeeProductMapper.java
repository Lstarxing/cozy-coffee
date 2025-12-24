package com.cozy.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.order.entity.CoffeeProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoffeeProductMapper extends BaseMapper<CoffeeProduct> {
}
