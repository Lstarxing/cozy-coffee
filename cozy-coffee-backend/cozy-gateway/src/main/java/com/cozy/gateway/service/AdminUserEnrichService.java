package com.cozy.gateway.service;

import com.cozy.gateway.util.PhoneMaskUtil;
import com.cozy.member.dto.response.PointsOrderDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.user.api.UserService;
import com.cozy.user.dto.response.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 管理端用户信息填充服务。
 * 为订单/兑换单详情填充关联用户信息（昵称、手机号脱敏等）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserEnrichService {

    @DubboReference(check = false)
    private UserService userService;

    public void enrichOrder(ShopOrderDTO order) {
        if (order.getUserId() == null) return;
        try {
            UserDTO user = userService.getUserDetail(order.getUserId());
            if (user != null) {
                order.setUsername(user.getUsername());
                order.setNickname(user.getNickname());
                order.setPhoneMasked(PhoneMaskUtil.mask(user.getPhone()));
            }
        } catch (Exception e) {
            order.setUsername(null);
            order.setNickname(null);
            order.setPhoneMasked("***");
        }
    }

    public void enrichRedemption(PointsOrderDTO order) {
        if (order.getUserId() == null) return;
        try {
            UserDTO user = userService.getUserDetail(order.getUserId());
            if (user != null) {
                order.setUsername(user.getUsername());
                order.setNickname(user.getNickname());
                order.setPhoneMasked(PhoneMaskUtil.mask(user.getPhone()));
            }
        } catch (Exception e) {
            order.setPhoneMasked("***");
        }
    }
}
