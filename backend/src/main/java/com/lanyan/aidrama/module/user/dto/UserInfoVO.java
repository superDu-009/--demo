package com.lanyan.aidrama.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户信息响应视图 (系分 4.1.1 GET /api/user/info 响应)
 */
@Data
public class UserInfoVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "状态: 0-禁用 1-启用")
    private Integer status;
}
