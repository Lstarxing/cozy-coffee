
package com.cozy.gateway.storage;

import com.cozy.gateway.exception.StorageException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * MinIO（S3 兼容）存储实现：key 结构与 URL 拼接规则与 OSS 完全一致，
 * 仅靠 storage.public-base-url 指向 Nginx 反代的 /media 路径即可匿名可读。
 */
@Slf4j
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final StorageProperties properties;
    private final MinioClient minioClient;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    public String upload(MultipartFile file, String subdir) {
        if (file == null || file.isEmpty()) throw new StorageException("请选择要上传的文件");
        if (file.getSize() > MAX_FILE_SIZE) throw new StorageException("文件大小不能超过5MB");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase()))
            throw new StorageException("只支持 jpg/png/gif/webp 格式的图片");

        String key = buildKey(file, subdir);
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("MinIO upload failed: bucket={}, key={}, msg={}",
                    properties.getBucket(), key, e.getMessage(), e);
            throw new StorageException("上传到对象存储失败", e);
        }
        String url = buildUrl(key);
        log.info("MinIO upload success: key={}, url={}", key, url);
        return url;
    }

    private String buildKey(MultipartFile file, String subdir) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return properties.getDirPrefix() + subdir + "/" + datePath + "/" + uuid + extension;
    }

    private String buildUrl(String key) {
        if (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().isBlank()) {
            return properties.getPublicBaseUrl() + "/" + key;
        }
        throw new StorageException("使用 MinIO 必须配置 storage.public-base-url");
    }
}
