package com.cozy.gateway.controller;

import com.cozy.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class FileUploadController {

    // 允许的图片类型
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");

    // 最大文件大小 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 上传根目录（通过 application.yml 配置，默认使用用户目录）
    @Value("${file.upload-dir:${user.home}/.cozycoffee/uploads}")
    private String uploadDir;

    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
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

            // 生成文件路径：uploads/images/yyyy/MM/uuid.ext
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 创建目录（使用绝对路径）
            Path baseDir = Paths.get(uploadDir).toAbsolutePath();
            Path dirPath = baseDir.resolve("images").resolve(datePath);
            Files.createDirectories(dirPath);

            // 保存文件
            Path filePath = dirPath.resolve(newFilename);
            file.transferTo(filePath.toFile());

            // 返回访问URL
            String url = "/uploads/images/" + datePath + "/" + newFilename;
            log.info("文件上传成功: {} -> {}", url, filePath);

            return Result.success(Map.of("url", url));

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取上传目录绝对路径（供 WebConfig 使用）
     */
    public String getUploadDir() {
        return Paths.get(uploadDir).toAbsolutePath().toString();
    }
}
