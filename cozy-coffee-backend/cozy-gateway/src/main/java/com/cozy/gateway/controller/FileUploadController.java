package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传控制器（仅 OSS 配置完整时启用）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@ConditionalOnBean(StorageService.class)
public class FileUploadController {

    private final StorageService storageService;

    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = storageService.upload(file, "products");
        return Result.success(Map.of("url", url));
    }
}
