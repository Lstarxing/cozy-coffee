package com.cozy.gateway.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.cozy.gateway.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class OssStorageService implements StorageService {

    private final StorageProperties properties;
    private final OSS ossClient;

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
        try {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(file.getContentType());
            meta.setContentLength(file.getSize());
            ossClient.putObject(properties.getBucket(), key, file.getInputStream(), meta);
        } catch (OSSException e) {
            log.error("OSS upload failed: code={}, msg={}", e.getErrorCode(), e.getErrorMessage(), e);
            throw new StorageException("上传到对象存储失败", e);
        } catch (IOException e) {
            throw new StorageException("读取文件流失败", e);
        }
        String url = buildUrl(key);
        log.info("OSS upload success: key={}, url={}", key, url);
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
        return "https://" + properties.getBucket() + "." + properties.getEndpoint() + "/" + key;
    }
}
