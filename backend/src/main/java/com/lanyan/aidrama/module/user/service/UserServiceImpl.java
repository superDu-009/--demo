package com.lanyan.aidrama.module.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.entity.SysUser;
import com.lanyan.aidrama.mapper.SysUserMapper;
import com.lanyan.aidrama.module.user.dto.LoginRequest;
import com.lanyan.aidrama.module.user.dto.LoginVO;
import com.lanyan.aidrama.module.user.dto.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类 (系分 v1.2 第 7.1 节)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    /** 连续输错密码次数阈值 */
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    /** 锁定时间（分钟） */
    private static final int LOCK_MINUTES = 10;
    /** Redis 前缀：登录失败次数 */
    private static final String LOGIN_FAIL_COUNT_KEY = "login:fail:count:";
    /** Redis 前缀：锁定标记 */
    private static final String LOGIN_LOCK_KEY = "login:lock:";

    @Override
    public LoginVO login(LoginRequest req) {
        // 1. 检查是否被锁定
        String lockKey = LOGIN_LOCK_KEY + req.getUsername();
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            throw new BusinessException(40102, "登录失败次数过多，请" + LOCK_MINUTES + "分钟后再试");
        }

        // 2. 根据用户名查询用户
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, req.getUsername());
        SysUser user = sysUserMapper.selectOne(queryWrapper);

        // 3. 校验用户是否存在
        if (user == null) {
            recordLoginFail(req.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_FAIL);
        }

        // 4. 校验密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            recordLoginFail(req.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_FAIL);
        }

        // 5. 登录成功，清除失败计数
        clearLoginFail(req.getUsername());

        // 6. 调用 Sa-Token 进行登录注册
        StpUtil.login(user.getId());

        // 7. 构建 LoginVO
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(StpUtil.getTokenValue());
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());

        return loginVO;
    }

    @Override
    public void logout() {
        StpUtil.logout();
        log.info("用户登出成功, userId: {}", StpUtil.getLoginIdAsLong());
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setStatus(user.getStatus());
        return vo;
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 校验旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(40006, "旧密码不正确");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        log.info("用户修改密码成功, userId: {}", userId);
    }

    @Override
    public void changeUsername(String newUsername) {
        // 校验用户名唯一性
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, newUsername);
        if (sysUserMapper.selectOne(queryWrapper) != null) {
            throw new BusinessException(40007, "用户名已存在");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        user.setUsername(newUsername);
        sysUserMapper.updateById(user);
        log.info("用户修改用户名成功, userId: {}, newUsername: {}", userId, newUsername);
    }

    @Override
    public void updateAvatar(String avatarUrl) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        user.setAvatarUrl(avatarUrl);
        sysUserMapper.updateById(user);
        log.info("用户更新头像成功, userId: {}", userId);
    }

    /**
     * 记录登录失败次数，达到阈值则锁定
     */
    private void recordLoginFail(String username) {
        String failKey = LOGIN_FAIL_COUNT_KEY + username;
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count >= MAX_LOGIN_FAIL_COUNT) {
            // 设置锁定标记，有效期 10 分钟
            stringRedisTemplate.opsForValue().set(
                    LOGIN_LOCK_KEY + username, "1", LOCK_MINUTES, TimeUnit.MINUTES);
            stringRedisTemplate.delete(failKey);
        } else if (count != null) {
            // 设置失败计数过期时间 10 分钟
            stringRedisTemplate.expire(failKey, LOCK_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * 登录成功后清除失败计数
     */
    private void clearLoginFail(String username) {
        stringRedisTemplate.delete(LOGIN_FAIL_COUNT_KEY + username);
        stringRedisTemplate.delete(LOGIN_LOCK_KEY + username);
    }
}
