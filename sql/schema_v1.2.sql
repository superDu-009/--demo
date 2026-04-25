-- AI漫剧生产平台 数据库建表脚本 v1.2
-- 基于 DESIGN_BACKEND_老克_v1.2.md
-- 创建日期: 2026-04-25

CREATE DATABASE IF NOT EXISTS ai_drama DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_drama;

-- ============================================================
-- 1. sys_user 用户表
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt加密密码',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. project 项目表
-- ============================================================
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '创建人ID',
    `name` VARCHAR(128) NOT NULL COMMENT '项目名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '项目描述',
    `novel_original_tos_path` VARCHAR(512) DEFAULT NULL COMMENT '原始小说文件TOS路径',
    `novel_tos_path` VARCHAR(512) DEFAULT NULL COMMENT '解析后纯文本TOS路径',
    `ratio` VARCHAR(16) DEFAULT '16:9' COMMENT '画面比例: 16:9 / 9:16',
    `definition` VARCHAR(16) DEFAULT '1080P' COMMENT '清晰度: 720P / 1080P',
    `style` VARCHAR(32) DEFAULT NULL COMMENT '风格: 2D次元风/日漫风/国漫风/古风/现代写实/自定义',
    `style_desc` VARCHAR(512) DEFAULT NULL COMMENT '风格描述（自定义风格时使用）',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- ============================================================
-- 3. episode 分集表
-- ============================================================
DROP TABLE IF EXISTS `episode`;
CREATE TABLE `episode` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` BIGINT NOT NULL COMMENT '所属项目ID',
    `title` VARCHAR(128) DEFAULT NULL COMMENT '标题',
    `summary` VARCHAR(1024) DEFAULT NULL COMMENT '分集摘要',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `content` TEXT DEFAULT NULL COMMENT '分集剧本内容',
    `asset_ids` JSON DEFAULT NULL COMMENT '关联资产ID数组',
    `parse_status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '解析状态: pending/analyzing/success/failed',
    `parse_error` VARCHAR(512) DEFAULT NULL COMMENT '解析失败原因',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分集表';

-- ============================================================
-- 4. shot 分镜表
-- ============================================================
DROP TABLE IF EXISTS `shot`;
CREATE TABLE `shot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `episode_id` BIGINT NOT NULL COMMENT '所属分集ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `prompt` TEXT DEFAULT NULL COMMENT 'AI提示词(中文)',
    `prompt_en` TEXT DEFAULT NULL COMMENT 'AI提示词(英文)',
    `duration` INT DEFAULT NULL COMMENT '时长(秒): 10/12/15',
    `scene_type` VARCHAR(32) DEFAULT NULL COMMENT '场景类型',
    `camera_move` VARCHAR(32) DEFAULT NULL COMMENT '运镜方式',
    `lines` JSON DEFAULT NULL COMMENT '台词数组',
    `generated_image_url` VARCHAR(512) DEFAULT NULL COMMENT '生成图片URL',
    `generated_video_url` VARCHAR(512) DEFAULT NULL COMMENT '生成视频URL',
    `last_frame_url` VARCHAR(512) DEFAULT NULL COMMENT '尾帧图片URL',
    `follow_last` TINYINT NOT NULL DEFAULT 1 COMMENT '是否承接上一分镜: 0-否 1-是',
    `draft_content` JSON DEFAULT NULL COMMENT '草稿内容',
    `prompt_status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '提示词状态: pending/generating/success/failed',
    `image_status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '图片状态: pending/generating/success/failed',
    `video_status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '视频状态: pending/generating/success/failed',
    `error_msg` VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_episode_id` (`episode_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分镜表';

-- ============================================================
-- 5. asset 资产表
-- ============================================================
DROP TABLE IF EXISTS `asset`;
CREATE TABLE `asset` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` BIGINT NOT NULL COMMENT '所属项目ID',
    `asset_type` VARCHAR(32) NOT NULL COMMENT '资产类型: character/scene/prop/voice',
    `name` VARCHAR(128) NOT NULL COMMENT '资产名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT 'AI描述文本',
    `reference_images` JSON DEFAULT NULL COMMENT '参考图URL数组',
    `parent_ids` JSON DEFAULT NULL COMMENT '父资产ID数组（多对多）',
    `is_sub_asset` TINYINT NOT NULL DEFAULT 0 COMMENT '是否子资产: 0-否 1-是',
    `draft_content` JSON DEFAULT NULL COMMENT '草稿内容',
    `status` VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/confirmed/deprecated',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产表';

-- ============================================================
-- 6. shot_asset_ref 分镜资产关联表
-- ============================================================
DROP TABLE IF EXISTS `shot_asset_ref`;
CREATE TABLE `shot_asset_ref` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `shot_id` BIGINT NOT NULL COMMENT '分镜ID',
    `asset_id` BIGINT NOT NULL COMMENT '资产ID',
    `asset_type` VARCHAR(32) NOT NULL COMMENT '资产类型',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_shot_id` (`shot_id`),
    KEY `idx_asset_id` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分镜资产关联表';

-- ============================================================
-- 7. task 统一任务表（替代旧 ai_task）
-- ============================================================
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `type` VARCHAR(32) NOT NULL COMMENT '任务类型: script_analyze/shot_split/asset_extract/prompt_gen/image_gen/video_gen',
    `project_id` BIGINT DEFAULT NULL COMMENT '所属项目ID',
    `episode_id` BIGINT DEFAULT NULL COMMENT '所属分集ID',
    `shot_id` BIGINT DEFAULT NULL COMMENT '所属分镜ID',
    `batch_id` VARCHAR(64) DEFAULT NULL COMMENT '批量ID（UUID）',
    `provider_task_id` VARCHAR(128) DEFAULT NULL COMMENT '第三方API返回的任务ID',
    `input_data` JSON DEFAULT NULL COMMENT '输入数据(JSON)',
    `result_data` JSON DEFAULT NULL COMMENT '结果数据(JSON)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待处理 1-处理中 2-成功 3-失败',
    `result_url` VARCHAR(512) DEFAULT NULL COMMENT '结果URL',
    `error_msg` VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    `progress` INT DEFAULT 0 COMMENT '进度百分比 0-100',
    `next_poll_time` DATETIME DEFAULT NULL COMMENT '下次轮询时间',
    `poll_count` INT NOT NULL DEFAULT 0 COMMENT '已轮询次数',
    `last_poll_time` DATETIME DEFAULT NULL COMMENT '上次轮询时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_episode_id` (`episode_id`),
    KEY `idx_shot_id` (`shot_id`),
    KEY `idx_batch_id` (`batch_id`),
    KEY `idx_status_next_poll` (`status`, `next_poll_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一任务表';

-- ============================================================
-- 8. prompt_config Prompt配置表
-- ============================================================
DROP TABLE IF EXISTS `prompt_config`;
CREATE TABLE `prompt_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `prompt_key` VARCHAR(64) NOT NULL COMMENT 'Prompt标识',
    `prompt_text` TEXT NOT NULL COMMENT 'Prompt内容',
    `model` VARCHAR(64) DEFAULT NULL COMMENT '适用模型',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '描述',
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_prompt_key` (`prompt_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt配置表';

-- ============================================================
-- 初始化 prompt_config 数据
-- ============================================================
INSERT INTO `prompt_config` (`prompt_key`, `prompt_text`, `model`, `description`, `version`) VALUES
('script_split', '你是一个专业的剧本拆分专家。请将以下小说内容拆分为多个分集。每个分集需要有完整的剧情段落，适合制作成漫剧。返回JSON数组格式，每个分集包含：title(分集标题), summary(分集摘要), content(分集正文)。只输出JSON数组，不要其他文字。', 'doubao-seed-2-0-pro-260215', '剧本分析-分集拆分', 1),
('shot_split', '你是一个专业的分镜拆分专家。请将以下分集内容按标准时长（{duration}秒）拆分为多个分镜。每个分镜包含：prompt(中文提示词), sceneType(场景类型), cameraMove(运镜方式), duration(时长), lines(台词)。只输出JSON数组，不要其他文字。', 'doubao-seed-2-0-pro-260215', '分镜拆分', 1),
('asset_extract', '你是一个专业的资产提取专家。请从以下分集文本中提取所有角色(character)、场景(scene)、物品(prop)资产。每个资产包含：name(名称), type(类型), description(描述)。只输出JSON数组，不要其他文字。', 'doubao-seed-2-0-pro-260215', '资产提取', 1),
('prompt_gen', '你是一个专业的AI绘画提示词生成专家。请根据以下分镜描述生成英文的图片生成提示词（prompt），要求描述清晰、具体、适合AI绘画。只输出英文提示词，不要其他文字。', 'doubao-seed-2-0-pro-260215', '分镜提示词生成', 1);
