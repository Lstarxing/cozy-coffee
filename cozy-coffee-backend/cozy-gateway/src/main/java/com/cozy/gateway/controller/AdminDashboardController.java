package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.service.AdminDashboardService;
import com.cozy.mall.dto.response.PointsOrderDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(dashboardService.getStats(startDate, endDate));
    }

    @GetMapping("/analytics/trend")
    public Result<List<Map<String, Object>>> getAnalyticsTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "day") String granularity) {
        return Result.success(dashboardService.getTrend(startDate, endDate, granularity));
    }

    @GetMapping("/analytics/distribution")
    public Result<List<Map<String, Object>>> getAnalyticsDistribution(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam String domain) {
        return Result.success(dashboardService.getDistribution(startDate, endDate, domain));
    }

    @GetMapping("/analytics/rank")
    public Result<List<Map<String, Object>>> getAnalyticsRank(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam String domain,
            @RequestParam(defaultValue = "count") String metric,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(dashboardService.getRank(startDate, endDate, domain, metric, limit));
    }

    @GetMapping("/orders/recent")
    public Result<List<ShopOrderDTO>> getRecentOrders(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(dashboardService.getRecentOrders(limit));
    }

    @GetMapping("/redemptions/recent")
    public Result<List<PointsOrderDTO>> getRecentRedemptions(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(dashboardService.getRecentRedemptions(limit));
    }
}
