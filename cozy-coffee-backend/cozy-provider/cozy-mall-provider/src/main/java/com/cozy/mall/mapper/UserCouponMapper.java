package com.cozy.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cozy.mall.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /** CAS 冻结：仅 ISSUED 状态可冻结，防止同券并发被多笔订单占用 */
    @Update("UPDATE user_coupons SET status = 'FROZEN', used_at = #{now} " +
            "WHERE id = #{id} AND status = 'ISSUED'")
    int freezeIfIssued(@Param("id") Long id, @Param("now") LocalDateTime now);
}
