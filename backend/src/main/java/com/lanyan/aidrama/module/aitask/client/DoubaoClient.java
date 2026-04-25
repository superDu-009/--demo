package com.lanyan.aidrama.module.aitask.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Doubao 对话客户端 (火山引擎方舟 Ark)
 * 使用统一的 ArkApiClient 发送请求，专注请求体构建和响应解析
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoubaoClient {

    private final DoubaoConfig doubaoConfig;
    private final ArkApiClient arkApiClient;

    /**
     * 调用 Doubao 对话模型，生成文本结果
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return AI 回复内容
     */
    public String chat(String systemPrompt, String userPrompt) {
        String model = doubaoConfig.getModel();
        log.info("调用 Doubao 模型, model: {}, systemPrompt length: {}, userPrompt length: {}",
                model,
                systemPrompt != null ? systemPrompt.length() : 0,
                userPrompt != null ? userPrompt.length() : 0);

        ObjectNode requestBody = arkApiClient.getObjectMapper().createObjectNode();
        requestBody.put("model", model);

        ArrayNode messages = requestBody.putArray("messages");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
        }
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);

        String response = arkApiClient.post(arkApiClient.toJson(requestBody), "Doubao 对话");
        return parseContent(response);
    }

    /**
     * 解析 API 响应，提取 AI 回复内容
     */
    private String parseContent(String responseBody) {
        try {
            JsonNode root = arkApiClient.getObjectMapper().readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            log.warn("Doubao 响应无 choices 字段, body: {}", responseBody);
            return "";
        } catch (Exception e) {
            log.error("解析 Doubao 响应失败, body: {}", responseBody, e);
            throw new RuntimeException("解析 AI 响应失败", e);
        }
    }
}
