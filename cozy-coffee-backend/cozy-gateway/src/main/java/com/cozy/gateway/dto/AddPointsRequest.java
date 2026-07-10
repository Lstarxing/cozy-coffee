package com.cozy.gateway.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddPointsRequest {

    @Min(value = 1, message = "积分数量必须大于0")
    private int points;

    @NotBlank(message = "积分来源类型不能为空")
    private String sourceType;

    private String description;
}
