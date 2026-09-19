package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.service.AdminListService;
import com.cozy.gateway.service.AdminUserProfileCoordinator;
import com.cozy.gateway.service.AdminUserService;
import com.cozy.user.dto.response.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminListService listService;
    private final AdminUserService adminUserService;
    private final AdminUserProfileCoordinator adminUserProfileCoordinator;

    @GetMapping("/users")
    public Result<List<UserDTO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String memberLevel,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(listService.listUsers(keyword, memberLevel, startDate, endDate));
    }

    @PostMapping("/users/{userId}/points")
    public Result<Void> adjustUserPoints(@PathVariable Long userId, @RequestParam int amount, @RequestParam String reason) {
        adminUserService.adjustPoints(userId, amount, reason);
        return Result.success(null);
    }

    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId, @RequestParam String status) {
        adminUserService.updateUserStatus(userId, status);
        return Result.success(null);
    }

    @GetMapping("/users/{userId}")
    public Result<UserDTO> getUserDetail(@PathVariable Long userId) {
        return Result.success(adminUserProfileCoordinator.getUserDetail(userId));
    }
}
