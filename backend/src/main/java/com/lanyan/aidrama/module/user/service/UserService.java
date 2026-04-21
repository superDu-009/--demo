package com.lanyan.aidrama.module.user.service;

import com.lanyan.aidrama.module.user.dto.LoginRequest;
import com.lanyan.aidrama.module.user.dto.LoginVO;

/**
 * 用户服务接口 (系分 4.1.2)
 * 负责用户认证（登录/登出/信息获取）
 */
public interface UserService {

    /**
     * 用户登录 (系分 4.1.2)
     * 校验用户名密码，BCrypt 比对，成功后 StpUtil.login(userId) 返回 Token
     * @param req 登录请求参数
     * @return LoginVO 包含 Token 和用户基本信息
     */
    LoginVO login(LoginRequest req);

    /**
     * 用户登出 (系分 4.1.2)
     * 调用 StpUtil.logout() 清除登录态
     */
    void logout();
}
