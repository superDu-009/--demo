package com.lanyan.aidrama.module.user.controller;

import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.user.dto.LoginRequest;
import com.lanyan.aidrama.module.user.dto.LoginVO;
import com.lanyan.aidrama.module.user.dto.UserInfoVO;
import com.lanyan.aidrama.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户模块 Controller (系分 4.1.1)
 * 提供登录、登出、获取用户信息接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户模块", description = "登录、注册等")
public class UserController {

    private final UserService userService;

    /**
     * 用户登录接口 (POST /api/user/login)
     * 接收 JSON 参数 (username, password)，返回统一格式 Result<LoginVO>
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "校验用户名密码，成功后返回 Token 和用户信息")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest req) {
        LoginVO loginVO = userService.login(req);
        return Result.ok(loginVO);
    }

    /**
     * 用户登出接口 (POST /api/user/logout)
     * 需要登录鉴权，调用 Sa-Token 登出清除 Redis 中的 Token
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "清除登录态，退出登录")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok(null);
    }
}
