-- ============================================
-- AI漫剧生产平台 数据库初始化脚本 v1.2
-- MySQL 8.0+
-- 共 10 张表: sys_user, project, asset, episode, scene, shot,
--            shot_asset_ref, workflow_task, ai_task, api_call_log
-- ============================================

CREATE DATABASE IF NOT EXISTS ai_drama
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_drama;

-- ============================================
-- 1. sys_user 用户表
-- ============================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    `nickname`    VARCHAR(50)  DEFAULT NULL  COMMENT '昵称',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. project 项目表
-- ============================================
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT        NOT NULL COMMENT '创建人ID',
    `name`            VARCHAR(200)  NOT NULL COMMENT '项目名称',
    `description`     TEXT          DEFAULT NULL  COMMENT '项目描述',
    `novel_tos_path`  VARCHAR(500)  DEFAULT NULL  COMMENT '小说文件TOS路径',
    `workflow_config` JSON          DEFAULT NULL  COMMENT '流程配置(JSON数组)',
    `style_preset`    JSON          DEFAULT NULL  COMMENT '全局风格预设(JSON)',
    `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿 1-进行中 2-已完成',
    `execution_lock`  TINYINT       NOT NULL DEFAULT 0 COMMENT '执行锁: 0-未执行 1-执行中(仅查询展示+恢复触发信号)',
    `version`         INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- ============================================
-- 3. asset 资产表
-- ============================================
DROP TABLE IF EXISTS `asset`;
CREATE TABLE `asset` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`       BIGINT       NOT NULL COMMENT '所属项目ID',
    `asset_type`       VARCHAR(20)  NOT NULL COMMENT '资产类型: character/scene/prop/voice',
    `name`             VARCHAR(100) NOT NULL COMMENT '资产名称',
    `description`      TEXT         DEFAULT NULL  COMMENT 'AI描述文本',
    `reference_images` JSON         DEFAULT NULL  COMMENT '参考图URL数组, 第一个为主图',
    `style_preset`     JSON         DEFAULT NULL  COMMENT '风格预设(JSON)',
    `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿 1-已确认 2-已废弃',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_asset_type` (`asset_type`),
    KEY `idx_project_type` (`project_id`, `asset_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产表';

-- ============================================
-- 4. episode 分集表
-- ============================================
DROP TABLE IF EXISTS `episode`;
CREATE TABLE `episode` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`  BIGINT       NOT NULL COMMENT '所属项目ID',
    `title`       VARCHAR(200) NOT NULL COMMENT '标题',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `content`     TEXT         DEFAULT NULL  COMMENT '分集剧本内容',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理 1-进行中 2-已完成',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    INDEX `idx_project_deleted_sort` (`project_id`, `deleted`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分集表';

-- ============================================
-- 5. scene 分场表
-- ============================================
DROP TABLE IF EXISTS `scene`;
CREATE TABLE `scene` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `episode_id`  BIGINT       NOT NULL COMMENT '所属分集ID',
    `title`       VARCHAR(200) NOT NULL COMMENT '标题',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `content`     TEXT         DEFAULT NULL  COMMENT '分场描述',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理 1-进行中 2-已完成',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_episode_id` (`episode_id`),
    INDEX `idx_episode_deleted_sort` (`episode_id`, `deleted`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分场表';

-- ============================================
-- 6. shot 分镜表
-- ============================================
DROP TABLE IF EXISTS `shot`;
CREATE TABLE `shot` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `scene_id`              BIGINT       NOT NULL COMMENT '所属分场ID',
    `sort_order`            INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `prompt`                TEXT         DEFAULT NULL  COMMENT 'AI生图提示词(中文)',
    `prompt_en`             TEXT         DEFAULT NULL  COMMENT 'AI生图提示词(英文)',
    `generated_image_url`   VARCHAR(500) DEFAULT NULL  COMMENT '生成图片URL(TOS)',
    `generated_video_url`   VARCHAR(500) DEFAULT NULL  COMMENT '生成视频URL(TOS)',
    `status`                TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理 1-生成中 2-待审核 3-已通过 4-已打回 5-已完成',
    `review_comment`        TEXT         DEFAULT NULL  COMMENT '审核意见',
    `version`               INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `generation_attempts`   INT          NOT NULL DEFAULT 0 COMMENT '生成尝试次数',
    `deleted`               TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_scene_id` (`scene_id`),
    KEY `idx_status` (`status`),
    INDEX `idx_scene_deleted_sort` (`scene_id`, `deleted`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分镜表';

-- ============================================
-- 7. shot_asset_ref 分镜-资产关联表
-- ============================================
DROP TABLE IF EXISTS `shot_asset_ref`;
CREATE TABLE `shot_asset_ref` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `shot_id`    BIGINT      NOT NULL COMMENT '分镜ID',
    `asset_id`   BIGINT      NOT NULL COMMENT '资产ID',
    `asset_type` VARCHAR(20) NOT NULL COMMENT '资产类型: character/scene/prop',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shot_asset` (`shot_id`, `asset_id`),
    KEY `idx_asset_id` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分镜-资产关联表';

-- ============================================
-- 8. workflow_task 流程任务表
-- ============================================
DROP TABLE IF EXISTS `workflow_task`;
CREATE TABLE `workflow_task` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`      BIGINT      NOT NULL COMMENT '所属项目ID',
    `episode_id`      BIGINT      DEFAULT NULL  COMMENT '当前处理的分集ID(可为空)',
    `step_type`       VARCHAR(50) NOT NULL COMMENT '步骤类型: import/asset_extract/shot_gen/image_gen/video_gen/export',
    `step_order`      INT         NOT NULL COMMENT '步骤顺序',
    `status`          TINYINT     NOT NULL DEFAULT 0 COMMENT '状态: 0-未执行 1-执行中 2-成功 3-失败 4-待审核',
    `sub_step`        VARCHAR(50) DEFAULT NULL  COMMENT '子步骤: submit/polling/download/upload_tos(用于断点续跑)',
    `input_data`      JSON        DEFAULT NULL  COMMENT '输入数据(JSON)',
    `output_data`     JSON        DEFAULT NULL  COMMENT '输出数据(JSON), 保存中间结果',
    `review_comment`  TEXT        DEFAULT NULL  COMMENT '审核意见',
    `error_msg`       TEXT        DEFAULT NULL  COMMENT '错误信息',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_step_order` (`step_order`),
    KEY `idx_status` (`status`),
    UNIQUE KEY `uk_project_step_episode` (`project_id`, `step_type`, `episode_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程任务表';

-- ============================================
-- 9. ai_task AI任务表
-- ============================================
DROP TABLE IF EXISTS `ai_task`;
CREATE TABLE `ai_task` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`       BIGINT       NOT NULL COMMENT '项目ID',
    `shot_id`          BIGINT       DEFAULT NULL  COMMENT '关联分镜ID',
    `task_type`        VARCHAR(20)  NOT NULL COMMENT '任务类型: image_gen/video_gen',
    `provider_task_id` VARCHAR(100) DEFAULT NULL  COMMENT '第三方API返回的任务ID',
    `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-提交中 1-处理中 2-成功 3-失败',
    `result_url`       VARCHAR(500) DEFAULT NULL  COMMENT '结果URL(TOS)',
    `error_msg`        TEXT         DEFAULT NULL  COMMENT '错误信息',
    `next_poll_time`   DATETIME     DEFAULT NULL  COMMENT '下次轮询时间(指数退避)',
    `poll_count`       INT          NOT NULL DEFAULT 0 COMMENT '已轮询次数',
    `last_poll_time`   DATETIME     DEFAULT NULL  COMMENT '上次轮询时间',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_shot_id` (`shot_id`),
    KEY `idx_provider_task_id` (`provider_task_id`),
    KEY `idx_status` (`status`),
    INDEX `idx_status_next_poll` (`status`, `next_poll_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI任务表';

-- ============================================
-- 10. api_call_log API调用记录表
-- ============================================
DROP TABLE IF EXISTS `api_call_log`;
CREATE TABLE `api_call_log` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        BIGINT         NOT NULL COMMENT '用户ID',
    `project_id`     BIGINT         DEFAULT NULL  COMMENT '项目ID',
    `api_provider`   VARCHAR(50)    NOT NULL COMMENT '提供商',
    `api_endpoint`   VARCHAR(200)   NOT NULL COMMENT '接口名称',
    `request_params` JSON           DEFAULT NULL  COMMENT '请求摘要(脱敏)',
    `token_usage`    INT            DEFAULT 0     COMMENT 'Token消耗',
    `cost`           DECIMAL(10,4)  DEFAULT 0.0000 COMMENT '费用(元)',
    `status`         TINYINT        NOT NULL DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
    `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_provider` (`api_provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用记录表';

-- ============================================
-- 初始化测试用户数据
-- ============================================
-- 用户名: admin, 密码: 123456 (BCrypt加密, strength=12)
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `status`, `deleted`)
VALUES (1, 'admin', '$2b$12$8j4MzrG3ZDiVQra98hZFbuOl/w8RVftFoxPyBIw1Bzq1CmGFG8/Iq', '管理员', 1, 0);
