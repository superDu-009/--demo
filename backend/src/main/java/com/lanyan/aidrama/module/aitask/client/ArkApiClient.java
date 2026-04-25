package com.lanyan.aidrama.module.aitask.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 统一的 Ark API 客户端 (火山引擎方舟)
 * 封装 Bearer Auth、重试、错误处理、JSON 序列化
 * DoubaoClient / ImageGenClient / VideoGenClient 复用此类发送 HTTP 请求
 * 重试策略：最多重试 1 次（符合重试规范）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArkApiClient {

    private final DoubaoConfig doubaoConfig;
    private final @Qualifier("doubaoRestTemplate") RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发送 POST 请求到 Ark API
     */
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 2,
            backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    public String post(String requestBodyJson, String apiName) {
        String url = doubaoConfig.getApiUrl();
        return postWithUrl(requestBodyJson, apiName, url);
    }

    /**
     * 发送 POST 请求到指定 Ark API URL
     */
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 2,
            backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    public String postToUrl(String requestBodyJson, String apiName, String url) {
        return postWithUrl(requestBodyJson, apiName, url);
    }

    /**
     * 发送 GET 请求到指定 URL
     */
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 2,
            backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    public String get(String url, String apiName) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException(apiName + " 返回非 200 状态: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException e) {
            log.error("{} 调用异常, url: {}", apiName, url, e);
            throw e;
        }
    }

    private String postWithUrl(String requestBodyJson, String apiName, String url) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException(apiName + " 返回非 200 状态: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException e) {
            log.error("{} 调用异常, url: {}", apiName, url, e);
            throw e;
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(doubaoConfig.getApiKey());
        return headers;
    }

    /**
     * 获取内部 ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 将对象序列化为 JSON 字符串
     */
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }
}
