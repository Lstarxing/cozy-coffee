package com.cozy.gateway.service;

import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.response.CouponSummaryDTO;
import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 会员资料组合（BFF）：主体信息来自 member 域，券包数量来自 mall 域。
 *
 * <p>券数量原本由 member-provider 反向调 mall 查询（member → mall 依赖环，且那个调用用的
 * "available" 状态并不存在、恒返 0）。改为在网关这一层聚合后：
 * member-provider 不再依赖 cozy-mall-api，券数量也拿到真实口径。
 *
 * <p>这是纯展示字段的组合，故沿用扁平 MemberDTO（三端无需改动）。失败语义：
 * <b>member 主体信息失败正常向上抛出</b>（会员资料拿不到就是真的失败），
 * <b>mall 券统计失败降级为 0</b>（次要展示字段，不该拖垮主接口）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberProfileCoordinator {

    @DubboReference(check = false)
    private MemberService memberService;

    // 券数量是次要展示字段：给短超时，别让会员资料接口被 mall 拖住
    @DubboReference(check = false, timeout = 1000, retries = 0)
    private PointsMallService pointsMallService;

    public MemberDTO getMemberProfile(Long userId) {
        MemberDTO member = memberService.getMemberByUserId(userId);
        if (member == null) {
            return null;
        }
        try {
            CouponSummaryDTO summary = pointsMallService.getCouponSummary(userId);
            member.setCouponCount(summary == null || summary.getAvailableCount() == null
                    ? 0 : summary.getAvailableCount());
            member.setExchangeCouponCount(summary == null || summary.getExchangeCount() == null
                    ? 0 : summary.getExchangeCount());
        } catch (Exception e) {
            // 降级：会员主体信息照常返回，券数量回退 0
            log.warn("券包汇总查询失败，券数量降级为 0: userId={}, error={}", userId, e.getMessage());
            member.setCouponCount(0);
            member.setExchangeCouponCount(0);
        }
        return member;
    }
}
