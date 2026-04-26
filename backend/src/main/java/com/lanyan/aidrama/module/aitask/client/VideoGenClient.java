package com.lanyan.aidrama.module.aitask.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Doubao 视频生成客户端 (火山引擎方舟 Ark)
 * 异步任务模式：提交任务返回 taskId，查询状态获取视频 URL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoGenClient {

    private final DoubaoConfig doubaoConfig;
    private final ArkApiClient arkApiClient;

    /**
     * 提交视频生成任务（异步）
     * @param prompt 提示词
     * @param firstFrameImage 首帧图片 URL（承接上一分镜时使用）
     * @param referenceImages 参考图 URL 列表（绑定资产主参考图）
     * @return 第三方任务 ID
     */
    public String submitVideoTask(String prompt, String firstFrameImage, List<String> referenceImages) {
        log.info("调用 Doubao 视频生成, model: {}, prompt length: {}",
                doubaoConfig.getVideoModel(), prompt != null ? prompt.length() : 0);

        ObjectNode requestBody = arkApiClient.getObjectMapper().createObjectNode();
        requestBody.put("model", doubaoConfig.getVideoModel());

        // 构建 content 数组
        ArrayNode contentArray = requestBody.putArray("content");

        // 1. 文本提示词
        ObjectNode textNode = contentArray.addObject();
        textNode.put("type", "text");
        textNode.put("text", prompt);

        // 2. 首帧图片（承接上一分镜）
        if (firstFrameImage != null && !firstFrameImage.isBlank()) {
            ObjectNode imageNode = contentArray.addObject();
            imageNode.put("type", "image_url");
            ObjectNode imageUrl = imageNode.putObject("image_url");
            imageUrl.put("url", firstFrameImage);
            imageNode.put("role", "first_frame");
        }

        // 3. 参考图（绑定资产主参考图）
        if (referenceImages != null) {
            for (String refUrl : referenceImages) {
                ObjectNode refNode = contentArray.addObject();
                refNode.put("type", "image_url");
                ObjectNode refUrlNode = refNode.putObject("image_url");
                refUrlNode.put("url", refUrl);
                refNode.put("role", "reference_image");
            }
        }

        String response = arkApiClient.postToUrl(
                arkApiClient.toJson(requestBody),
                "Doubao 视频生成",
                doubaoConfig.getVideoApiUrl());

        String taskId = parseTaskId(response);
        log.info("视频生成任务已提交, taskId: {}", taskId);
        return taskId;
    }

    /**
     * 查询视频生成任务状态
     */
    public Map<String, String> queryTaskStatus(String taskId) {
        String url = doubaoConfig.getVideoApiUrl() + "/" + taskId;
        String response = arkApiClient.get(url, "Doubao 视频任务查询");
        return parseTaskResult(response);
    }

    private String parseTaskId(String responseBody) {
        try {
            JsonNode root = arkApiClient.getObjectMapper().readTree(responseBody);
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

    private Map<String, String> parseTaskResult(String responseBody) {
        try {
            JsonNode root = arkApiClient.getObjectMapper().readTree(responseBody);
            String status = root.has("status") ? root.get("status").asText() : "unknown";

            HashMap<String, String> result = new HashMap<>();
            result.put("status", status);

            if ("succeeded".equals(status)) {
                JsonNode content = root.get("content");
                if (content != null && content.isArray()) {
                    for (JsonNode item : content) {
                        if (!item.has("type")) {
                            continue;
                        }
                        String type = item.get("type").asText();
                        if ("video_url".equals(type)) {
                            JsonNode videoUrlNode = item.get("video_url");
                            if (videoUrlNode != null && videoUrlNode.has("url")) {
                                result.put("videoUrl", videoUrlNode.get("url").asText());
                            }
                        } else if ("image_url".equals(type) || "last_frame".equals(type) || "last_frame_image_url".equals(type)) {
                            JsonNode imageUrlNode = item.has("image_url") ? item.get("image_url") : item.get("last_frame_image_url");
                            if (imageUrlNode != null && imageUrlNode.has("url")) {
                                result.put("lastFrameUrl", imageUrlNode.get("url").asText());
                            }
                        }
                    }
                }

                JsonNode data = root.get("data");
                if (data != null) {
                    if (!result.containsKey("videoUrl") && data.has("video_url")) {
                        result.put("videoUrl", data.get("video_url").asText());
                    }
                    if (!result.containsKey("lastFrameUrl")) {
                        if (data.has("last_frame_url")) {
                            result.put("lastFrameUrl", data.get("last_frame_url").asText());
                        } else if (data.has("last_frame")) {
                            result.put("lastFrameUrl", data.get("last_frame").asText());
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
}
