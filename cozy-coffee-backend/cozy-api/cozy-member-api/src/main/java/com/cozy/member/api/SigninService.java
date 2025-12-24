package com.cozy.member.api;

import com.cozy.member.dto.response.SigninResultDTO;

public interface SigninService {
    SigninResultDTO signIn(Long userId);
}
