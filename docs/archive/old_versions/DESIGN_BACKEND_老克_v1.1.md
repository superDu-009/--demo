# AI漫剧生产平台 — 后端系统设计规格书 (Backend Design Spec)

## 文档信息

| 项目 | 内容 |
|------|------|
| 产品名 | AI漫剧生产平台 |
| 版本 | v1.0 |
| 基于 | PRD v1.1 MVP 终版 |
| 创建日期 | 2026-04-19 |
| 状态 | 待评审 |

---

## 目录

- [1. 技术栈与基础配置](#1-技术栈与基础配置)
- [2. 全局约定](#2-全局约定)
  - [2.1 统一响应格式](#21-统一响应格式)
  - [2.2 错误码规范](#22-错误码规范)
  - [2.3 Sa-Token 鉴权约定](#23-sa-token-鉴权约定)
  - [2.4 JSON 字段 TypeHandler 配置](#24-json-字段-typehandler-配置)
- [3. 数据库 DDL](#3-数据库-ddl)
- [4. 模块详细设计](#4-模块详细设计)
  - [4.1 用户模块 (user)](#41-用户模块-user)
  - [4.2 项目模块 (project)](#42-项目模块-project)
  - [4.3 资产模块 (asset)](#43-资产模块-asset)
  - [4.4 内容模块 (content)](#44-内容模块-content)
  - [4.5 流程引擎模块 (workflow)](#45-流程引擎模块-workflow)
  - [4.6 AI任务模块 (aitask)](#46-ai任务模块-aitask)
  - [4.7 存储模块 (storage)](#47-存储模块-storage)
- [5. 流程引擎详细设计](#5-流程引擎详细设计)
- [6. 异步任务与线程池](#6-异步任务与线程池)
- [7. Redis 使用规范](#7-redis-使用规范)
- [8. AI 一致性保障](#8-ai-一致性保障)
- [9. 进度追踪清单](#9-进度追踪清单)

---

## 1. 技术栈与基础配置

| 层 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17+ |
| 框架 | Spring Boot | 3.2.x |
| ORM | MyBatis Plus | 3.5.5+ |
| 认证 | Sa-Token (spring-boot3-starter) | 1.37+ |
| 缓存 | Redis + Lettuce | Spring Boot 内置 |
| 数据库 | MySQL | 8.0+ |
| 存储 | 火山 TOS (AWS S3 兼容 SDK) | 最新 |
| 视频处理 | FFmpeg | 系统命令调用 |
| JSON | Jackson | Spring Boot 内置 |
| 构建 | Maven | 3.8+ |

### pom.xml 核心依赖

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MyBatis Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.5</version>
    </dependency>

    <!-- Sa-Token (Spring Boot 3) -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.37.0</version>
    </dependency>
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
        <version>1.37.0</version>
    </dependency>

    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- AWS S3 SDK (火山TOS兼容) -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <version>2.25.0</version>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 2. 全局约定

### 2.1 统一响应格式

所有接口返回统一使用 `Result<T>` 包装：

```java
@Data
public class Result<T> {
    private int code;       // 业务状态码，0 表示成功
    private String message; // 提示信息
    private T data;         // 业务数据
    private long timestamp; // 时间戳

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMessage("success");
        r.setData(data);
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }
}
```

**成功响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": { "id": 1, "name": "示例项目" },
  "timestamp": 1713500000000
}
```

**失败响应示例：**
```json
{
  "code": 40001,
  "message": "用户名或密码错误",
  "data": null,
  "timestamp": 1713500000000
}
```

### 2.2 错误码规范

| 错误码范围 | 模块 | 示例 |
|-----------|------|------|
| `0` | 成功 | — |
| `40000-40099` | 参数校验 | `40001` 用户名或密码错误 |
| `40100-40199` | 认证鉴权 | `40100` 未登录, `40101` Token过期 |
| `40300-40399` | 权限不足 | `40300` 无操作权限 |
| `40400-40499` | 资源不存在 | `40400` 项目不存在 |
| `40900-40999` | 业务冲突 | `40900` 项目正在执行中 |
| `50000-50099` | 系统异常 | `50000` 服务器内部错误 |
| `51000-51099` | AI服务异常 | `51000` AI调用失败 |
| `51100-51199` | 存储异常 | `51100` TOS上传失败 |

### 2.3 Sa-Token 鉴权约定

```java
// SaTokenConfig.java
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 白名单：登录相关 + 预签名URL
            SaRouter.match("/**")
                .notMatch("/api/user/login")
                .notMatch("/api/tos/presign")
                .notMatch("/error")
                .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }

    @Bean
    public SaTokenConfig saTokenConfig() {
        return new SaTokenConfig()
            .setTokenName("Authorization")          // 请求头名称
            .setTokenStyle("Bearer {token}")        // 前端传参格式
            .setTimeout(86400)                       // Token有效期 24h
            .setActiveTimeout(1800)                  // 活跃超时 30min
            .setIsConcurrent(true)                   // 允许同端并发登录
            .setTokenPrefix("Bearer");
    }
}
```

**前端请求约定：**
- 请求头: `Authorization: Bearer {token}`
- Sa-Token 自动从 header 中提取并校验
- 用户ID 获取: `StpUtil.getLoginIdAsLong()`

### 2.4 JSON 字段 TypeHandler 配置

```java
// MybatisPlusConfig.java
@Configuration
@MapperScan("com.lanyan.aidrama.mapper")
public class MybatisPlusConfig {

    @Bean
    public JacksonTypeHandler jacksonTypeHandler() {
        return new JacksonTypeHandler(Object.class);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 逻辑删除插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

**实体类 JSON 字段标注示例：**
```java
// Project.java
@TableField(typeHandler = JacksonTypeHandler.class)
private WorkflowConfig workflowConfig;

@TableField(typeHandler = JacksonTypeHandler.class)
private StylePreset stylePreset;
```

**所有涉及 JSON 字段的 Mapper XML 需声明 typeHandler：**
```xml
<result column="workflow_config" property="workflowConfig"
        typeHandler="com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler"/>
```

---

## 3. 数据库 DDL

```sql
-- ============================================
-- AI漫剧生产平台 数据库初始化脚本
-- MySQL 8.0+
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
    `execution_lock`  TINYINT       NOT NULL DEFAULT 0 COMMENT '执行锁: 0-未执行 1-执行中',
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
    KEY `idx_sort_order` (`sort_order`)
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
    KEY `idx_sort_order` (`sort_order`)
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
    KEY `idx_sort_order` (`sort_order`)
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
    `input_data`      JSON        DEFAULT NULL  COMMENT '输入数据(JSON)',
    `output_data`     JSON        DEFAULT NULL  COMMENT '输出数据(JSON)',
    `review_comment`  TEXT        DEFAULT NULL  COMMENT '审核意见',
    `error_msg`       TEXT        DEFAULT NULL  COMMENT '错误信息',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_step_order` (`step_order`),
    KEY `idx_status` (`status`)
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
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_shot_id` (`shot_id`),
    KEY `idx_provider_task_id` (`provider_task_id`),
    KEY `idx_status` (`status`)
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
```

---

## 4. 模块详细设计

### 4.1 用户模块 (user)

> 负责用户认证（登录/登出/信息获取）。

#### 4.1.1 Controller: `UserController`

| 接口 | 方法 | 路径 | 鉴权 | 状态 |
|------|------|------|------|------|
| 登录 | `POST` | `/api/user/login` | 否 | `[ ]` |
| 登出 | `POST` | `/api/user/logout` | 是 | `[ ]` |
| 获取当前用户信息 | `GET` | `/api/user/info` | 是 | `[ ]` |

**POST /api/user/login**

请求体:
```json
{
  "username": "string, 必填, 用户名",
  "password": "string, 必填, 明文密码"
}
```

响应体:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "string, Sa-Token值，前端存LocalStorage",
    "userId": "long",
    "username": "string",
    "nickname": "string"
  }
}
```

**POST /api/user/logout**

请求体: 无（通过 Authorization header 传 Token）

响应体:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

**GET /api/user/info**

请求参数: 无（通过 Authorization header 传 Token）

响应体:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "long",
    "username": "string",
    "nickname": "string",
    "status": "int, 0-禁用 1-启用"
  }
}
```

#### 4.1.2 Service: `UserService`

| 方法 | 说明 |
|------|------|
| `LoginResult login(LoginRequest req)` | 校验用户名密码，BCrypt比对，成功后 StpUtil.login(userId) 返回 Token |
| `void logout()` | StpUtil.logout() |
| `UserInfoVO getCurrentUserInfo()` | StpUtil.getLoginIdAsLong() 查用户表，返回脱敏信息 |

#### 4.1.3 Mapper: `UserMapper extends BaseMapper<SysUser>`

| 方法 | 说明 |
|------|------|
| `SysUser selectByUsername(String username)` | 根据用户名查询（MyBatis Plus Wrapper） |

---

### 4.2 项目模块 (project)

> 负责项目 CRUD、小说文件 TOS 路径绑定、流程配置保存。

#### 4.2.1 Controller: `ProjectController`

| 接口 | 方法 | 路径 | 鉴权 | 状态 |
|------|------|------|------|------|
| 项目列表 | `GET` | `/api/project/list` | 是 | `[ ]` |
| 创建项目 | `POST` | `/api/project` | 是 | `[ ]` |
| 项目详情 | `GET` | `/api/project/{id}` | 是 | `[ ]` |
| 更新项目 | `PUT` | `/api/project/{id}` | 是 | `[ ]` |
| 删除项目 | `DELETE` | `/api/project/{id}` | 是 | `[ ]` |
| 保存流程配置 | `PUT` | `/api/project/{id}/workflow` | 是 | `[ ]` |

**GET /api/project/list**

查询参数: `page` (int, 可选, 默认1), `size` (int, 可选, 默认10)

响应体:
```json
{
  "code": 0,
  "data": {
    "total": "long",
    "list": [{
      "id": "long",
      "name": "string",
      "description": "string",
      "status": "int, 0-草稿 1-进行中 2-已完成",
      "executionLock": "int, 0-未执行 1-执行中",
      "createTime": "string, ISO-8601"
    }]
  }
}
```

**POST /api/project**

请求体:
```json
{
  "name": "string, 必填, 项目名称, 最大200字",
  "description": "string, 可选, 项目描述",
  "novelTosPath": "string, 可选, 小说文件TOS路径"
}
```

响应体: `data` 为创建的项目 ID (`long`)。

**GET /api/project/{id}**

响应体: 完整项目信息，含 `workflowConfig`、`stylePreset` 等 JSON 字段。

**PUT /api/project/{id}**

请求体: 同创建，所有字段可选，仅传需更新字段。

**DELETE /api/project/{id}**

请求体: 无。逻辑删除 (`deleted=1`)。

**PUT /api/project/{id}/workflow**

请求体:
```json
{
  "workflowConfig": {
    "steps": [
      {
        "stepType": "string, 必填, import/asset_extract/shot_gen/image_gen/video_gen/export",
        "enabled": "boolean, 必填, 是否启用",
        "review": "boolean, 必填, 是否需要审核",
        "config": "object, 可选, 步骤级额外配置"
      }
    ]
  },
  "stylePreset": {
    "artStyle": "string, 可选, 画风",
    "colorTone": "string, 可选, 色调"
  }
}
```

#### 4.2.2 Service: `ProjectService`

| 方法 | 说明 |
|------|------|
| `PageResult<ProjectVO> listProjects(Long userId, int page, int size)` | 分页查用户项目列表，自动过滤 deleted=1 |
| `Long createProject(ProjectCreateRequest req, Long userId)` | 创建项目，绑定 user_id |
| `ProjectVO getProjectDetail(Long id)` | 查详情，校验权限（项目归属） |
| `void updateProject(Long id, ProjectUpdateRequest req)` | 更新项目字段 |
| `void deleteProject(Long id)` | 逻辑删除，需校验 execution_lock=0 |
| `void saveWorkflowConfig(Long id, WorkflowConfigRequest req)` | 保存 workflow_config JSON，仅允许 status=草稿 时修改 |

#### 4.2.3 Mapper: `ProjectMapper extends BaseMapper<Project>`

| 方法 | 说明 |
|------|------|
| `Project selectByIdWithConfig(Long id)` | 查项目含 JSON 字段 |

---

### 4.3 资产模块 (asset)

> 负责角色/场景/物品/声音资产 CRUD，支持多参考图。

#### 4.3.1 Controller: `AssetController`

| 接口 | 方法 | 路径 | 鉴权 | 状态 |
|------|------|------|------|------|
| 资产列表 | `GET` | `/api/project/{projectId}/assets` | 是 | `[ ]` |
| 创建资产 | `POST` | `/api/project/{projectId}/assets` | 是 | `[ ]` |
| 更新资产 | `PUT` | `/api/asset/{id}` | 是 | `[ ]` |
| 删除资产 | `DELETE` | `/api/asset/{id}` | 是 | `[ ]` |
| 确认资产 | `PUT` | `/api/asset/{id}/confirm` | 是 | `[ ]` |

**GET /api/project/{projectId}/assets**

查询参数: `assetType` (string, 可选, character/scene/prop/voice)

响应体:
```json
{
  "code": 0,
  "data": [{
    "id": "long",
    "projectId": "long",
    "assetType": "string",
    "name": "string",
    "description": "string",
    "referenceImages": ["string, URL数组"],
    "stylePreset": { "artStyle": "string" },
    "status": "int, 0-草稿 1-已确认 2-已废弃",
    "createTime": "string"
  }]
}
```

**POST /api/project/{projectId}/assets**

请求体:
```json
{
  "assetType": "string, 必填, character/scene/prop/voice",
  "name": "string, 必填, 资产名称",
  "description": "string, 可选, AI描述",
  "referenceImages": ["string, 可选, 参考图URL数组, 第一个为主图"],
  "stylePreset": { "artStyle": "string, 可选" }
}
```

**PUT /api/asset/{id}**

请求体: 同创建，所有字段可选。

**PUT /api/asset/{id}/confirm**

请求体: 无。将资产 status 设为 1（已确认）。

#### 4.3.2 Service: `AssetService`

| 方法 | 说明 |
|------|------|
| `List<AssetVO> listAssets(Long projectId, String assetType)` | 查项目下某类型资产列表 |
| `Long createAsset(Long projectId, AssetCreateRequest req)` | 创建资产，校验 projectId 归属 |
| `void updateAsset(Long id, AssetUpdateRequest req)` | 更新资产 |
| `void deleteAsset(Long id)` | 逻辑删除，需检查 shot_asset_ref 是否有关联 |
| `void confirmAsset(Long id)` | 确认资产（status=1） |
| `AssetVO getPrimaryImage(Long assetId)` | 获取资产主参考图（referenceImages[0]） |

#### 4.3.3 Mapper: `AssetMapper extends BaseMapper<Asset>`

| 方法 | 说明 |
|------|------|
| `List<Asset> selectByProjectAndType(Long projectId, String assetType)` | 按项目+类型查资产 |

---

### 4.4 内容模块 (content)

> 负责分集/分场/分镜 CRUD、批量审核、资产绑定。

#### 4.4.1 Controller: `ContentController`

| 接口 | 方法 | 路径 | 鉴权 | 状态 |
|------|------|------|------|------|
| 分集列表 | `GET` | `/api/project/{projectId}/episodes` | 是 | `[ ]` |
| 创建分集 | `POST` | `/api/project/{projectId}/episodes` | 是 | `[ ]` |
| 更新分集 | `PUT` | `/api/episode/{id}` | 是 | `[ ]` |
| 删除分集 | `DELETE` | `/api/episode/{id}` | 是 | `[ ]` |
| 分场列表 | `GET` | `/api/episode/{episodeId}/scenes` | 是 | `[ ]` |
| 创建分场 | `POST` | `/api/episode/{episodeId}/scenes` | 是 | `[ ]` |
| 更新分场 | `PUT` | `/api/scene/{id}` | 是 | `[ ]` |
| 删除分场 | `DELETE` | `/api/scene/{id}` | 是 | `[ ]` |
| 分镜列表 | `GET` | `/api/scene/{sceneId}/shots` | 是 | `[ ]` |
| 创建分镜 | `POST` | `/api/scene/{sceneId}/shots` | 是 | `[ ]` |
| 更新分镜 | `PUT` | `/api/shot/{id}` | 是 | `[ ]` |
| 删除分镜 | `DELETE` | `/api/shot/{id}` | 是 | `[ ]` |
| 批量审核 | `POST` | `/api/shot/batch-review` | 是 | `[ ]` |
| 绑定资产到分镜 | `POST` | `/api/shot/{shotId}/assets` | 是 | `[ ]` |
| 解绑分镜资产 | `DELETE` | `/api/shot/{shotId}/assets/{assetId}` | 是 | `[ ]` |

**GET /api/project/{projectId}/episodes**

响应体:
```json
{
  "code": 0,
  "data": [{
    "id": "long",
    "title": "string",
    "sortOrder": "int",
    "content": "string",
    "status": "int, 0-待处理 1-进行中 2-已完成"
  }]
}
```

**GET /api/scene/{sceneId}/shots**

响应体:
```json
{
  "code": 0,
  "data": [{
    "id": "long",
    "sceneId": "long",
    "sortOrder": "int",
    "prompt": "string",
    "promptEn": "string",
    "generatedImageUrl": "string",
    "generatedVideoUrl": "string",
    "status": "int, 0-待处理 1-生成中 2-待审核 3-已通过 4-已打回 5-已完成",
    "reviewComment": "string",
    "version": "int",
    "generationAttempts": "int",
    "assetRefs": [{
      "assetId": "long",
      "assetType": "string",
      "assetName": "string",
      "primaryImage": "string"
    }]
  }]
}
```

**POST /api/shot/batch-review**

请求体:
```json
{
  "shotIds": ["long, 必填, 分镜ID数组"],
  "action": "string, 必填, approve(通过) / reject(打回)",
  "comment": "string, 打回时必填, 审核意见"
}
```

**POST /api/shot/{shotId}/assets**

请求体:
```json
{
  "assetId": "long, 必填, 资产ID",
  "assetType": "string, 必填, character/scene/prop"
}
```

#### 4.4.2 Service: `ContentService`

| 方法 | 说明 |
|------|------|
| `List<EpisodeVO> listEpisodes(Long projectId)` | 查项目下分集 |
| `Long createEpisode(Long projectId, EpisodeCreateRequest req)` | 创建分集，sort_order 自动递增 |
| `void updateEpisode(Long id, EpisodeUpdateRequest req)` | 更新分集 |
| `void deleteEpisode(Long id)` | 级联逻辑删除（分场→分镜→资产关联） |
| `List<SceneVO> listScenes(Long episodeId)` | 查分集下分场 |
| `Long createScene(Long episodeId, SceneCreateRequest req)` | 创建分场 |
| `void updateScene(Long id, SceneUpdateRequest req)` | 更新分场 |
| `void deleteScene(Long id)` | 级联删除分镜及资产关联 |
| `List<ShotVO> listShots(Long sceneId)` | 查分场下分镜，含 assetRefs |
| `Long createShot(Long sceneId, ShotCreateRequest req)` | 创建分镜 |
| `void updateShot(Long id, ShotUpdateRequest req)` | 更新分镜 |
| `void batchReviewShots(List<Long> shotIds, String action, String comment)` | 批量审核，action=approve→status=3, reject→status=4 |
| `void bindAssetToShot(Long shotId, Long assetId, String assetType)` | 绑定资产到分镜，插入 shot_asset_ref |
| `void unbindAssetFromShot(Long shotId, Long assetId)` | 解绑 |

#### 4.4.3 Mapper

| Mapper | 说明 |
|--------|------|
| `EpisodeMapper extends BaseMapper<Episode>` | — |
| `SceneMapper extends BaseMapper<Scene>` | — |
| `ShotMapper extends BaseMapper<Shot>` | — |
| `ShotAssetRefMapper extends BaseMapper<ShotAssetRef>` | 含 `List<ShotAssetRef> selectByShotId(Long shotId)` |

---

### 4.5 流程引擎模块 (workflow)

> 负责流程保存、启动、进度查询、审核、停止。详见 [第5节 流程引擎详细设计](#5-流程引擎详细设计)。

#### 4.5.1 Controller: `WorkflowController`

| 接口 | 方法 | 路径 | 鉴权 | 状态 |
|------|------|------|------|------|
| 开始执行 | `POST` | `/api/project/{id}/workflow/start` | 是 | `[ ]` |
| 查询进度 | `GET` | `/api/project/{id}/workflow/status` | 是 | `[ ]` |
| 审核步骤 | `POST` | `/api/project/{id}/workflow/review` | 是 | `[ ]` |
| 停止执行 | `POST` | `/api/project/{id}/workflow/stop` | 是 | `[ ]` |

**POST /api/project/{id}/workflow/start**

请求体: 无。

响应体:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

错误场景: `execution_lock=1` 返回 `40900 项目正在执行中`。

**GET /api/project/{id}/workflow/status**

响应体:
```json
{
  "code": 0,
  "data": {
    "executionLock": "int, 0-未执行 1-执行中",
    "currentStep": "string, 当前执行的 stepType",
    "progress": "int, 已完成步骤数 / 总步骤数",
    "steps": [{
      "stepType": "string",
      "stepOrder": "int",
      "status": "int, 0-未执行 1-执行中 2-成功 3-失败 4-待审核",
      "errorMsg": "string",
      "reviewComment": "string"
    }]
  }
}
```

**POST /api/project/{id}/workflow/review**

请求体:
```json
{
  "stepType": "string, 必填, 审核步骤类型",
  "action": "string, 必填, approve(通过) / reject(打回)",
  "comment": "string, 可选, 审核意见"
}
```

行为:
- `approve`: 将该 workflow_task 状态置为 2（成功），从 Redis 删除锁标记，继续执行下一步
- `reject`: 将该 workflow_task 状态置为 3（失败），记录 errorMsg，停止执行

**POST /api/project/{id}/workflow/stop**

请求体: 无。

行为: 设置 `execution_lock=0`，写入 Redis stop 标记，引擎检查到后终止。

#### 4.5.2 Service: `WorkflowService`

| 方法 | 说明 |
|------|------|
| `void startWorkflow(Long projectId)` | 校验 lock → 初始化 workflow_task 记录 → 加 Redis 锁 → @Async 触发引擎 |
| `WorkflowStatusVO getWorkflowStatus(Long projectId)` | 查 workflow_task 表最新状态 |
| `void reviewStep(Long projectId, ReviewRequest req)` | 审核通过/打回 |
| `void stopWorkflow(Long projectId)` | 停止执行 |
| `void recoverWorkflow(Long projectId)` | 服务重启后恢复执行中的流程 |

---

### 4.6 AI任务模块 (aitask)

> 负责 AI 图片/视频生成的异步提交、结果轮询、状态管理。

#### 4.6.1 Controller: `AiTaskController`

| 接口 | 方法 | 路径 | 鉴权 | 状态 |
|------|------|------|------|------|
| 触发图片生成 | `POST` | `/api/ai/image-gen` | 是 | `[ ]` |
| 触发视频生成 | `POST` | `/api/ai/video-gen` | 是 | `[ ]` |
| 查询AI任务状态 | `GET` | `/api/ai/task/{taskId}` | 是 | `[ ]` |

**POST /api/ai/image-gen**

请求体:
```json
{
  "shotId": "long, 必填, 分镜ID",
  "prompt": "string, 必填, 英文提示词",
  "referenceImages": ["string, 可选, 资产参考图URL"],
  "stylePreset": { "artStyle": "string, 可选" }
}
```

响应体: `data` 为 `aiTaskId` (`long`)。

**POST /api/ai/video-gen**

请求体:
```json
{
  "shotId": "long, 必填, 分镜ID",
  "firstFrameImage": "string, 必填, 首帧图URL",
  "prompt": "string, 必填, 英文提示词",
  "referenceImages": ["string, 可选, 资产参考图URL"]
}
```

响应体: `data` 为 `aiTaskId` (`long`)。

**GET /api/ai/task/{taskId}**

响应体:
```json
{
  "code": 0,
  "data": {
    "id": "long",
    "taskType": "string, image_gen/video_gen",
    "status": "int, 0-提交中 1-处理中 2-成功 3-失败",
    "resultUrl": "string",
    "errorMsg": "string",
    "createTime": "string",
    "updateTime": "string"
  }
}
```

#### 4.6.2 Service: `AiTaskService`

| 方法 | 说明 |
|------|------|
| `Long submitImageGenTask(ImageGenRequest req, Long userId)` | 创建 ai_task(status=0) → @Async 调用 Seedance → 更新 provider_task_id → 轮询结果 → 上传TOS → 更新 status |
| `Long submitVideoGenTask(VideoGenRequest req, Long userId)` | 同上，任务类型为 video_gen |
| `AiTaskVO getTaskStatus(Long taskId)` | 查 ai_task 表状态 |
| `void pollTaskResult(Long taskId)` | 定时任务调用（每5s），查第三方API结果 |

#### 4.6.3 SeedanceClient

```java
@Component
public class SeedanceClient {

    @Value("${seedance.api-url}")
    private String apiUrl;

    @Value("${seedance.api-key}")
    private String apiKey;

    // 提交图片生成任务
    public String submitImage(ImageGenParams params);
    // 提交视频生成任务
    public String submitVideo(VideoGenParams params);
    // 查询任务结果
    public TaskResult queryTask(String providerTaskId);
    // 下载结果到临时文件
    public byte[] downloadResult(String resultUrl);
}
```

**请求头:**
```
Authorization: Bearer {apiKey}
Content-Type: application/json
```

#### 4.6.4 Mapper: `AiTaskMapper extends BaseMapper<AiTask>`

| 方法 | 说明 |
|------|------|
| `List<AiTask> selectPendingPollTasks()` | 查 status=1 的任务用于定时轮询 |

---

### 4.7 存储模块 (storage)

> 负责火山 TOS 预签名 URL 生成、结果文件上传。

#### 4.7.1 Controller: `TosController`

| 接口 | 方法 | 路径 | 鉴权 | 状态 |
|------|------|------|------|------|
| 获取预签名上传URL | `POST` | `/api/tos/presign` | 否* | `[ ]` |

> *注: 预签名接口需登录态（鉴权），但前端直传 TOS 不需要经过后端上传。

**POST /api/tos/presign**

请求体:
```json
{
  "fileName": "string, 必填, 原始文件名",
  "contentType": "string, 必填, MIME类型, 如 image/png, video/mp4",
  "projectDir": "string, 可选, 项目目录前缀, 如 projects/123"
}
```

响应体:
```json
{
  "code": 0,
  "data": {
    "uploadUrl": "string, 预签名PUT URL, 前端直接PUT文件内容",
    "accessUrl": "string, 上传完成后可访问的公开URL",
    "expiresIn": "int, 预签名有效期(秒), 默认3600"
  }
}
```

#### 4.7.2 Service: `TosService`

| 方法 | 说明 |
|------|------|
| `PresignResult generatePresignUrl(String fileName, String contentType, String projectDir)` | 生成 PUT 预签名 URL，TTL=1h |
| `String uploadFromUrl(String sourceUrl, String targetKey)` | 从第三方URL下载到TOS（AI结果回传场景） |
| `String uploadFromBytes(byte[] data, String targetKey)` | 直接上传字节数据 |
| `void deleteFile(String key)` | 删除 TOS 文件 |

**TOS Key 命名规范:**
```
projects/{projectId}/assets/{assetId}/{timestamp}_{fileName}
projects/{projectId}/episodes/{episodeId}/shots/{shotId}/image_v{version}.png
projects/{projectId}/episodes/{episodeId}/shots/{shotId}/video_v{version}.mp4
projects/{projectId}/output/final_{projectId}_{timestamp}.mp4
```

---

## 5. 流程引擎详细设计

### 5.1 线性执行逻辑

```
WorkflowEngine.execute(projectId)
    │
    ├── 1. 从 project.workflow_config 解析步骤数组
    ├── 2. 按 step_order 升序遍历
    │       │
    │       ├── 2a. 检查 step.enabled → false 则跳过, 标记 status=2(成功)
    │       ├── 2b. 检查 Redis stop 标记 → 存在则终止, 释放锁
    │       ├── 2c. 创建/更新 workflow_task (status=1 执行中)
    │       ├── 2d. 调用 NodeExecutor 执行具体逻辑
    │       ├── 2e. 执行成功:
    │       │       ├── step.review == true:
    │       │       │   → 标记 status=4(待审核), 暂停执行
    │       │       │   → 等待前端 review 接口
    │       │       └── step.review == false:
    │       │           → 标记 status=2(成功), 继续下一步
    │       └── 2f. 执行失败:
    │               → 标记 status=3(失败), 记录 error_msg, 终止执行
    └── 3. 所有步骤完成:
            → project.status=2(已完成), execution_lock=0
            → 释放 Redis 锁
```

### 5.2 NodeExecutor 策略模式

```java
public interface NodeExecutor {
    String getStepType(); // import, asset_extract, shot_gen, image_gen, video_gen, export
    NodeResult execute(Long projectId, Long episodeId, StepConfig config);
}

// 实现类：
// ImportNodeExecutor      → 读取 novel_tos_path，AI 拆分章节，创建 episode 记录
// AssetExtractNodeExecutor → 读取分集内容，AI 提取资产草稿
// ShotGenNodeExecutor      → 根据分场+资产，强模型生成分镜 prompt（中英文）
// ImageGenNodeExecutor     → 调用 AiTaskService.submitImageGenTask()
// VideoGenNodeExecutor     → 调用 AiTaskService.submitVideoGenTask()
// ExportNodeExecutor       → 收集所有视频URL，FFmpeg 合并，上传 TOS
```

### 5.3 Redis 锁实现

```java
// WorkflowService.startWorkflow()
public void startWorkflow(Long projectId) {
    String lockKey = "workflow:lock:" + projectId;
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", 30, TimeUnit.MINUTES);
    if (!Boolean.TRUE.equals(acquired)) {
        throw new BusinessException(40900, "项目正在执行中，请勿重复操作");
    }

    // 更新 project 表 execution_lock
    projectMapper.updateExecutionLock(projectId, 1);

    // @Async 触发引擎
    workflowEngine.executeAsync(projectId);
}

// WorkflowEngine 执行完毕或异常时释放
private void releaseLock(Long projectId) {
    String lockKey = "workflow:lock:" + projectId;
    redisTemplate.delete(lockKey);
    projectMapper.updateExecutionLock(projectId, 0);
}
```

### 5.4 断点续跑机制

```java
// WorkflowService.recoverWorkflow()
public void recoverWorkflow(Long projectId) {
    // 1. 查找最后一个 status=1(执行中) 的 workflow_task
    WorkflowTask runningTask = workflowTaskMapper.selectLatestRunning(projectId);
    if (runningTask == null) {
        return; // 无待恢复任务
    }

    // 2. 将当前执行中步骤标记为失败（因服务中断）
    workflowTaskMapper.updateStatus(runningTask.getId(), 3, "服务中断");

    // 3. 从下一个步骤继续执行
    int nextOrder = runningTask.getStepOrder() + 1;
    workflowEngine.resumeFrom(projectId, nextOrder);
}
```

**启动时自动恢复:**
```java
@Component
public class RecoveryRunner implements ApplicationRunner {
    @Autowired private WorkflowService workflowService;

    @Override
    public void run(ApplicationArguments args) {
        // 扫描所有 execution_lock=1 的项目
        List<Project> lockedProjects = projectMapper.selectLockedProjects();
        for (Project project : lockedProjects) {
            log.info("Recovering workflow for project: {}", project.getId());
            workflowService.recoverWorkflow(project.getId());
        }
    }
}
```

### 5.5 执行时序图

```
Frontend                     Backend                      Redis           Seedance
   │                            │                           │                │
   ├── POST /workflow/start ──→│                           │                │
   │                            ├── SET workflow:lock ────→│                │
   │                            ├── UPDATE execution_lock  │                │
   │                            ├── @Async execute ──┐     │                │
   │                            │                    │     │                │
   │←──── {code:0} ────────────│                    │     │                │
   │                            │                    │     │                │
   │                            │  遍历步骤...        │     │                │
   │                            ├── INSERT workflow_task   │                │
   │                            ├── 执行 NodeExecutor      │                │
   │                            │                         │                │
   │                            │                         │  调用AI ──────→│
   │                            │                         │                │
   │                            │←── 结果回调 ─────────────│←───────────────│
   │                            │                         │                │
   │  (前端3s轮询)              │                         │                │
   ├── GET /workflow/status ──→│                         │                │
   │←──── {steps:[...]} ───────│                         │                │
   │                            │                         │                │
   │  (审核节点到达时)           │                         │                │
   │                            ├── UPDATE status=4(待审核)│                │
   │                            ├── 暂停等待              │                │
   │                            │                         │                │
   ├── POST /workflow/review ──→│                         │                │
   │                            ├── UPDATE status=2(成功)  │                │
   │                            ├── 继续执行下一步          │                │
   │                            │                         │                │
   │                            ├── DEL workflow:lock ────→│                │
```

---

## 6. 异步任务与线程池

### 6.1 线程池配置

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("workflowExecutor")
    public ThreadPoolTaskExecutor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);        // 核心线程数 = 3
        executor.setMaxPoolSize(5);         // 最大线程数 = 5
        executor.setQueueCapacity(10);      // 队列容量 = 10
        executor.setThreadNamePrefix("workflow-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // 核心线程数 = 5
        executor.setMaxPoolSize(10);        // 最大线程数 = 10
        executor.setQueueCapacity(20);      // 队列容量 = 20
        executor.setThreadNamePrefix("ai-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 6.2 使用方式

```java
// 流程引擎异步执行
@Async("workflowExecutor")
public void executeAsync(Long projectId) {
    // ...
}

// AI任务异步提交
@Async("aiTaskExecutor")
public void submitImageGenAsync(Long taskId, ImageGenParams params) {
    // ...
}
```

### 6.3 定时任务

```java
@Configuration
@EnableScheduling
public class ScheduleConfig {

    // 每 5s 轮询处理中的 AI 任务结果
    @Scheduled(fixedDelay = 5000)
    public void pollAiTaskResults() {
        // 查 ai_task WHERE status=1, 批量查第三方API结果
    }

    // 每 10min 清理过期的 Redis 状态缓存
    @Scheduled(fixedDelay = 600000)
    public void cleanExpiredCache() {
        // Redis TTL 自动过期为主，此为兜底
    }
}
```

---

## 7. Redis 使用规范

| 用途 | Key 格式 | TTL | 说明 |
|------|----------|-----|------|
| Sa-Token session | `satoken:token:{token}` | Sa-Token 默认(24h) | 框架自动管理 |
| 流程执行锁 | `workflow:lock:{projectId}` | 30min | 防重复执行 |
| 流程停止标记 | `workflow:stop:{projectId}` | 30min | 用户停止后写入，引擎检查此标记 |
| AI任务状态缓存 | `ai:task:{taskId}` | 24h | 缓存最新状态，减少DB查询 |
| API限流 | `api:rate:{userId}` | 1min | 滑动窗口限流（可选） |

---

## 8. AI 一致性保障

### 8.1 资产强绑定

分镜生成时，通过 `shot_asset_ref` 表查出关联资产，取每个资产的 `reference_images[0]`（主图），作为 Seedance 调用的 `reference_image` 参数传入。

### 8.2 Seedance 调用参数映射

```java
// ImageGenParams 构建
{
    "prompt": shot.getPromptEn(),          // 英文提示词
    "first_frame_image": null,             // 图片生成不需要首帧
    "reference_images": [                  // 资产主参考图数组
        assetRef.getPrimaryImage(),
        ...
    ],
    "style": project.getStylePreset().getArtStyle(),
    "resolution": "1024x1024",
    "seed": null
}

// VideoGenParams 构建
{
    "prompt": shot.getPromptEn(),
    "first_frame_image": shot.getGeneratedImageUrl(),  // 分镜首帧图
    "reference_images": [...],                         // 资产主参考图
    "duration": 5,
    "resolution": "1024x1024"
}
```

---

## 9. 进度追踪清单

### Sprint 1: 基础骨架 (Day 1-3)

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 1.1 | 数据库建表 + init.sql | 10张完整DDL + 索引 | `[ ]` |
| 1.2 | SpringBoot 骨架搭建 | 项目结构、pom.xml、application.yml | `[ ]` |
| 1.3 | 全局响应 + 异常处理 | Result<T>、GlobalExceptionHandler、BusinessException | `[ ]` |
| 1.4 | Sa-Token 配置 + 登录模块 | SaTokenConfig、UserController、UserService | `[ ]` |
| 1.5 | MyBatis Plus 配置 + TypeHandler | MybatisPlusConfig、JacksonTypeHandler 标注 | `[ ]` |
| 1.6 | Redis 配置 | RedisConfig、连接池配置 | `[ ]` |
| 1.7 | 火山 TOS 封装 + 预签名URL | TosConfig、TosService、TosController | `[ ]` |
| 1.8 | AsyncConfig 线程池 | workflowExecutor + aiTaskExecutor | `[ ]` |

### Sprint 2: 核心业务 (Day 4-7)

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 2.1 | 项目模块 CRUD | ProjectController/Service/Mapper | `[ ]` |
| 2.2 | 小说上传TOS | 集成 TosService | `[ ]` |
| 2.3 | 资产模块 CRUD | AssetController/Service/Mapper | `[ ]` |
| 2.4 | 资产确认接口 | confirmAsset 方法 | `[ ]` |
| 2.5 | 分集 CRUD | Episode Controller/Service/Mapper | `[ ]` |
| 2.6 | 分场 CRUD | Scene Controller/Service/Mapper | `[ ]` |
| 2.7 | 分镜 CRUD | Shot Controller/Service/Mapper | `[ ]` |
| 2.8 | 分镜-资产关联 CRUD | ShotAssetRef Controller/Service/Mapper | `[ ]` |
| 2.9 | 批量审核接口 | batchReviewShots | `[ ]` |

### Sprint 3: 流程引擎 + AI集成 (Day 8-12)

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 3.1 | WorkflowEngine 线性执行引擎 | WorkflowEngine.java | `[ ]` |
| 3.2 | 6种 NodeExecutor 实现 | Import/AssetExtract/ShotGen/ImageGen/VideoGen/Export | `[ ]` |
| 3.3 | Redis 分布式锁 | startWorkflow + releaseLock | `[ ]` |
| 3.4 | 断点续跑 + 启动恢复 | recoverWorkflow + RecoveryRunner | `[ ]` |
| 3.5 | 流程 Controller 层 | WorkflowController (start/status/review/stop) | `[ ]` |
| 3.6 | AiTaskService 异步处理 | submit + poll + @Async | `[ ]` |
| 3.7 | SeedanceClient 封装 | HTTP 调用 + 结果解析 | `[ ]` |
| 3.8 | AI任务定时轮询 | @Scheduled pollAiTaskResults | `[ ]` |
| 3.9 | PromptEngine 提示词生成 | 强模型翻译+生成 prompt_en | `[ ]` |
| 3.10 | FFmpeg 导出 | ExportService (合并视频) | `[ ]` |

### Sprint 4: 联调测试 (Day 13-15)

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 4.1 | API消耗记录 + 统计 | api_call_log 写入 + 聚合查询接口 | `[ ]` |
| 4.2 | 预算控制 | 调用前余额检查 | `[ ]` |
| 4.3 | 全流程联调 | 导入→资产→分镜→生图→生视频→导出 | `[ ]` |
| 4.4 | 边界测试 | 断点续跑、并发锁、重试、权限校验 | `[ ]` |
| 4.5 | API 文档 | Swagger/OpenAPI 导出 | `[ ]` |

---

## 附录 A: application.yml 参考配置

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_drama?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      timeout: 3000ms

  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

# Sa-Token
sa-token:
  token-name: Authorization
  timeout: 86400
  active-timeout: 1800
  is-concurrent: true
  is-read-header: true
  token-prefix: Bearer

# MyBatis Plus
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.lanyan.aidrama.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# Seedance
seedance:
  api-url: https://api.seedance.com/v1
  api-key: ${SEEDANCE_API_KEY}

# TOS (火山引擎 S3 兼容)
tos:
  endpoint: ${TOS_ENDPOINT}
  access-key: ${TOS_ACCESS_KEY}
  secret-key: ${TOS_SECRET_KEY}
  bucket-name: ${TOS_BUCKET}
  base-url: ${TOS_BASE_URL}
  presign-expire: 3600
```

## 附录 B: 项目包结构

```
com.lanyan.aidrama/
├── AiDramaApplication.java
├── common/
│   ├── Result.java                  # 统一响应
│   ├── BusinessException.java       # 业务异常
│   ├── GlobalExceptionHandler.java  # 全局异常处理
│   └── PageResult.java             # 分页响应
├── config/
│   ├── SaTokenConfig.java
│   ├── MybatisPlusConfig.java
│   ├── RedisConfig.java
│   ├── AsyncConfig.java
│   ├── TosConfig.java
│   └── ScheduleConfig.java
├── entity/                          # 数据库实体（对应10张表）
│   ├── SysUser.java
│   ├── Project.java
│   ├── Asset.java
│   ├── Episode.java
│   ├── Scene.java
│   ├── Shot.java
│   ├── ShotAssetRef.java
│   ├── WorkflowTask.java
│   ├── AiTask.java
│   └── ApiCallLog.java
├── mapper/                          # MyBatis Mapper
│   ├── UserMapper.java
│   ├── ProjectMapper.java
│   ├── AssetMapper.java
│   ├── EpisodeMapper.java
│   ├── SceneMapper.java
│   ├── ShotMapper.java
│   ├── ShotAssetRefMapper.java
│   ├── WorkflowTaskMapper.java
│   ├── AiTaskMapper.java
│   └── ApiCallLogMapper.java
├── module/
│   ├── user/
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   └── dto/
│   │       ├── LoginRequest.java
│   │       ├── LoginResult.java
│   │       └── UserInfoVO.java
│   ├── project/
│   │   ├── controller/ProjectController.java
│   │   ├── service/ProjectService.java
│   │   └── dto/
│   ├── asset/
│   │   ├── controller/AssetController.java
│   │   ├── service/AssetService.java
│   │   └── dto/
│   ├── content/
│   │   ├── controller/ContentController.java
│   │   ├── service/ContentService.java
│   │   └── dto/
│   ├── workflow/
│   │   ├── controller/WorkflowController.java
│   │   ├── service/WorkflowService.java
│   │   ├── engine/
│   │   │   ├── WorkflowEngine.java
│   │   │   └── RecoveryRunner.java
│   │   ├── executor/
│   │   │   ├── NodeExecutor.java          # 接口
│   │   │   ├── ImportNodeExecutor.java
│   │   │   ├── AssetExtractNodeExecutor.java
│   │   │   ├── ShotGenNodeExecutor.java
│   │   │   ├── ImageGenNodeExecutor.java
│   │   │   ├── VideoGenNodeExecutor.java
│   │   │   └── ExportNodeExecutor.java
│   │   └── dto/
│   ├── aitask/
│   │   ├── controller/AiTaskController.java
│   │   ├── service/AiTaskService.java
│   │   └── client/
│   │       ├── SeedanceClient.java
│   │       └── dto/
│   └── storage/
│       ├── controller/TosController.java
│       └── service/TosService.java
└── aspect/
    └── ApiCallLogAspect.java          # AOP 记录 API 调用日志
```

## 附录 C: DTO 命名规范

| 后缀 | 用途 | 示例 |
|------|------|------|
| `Request` | 接收前端请求体 | `LoginRequest`, `ProjectCreateRequest` |
| `VO` | 返回前端视图对象 | `UserInfoVO`, `ProjectVO` |
| `DTO` | 内部数据传输 | `ImageGenParams`, `TaskResult` |

---

> **文档维护**: 每完成一个功能点，将对应 `[ ]` 标记改为 `[x]`，并附上完成日期。
