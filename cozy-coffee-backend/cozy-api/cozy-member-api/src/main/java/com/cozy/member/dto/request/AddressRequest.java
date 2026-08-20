package com.cozy.member.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 添加/更新地址请求
 */
@Data
public class AddressRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    private String gender = "MALE"; // MALE男 / FEMALE女

    private String label = "HOME"; // HOME家 / COMPANY公司 / SCHOOL学校

    @NotBlank(message = "收货人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "区/县不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    private Boolean isDefault = false;
}
