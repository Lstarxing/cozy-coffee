package com.cozy.api.member;

import com.cozy.api.member.dto.SigninResultDTO;

/**
 * 签到服务 Dubbo 接口
 */
public interface SigninService {

    /**
     * 执行签到
     */
    SigninResultDTO signIn(Long userId);
}
