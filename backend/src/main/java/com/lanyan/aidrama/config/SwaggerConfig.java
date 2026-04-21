package com.lanyan.aidrama.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / Swagger API 文档配置 (系分 2.7)
 * MVP 阶段保持轻量，自动生成交互式 API 文档
 *
 * 访问路径:
 *   - Swagger UI: /swagger-ui.html
 *   - OpenAPI JSON: /v3/api-docs
 *
 * 生产环境通过 application-prod.yml 关闭
 */
@Configuration
public class SwaggerConfig {

    /**
     * 自定义 OpenAPI 配置
     * 按模块分组，便于前端查阅
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI漫剧生产平台 API")
                        .version("v1.0")
                        .description("AI漫剧生产平台后端接口文档"))
                // 按模块分组，便于前端查阅
                .addTagsItem(new Tag().name("用户模块").description("登录、注册等"))
                .addTagsItem(new Tag().name("项目模块").description("项目 CRUD、工作流操作"))
                .addTagsItem(new Tag().name("资产模块").description("资产 CRUD、确认、引用查询"))
                .addTagsItem(new Tag().name("内容模块").description("分集、分场、分镜 CRUD"))
                .addTagsItem(new Tag().name("流程引擎").description("工作流启动、状态查询、停止"))
                .addTagsItem(new Tag().name("AI任务").description("AI 任务提交、状态查询"))
                .addTagsItem(new Tag().name("存储模块").description("TOS 预签名 URL、上传通知"));
    }
}
