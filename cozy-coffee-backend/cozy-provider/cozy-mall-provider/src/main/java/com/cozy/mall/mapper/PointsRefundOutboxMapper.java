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
            + "WHERE status = 'PENDING' AND next_retry_at <= #{now} "
            + "ORDER BY id ASC LIMIT #{limit}")
    List<PointsRefundOutbox> selectPendingDue(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("UPDATE points_refund_outbox SET status='SENT', updated_at=#{now} "
            + "WHERE id=#{id} AND status='PENDING'")
    int markSent(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("UPDATE points_refund_outbox SET status=#{status}, retry_count=#{retryCount}, "
            + "next_retry_at=#{nextRetryAt}, updated_at=#{now} "
            + "WHERE id=#{id} AND status='PENDING'")
    int markFailed(@Param("id") Long id, @Param("status") String status,
            @Param("retryCount") int retryCount, @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("now") LocalDateTime now);
}
