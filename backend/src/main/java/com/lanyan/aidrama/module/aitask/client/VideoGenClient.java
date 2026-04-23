package com.lanyan.aidrama.module.aitask.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Doubao 视频生成客户端 (火山引擎方舟 Ark)
 * 模型: doubao-seedance-2-0-260128
 * 接口: POST /api/v3/content/generation/tasks (异步任务模式)
 *       GET  /api/v3/content/generation/tasks/{task_id} (轮询状态)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoGenClient {

    private final DoubaoConfig doubaoConfig;
    private final @Qualifier("doubaoRestTemplate") RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 提交视频生成任务（异步）
     * @param prompt 提示词
     * @param firstFrameImage 首帧图片 URL（可选）
     * @param referenceImages 参考图 URL 列表（可选）
     * @return 任务 ID
     */
    @Retryable(
            value = {RestClientException.class, TimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public String submitVideoTask(String prompt, String firstFrameImage, java.util.List<String> referenceImages) {
        log.info("调用 Doubao 视频生成, model: {}, prompt length: {}",
                doubaoConfig.getVideoModel(), prompt.length());

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", doubaoConfig.getVideoModel());
        requestBody.put("generate_audio", true);
        requestBody.put("ratio", "16:9");
        requestBody.put("duration", 5);
        requestBody.put("watermark", false);

        // 构建 content 数组
        ArrayNode contentArray = requestBody.putArray("content");

        // 1. 文本提示词
        ObjectNode textNode = contentArray.addObject();
        textNode.put("type", "text");
        textNode.put("text", prompt);

        // 2. 首帧图片
        if (firstFrameImage != null && !firstFrameImage.isBlank()) {
            ObjectNode imageNode = contentArray.addObject();
            imageNode.put("type", "image_url");
            ObjectNode imageUrl = imageNode.putObject("image_url");
            imageUrl.put("url", firstFrameImage);
            imageNode.put("role", "first_frame");
        }

        // 3. 参考图
        if (referenceImages != null) {
            for (String refUrl : referenceImages) {
                ObjectNode refNode = contentArray.addObject();
                refNode.put("type", "image_url");
                ObjectNode refUrlNode = refNode.putObject("image_url");
                refUrlNode.put("url", refUrl);
                refNode.put("role", "reference_image");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(doubaoConfig.getApiKey());

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    doubaoConfig.getVideoApiUrl(), HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("视频生成 API 返回非 200 状态: " + response.getStatusCode());
            }

            String taskId = parseTaskId(response.getBody());
            log.info("视频生成任务已提交, taskId: {}", taskId);
            return taskId;
        } catch (RestClientException e) {
            log.error("Doubao 视频生成调用异常", e);
            throw e;
        }
    }

    /**
     * 查询视频生成任务状态
     * @param taskId 任务 ID
     * @return 任务结果: { "status": "pending/running/succeeded/failed", "videoUrl": "...", "error": "..." }
     */
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000, multiplier = 1)
    )
    public Map<String, String> queryTaskStatus(String taskId) {
        String url = doubaoConfig.getVideoApiUrl() + "/" + taskId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(doubaoConfig.getApiKey());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("查询任务状态返回非 200: " + response.getStatusCode());
        }

        return parseTaskResult(response.getBody());
    }

    // ============ 内部方法 ============

    /**
     * 解析创建任务响应，提取 task_id
     */
    private String parseTaskId(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode id = root.get("id");
            if (id != null) {
                return id.asText();
            }
            throw new RuntimeException("响应中无 id 字段, body: " + responseBody);
        } catch (Exception e) {
            log.error("解析视频任务 ID 失败, body: {}", responseBody, e);
            throw new RuntimeException("解析视频任务 ID 失败", e);
        }
    }

    /**
     * 解析任务查询响应
     * @return map 包含: status, videoUrl(成功时), error(失败时)
     */
    private Map<String, String> parseTaskResult(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String status = root.has("status") ? root.get("status").asText() : "unknown";

            java.util.HashMap<String, String> result = new java.util.HashMap<>();
            result.put("status", status);

            if ("succeeded".equals(status)) {
                // 从 content 数组中提取视频 URL
                JsonNode content = root.get("content");
                if (content != null && content.isArray()) {
                    for (JsonNode item : content) {
                        if ("video_url".equals(item.get("type").asText())) {
                            JsonNode videoUrlNode = item.get("video_url");
                            if (videoUrlNode != null && videoUrlNode.has("url")) {
                                result.put("videoUrl", videoUrlNode.get("url").asText());
                            }
                        }
                    }
                }
            } else if ("failed".equals(status)) {
                JsonNode errorNode = root.get("error");
                if (errorNode != null) {
                    result.put("error", errorNode.asText());
                }
            }

            return result;
        } catch (Exception e) {
            log.error("解析视频任务结果失败, body: {}", responseBody, e);
            throw new RuntimeException("解析视频任务结果失败", e);
        }
    }

    /**
     * 防止 API Key 意外泄露到日志
     */
    @Override
    public String toString() {
        return "VideoGenClient{apiKey='***', model='" + doubaoConfig.getVideoModel() + "'}";
    }
}
