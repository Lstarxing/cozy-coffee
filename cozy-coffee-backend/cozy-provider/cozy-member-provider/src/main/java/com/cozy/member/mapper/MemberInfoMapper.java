package com.cozy.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.member.entity.MemberInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberInfoMapper extends BaseMapper<MemberInfo> {
}
