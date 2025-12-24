package com.cozy.member.api;

import com.cozy.member.dto.request.AddressRequest;
import com.cozy.member.dto.response.AddressDTO;
import java.util.List;

public interface AddressService {
    List<AddressDTO> listByUserId(Long userId);

    AddressDTO getDefaultAddress(Long userId);

    AddressDTO getById(Long id);

    AddressDTO create(Long userId, AddressRequest request);

    AddressDTO update(Long userId, Long addressId, AddressRequest request);

    boolean delete(Long id, Long userId);

    boolean setDefault(Long id, Long userId);
}
