package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.util.AuthUtil;
import com.cozy.member.api.AddressService;
import com.cozy.member.dto.request.AddressRequest;
import com.cozy.member.dto.response.AddressDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/member/addresses")
public class AddressController {

    @DubboReference(check = false)
    private AddressService addressService;

    @GetMapping
    public Result<List<AddressDTO>> list() {
        return Result.success(addressService.listByUserId(AuthUtil.requireUserId()));
    }

    @GetMapping("/default")
    public Result<AddressDTO> getDefault() {
        return Result.success(addressService.getDefaultAddress(AuthUtil.requireUserId()));
    }

    @PostMapping
    public Result<AddressDTO> create(@Valid @RequestBody AddressRequest request) {
        return Result.success(addressService.create(AuthUtil.requireUserId(), request), "地址添加成功");
    }

    @PutMapping("/{id}")
    public Result<AddressDTO> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return Result.success(addressService.update(AuthUtil.requireUserId(), id, request), "地址更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id, AuthUtil.requireUserId());
        return Result.success(null, "地址删除成功");
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id, AuthUtil.requireUserId());
        return Result.success(null, "已设为默认地址");
    }
}
