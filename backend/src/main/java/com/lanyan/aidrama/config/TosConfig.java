package com.lanyan.aidrama.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.net.URI;

/**
 * 火山引擎TOS配置类
 * 基于AWS S3兼容SDK实现，配置项全部留空由运维后续填写
 */
@Configuration
public class TosConfig {

    private static final String ENDPOINT_SCHEME = "https://";

    @Value("${tos.access-key:}")
    private String accessKey;

    @Value("${tos.secret-key:}")
    private String secretKey;

    @Value("${tos.endpoint:}")
    private String endpoint;

    @Value("${tos.region:cn-beijing}")
    private String region;

    @Value("${tos.bucket:}")
    private String bucket;

    /**
     * 初始化S3客户端（兼容火山TOS）
     * @return S3Client实例
     */
    @Bean
    public S3Client s3Client() {
        // 配置AK/SK
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        // 构建客户端
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create(ENDPOINT_SCHEME + endpoint))
                .region(Region.of(region))
                .build();
    }

    /**
     * 初始化S3预签名生成器
     * @return S3Presigner实例
     */
    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create(ENDPOINT_SCHEME + endpoint))
                .region(Region.of(region))
                .build();
    }

    public String getBucket() {
        return bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRegion() {
        return region;
    }
}

