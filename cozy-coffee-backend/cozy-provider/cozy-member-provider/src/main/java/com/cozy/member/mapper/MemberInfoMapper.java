package com.cozy.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.member.entity.MemberInfo;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberInfoMapper extends BaseMapper<MemberInfo> {
    @Select("SELECT * FROM member_info WHERE user_id = #{userId} FOR UPDATE")
    MemberInfo selectByUserIdForUpdate(Long userId);

    @Select("SELECT m.user_id FROM member_info m " +
            "LEFT JOIN (SELECT user_id, SUM(remaining) as sum_rem FROM points_lots GROUP BY user_id) l " +
            "ON m.user_id = l.user_id " +
            "WHERE m.current_points != IFNULL(l.sum_rem, 0)")
    List<Long> findInconsistentUserIds();
}
