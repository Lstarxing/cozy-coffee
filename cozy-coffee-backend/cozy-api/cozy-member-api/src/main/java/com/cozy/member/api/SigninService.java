package com.cozy.member.api;

import com.cozy.member.dto.response.SigninResultDTO;

import java.util.Map;

public interface SigninService {
    SigninResultDTO signIn(Long userId);

    Map<String, Object> getSigninCalendar(Long userId, String month);

    Map<String, Object> getSigninMonthStats(Long userId, String month);
}
