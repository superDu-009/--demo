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

import java.util.concurrent.TimeoutException;

/**
 * Doubao AI 客户端 (火山引擎方舟 Ark)
 * 兼容 OpenAI Chat Completions API 格式
 * 默认模型: doubao-seed-2-0-pro-260215
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DoubaoClient {

    private final DoubaoConfig doubaoConfig;
    private final @Qualifier("doubaoRestTemplate") RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 调用 Doubao 对话模型，生成文本结果
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return AI 回复内容
     */
    @Retryable(
            value = {RestClientException.class, TimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public String chat(String systemPrompt, String userPrompt) {
        String apiUrl = doubaoConfig.getApiUrl();
        String model = doubaoConfig.getModel();
        String apiKey = doubaoConfig.getApiKey();

        log.info("调用 Doubao 模型, model: {}, systemPrompt length: {}, userPrompt length: {}",
                model,
                systemPrompt != null ? systemPrompt.length() : 0,
                userPrompt != null ? userPrompt.length() : 0);

        // 构建请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Doubao API 返回非 200 状态: " + response.getStatusCode());
            }

            return parseContent(response.getBody());
        } catch (RestClientException e) {
            log.error("Doubao API 调用异常, model: {}", model, e);
            throw e;
        }
    }

    /**
     * 调用 Doubao 翻译提示词（中文 → 英文）
     */
    public String translatePrompt(String promptCn) {
        String systemPrompt = "You are a professional translator. Translate the following Chinese prompt into English. " +
                "Only output the English translation, nothing else.";
        return chat(systemPrompt, promptCn);
    }

    /**
     * 调用 Doubao 解析小说章节
     * @param novelContent 小说内容
     * @return JSON 格式的章节列表，格式: [{"title": "第一章", "content": "...", "sortOrder": 0}]
     */
    public String parseNovelChapters(String novelContent) {
        String systemPrompt = "你是一个小说解析助手。请将以下小说内容按章节拆分，返回 JSON 数组格式。\n" +
                "每个章节对象包含: title(章节标题), content(章节正文内容), sortOrder(从0开始的序号)。\n" +
                "只输出 JSON 数组，不要输出任何其他文字。";
        return chat(systemPrompt, novelContent);
    }

    /**
     * 调用 Doubao 从分集内容中提取资产
     * @param episodeContent 分集内容
     * @return JSON 格式的资产列表，格式: [{"name": "角色名/场景名", "type": "character/scene/prop", "description": "描述"}]
     */
    public String extractAssets(String episodeContent) {
        String systemPrompt = "你是一个资产提取助手。请从以下文本中提取所有角色(character)、场景(scene)、物品(prop)。\n" +
                "返回 JSON 数组格式，每个对象包含: name(名称), type(character/scene/prop), description(简短描述)。\n" +
                "只输出 JSON 数组，不要输出任何其他文字。";
        return chat(systemPrompt, episodeContent);
    }

    // ============ 内部方法 ============

    /**
     * 解析 API 响应，提取 AI 回复内容
     */
    private String parseContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            // 兼容无 choices 的异常格式
            log.warn("Doubao 响应无 choices 字段, body: {}", responseBody);
            return "";
        } catch (Exception e) {
            log.error("解析 Doubao 响应失败, body: {}", responseBody, e);
            throw new RuntimeException("解析 AI 响应失败", e);
        }
    }

    /**
     * 防止 API Key 意外泄露到日志
     */
    @Override
    public String toString() {
        return "DoubaoClient{apiUrl='" + doubaoConfig.getApiUrl() + "', apiKey='***', model='" + doubaoConfig.getModel() + "'}";
    }
}
