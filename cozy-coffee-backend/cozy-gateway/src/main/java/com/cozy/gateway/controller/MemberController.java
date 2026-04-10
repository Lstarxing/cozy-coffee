package com.cozy.gateway.controller;

import com.cozy.common.context.UserContext;
import com.cozy.common.result.Result;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.MonthlyTaskService;
import com.cozy.member.api.SigninService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.dto.response.MonthlyTaskDTO;
import com.cozy.member.dto.response.SigninResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/member")
public class MemberController {

    @DubboReference(check = false)
    private MemberService memberService;

    @DubboReference(check = false)
    private SigninService signinService;

    @DubboReference(check = false)
    private MonthlyTaskService monthlyTaskService;

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

    @GetMapping("/signin/calendar")
    public Result<java.util.Map<String, Object>> getSigninCalendar(@RequestParam(required = false) String month) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return Result.success(signinService.getSigninCalendar(userId, month));
        } catch (Exception e) {
            log.error("获取签到日历失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/signin/stats")
    public Result<java.util.Map<String, Object>> getSigninStats(@RequestParam(required = false) String month) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            return Result.success(signinService.getSigninMonthStats(userId, month));
        } catch (Exception e) {
            log.error("获取签到统计失败", e);
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

    /**
     * v4.2: 获取当月任务进度
     */
    @GetMapping("/monthly-task")
    public Result<MonthlyTaskDTO> getMonthlyTask() {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            MonthlyTaskDTO task = monthlyTaskService.getCurrentMonthTask(userId);
            return Result.success(task);
        } catch (Exception e) {
            log.error("获取月度任务失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * v4.2: 获取即将到期的积分
     */
    @GetMapping("/points/expiring")
    public Result<Integer> getExpiringPoints(@RequestParam(defaultValue = "30") int days) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            int expiringPoints = memberService.getExpiringPoints(userId, days);
            return Result.success(expiringPoints, days + "天内到期积分: " + expiringPoints);
        } catch (Exception e) {
            log.error("获取到期积分失败", e);
            return Result.fail(e.getMessage());
        }

    }

    @GetMapping("/test/trigger-birthday")
    public Result<String> triggerBirthdayRewards() {
        try {
            memberService.processBirthdayRewards();
            return Result.success("手动触发生日福利发放成功");
        } catch (Exception e) {
            log.error("手动触发生日福利失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * v5.5: 获取本月权益领取状态
     */
    @GetMapping("/benefits/status")
    public Result<java.util.Map<String, Object>> getMonthlyBenefitStatus() {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            java.util.Map<String, Object> status = memberService.getMonthlyBenefitStatus(userId);
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取权益状态失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * v5.5: 领取本月等级权益
     */
    @PostMapping("/benefits/receive-monthly")
    public Result<Void> receiveMonthlyBenefit() {
        try {
            Long userId = UserContext.getUserIdOrNull();
            if (userId == null) {
                return Result.fail("用户未登录");
            }
            memberService.receiveMonthlyBenefit(userId);
            return Result.success(null, "领取成功");
        } catch (Exception e) {
            log.error("领取月度权益失败", e);
            return Result.fail(e.getMessage());
        }
    }
}
