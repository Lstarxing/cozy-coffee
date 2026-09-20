package com.cozy.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 条件更新认领邀请奖励资格：并发/重投下只有一个调用能把 0 改成 1（见 docs/adr/0001 C4）。
     * 不能用 selectById 判断后再 updateById —— 那是"先查后写"，存在 TOCTOU 窗口。
     *
     * @return 受影响行数；0 表示已被认领过（或用户不存在）
     */
    @Update("UPDATE users SET invite_reward_granted = 1 WHERE id = #{userId} AND invite_reward_granted = 0")
    int claimInviteReward(@Param("userId") Long userId);
}
