package com.lanyan.aidrama.module.user.controller;

import com.lanyan.aidrama.common.Result;
import com.lanyan.aidrama.module.user.dto.LoginRequest;
import com.lanyan.aidrama.module.user.dto.LoginVO;
import com.lanyan.aidrama.module.user.dto.UserInfoVO;
import com.lanyan.aidrama.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户模块 Controller (系分 v1.2 第 7.1 节)
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户模块", description = "登录/登出/用户信息管理")
public class UserController {

    private final UserService userService;

    /**
     * 用户登录接口 (POST /api/user/login)
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "校验用户名密码，连续输错5次锁定10分钟")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(userService.login(req));
    }

    /**
     * 用户登出接口 (POST /api/user/logout)
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "清除登录态")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok(null);
    }

    /**
     * 获取当前用户信息 (GET /api/user/info)
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "返回登录用户的基本信息")
    public Result<UserInfoVO> getUserInfo() {
        return Result.ok(userService.getCurrentUserInfo());
    }

    /**
     * 修改密码 (PUT /api/user/password)
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "校验旧密码后修改为新密码")
    public Result<Void> changePassword(@RequestParam String oldPassword,
                                        @RequestParam String newPassword) {
        userService.changePassword(oldPassword, newPassword);
        return Result.ok(null);
    }

    /**
     * 修改用户名 (PUT /api/user/username)
     */
    @PutMapping("/username")
    @Operation(summary = "修改用户名", description = "修改用户名，需保证唯一性")
    public Result<Void> changeUsername(@RequestParam String newUsername) {
        userService.changeUsername(newUsername);
        return Result.ok(null);
    }

    /**
     * 更新头像 (PUT /api/user/avatar)
     */
    @PutMapping("/avatar")
    @Operation(summary = "更新头像", description = "更新用户头像URL")
    public Result<Void> updateAvatar(@RequestParam String avatarUrl) {
        userService.updateAvatar(avatarUrl);
        return Result.ok(null);
    }
}
