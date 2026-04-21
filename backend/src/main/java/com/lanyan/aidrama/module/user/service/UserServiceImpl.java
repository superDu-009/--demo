package com.lanyan.aidrama.module.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.entity.SysUser;
import com.lanyan.aidrama.mapper.SysUserMapper;
import com.lanyan.aidrama.module.user.dto.LoginRequest;
import com.lanyan.aidrama.module.user.dto.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类 (系分 4.1.2)
 * 实现登录/登出逻辑，使用 BCrypt 密码校验 + Sa-Token 管理登录态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginRequest req) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, req.getUsername());
        SysUser user = sysUserMapper.selectOne(queryWrapper);

        // 2. 校验用户是否存在
        if (user == null) {
            // 用户名不存在，返回登录失败错误码 40001
            throw new BusinessException(ErrorCode.LOGIN_FAIL);
        }

        // 3. 校验密码：使用 BCryptPasswordEncoder 进行密码匹配
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // 密码不匹配，返回登录失败错误码 40001
            throw new BusinessException(ErrorCode.LOGIN_FAIL);
        }

        // 4. 校验通过：调用 StpUtil.login 进行 Sa-Token 登录注册
        StpUtil.login(user.getId());

        // 5. 构建 LoginVO 返回 Token 和用户基本信息
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(StpUtil.getTokenValue());
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());

        return loginVO;
    }

    @Override
    public void logout() {
        // 调用 Sa-Token 登出，清除 Redis 中的 Token 信息
        StpUtil.logout();
    }
}
