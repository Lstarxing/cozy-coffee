package com.cozy.gateway.service;

import com.cozy.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 微信小程序登录：用 wx.login 的 code 换 openid。
 */
@Slf4j
@Service
public class WechatService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${cozy.auth.wechat.appid:}")
    private String appid;

    @Value("${cozy.auth.wechat.secret:}")
    private String secret;

    public WechatService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean isConfigured() {
        return appid != null && !appid.isBlank() && secret != null && !secret.isBlank();
    }

    /**
     * 用微信登录 code 换取 openid。
     */
    public String code2Session(String code) {
        if (!isConfigured()) {
            throw new BusinessException("微信小程序 appid/secret 未配置");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + appid
                + "&secret=" + secret
                + "&js_code=" + code
                + "&grant_type=authorization_code";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BusinessException("微信登录服务异常");
            }
            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
            Object errcode = body.get("errcode");
            if (errcode != null && !"0".equals(String.valueOf(errcode))) {
                String errmsg = String.valueOf(body.getOrDefault("errmsg", "code 无效或已过期"));
                log.warn("微信 code2Session 失败: errcode={}, errmsg={}", errcode, errmsg);
                throw new BusinessException("微信登录失败：" + errmsg);
            }
            String openid = (String) body.get("openid");
            if (openid == null || openid.isBlank()) {
                throw new BusinessException("微信登录未返回 openid");
            }
            return openid;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("微信登录网络异常");
        } catch (Exception e) {
            log.warn("微信 code2Session 调用异常", e);
            throw new BusinessException("微信登录网络异常");
        }
    }
}
