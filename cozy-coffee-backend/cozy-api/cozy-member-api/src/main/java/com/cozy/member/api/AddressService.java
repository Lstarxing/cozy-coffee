package com.cozy.member.api;

import com.cozy.common.exception.BusinessException;
import com.cozy.member.dto.request.AddressRequest;
import com.cozy.member.dto.response.AddressDTO;
import java.util.List;

public interface AddressService {
    List<AddressDTO> listByUserId(Long userId) throws BusinessException;

    AddressDTO getDefaultAddress(Long userId) throws BusinessException;

    AddressDTO getById(Long id) throws BusinessException;

    AddressDTO create(Long userId, AddressRequest request) throws BusinessException;

    AddressDTO update(Long userId, Long addressId, AddressRequest request) throws BusinessException;

    boolean delete(Long id, Long userId) throws BusinessException;

    boolean setDefault(Long id, Long userId) throws BusinessException;
}
