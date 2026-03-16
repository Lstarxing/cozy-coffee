package com.cozy.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.member.entity.MonthlyTaskOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 月度任务订单Mapper
 */
@Mapper
public interface MonthlyTaskOrderMapper extends BaseMapper<MonthlyTaskOrder> {
}
