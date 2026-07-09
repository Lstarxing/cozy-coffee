package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import com.cozy.gateway.exception.StorageException;
import com.cozy.gateway.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class FileUploadController {

    private final StorageService storageService;

    // 允许的图片类型
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");

    // 最大文件大小 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        // 验证文件
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过5MB");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return Result.error("只支持 jpg/png/gif/webp 格式的图片");
        }

        try {
            String url = storageService.upload(file, "products");
            return Result.success(Map.of("url", url));
        } catch (StorageException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
