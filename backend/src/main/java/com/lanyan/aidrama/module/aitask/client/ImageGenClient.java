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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Doubao 图片生成客户端 (火山引擎方舟 Ark)
 * 模型: doubao-seedream-5-0-260128
 * 接口: /api/v3/images/generations (OpenAI 兼容格式)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenClient {

    private final DoubaoConfig doubaoConfig;
    private final @Qualifier("doubaoRestTemplate") RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 图片生成 API URL */
    private static final String IMAGE_API_URL = "https://ark.cn-beijing.volces.com/api/v3/images/generations";

    /**
     * 提交图片生成任务（图生图）
     * @param prompt 提示词（英文）
     * @param referenceImages 参考图 URL 列表（可选）
     * @return 生成的图片 URL 列表
     */
    @Retryable(
            value = {RestClientException.class, TimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public List<String> generateImage(String prompt, List<String> referenceImages) {
        log.info("调用 Doubao 图片生成, model: {}, prompt length: {}, refImages: {}",
                doubaoConfig.getImageModel(), prompt.length(), referenceImages != null ? referenceImages.size() : 0);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", doubaoConfig.getImageModel());
        requestBody.put("prompt", prompt);
        requestBody.put("size", "2K");
        requestBody.put("output_format", "png");
        requestBody.put("watermark", false);

        // 添加参考图（图生图模式）
        if (referenceImages != null && !referenceImages.isEmpty()) {
            ArrayNode imageArray = requestBody.putArray("image");
            for (String url : referenceImages) {
                imageArray.add(url);
            }
            // 自动顺序生成
            requestBody.put("sequential_image_generation", "auto");
            ObjectNode options = requestBody.putObject("sequential_image_generation_options");
            options.put("max_images", 1);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(doubaoConfig.getApiKey());

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    IMAGE_API_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("图片生成 API 返回非 200 状态: " + response.getStatusCode());
            }

            return parseImageUrls(response.getBody());
        } catch (RestClientException e) {
            log.error("Doubao 图片生成调用异常", e);
            throw e;
        }
    }

    /**
     * 解析图片生成响应
     */
    private List<String> parseImageUrls(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                log.warn("图片生成响应无 data 字段, body: {}", responseBody);
                return List.of();
            }
            java.util.ArrayList<String> urls = new java.util.ArrayList<>();
            for (JsonNode item : data) {
                if (item.has("url")) {
                    urls.add(item.get("url").asText());
                } else if (item.has("b64_json")) {
                    // base64 格式暂不支持，跳过
                    log.warn("图片生成返回 b64_json 格式，暂不支持处理");
                }
            }
            return urls;
        } catch (Exception e) {
            log.error("解析图片生成响应失败, body: {}", responseBody, e);
            throw new RuntimeException("解析图片生成响应失败", e);
        }
    }

    /**
     * 防止 API Key 意外泄露到日志
     */
    @Override
    public String toString() {
        return "ImageGenClient{apiKey='***', model='" + doubaoConfig.getImageModel() + "'}";
    }
}
