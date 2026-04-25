package com.lanyan.aidrama.module.user.service;

import com.lanyan.aidrama.module.user.dto.LoginRequest;
import com.lanyan.aidrama.module.user.dto.LoginVO;
import com.lanyan.aidrama.module.user.dto.UserInfoVO;

/**
 * 用户服务接口 (系分 v1.2 第 7.1 节)
 */
public interface UserService {

    /**
     * 用户登录
     */
    LoginVO login(LoginRequest req);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 获取当前登录用户信息
     */
    UserInfoVO getCurrentUserInfo();

    /**
     * 修改密码
     */
    void changePassword(String oldPassword, String newPassword);

    /**
     * 修改用户名
     */
    void changeUsername(String newUsername);

    /**
     * 更新头像
     */
    void updateAvatar(String avatarUrl);
}
