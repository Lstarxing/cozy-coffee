package com.cozy.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.mall.entity.CouponRollbackInbox;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface CouponRollbackInboxMapper extends BaseMapper<CouponRollbackInbox> {

    @Insert("INSERT IGNORE INTO coupon_rollback_inbox(event_id, processed_at) VALUES(#{eventId}, #{now})")
    int insertIfAbsent(@Param("eventId") String eventId, @Param("now") LocalDateTime now);
}
