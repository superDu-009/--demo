package com.lanyan.aidrama.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应视图 (系分 v1.2 第 7.1 节)
 */
@Data
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "登录凭证 Token")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;
}
