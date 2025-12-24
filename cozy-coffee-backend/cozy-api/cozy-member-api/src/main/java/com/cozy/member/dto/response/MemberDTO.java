package com.cozy.member.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class MemberDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Integer currentPoints;
    private Integer totalPoints;
    private String memberLevel;
    private LocalDate lastSigninDate;
    private Integer consecutiveSignDays;
}
