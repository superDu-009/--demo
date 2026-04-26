package com.lanyan.aidrama.module.storage.service;

import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.common.exception.TosException;
import com.lanyan.aidrama.config.TosConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * TOS 存储服务 (系分 v1.2 第 7.6 节)
 * 使用 AWS S3 兼容接口访问火山 TOS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TosService {

    private final TosConfig tosConfig;

    /**
     * 生成预签名 PUT 上传 URL
     * @param fileName 文件名
     * @param contentType 文件类型
     * @param source 来源标识
     * @param businessId 业务ID
     * @return PresignResult（包含上传 URL 和文件 Key）
     */
    public com.lanyan.aidrama.module.storage.dto.PresignResult generatePresignUrlWithUser(
            String fileName, String contentType, String source, String businessId, Long userId) {

        String fileKey = buildFileKey(source, businessId, userId, fileName);

        // 构建 TOS 客户端
        S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(tosConfig.getEndpointUri())
                .region(Region.of(tosConfig.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(tosConfig.getAccessKey(), tosConfig.getSecretKey())))
                .build();

        // 生成预签名 PUT 上传 URL，有效期 1 小时
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(tosConfig.getBucket())
                        .key(fileKey)
                        .contentType(contentType)
                        .build())
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        com.lanyan.aidrama.module.storage.dto.PresignResult result =
                new com.lanyan.aidrama.module.storage.dto.PresignResult();
        result.setUploadUrl(presignedRequest.url().toString());
        result.setFileKey(fileKey);
        result.setExpireSeconds(3600);
        return result;
    }

    /**
     * 上传完成通知：校验 TOS 文件真实存在，返回公网 URL
     */
    public String completeUpload(com.lanyan.aidrama.module.storage.dto.TosCompleteRequest req) {
        // 校验文件 key 归属
        if (!req.getFileKey().startsWith(buildPrefix(req.getSource(), req.getUserId()))) {
            throw new BusinessException(40102, "文件归属校验失败");
        }

        // 使用 S3 客户端校验文件存在
        S3Client s3Client = S3Client.builder()
                .endpointOverride(tosConfig.getEndpointUri())
                .region(Region.of(tosConfig.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(tosConfig.getAccessKey(), tosConfig.getSecretKey())))
                .build();

        try {
            s3Client.headObject(builder -> builder
                    .bucket(tosConfig.getBucket())
                    .key(req.getFileKey())
                    .build());
        } catch (Exception e) {
            throw new TosException("文件在 TOS 中不存在: " + req.getFileKey());
        }

        // 返回公网可访问 URL
        return buildPublicUrl(req.getFileKey());
    }

    public byte[] readFileBytes(String fileKey) {
        try (S3Client s3Client = buildS3Client()) {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(tosConfig.getBucket())
                            .key(fileKey)
                            .build())
                    .asByteArray();
        } catch (NoSuchKeyException e) {
            throw new TosException("文件在 TOS 中不存在: " + fileKey);
        } catch (Exception e) {
            throw new TosException("读取 TOS 文件失败: " + fileKey);
        }
    }

    public String readFileAsText(String fileKey) {
        return new String(readFileBytes(fileKey), StandardCharsets.UTF_8);
    }

    public String uploadTextWithUser(String source, String businessId, Long userId, String fileName, String text) {
        String fileKey = buildFileKey(source, businessId, userId, fileName);
        try (S3Client s3Client = buildS3Client()) {
            PutObjectResponse response = s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(tosConfig.getBucket())
                            .key(fileKey)
                            .contentType("text/plain; charset=utf-8")
                            .build(),
                    RequestBody.fromString(text, StandardCharsets.UTF_8));
            log.info("上传文本到 TOS 成功, key: {}, eTag: {}", fileKey, response.eTag());
            return fileKey;
        } catch (Exception e) {
            throw new TosException("上传文本到 TOS 失败: " + fileKey);
        }
    }

    public String uploadBytesWithUser(String source, String businessId, Long userId, String fileName, byte[] bytes, String contentType) {
        String fileKey = buildFileKey(source, businessId, userId, fileName);
        try (S3Client s3Client = buildS3Client()) {
            PutObjectResponse response = s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(tosConfig.getBucket())
                            .key(fileKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(bytes));
            log.info("上传二进制文件到 TOS 成功, key: {}, eTag: {}", fileKey, response.eTag());
            return fileKey;
        } catch (Exception e) {
            throw new TosException("上传文件到 TOS 失败: " + fileKey);
        }
    }

    /**
     * 构建文件 Key 路径
     */
    private String buildFileKey(String source, String businessId, Long userId, String fileName) {
        return String.format("%s/%s/%s/%s_%s", buildPrefix(source, userId), source, businessId, userId, fileName);
    }

    private String buildPrefix(String source, Long userId) {
        return String.format("users/%d", userId);
    }

    public String buildPublicUrl(String fileKey) {
        return "https://" + tosConfig.getBucket() + "." + tosConfig.getEndpointHost().replace("tos-s3-", "tos-") + "/" + fileKey;
    }

    /**
     * 构建小说文件的完整公网访问URL（预签名下载URL）
     * 有效期1小时，返回的 URL 可直接在浏览器中打开下载/预览
     */
    public String buildNovelPublicUrl(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return fileKey;
        }
        // 如果已经是完整URL则直接返回
        if (fileKey.startsWith("http://") || fileKey.startsWith("https://")) {
            return fileKey;
        }

        // 使用 S3 预签名生成 GET 下载 URL
        S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(tosConfig.getEndpointUri())
                .region(Region.of(tosConfig.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(tosConfig.getAccessKey(), tosConfig.getSecretKey())))
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(tosConfig.getBucket())
                        .key(fileKey)
                        .build())
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    private S3Client buildS3Client() {
        return S3Client.builder()
                .endpointOverride(tosConfig.getEndpointUri())
                .region(Region.of(tosConfig.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(tosConfig.getAccessKey(), tosConfig.getSecretKey())))
                .build();
    }
}
