package com.cozy.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cozy.common.exception.BusinessException;
import com.cozy.member.api.AddressService;
import com.cozy.member.dto.request.AddressRequest;
import com.cozy.member.dto.response.AddressDTO;
import com.cozy.member.entity.UserAddress;
import com.cozy.member.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@DubboService
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private static final int MAX_ADDRESS_COUNT = 20;
    private final UserAddressMapper addressMapper;

    @Override
    public List<AddressDTO> listByUserId(Long userId) {
        validateUserId(userId);

        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId).orderByDesc(UserAddress::getIsDefault);
        return addressMapper.selectList(wrapper).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public AddressDTO getDefaultAddress(Long userId) {
        validateUserId(userId);

        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId).eq(UserAddress::getIsDefault, true);
        UserAddress address = addressMapper.selectOne(wrapper);
        return address != null ? toDTO(address) : null;
    }

    @Override
    public AddressDTO getById(Long id) {
        if (id == null) {
            throw new BusinessException("地址ID不能为空");
        }
        UserAddress address = addressMapper.selectById(id);
        return address != null ? toDTO(address) : null;
    }

    @Override
    @Transactional
    public AddressDTO create(Long userId, AddressRequest request) {
        validateUserId(userId);
        validateAddressRequest(request);

        // 检查地址数量限制
        LambdaQueryWrapper<UserAddress> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(UserAddress::getUserId, userId);
        long count = addressMapper.selectCount(countWrapper);
        if (count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException("地址数量已达上限（最多" + MAX_ADDRESS_COUNT + "个），请删除一些旧地址后再添加");
        }

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        address.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddress(userId);
        }
        addressMapper.insert(address);
        return toDTO(address);
    }

    @Override
    @Transactional
    public AddressDTO update(Long userId, Long addressId, AddressRequest request) {
        validateUserId(userId);
        if (addressId == null) {
            throw new BusinessException("地址ID不能为空");
        }
        validateAddressRequest(request);

        UserAddress address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("无权限修改此地址");
        }

        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddress(userId);
        }
        addressMapper.updateById(address);
        return toDTO(address);
    }

    @Override
    @Transactional
    public boolean delete(Long id, Long userId) {
        validateUserId(userId);
        if (id == null) {
            throw new BusinessException("地址ID不能为空");
        }

        UserAddress address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除此地址");
        }

        return addressMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean setDefault(Long id, Long userId) {
        validateUserId(userId);
        if (id == null) {
            throw new BusinessException("地址ID不能为空");
        }

        UserAddress address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("无权限操作此地址");
        }

        clearDefaultAddress(userId);
        address.setIsDefault(true);
        addressMapper.updateById(address);
        return true;
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
    }

    private void validateAddressRequest(AddressRequest request) {
        if (request == null) {
            throw new BusinessException("地址信息不能为空");
        }
        if (request.getReceiverName() == null || request.getReceiverName().trim().isEmpty()) {
            throw new BusinessException("收货人姓名不能为空");
        }
        if (request.getReceiverPhone() == null || request.getReceiverPhone().trim().isEmpty()) {
            throw new BusinessException("收货人电话不能为空");
        }
        if (request.getProvince() == null || request.getProvince().trim().isEmpty()) {
            throw new BusinessException("省份不能为空");
        }
        if (request.getCity() == null || request.getCity().trim().isEmpty()) {
            throw new BusinessException("城市不能为空");
        }
        if (request.getDetailAddress() == null || request.getDetailAddress().trim().isEmpty()) {
            throw new BusinessException("详细地址不能为空");
        }
    }

    private void clearDefaultAddress(Long userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId).eq(UserAddress::getIsDefault, true);
        List<UserAddress> defaults = addressMapper.selectList(wrapper);
        for (UserAddress addr : defaults) {
            addr.setIsDefault(false);
            addressMapper.updateById(addr);
        }
    }

    private AddressDTO toDTO(UserAddress entity) {
        AddressDTO dto = new AddressDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setReceiverName(entity.getReceiverName());
        dto.setReceiverPhone(entity.getReceiverPhone());
        dto.setProvince(entity.getProvince());
        dto.setCity(entity.getCity());
        dto.setDistrict(entity.getDistrict());
        dto.setDetailAddress(entity.getDetailAddress());
        dto.setIsDefault(entity.getIsDefault());
        return dto;
    }
}
