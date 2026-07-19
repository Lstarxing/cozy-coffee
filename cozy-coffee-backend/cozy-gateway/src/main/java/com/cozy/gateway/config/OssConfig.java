package com.cozy.gateway.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.cozy.gateway.storage.OssStorageService;
import com.cozy.gateway.storage.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class OssConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "storage.type", havingValue = "oss", matchIfMissing = true)
    @ConditionalOnExpression("!'${storage.access-key-id:}'.isEmpty()")
    public OSS ossClient(StorageProperties properties) {
        log.info("Initializing OSS client: endpoint={}, bucket={}", properties.getEndpoint(), properties.getBucket());
        return new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "oss", matchIfMissing = true)
    @ConditionalOnExpression("!'${storage.access-key-id:}'.isEmpty()")
    public OssStorageService ossStorageService(StorageProperties properties, OSS ossClient) {
        return new OssStorageService(properties, ossClient);
    }
}
