package com.cozy.gateway.controller;

import com.cozy.common.context.UserContext;
import com.cozy.common.result.Result;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.SigninService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.dto.response.SigninResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/member")
@CrossOrigin(origins = "*")
public class MemberController {

    @DubboReference(check = false)
    private MemberService memberService;

    @DubboReference(check = false)
    private SigninService signinService;

    @GetMapping("/info")
    public Result<MemberDTO> getMemberInfo() {
        try {
            Long userId = UserContext.getUserIdOrNull();
            MemberDTO memberDTO = memberService.getMemberByUserId(userId);
            return Result.success(memberDTO);
        } catch (Exception e) {
            log.error("获取会员信息失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/signin")
    public Result<SigninResultDTO> signIn() {
        try {
            Long userId = UserContext.getUserIdOrNull();
            SigninResultDTO result = signinService.signIn(userId);
            return Result.success(result, result.getMessage());
        } catch (Exception e) {
            log.error("签到失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("Member API is running!");
    }

    /**
     * 添加积分（用于模拟消费等场景）
     */
    @PostMapping("/points/add")
    public Result<Void> addPoints(@RequestBody AddPointsRequest request) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            memberService.addPoints(userId, request.getPoints(), request.getSourceType(), request.getDescription());
            return Result.success(null, "积分添加成功");
        } catch (Exception e) {
            log.error("添加积分失败", e);
            return Result.fail(e.getMessage());
        }
    }

    // 内部请求类
    public static class AddPointsRequest {
        private int points;
        private String sourceType;
        private String description;

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 获取积分流水记录
     */
    @GetMapping("/points/transactions")
    public Result<java.util.List<com.cozy.member.dto.response.PointsTransactionDTO>> getPointsTransactions(
            @RequestParam(defaultValue = "20") int limit) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            var transactions = memberService.getPointsTransactions(userId, limit);
            return Result.success(transactions);
        } catch (Exception e) {
            log.error("获取积分流水失败", e);
            return Result.fail(e.getMessage());
        }
    }
}
