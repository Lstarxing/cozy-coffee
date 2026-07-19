package com.cozy.gateway.storage;

import com.cozy.gateway.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
public class LocalStorageService implements StorageService {

    private final StorageProperties properties;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public LocalStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String upload(MultipartFile file, String subdir) {
        if (file == null || file.isEmpty()) throw new StorageException("请选择要上传的文件");
        if (file.getSize() > MAX_FILE_SIZE) throw new StorageException("文件大小不能超过5MB");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase()))
            throw new StorageException("只支持 jpg/png/gif/webp 格式的图片");

        String relativePath = buildRelativePath(file, subdir);
        Path targetPath = Paths.get(properties.getLocalUploadDir(), relativePath).toAbsolutePath().normalize();

        // 安全检查：确保写入路径在 uploadDir 内
        Path uploadRoot = Paths.get(properties.getLocalUploadDir()).toAbsolutePath().normalize();
        if (!targetPath.startsWith(uploadRoot)) {
            throw new StorageException("非法文件路径");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            log.error("本地文件写入失败: path={}", targetPath, e);
            throw new StorageException("文件保存失败", e);
        }

        String url = "/uploads/" + relativePath.replace('\\', '/');
        log.info("本地存储上传成功: path={}, url={}", targetPath, url);
        return url;
    }

    private String buildRelativePath(MultipartFile file, String subdir) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String dirPrefix = properties.getDirPrefix() != null ? properties.getDirPrefix() : "images/";
        return dirPrefix + subdir + "/" + datePath + "/" + uuid + extension;
    }
}
