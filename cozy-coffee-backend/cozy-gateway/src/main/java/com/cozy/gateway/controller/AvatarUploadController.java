package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.storage.StorageService;
import com.cozy.gateway.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@ConditionalOnBean(StorageService.class)
public class AvatarUploadController {

    private final StorageService storageService;

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        AuthUtil.requireUserId();
        String url = storageService.upload(file, "avatars");
        return Result.success(Map.of("url", url), "头像上传成功");
    }
}
