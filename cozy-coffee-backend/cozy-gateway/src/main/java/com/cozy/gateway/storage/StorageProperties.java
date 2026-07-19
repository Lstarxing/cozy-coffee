package com.cozy.gateway.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** 存储类型: oss | minio，默认 oss */
    private String type = "oss";

    /** 服务接入点，如 oss-cn-hangzhou.aliyuncs.com */
    private String endpoint;

    /** Bucket / 存储桶名称 */
    private String bucket;

    /** AccessKey ID */
    private String accessKeyId;

    /** AccessKey Secret */
    private String accessKeySecret;

    /** 上传路径前缀，默认 images/ */
    private String dirPrefix = "images/";

    /** 公网访问域名，不填则自动拼接 https://{bucket}.{endpoint} */
    private String publicBaseUrl;

    /** 本地存储目录（仅 storage.type=local 或无 OSS 时生效），默认 ./uploads */
    private String localUploadDir = "./uploads";
}
