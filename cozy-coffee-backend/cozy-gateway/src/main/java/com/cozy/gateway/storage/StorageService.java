package com.cozy.gateway.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * 上传文件到对象存储
     *
     * @param file   上传的文件
     * @param subdir 子目录名，如 "products"、"avatars"
     * @return 完整的公网可访问 URL
     */
    String upload(MultipartFile file, String subdir);
}
