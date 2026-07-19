package com.cozy.gateway.config;

import com.cozy.gateway.storage.LocalStorageService;
import com.cozy.gateway.storage.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LocalStorageConfig {

    @Bean
    @ConditionalOnExpression("'${storage.access-key-id:}'.isEmpty()")
    public LocalStorageService localStorageService(StorageProperties properties) {
        log.info("OSS 未配置，启用本地文件存储: uploadDir={}", properties.getLocalUploadDir());
        return new LocalStorageService(properties);
    }
}
