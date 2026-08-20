package com.cozy.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_addresses")
public class UserAddress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String receiverName;
    private String gender; // MALE男 / FEMALE女
    private String label; // HOME家 / COMPANY公司 / SCHOOL学校
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
