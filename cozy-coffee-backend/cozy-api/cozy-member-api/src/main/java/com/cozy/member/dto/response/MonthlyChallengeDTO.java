package com.cozy.member.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 月度挑战任务配置（后端单一事实源，含 target/reward，移动端不再硬编码）。
 * 对应后端 checkAndGrantRewards 的 4 个挑战：order/morning/delivery/newproduct。
 */
@Data
public class MonthlyChallengeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String key;
    private String title;
    private String description;
    private Integer target;
    private Integer reward;
    private Integer current;
    private Boolean claimed;
}
