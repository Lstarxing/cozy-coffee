package com.cozy.gateway.config;

import com.cozy.gateway.storage.LocalStorageService;
import com.cozy.gateway.storage.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LocalStorageConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "local")
    public LocalStorageService localStorageService(StorageProperties properties) {
        log.info("本地文件存储启用: uploadDir={}", properties.getLocalUploadDir());
        return new LocalStorageService(properties);
    }
}
