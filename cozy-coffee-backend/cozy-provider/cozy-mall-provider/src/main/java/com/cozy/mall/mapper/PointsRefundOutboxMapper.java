package com.cozy.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.mall.entity.PointsRefundOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PointsRefundOutboxMapper extends BaseMapper<PointsRefundOutbox> {

    @Select("SELECT * FROM points_refund_outbox "
            + "WHERE (status = 'PENDING' AND next_retry_at <= #{now}) "
            + "OR (status = 'PROCESSING' AND locked_at <= #{staleBefore}) "
            + "ORDER BY id ASC LIMIT #{limit}")
    List<PointsRefundOutbox> selectRelayCandidates(@Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore, @Param("limit") int limit);

    @Update("UPDATE points_refund_outbox SET status='PROCESSING', locked_at=#{now}, updated_at=#{now} "
            + "WHERE id=#{id} AND ((status='PENDING' AND next_retry_at <= #{now}) "
            + "OR (status='PROCESSING' AND locked_at <= #{staleBefore}))")
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore);

    @Update("UPDATE points_refund_outbox SET status='SENT', locked_at=NULL, last_error=NULL, updated_at=#{now} "
            + "WHERE id=#{id} AND status='PROCESSING'")
    int markSent(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("UPDATE points_refund_outbox SET status=#{status}, retry_count=#{retryCount}, "
            + "next_retry_at=#{nextRetryAt}, locked_at=NULL, last_error=#{lastError}, updated_at=#{now} "
            + "WHERE id=#{id} AND status='PROCESSING'")
    int markFailed(@Param("id") Long id, @Param("status") String status,
            @Param("retryCount") int retryCount, @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("lastError") String lastError, @Param("now") LocalDateTime now);

    @Select("SELECT * FROM points_refund_outbox WHERE status='DEAD' "
            + "ORDER BY updated_at ASC, id ASC LIMIT #{limit}")
    List<PointsRefundOutbox> selectDeadBatch(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM points_refund_outbox WHERE status='DEAD'")
    long countDead();

    @Update("UPDATE points_refund_outbox SET status='PENDING', retry_count=0, next_retry_at=#{now}, "
            + "locked_at=NULL, manual_retry_count=manual_retry_count+1, last_manual_retry_at=#{now}, "
            + "last_manual_retry_by=#{operatorId}, updated_at=#{now} WHERE id=#{id} AND status='DEAD'")
    int retryDead(@Param("id") Long id, @Param("operatorId") Long operatorId,
            @Param("now") LocalDateTime now);
}
