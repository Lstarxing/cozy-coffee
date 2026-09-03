package com.cozy.gateway.config;

import com.cozy.gateway.storage.MinioStorageService;
import com.cozy.gateway.storage.StorageProperties;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class MinioConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "minio")
    @ConditionalOnExpression("!'${storage.endpoint:}'.isEmpty()")
    public MinioClient minioClient(StorageProperties properties) {
        log.info("Initializing MinIO client: endpoint={}, bucket={}",
                properties.getEndpoint(), properties.getBucket());
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKeyId(), properties.getAccessKeySecret())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "minio")
    @ConditionalOnExpression("!'${storage.endpoint:}'.isEmpty()")
    public MinioStorageService minioStorageService(StorageProperties properties, MinioClient minioClient) {
        return new MinioStorageService(properties, minioClient);
    }
}
