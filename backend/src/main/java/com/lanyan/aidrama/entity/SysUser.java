package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类 (对应 sys_user 表，系分 3. DDL 第1张表)
 * 用于存储用户认证信息
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 (唯一) */
    private String username;

    /** BCrypt加密密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 逻辑删除: 0-正常 1-删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
