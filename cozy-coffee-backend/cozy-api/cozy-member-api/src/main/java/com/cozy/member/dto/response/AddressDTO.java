package com.cozy.member.dto.response;

import lombok.Data;
import java.io.Serializable;

@Data
public class AddressDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;
}
