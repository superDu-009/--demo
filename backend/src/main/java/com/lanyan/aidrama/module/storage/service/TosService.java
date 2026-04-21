package com.lanyan.aidrama.module.storage.service;

import com.lanyan.aidrama.config.TosConfig;
import com.lanyan.aidrama.module.storage.dto.PresignResult;
import com.lanyan.aidrama.module.storage.dto.TosCompleteRequest;
import com.lanyan.aidrama.common.exception.TosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 火山引擎TOS存储服务类
 * 核心功能：
 * 1. 生成预签名上传URL
 * 2. 上传完成回调校验
 * 3. 字节/URL上传
 * 4. 文件删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TosService {

    private final S3Client s3Client;
    private final TosConfig tosConfig;
    private final S3Presigner s3Presigner;

    // 预签名URL默认有效期：1小时
    private static final long DEFAULT_EXPIRE_SECONDS = 3600L;

    /**
     * 生成PUT预签名上传URL
     * @param fileName 原始文件名
     * @param contentType 文件MIME类型
     * @param source 上传来源：frontend-前端上传，backend-后端内部上传
     * @param businessId 关联业务ID
     * @return 预签名结果，包含uploadUrl、fileKey、expireSeconds
     */
    public PresignResult generatePresignUrl(String fileName, String contentType, String source, Long businessId) {
        try {
            /**
             * filekey生成规则：{source}/{businessId}/{date}/{fileType}/{随机8位数字}_{原文件名}
             * 1. source: frontend-前端上传，backend-后端内部上传
             * 2. businessId: 关联业务ID（如项目ID）
             * 3. 日期：yyyyMMdd格式（如20260421）
             * 4. 文件类型：根据ContentType前缀判断，image/*→image，video/*→video，text/application/*→document，其他→other
             * 5. 随机8位：0-99999999的纯数字，补前导零到8位
             * 6. 原文件名：转义特殊字符（空格、中文等）
             */
            // 1. 日期处理
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            // 2. 文件类型判断
            String fileType = "other";
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    fileType = "image";
                } else if (contentType.startsWith("video/")) {
                    fileType = "video";
                } else if (contentType.startsWith("text/") || contentType.startsWith("application/")) {
                    fileType = "document";
                }
            }
            // 3. 生成8位随机数字
            int randomNum = new Random().nextInt(100000000);
            String randomStr = String.format("%08d", randomNum);
            // 4. 原文件名转义特殊字符
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            // 拼接最终fileKey
            String fileKey = String.format("%s/%s/%s/%s/%s_%s", source, businessId, date, fileType, randomStr, encodedFileName);

            // 构建预签名请求
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(tosConfig.getBucket())
                    .key(fileKey)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(DEFAULT_EXPIRE_SECONDS))
                    .putObjectRequest(putObjectRequest)
                    .build();

            // 生成预签名URL
            URL presignedUrl = s3Presigner.presignPutObject(presignRequest).url();

            // 封装返回结果
            PresignResult result = new PresignResult();
            result.setUploadUrl(presignedUrl.toString());
            result.setFileKey(fileKey);
            result.setExpireSeconds(DEFAULT_EXPIRE_SECONDS);

            log.info("生成TOS预签名URL成功，fileKey: {}", fileKey);
            return result;
        } catch (Exception e) {
            log.error("生成TOS预签名URL失败，fileName: {}, source: {}", fileName, source, e);
            throw new TosException(51101, "生成上传链接失败：" + e.getMessage());
        }
    }

    /**
     * 上传完成回调校验
     * 1. 校验文件是否真实存在于TOS
     * 2. 校验文件大小是否匹配
     * @param req 上传完成请求
     * @return 文件公开访问URL
     */
    public String completeUpload(TosCompleteRequest req) {
        try {
            // 1. HEAD请求校验文件是否存在
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(tosConfig.getBucket())
                    .key(req.getFileKey())
                    .build();

            HeadObjectResponse headResponse = s3Client.headObject(headRequest);

            // 2. 校验文件大小是否匹配（可选，前端传递时校验）
            if (req.getFileSize() != null && !headResponse.contentLength().equals(req.getFileSize())) {
                log.error("TOS文件大小校验失败，fileKey: {}, 期望大小: {}, 实际大小: {}", 
                        req.getFileKey(), req.getFileSize(), headResponse.contentLength());
                throw new TosException(51102, "文件校验失败：大小不匹配");
            }

            // 3. 生成公开访问URL（如果桶配置了公共读）
            String publicUrl = getPublicUrl(req.getFileKey());

            log.info("TOS上传完成校验成功，fileKey: {}, publicUrl: {}", req.getFileKey(), publicUrl);
            return publicUrl;
        } catch (NoSuchKeyException e) {
            log.error("TOS文件不存在，fileKey: {}", req.getFileKey(), e);
            throw new TosException(51102, "文件校验失败：文件不存在或链接已过期");
        } catch (Exception e) {
            log.error("TOS上传完成校验失败，fileKey: {}", req.getFileKey(), e);
            throw new TosException(51102, "文件校验失败：" + e.getMessage());
        }
    }

    /**
     * 从字节数组上传文件到TOS
     * @param data 字节数组
     * @param targetKey 目标存储Key
     * @return 公开访问URL
     */
    public String uploadFromBytes(byte[] data, String targetKey) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(tosConfig.getBucket())
                    .key(targetKey)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(data));
            log.info("TOS字节上传成功，targetKey: {}", targetKey);
            return getPublicUrl(targetKey);
        } catch (Exception e) {
            log.error("TOS字节上传失败，targetKey: {}", targetKey, e);
            throw new TosException(51103, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 从第三方URL下载文件并上传到TOS
     * @param sourceUrl 源文件URL
     * @param targetKey 目标存储Key
     * @return 公开访问URL
     */
    public String uploadFromUrl(String sourceUrl, String targetKey) {
        try {
            // 下载URL内容
            byte[] data = URI.create(sourceUrl).toURL().openStream().readAllBytes();
            return uploadFromBytes(data, targetKey);
        } catch (IOException e) {
            log.error("从URL下载文件失败，sourceUrl: {}", sourceUrl, e);
            throw new TosException(51103, "下载源文件失败：" + e.getMessage());
        }
    }

    /**
     * 删除TOS文件
     * @param key 文件Key
     */
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(tosConfig.getBucket())
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("TOS文件删除成功，key: {}", key);
        } catch (Exception e) {
            log.error("TOS文件删除失败，key: {}", key, e);
            throw new TosException(51104, "文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件后缀名
     * @param fileName 文件名
     * @return 后缀名（包含.）
     */
    private String getFileSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 生成公开访问URL
     * @param key 文件Key
     * @return 公开URL
     */
    private String getPublicUrl(String key) {
        return String.format("%s/%s/%s", 
                tosConfig.getEndpoint().replace("https://", "https://" + tosConfig.getBucket() + "."),
                tosConfig.getRegion(), key);
    }
}
