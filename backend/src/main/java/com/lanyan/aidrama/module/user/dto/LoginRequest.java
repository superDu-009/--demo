package com.lanyan.aidrama.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 登录请求参数 (系分 4.1.1)
 */
@Data
public class LoginRequest {

    /** 用户名 (必填) */
    @NotBlank(message = "用户名不能为空")
    @Length(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    /** 密码 (必填) */
    @NotBlank(message = "密码不能为空")
    private String password;
}
