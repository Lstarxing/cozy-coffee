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
    public Result<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "products") String type) {
        // 业务域目录白名单：coffee/points -> MinIO images/products/{coffee,points}/...
        // 未知类型回退 products（uuid 隔离，不越权）
        String subdir = switch (type) {
            case "coffee" -> "products/coffee";
            case "points" -> "products/points";
            default -> "products";
        };
        String url = storageService.upload(file, subdir);
        return Result.success(Map.of("url", url));
    }
}
