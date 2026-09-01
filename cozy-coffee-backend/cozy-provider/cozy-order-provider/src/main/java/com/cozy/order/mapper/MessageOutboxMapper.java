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

    @Select("SELECT * FROM message_outbox WHERE status = 'DEAD' ORDER BY updated_at ASC, id ASC LIMIT #{limit}")
    List<MessageOutbox> selectDeadBatch(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM message_outbox WHERE status = 'DEAD'")
    long countDead();

    @Update("UPDATE message_outbox SET status='SENT', last_error=NULL, updated_at=#{now} WHERE id=#{id}")
    int markSent(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("UPDATE message_outbox SET status='PENDING', retry_count=0, next_retry_at=#{now}, "
            + "manual_retry_count=manual_retry_count+1, last_manual_retry_at=#{now}, "
            + "last_manual_retry_by=#{operatorId}, updated_at=#{now} WHERE id=#{id} AND status='DEAD'")
    int retryDead(@Param("id") Long id, @Param("operatorId") Long operatorId,
            @Param("now") LocalDateTime now);
}
