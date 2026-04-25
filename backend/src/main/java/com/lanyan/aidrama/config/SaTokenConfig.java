package com.lanyan.aidrama.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 鉴权配置 (系分 2.4)
 * v1.5 修正: 仅排除 /api/user/login 和 /error
 * 注意: /api/tos/presign 不再排除，需要登录鉴权
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * CORS 跨域配置
     * 允许前端开发服务器 (localhost:5173 / 192.168.3.9:5173) 跨域访问后端接口
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(java.util.List.of("*"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，对所有路径进行鉴权校验
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 匹配所有路径，排除登录接口、错误页面、Swagger文档相关路径
            SaRouter.match("/**")
                    .notMatch("/api/user/login")  // 登录接口无需鉴权
                    .notMatch("/error")            // SpringBoot 错误页面
                    .notMatch("/swagger-ui.html")  // Swagger UI 页面
                    .notMatch("/v3/api-docs/**")   // OpenAPI 文档接口
                    .notMatch("/swagger-ui/**")    // Swagger UI 静态资源
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
