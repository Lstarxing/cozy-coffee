package com.cozy.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.order.entity.MessageOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {

    /** 扫 PENDING 且 next_retry_at <= now 的消息，按 id 升序取 N 条 */
    @Select("SELECT * FROM message_outbox WHERE status = 'PENDING' AND next_retry_at <= #{now} "
            + "ORDER BY id ASC LIMIT #{limit}")
    List<MessageOutbox> selectPendingBatch(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
