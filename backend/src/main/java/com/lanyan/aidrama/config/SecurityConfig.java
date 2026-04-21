package com.lanyan.aidrama.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全配置类 (系分 10.2 密码加密)
 * 注册 BCryptPasswordEncoder Bean，用于登录密码校验和初始化数据加密
 * strength=12 确保密码加密强度足够高
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt 密码编码器 (系分 10.2)
     * strength=12，用于用户密码的加密存储和登录时的 BCrypt 比对
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
