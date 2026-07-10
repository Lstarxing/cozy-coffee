package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.dto.AddPointsRequest;
import com.cozy.gateway.util.AuthUtil;
import com.cozy.member.api.MemberService;
import com.cozy.member.api.MonthlyTaskService;
import com.cozy.member.api.SigninService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.member.dto.response.MonthlyTaskDTO;
import com.cozy.member.dto.response.PointsTransactionDTO;
import com.cozy.member.dto.response.SigninResultDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    @DubboReference(check = false)
    private MemberService memberService;

    @DubboReference(check = false)
    private SigninService signinService;

    @DubboReference(check = false)
    private MonthlyTaskService monthlyTaskService;

    @GetMapping("/info")
    public Result<MemberDTO> getMemberInfo() {
        return Result.success(memberService.getMemberByUserId(AuthUtil.requireUserId()));
    }

    @PostMapping("/signin")
    public Result<SigninResultDTO> signIn() {
        SigninResultDTO result = signinService.signIn(AuthUtil.requireUserId());
        return Result.success(result, result.getMessage());
    }

    @GetMapping("/signin/calendar")
    public Result<Map<String, Object>> getSigninCalendar(@RequestParam(required = false) String month) {
        return Result.success(signinService.getSigninCalendar(AuthUtil.requireUserId(), month));
    }

    @GetMapping("/signin/stats")
    public Result<Map<String, Object>> getSigninStats(@RequestParam(required = false) String month) {
        return Result.success(signinService.getSigninMonthStats(AuthUtil.requireUserId(), month));
    }

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("Member API is running!");
    }

    @PostMapping("/points/add")
    public Result<Void> addPoints(@Valid @RequestBody AddPointsRequest request) {
        memberService.addPoints(AuthUtil.requireUserId(), request.getPoints(), request.getSourceType(), request.getDescription());
        return Result.success(null, "积分添加成功");
    }

    @GetMapping("/points/transactions")
    public Result<List<PointsTransactionDTO>> getPointsTransactions(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(memberService.getPointsTransactions(AuthUtil.requireUserId(), limit));
    }

    @GetMapping("/monthly-task")
    public Result<MonthlyTaskDTO> getMonthlyTask() {
        return Result.success(monthlyTaskService.getCurrentMonthTask(AuthUtil.requireUserId()));
    }

    @GetMapping("/points/expiring")
    public Result<Integer> getExpiringPoints(@RequestParam(defaultValue = "30") int days) {
        return Result.success(memberService.getExpiringPoints(AuthUtil.requireUserId(), days), days + "天内到期积分");
    }

    @GetMapping("/test/trigger-birthday")
    public Result<String> triggerBirthdayRewards() {
        memberService.processBirthdayRewards();
        return Result.success("手动触发生日福利发放成功");
    }

    @GetMapping("/benefits/status")
    public Result<Map<String, Object>> getMonthlyBenefitStatus() {
        return Result.success(memberService.getMonthlyBenefitStatus(AuthUtil.requireUserId()));
    }

    @PostMapping("/benefits/receive-monthly")
    public Result<Void> receiveMonthlyBenefit() {
        memberService.receiveMonthlyBenefit(AuthUtil.requireUserId());
        return Result.success(null, "领取成功");
    }
}
