package com.cozy.member.api;

import com.cozy.common.exception.BusinessException;
import com.cozy.member.dto.response.SigninResultDTO;

import java.util.Map;

public interface SigninService {
    SigninResultDTO signIn(Long userId) throws BusinessException;

    Map<String, Object> getSigninCalendar(Long userId, String month) throws BusinessException;

    Map<String, Object> getSigninMonthStats(Long userId, String month) throws BusinessException;
}
