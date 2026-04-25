package com.lanyan.aidrama.module.aitask.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Doubao 图片生成客户端 (火山引擎方舟 Ark)
 * 使用统一的 ArkApiClient 发送请求
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenClient {

    private final DoubaoConfig doubaoConfig;
    private final ArkApiClient arkApiClient;

    /**
     * 提交图片生成任务（同步返回图片 URL 列表）
     * @param prompt 提示词（英文）
     * @param referenceImages 参考图 URL 列表
     * @return 生成的图片 URL 列表
     */
    public List<String> generateImage(String prompt, List<String> referenceImages) {
        int promptLength = prompt != null ? prompt.length() : 0;
        log.info("调用 Doubao 图片生成, model: {}, prompt length: {}, refImages: {}",
                doubaoConfig.getImageModel(), promptLength,
                referenceImages != null ? referenceImages.size() : 0);

        ObjectNode requestBody = arkApiClient.getObjectMapper().createObjectNode();
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
        }

        String response = arkApiClient.postToUrl(
                arkApiClient.toJson(requestBody),
                "Doubao 图片生成",
                doubaoConfig.getImageApiUrl());

        return parseImageUrls(response);
    }

    private List<String> parseImageUrls(String responseBody) {
        try {
            JsonNode root = arkApiClient.getObjectMapper().readTree(responseBody);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                log.warn("图片生成响应无 data 字段, body: {}", responseBody);
                return List.of();
            }
            ArrayList<String> urls = new ArrayList<>();
            for (JsonNode item : data) {
                if (item.has("url")) {
                    urls.add(item.get("url").asText());
                }
            }
            return urls;
        } catch (Exception e) {
            log.error("解析图片生成响应失败, body: {}", responseBody, e);
            throw new RuntimeException("解析图片生成响应失败", e);
        }
    }
}
