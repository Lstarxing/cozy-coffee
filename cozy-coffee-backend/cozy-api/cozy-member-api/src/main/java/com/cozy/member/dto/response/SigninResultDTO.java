package com.cozy.member.dto.response;

import lombok.Data;
import java.io.Serializable;

@Data
public class SigninResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private Integer pointsEarned;
    private Integer consecutiveDays;
    private Integer currentPoints;
    private Integer totalPoints;
}
