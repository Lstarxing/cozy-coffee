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
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class OssStorageService implements StorageService {

    private final StorageProperties properties;
    private final OSS ossClient;

    @Override
    public String upload(MultipartFile file, String subdir) {
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
