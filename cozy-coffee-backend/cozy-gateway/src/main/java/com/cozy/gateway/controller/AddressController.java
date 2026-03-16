package com.cozy.gateway.controller;

import com.cozy.common.context.UserContext;
import com.cozy.common.result.Result;
import com.cozy.member.api.AddressService;
import com.cozy.member.dto.request.AddressRequest;
import com.cozy.member.dto.response.AddressDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/member/addresses")
public class AddressController {

    @DubboReference(check = false)
    private AddressService addressService;

    @GetMapping
    public Result<List<AddressDTO>> list() {
        try {
            Long userId = UserContext.getUserIdOrNull();
            List<AddressDTO> addresses = addressService.listByUserId(userId);
            return Result.success(addresses);
        } catch (Exception e) {
            log.error("获取地址列表失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/default")
    public Result<AddressDTO> getDefault() {
        try {
            Long userId = UserContext.getUserIdOrNull();
            AddressDTO address = addressService.getDefaultAddress(userId);
            return Result.success(address);
        } catch (Exception e) {
            log.error("获取默认地址失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping
    public Result<AddressDTO> create(@Valid @RequestBody AddressRequest request) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            AddressDTO created = addressService.create(userId, request);
            return Result.success(created, "地址添加成功");
        } catch (Exception e) {
            log.error("创建地址失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<AddressDTO> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            AddressDTO updated = addressService.update(userId, id, request);
            return Result.success(updated, "地址更新成功");
        } catch (Exception e) {
            log.error("更新地址失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            addressService.delete(id, userId);
            return Result.success(null, "地址删除成功");
        } catch (Exception e) {
            log.error("删除地址失败", e);
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        try {
            Long userId = UserContext.getUserIdOrNull();
            addressService.setDefault(id, userId);
            return Result.success(null, "已设为默认地址");
        } catch (Exception e) {
            log.error("设置默认地址失败", e);
            return Result.fail(e.getMessage());
        }
    }
}
