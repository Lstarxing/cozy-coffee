package com.cozy.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.user.entity.UserEventOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserEventOutboxMapper extends BaseMapper<UserEventOutbox> {

    @Select("SELECT * FROM user_event_outbox WHERE status = 'PENDING' AND next_retry_at <= #{now} "
            + "ORDER BY id ASC LIMIT #{limit}")
    List<UserEventOutbox> selectPendingBatch(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_event_outbox WHERE status = #{status}")
    long countByStatus(@Param("status") String status);

    @Select("SELECT MIN(created_at) FROM user_event_outbox WHERE status = 'PENDING'")
    LocalDateTime oldestPendingCreatedAt();
}
