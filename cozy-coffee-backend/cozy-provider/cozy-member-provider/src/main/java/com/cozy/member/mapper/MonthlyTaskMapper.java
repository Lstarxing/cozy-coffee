package com.cozy.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.member.entity.MonthlyTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 月度任务Mapper
 */
@Mapper
public interface MonthlyTaskMapper extends BaseMapper<MonthlyTask> {
}
