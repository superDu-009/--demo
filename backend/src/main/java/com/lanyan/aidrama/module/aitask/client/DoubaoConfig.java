package com.lanyan.aidrama.module.aitask.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Doubao AI 客户端配置
 */
@Configuration
public class DoubaoConfig {

    /** API 地址 (OpenAI 兼容格式) */
    @Value("${doubao.api-url:https://ark.cn-beijing.volces.com/api/v3/chat/completions}")
    private String apiUrl;

    /** API Key */
    @Value("${doubao.api-key:}")
    private String apiKey;

    /** 默认模型 */
    @Value("${doubao.model:doubao-seed-2-0-pro-260215}")
    private String model;

    /** 请求超时（毫秒） */
    @Value("${doubao.timeout:120000}")
    private int timeout;

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Doubao 专用 RestTemplate Bean，配置超时
     */
    @Bean("doubaoRestTemplate")
    public RestTemplate doubaoRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }
}
