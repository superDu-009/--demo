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

    /** 对话 API 地址 */
    @Value("${doubao.api-url:https://ark.cn-beijing.volces.com/api/v3/chat/completions}")
    private String apiUrl;

    /** 图片生成 API 地址 */
    @Value("${doubao.image-api-url:https://ark.cn-beijing.volces.com/api/v3/images/generations}")
    private String imageApiUrl;

    /** 视频生成 API 地址 */
    @Value("${doubao.video-api-url:https://ark.cn-beijing.volces.com/api/v3/content/generation/tasks}")
    private String videoApiUrl;

    /** API Key */
    @Value("${doubao.api-key:}")
    private String apiKey;

    /** 对话模型 */
    @Value("${doubao.model:doubao-seed-2-0-pro-260215}")
    private String model;

    /** 图片生成模型 */
    @Value("${doubao.image-model:doubao-seedream-5-0-260128}")
    private String imageModel;

    /** 视频生成模型 */
    @Value("${doubao.video-model:doubao-seedance-2-0-260128}")
    private String videoModel;

    /** 请求超时（毫秒） */
    @Value("${doubao.timeout:120000}")
    private int timeout;

    public String getApiUrl() {
        return apiUrl;
    }

    public String getImageApiUrl() {
        return imageApiUrl;
    }

    public String getVideoApiUrl() {
        return videoApiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getImageModel() {
        return imageModel;
    }

    public String getVideoModel() {
        return videoModel;
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
