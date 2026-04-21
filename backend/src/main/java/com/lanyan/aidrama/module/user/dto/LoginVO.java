package com.lanyan.aidrama.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应视图 (系分 4.1.1 登录接口响应)
 * 封装 Token 和用户基本信息返回给前端
 */
@Data
public class LoginVO {

    /** 登录凭证 Token */
    @Schema(description = "登录凭证 Token")
    private String token;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 用户名 */
    @Schema(description = "用户名")
    private String username;

    /** 昵称 */
    @Schema(description = "昵称")
    private String nickname;
}
