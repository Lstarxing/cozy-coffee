package com.cozy.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.member.entity.CouponGrantOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CouponGrantOutboxMapper extends BaseMapper<CouponGrantOutbox> {

    /** 扫 PENDING 且 next_retry_at <= now 的消息，按 id 升序取 N 条 */
    @Select("SELECT * FROM coupon_grant_outbox WHERE status = 'PENDING' AND next_retry_at <= #{now} "
            + "ORDER BY id ASC LIMIT #{limit}")
    List<CouponGrantOutbox> selectPendingBatch(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /** 指标用：按状态计数 */
    @Select("SELECT COUNT(*) FROM coupon_grant_outbox WHERE status = #{status}")
    long countByStatus(@Param("status") String status);

    /** 指标用：最老的 PENDING 创建时间（无则 null） */
    @Select("SELECT MIN(created_at) FROM coupon_grant_outbox WHERE status = 'PENDING'")
    LocalDateTime oldestPendingCreatedAt();
}
