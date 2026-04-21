# AI漫剧生产平台 — 后端系统设计规格书 (Backend Design Spec)

## 文档信息

| 项目 | 内容 |
|------|------|
| 产品名 | AI漫剧生产平台 |
| 版本 | **v1.7** (TosController接口开发) |
| 基于 | PRD v1.1 MVP 终版 + 评审回复 (小欧/阿典) |
| 创建日期 | 2026-04-19 |
| 更新日期 | 2026-04-21 |
| 状态 | **评审通过，待开发** |

### 版本变更

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| v1.1 | 2026-04-19 | 初始版本，待评审 |
| **v1.2 终版** | 2026-04-19 | 整合小欧(4P0+10P1)和阿典(3P0+5P1)全部评审意见，共22项修改落地 |
| **v1.3** | 2026-04-20 | 新增代码注释规范：关键行需有清晰明确的中文注释 |
| **v1.4** | 2026-04-20 | 新增 SpringDoc / Swagger API 文档自动生成功能设计 |
| **v1.5** | 2026-04-20 | 修复内部矛盾：SaTokenConfig 中移除 presign 接口的 .notMatch，与 4.7.1 鉴权标注保持一致 |
| **v1.6** | 2026-04-21 | 新增Hutool通用工具类库依赖，后续开发优先使用其字符串、日期、加密、文件等工具API，避免重复造轮子 |
| **v1.7** | 2026-04-21 | 新增TosController两个接口定义（presign/complete），补充DTO字段和完整接口说明，fileKey生成规则加入businessId

---

## 目录

- [1. 技术栈与基础配置](#1-技术栈与基础配置)
- [2. 全局约定](#2-全局约定)
  - [2.1 统一响应格式](#21-统一响应格式)
  - [2.2 错误码规范](#22-错误码规范)
  - [2.3 前端错误处理约定](#23-前端错误处理约定)
  - [2.4 Sa-Token 鉴权约定](#24-sa-token-鉴权约定)
  - [2.5 JSON 字段 TypeHandler 配置](#25-json-字段-typehandler-配置)
  - [2.6 代码注释规范](#26-代码注释规范)
  - [2.7 SpringDoc / Swagger API 文档](#27-springdoc--swagger-api-文档)
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
- [7. Redis 与分布式锁规范](#7-redis-与分布式锁规范)
- [8. AI 任务轮询与熔断](#8-ai-任务轮询与熔断)
- [9. AI 一致性保障](#9-ai-一致性保障)
- [10. 安全保护](#10-安全保护)
- [11. Phase 2 预留功能](#11-phase-2-预留功能)
- [12. 进度追踪清单](#12-进度追踪清单)

---

## 1. 技术栈与基础配置

| 层 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17+ |
| 框架 | Spring Boot | 3.2.x |
| ORM | MyBatis Plus | 3.5.5+ |
| 认证 | Sa-Token (spring-boot3-starter) | 1.37+ |
| 缓存/锁 | Redis + Redisson | Spring Boot 内置 + 3.25.0 |
| 数据库 | MySQL | 8.0+ |
| 存储 | 火山 TOS (AWS S3 兼容 SDK) | 最新 |
| 视频处理 | FFmpeg | 系统命令调用 |
| 重试 | Spring Retry | Spring Boot 内置 |
| JSON | Jackson | Spring Boot 内置 |
| 工具类 | Hutool | 5.8.32+ |
| 构建 | Maven | 3.8+ |

> **Hutool 用途说明**：通用工具类库，后续开发优先使用其提供的字符串、日期、加密、文件等工具API，避免重复造轮子。

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

    <!-- Redisson (分布式锁替代原生 RedisTemplate) -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.25.0</version>
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

    <!-- Spring Retry (AI 任务重试) -->
    <dependency>
        <groupId>org.springframework.retry</groupId>
        <artifactId>spring-retry</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-aspects</artifactId>
    </dependency>

    <!-- Hutool 通用工具类库 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
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

### 2.6 代码注释规范 (P0)

**开发时，所有关键行必须有清晰明确的中文注释。** 具体要求：
- **关键业务逻辑**：每一段核心业务代码上方必须有注释说明"做什么"和"为什么这样做"。
- **复杂条件判断**：if/else 分支条件需注释判断的业务含义。
- **API 调用**：调用第三方 API（Seedance、TOS 等）需注释参数含义和返回值处理逻辑。
- **异步/并发代码**：线程池任务、分布式锁相关代码需注释并发场景和保护机制。
- **SQL/MyBatis**：查询语句需注释查询意图和索引使用情况。
- **错误处理**：catch 块需注释异常场景和恢复策略。

> 示例：
```java
// 火山引擎回调签名校验：防止伪造回调请求
// 验证流程：解析 Authorization Header → 提取签名参数 → 用 AccessKeySecret 重新计算签名 → 对比
WebhookSignature verifier = WebhookSignatureVerifier.verify(request);
```

### 2.7 SpringDoc / Swagger API 文档

> **原则**：MVP 阶段保持轻量，自动生成交互式 API 文档，减少手写文档成本。

#### 2.7.1 Maven 依赖

```xml
<!-- SpringDoc OpenAPI (Swagger UI) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

#### 2.7.2 SwaggerConfig 配置类

```java
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI漫剧生产平台 API")
                        .version("v1.0")
                        .description("AI漫剧生产平台后端接口文档"))
                // 按模块分组，便于前端查阅
                .addTagsItem(new Tag().name("用户模块").description("登录、注册等"))
                .addTagsItem(new Tag().name("项目模块").description("项目 CRUD、工作流操作"))
                .addTagsItem(new Tag().name("资产模块").description("资产 CRUD、确认、引用查询"))
                .addTagsItem(new Tag().name("内容模块").description("分集、分场、分镜 CRUD"))
                .addTagsItem(new Tag().name("流程引擎").description("工作流启动、状态查询、停止"))
                .addTagsItem(new Tag().name("AI任务").description("AI 任务提交、状态查询"))
                .addTagsItem(new Tag().name("存储模块").description("TOS 预签名 URL、上传通知"));
    }
}
```

#### 2.7.3 访问路径

| 用途 | 路径 |
|------|------|
| Swagger UI 页面 | `/swagger-ui.html` |
| OpenAPI JSON 规范 | `/v3/api-docs` |

> 启动后访问 `http://localhost:8080/swagger-ui.html` 即可查看交互式文档。

#### 2.7.4 Controller 层注解规范

所有 Controller **必须**标注 `@Tag` 和 `@Operation` 注解，确保文档完整：

```java
@RestController
@RequestMapping("/api/project")
@Tag(name = "项目模块", description = "项目 CRUD、工作流操作")
public class ProjectController {

    @PostMapping
    @Operation(summary = "创建项目", description = "创建新的漫剧项目，返回项目ID")
    public Result<Long> createProject(@Valid @RequestBody ProjectCreateRequest req) {
        // ...
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询项目详情", description = "根据ID获取项目完整信息")
    @Parameter(name = "id", description = "项目ID", required = true)
    public Result<ProjectVO> getProject(@PathVariable Long id) {
        // ...
    }
}
```

**注解使用要点：**
- `@Tag`：标注在 Controller 类上，按模块分组（name 必须与 SwaggerConfig 中一致）
- `@Operation`：标注在每个接口方法上，`summary` 用一句话概括，`description` 补充细节
- `@Parameter`：标注路径/查询参数，说明参数含义
- `@Schema`：标注在 DTO 字段上，说明字段含义（如 `@Schema(description = "项目名称")`）

#### 2.7.5 环境控制

**MVP 阶段仅在开发/测试环境启用，生产环境关闭：**

```yaml
# application-dev.yml / application-test.yml
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
  api-docs:
    enabled: true
    path: /v3/api-docs

# application-prod.yml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

> 后续如需对外提供 API 文档，可考虑导出静态 HTML 或使用 Knife4j 增强。

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

**分页响应统一使用 `PageResult<T>`：**

```java
@Data
public class PageResult<T> {
    private long total;        // 总记录数
    private int page;          // 当前页码 (1-based)
    private int size;          // 每页大小
    private boolean hasNext;   // 是否有下一页
    private List<T> list;      // 当前页数据

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setHasNext((long) page * size < total);
        return result;
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
| `42900-42999` | 频率限制 | `42900` 请求频率过高 |
| `50000-50099` | 系统异常 | `50000` 服务器内部错误 |
| `51000-51099` | AI服务异常 | `51000` AI调用失败 |
| `51100-51199` | 存储异常 | `51100` TOS上传失败 |

**细分错误码（前端高频场景）：**

| 错误码 | 场景 | 前端交互 |
|--------|------|----------|
| `40001` | 用户名或密码错误 | 表单高亮对应字段 |
| `40002` | 字段校验失败（通用） | 表单高亮对应字段 |
| `40003` | 文件类型不支持 | Toast 提示允许的类型 |
| `40004` | 文件大小超限 | Toast 提示最大限制 |
| `40005` | 预签名 URL 已过期 | 自动重新请求 presign |
| `40100` | 未登录 | 跳转登录页 |
| `40101` | Token 过期 | 跳转登录页 |
| `40300` | 无操作权限 | Toast "无操作权限" |
| `40301` | 非项目创建者无权操作 | 跳转无权限页 |
| `40400` | 资源不存在 | Toast 提示 |
| `40900` | 项目正在执行中 | Toast 提示 |
| `40901` | 资产已被分镜引用，不可删除 | Toast 提示引用关系 |
| `40902` | 分镜状态不支持当前操作 | 禁用对应按钮 |
| `40903` | 数据已被修改，请刷新后重试 | Toast 提示 + 刷新 |
| `42900` | 请求频率过高（AI 调用限流） | 倒计时后重试 |
| `50000` | 服务器内部错误 | 全局错误页 |
| `51000` | AI 调用失败 | Toast 提示 |
| `51001` | AI 模型超时/排队中 | 展示排队状态 |
| `51002` | AI 生成内容为空/不合格 | 提示重新生成 |
| `51100` | TOS 上传失败 | Toast 提示 |
| `51101` | TOS 存储空间不足 | 联系管理员 |

### 2.3 前端错误处理约定

前后端对错误处理行为达成共识：

| HTTP 状态码 | 业务码范围 | 前端行为 |
|------------|-----------|---------|
| `401` | `40100-40199` | 自动跳转登录页 |
| `403` | `40300-40399` | Toast "无操作权限" |
| `200` | `40000-40099` | 根据错误码做对应交互（表单高亮/Toast 等） |
| `200` | `40900-40999` | Toast 提示业务冲突原因 |
| `500` | `50000-50099` | 展示全局错误页 |
| `200` | `51000-51199` | Toast 提示服务异常 |

**约定：**
- 鉴权相关错误（401xx、403xx）返回对应 HTTP 状态码（401/403）
- 其他业务错误统一返回 HTTP 200 + 业务码
- 前端 axios 拦截器根据 HTTP 状态码 + `Result.code` 统一处理

### 2.4 Sa-Token 鉴权约定

```java
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**")
                .notMatch("/api/user/login")
                .notMatch("/error")
                .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
```

**前端请求约定：**
- 请求头: `Authorization: Bearer {token}`
- Sa-Token 自动从 header 中提取并校验
- 用户ID 获取: `StpUtil.getLoginIdAsLong()`
- Token 有效期 24h，活跃超时 30min，允许同端并发登录

### 2.5 JSON 字段 TypeHandler 配置

```java
@Configuration
@MapperScan("com.lanyan.aidrama.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
```

**实体类 JSON 字段标注示例：**
```java
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
-- AI漫剧生产平台 数据库初始化脚本 v1.2
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
```

### DDL 变更说明（v1.1 → v1.2）

```sql
-- 新增字段
ALTER TABLE project ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE workflow_task ADD COLUMN sub_step VARCHAR(50) DEFAULT NULL COMMENT '子步骤(断点续跑用)';
ALTER TABLE ai_task ADD COLUMN next_poll_time DATETIME DEFAULT NULL COMMENT '下次轮询时间(指数退避)';
ALTER TABLE ai_task ADD COLUMN poll_count INT NOT NULL DEFAULT 0 COMMENT '已轮询次数';
ALTER TABLE ai_task ADD COLUMN last_poll_time DATETIME DEFAULT NULL COMMENT '上次轮询时间';

-- 新增索引
ALTER TABLE shot ADD INDEX idx_scene_deleted_sort (scene_id, deleted, sort_order);
ALTER TABLE episode ADD INDEX idx_project_deleted_sort (project_id, deleted, sort_order);
ALTER TABLE scene ADD INDEX idx_episode_deleted_sort (episode_id, deleted, sort_order);
ALTER TABLE ai_task ADD INDEX idx_status_next_poll (status, next_poll_time);

-- 新增唯一约束
ALTER TABLE workflow_task ADD UNIQUE KEY uk_project_step_episode (project_id, step_type, episode_id);
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
  "password": "***"
}
```

响应体:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "string",
    "userId": "long",
    "username": "string",
    "nickname": "string"
  }
}
```

**GET /api/user/info**

响应体:
```json
{
  "code": 0,
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

---

### 4.2 项目模块 (project)

> 负责项目 CRUD、小说文件 TOS 路径绑定、流程配置保存。

#### 4.2.1 Controller: `ProjectController`

| 接口 | 方法 | 路径 | 鉴权 | 状态 |
|------|------|------|------|------|
| 项目列表(分页) | `GET` | `/api/project/list` | 是 | `[ ]` |
| 创建项目 | `POST` | `/api/project` | 是 | `[ ]` |
| 项目详情 | `GET` | `/api/project/{id}` | 是 | `[ ]` |
| 更新项目 | `PUT` | `/api/project/{id}` | 是 | `[ ]` |
| 删除项目 | `DELETE` | `/api/project/{id}` | 是 | `[ ]` |
| 保存流程配置 | `PUT` | `/api/project/{id}/workflow` | 是 | `[ ]` |
| 项目级分镜查询(分页) | `GET` | `/api/project/{projectId}/shots` | 是 | `[ ]` |

**GET /api/project/list**

查询参数: `page` (int, 可选, 默认1), `size` (int, 可选, 默认10)

响应体:
```json
{
  "code": 0,
  "data": {
    "total": "long",
    "page": 1,
    "size": 10,
    "hasNext": true,
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

**PUT /api/project/{id}/workflow**

请求体:
```json
{
  "version": "int, 必填, 当前乐观锁版本号",
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

**GET /api/project/{projectId}/shots**（新增）

> 项目级分镜聚合查询，支持跨分场查看所有分镜。

查询参数: `sceneId` (long, 可选, 按分场过滤), `status` (int, 可选, 按状态过滤), `page` (int, 默认1), `size` (int, 默认20)

响应体: `PageResult<ShotVO>`，ShotVO 内嵌 `assetRefs` 和 `currentAiTask`。

#### 4.2.2 Service: `ProjectService`

| 方法 | 说明 |
|------|------|
| `PageResult<ProjectVO> listProjects(Long userId, int page, int size)` | 分页查用户项目列表，自动过滤 deleted=1 |
| `Long createProject(ProjectCreateRequest req, Long userId)` | 创建项目，绑定 user_id |
| `ProjectVO getProjectDetail(Long id)` | 查详情，校验权限（项目归属） |
| `void updateProject(Long id, ProjectUpdateRequest req)` | 更新项目字段 |
| `void deleteProject(Long id)` | 逻辑删除，需校验 execution_lock=0 |
| `void saveWorkflowConfig(Long id, WorkflowConfigRequest req)` | 保存 workflow_config JSON，仅允许 status=草稿 时修改，带乐观锁 |

> **乐观锁说明**：project 表新增 `version` 字段，Entity 加 `@Version` 注解。`saveWorkflowConfig` 请求体需传入当前版本号，并发更新时抛出 `OptimisticLockException`，返回错误码 `40903 数据已被修改，请刷新后重试`。

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
| 资产引用查询(分页) | `GET` | `/api/asset/{assetId}/references` | 是 | `[ ]` |

**GET /api/asset/{assetId}/references**（新增）

> 查询资产被哪些分镜引用，用于资产详情页展示引用关系。

查询参数: `page` (int, 默认1), `size` (int, 默认20)

响应体: `PageResult<ShotReferenceVO>`
```json
{
  "total": 5,
  "page": 1,
  "size": 20,
  "hasNext": false,
  "list": [{
    "shotId": "long",
    "sceneId": "long",
    "episodeId": "long",
    "shotStatus": "int"
  }]
}
```

#### 4.3.2 Service: `AssetService`

| 方法 | 说明 |
|------|------|
| `List<AssetVO> listAssets(Long projectId, String assetType)` | 查项目下某类型资产列表 |
| `Long createAsset(Long projectId, AssetCreateRequest req)` | 创建资产，校验 projectId 归属 |
| `void updateAsset(Long id, AssetUpdateRequest req)` | 更新资产 |
| `void deleteAsset(Long id)` | 逻辑删除，需检查 shot_asset_ref 是否有关联 |
| `void confirmAsset(Long id)` | 确认资产（status=1） |
| `PageResult<ShotReferenceVO> getAssetReferences(Long assetId, int page, int size)` | 查询资产被哪些分镜引用 |

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
| 分镜列表(分页) | `GET` | `/api/scene/{sceneId}/shots` | 是 | `[ ]` |
| 创建分镜 | `POST` | `/api/scene/{sceneId}/shots` | 是 | `[ ]` |
| 更新分镜 | `PUT` | `/api/shot/{id}` | 是 | `[ ]` |
| 删除分镜 | `DELETE` | `/api/shot/{id}` | 是 | `[ ]` |
| 批量审核 | `POST` | `/api/shot/batch-review` | 是 | `[ ]` |
| 绑定资产到分镜 | `POST` | `/api/shot/{shotId}/assets` | 是 | `[ ]` |
| 解绑分镜资产 | `DELETE` | `/api/shot/{shotId}/assets/{assetId}` | 是 | `[ ]` |

**GET /api/scene/{sceneId}/shots**

> **v1.2 变更**：增加分页参数

查询参数: `page` (int, 可选, 默认1), `size` (int, 可选, 默认20), `status` (int, 可选, 按状态过滤)

响应体: `PageResult<ShotVO>`
```json
{
  "code": 0,
  "data": {
    "total": "long",
    "page": 1,
    "size": 20,
    "hasNext": true,
    "list": [{
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
      }],
      "currentAiTask": {
        "taskId": "long",
        "taskType": "string, image_gen/video_gen",
        "status": "int, 0-提交中 1-处理中 2-成功 3-失败"
      }
    }]
  }
}
```

> **v1.2 变更**：ShotVO 新增 `currentAiTask` 字段，通过 LEFT JOIN ai_task (WHERE shot_id = ? ORDER BY id DESC LIMIT 1) 内嵌当前最新 AI 任务状态。前端可据此展示"生成中"状态及任务详情。

**POST /api/shot/batch-review**

> **v1.2 变更**：响应增加结果明细

请求体:
```json
{
  "shotIds": ["long, 必填, 分镜ID数组"],
  "action": "string, 必填, approve(通过) / reject(打回)",
  "comment": "string, 打回时必填, 审核意见"
}
```

响应体:
```json
{
  "code": 0,
  "data": {
    "totalCount": 10,
    "successCount": 8,
    "failedCount": 2,
    "failedDetails": [
      { "shotId": 123, "reason": "分镜状态为已打回，无法再次打回" }
    ]
  }
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
| `PageResult<ShotVO> listShots(Long sceneId, int page, int size, Integer status)` | 分页查分场下分镜，含 assetRefs + currentAiTask |
| `Long createShot(Long sceneId, ShotCreateRequest req)` | 创建分镜 |
| `void updateShot(Long id, ShotUpdateRequest req)` | 更新分镜 |
| `BatchReviewResult batchReviewShots(List<Long> shotIds, String action, String comment)` | 批量审核，返回成功/失败明细 |
| `void bindAssetToShot(Long shotId, Long assetId, String assetType)` | 绑定资产到分镜 |
| `void unbindAssetFromShot(Long shotId, Long assetId)` | 解绑 |

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
| SSE进度推送 | `GET` | `/api/project/{id}/workflow/stream` | 是 | Phase 2 |

> **Phase 2 预留**：`GET /api/project/{id}/workflow/stream` 使用 Spring WebFlux SseEmitter 实现 EventStream 推送，MVP 阶段不实现。

**GET /api/project/{id}/workflow/status**

> **v1.2 变更**：增加精细进度字段

响应体:
```json
{
  "code": 0,
  "data": {
    "executionLock": "int, 0-未执行 1-执行中",
    "currentStep": "string, 当前执行的 stepType",
    "currentEpisodeId": "long, 当前处理的分集ID",
    "currentEpisodeTitle": "string, 当前分集标题",
    "totalEpisodes": "int, 总分集数",
    "overallProgress": "int, 整体百分比 0-100",
    "totalShots": "int, 总待处理分镜数",
    "processedShots": "int, 已处理分镜数",
    "estimatedRemainingSeconds": "int, 预估剩余秒数",
    "steps": [{
      "stepType": "string",
      "stepOrder": "int",
      "status": "int, 0-未执行 1-执行中 2-成功 3-失败 4-待审核",
      "progress": "int, 步骤内部进度百分比 0-100",
      "currentDetail": "string, 用户可读描述, 如'正在生成第45/100个分镜'",
      "errorMsg": "string",
      "reviewComment": "string"
    }]
  }
}
```

> **v1.2 变更说明**：
> - `overallProgress`：综合各步骤完成度和子任务进度加权计算
> - `processedShots`：在 image_gen/video_gen 步骤执行时实时更新
> - `estimatedRemainingSeconds`：根据历史执行时长动态估算（初始简化为固定值）
> - `currentEpisodeId` / `currentEpisodeTitle` / `totalEpisodes`：前端展示"正在处理第X集/共Y集"
> - 步骤级 `progress`：每个步骤内部百分比进度

**POST /api/project/{id}/workflow/review**

> **v1.2 变更**：审核通过/打回后不再持锁等待，改为分配新线程继续执行

请求体:
```json
{
  "stepType": "string, 必填, 审核步骤类型",
  "action": "string, 必填, approve(通过) / reject(打回)",
  "comment": "string, 可选, 审核意见"
}
```

行为:
- `approve`: 将该 workflow_task 状态置为 2（成功），分配新的 @Async 线程继续执行下一步
- `reject`: 将该 workflow_task 状态置为 3（失败），记录 errorMsg，停止执行，释放 Redis 锁

#### 4.5.2 Service: `WorkflowService`

| 方法 | 说明 |
|------|------|
| `void startWorkflow(Long projectId)` | 校验 lock → 初始化 workflow_task → 加 Redisson 锁 → @Async 触发引擎 |
| `WorkflowStatusVO getWorkflowStatus(Long projectId)` | 查 workflow_task 表最新状态，含精细进度 |
| `void reviewStep(Long projectId, ReviewRequest req)` | 审核通过/打回，分配新线程继续 |
| `void stopWorkflow(Long projectId)` | 停止执行 |
| `void recoverWorkflow(Long projectId)` | 服务重启后恢复执行中的流程（基于 sub_step 断点续跑） |

---

### 4.6 AI任务模块 (aitask)

> 负责 AI 图片/视频生成的异步提交、结果轮询、状态管理。

#### 4.6.1 Controller: `AiTaskController`

| 接口 | 方法 | 路径 | 鉴权 | 状态 |
|------|------|------|------|------|
| 查询AI任务状态 | `GET` | `/api/ai/task/{taskId}` | 是 | `[ ]` |
| 查询分镜最新AI任务 | `GET` | `/api/ai/task/latest` | 是 | `[ ]` |

> **v1.2 变更**：移除手动触发 image-gen/video-gen 接口（由 WorkflowEngine 内部驱动），保留查询接口。

**GET /api/ai/task/latest**（新增）

查询参数: `shotId` (long, 必填)

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
| `Long submitImageGenTask(ImageGenRequest req, Long userId)` | 创建 ai_task(status=0) → @Async 调用 Seedance |
| `Long submitVideoGenTask(VideoGenRequest req, Long userId)` | 同上，任务类型为 video_gen |
| `AiTaskVO getTaskStatus(Long taskId)` | 查 ai_task 表状态 |
| `AiTaskVO getLatestTaskByShotId(Long shotId)` | 按 shotId 查最新 AI 任务 |
| `void pollTaskResult(Long taskId)` | 定时任务调用（指数退避轮询） |

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
    // 查询任务结果（带重试）
    @Retryable(
        value = {ApiException.class, TimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public TaskResult queryTask(String providerTaskId);
    // 下载结果到临时文件
    public byte[] downloadResult(String resultUrl);

    // 防止 API Key 意外泄露到日志
    @Override
    public String toString() {
        return "SeedanceClient{apiUrl='" + apiUrl + "', apiKey='***'}";
    }
}
```

> **v1.2 变更**：queryTask 方法增加 `@Retryable` 注解，连续失败触发熔断。

#### 4.6.4 AI 任务重试与熔断

- **重试策略**：通过 Spring Retry 实现，超时/限流异常最多重试 3 次，退避延迟 5s → 10s → 20s
- **熔断机制**（手动实现）：
  - 记录 Seedance API 连续失败次数
  - 连续失败 5 次后进入**熔断状态**（30s 内不再提交新任务）
  - 熔断期间新任务直接标记为 `3-失败`，错误信息 "AI服务暂时不可用，请稍后重试"
  - 30s 后进入**半开状态**，允许 1 个请求试探
- **任务超时**：`poll_count > 30`（约 30 分钟无结果）时自动标记失败

---

### 4.7 存储模块 (storage)

> 负责火山 TOS 预签名 URL 生成、结果文件上传、上传完成通知。

#### 4.7.1 Controller: `TosController`

| 接口 | 方法 | 路径 | 鉴权 | 状态 |
|------|------|------|------|------|
| 获取预签名上传URL | `POST` | `/api/tos/presign` | 是 | `[ ]` |
| 上传完成通知 | `POST` | `/api/tos/complete` | 是 | `[ ]` |

> **v1.2 变更**：预签名接口鉴权标注修正为"是"。新增上传完成通知接口。

**POST /api/tos/presign**

请求体:
```json
{
  "fileName": "string, 必填, 原始文件名",
  "contentType": "string, 必填, MIME类型, 如 image/png, video/mp4",
  "source": "string, 必填, 枚举: frontend(前端上传) / backend(后端内部上传)",
  "businessId": "long, 必填, 关联业务ID，如项目ID、资产ID"
}
```

> **v1.7 变更**：移除 `projectDir` 字段，新增 `source`（枚举）和 `businessId`（业务关联ID）字段

响应体:
```json
{
  "code": 0,
  "data": {
    "uploadUrl": "string, 预签名PUT URL",
    "fileKey": "string, TOS中存储的唯一文件Key",
    "expireSeconds": "int, 预签名有效期(秒), 默认3600"
  }
}
```

> **v1.7 变更**：移除 `maxFileSize` 和 `allowedContentTypes`，fileKey 生成规则改为 `{source}/{businessId}/{yyyyMMdd}/{fileType}/{8位随机数}_{URL编码文件名}`

**POST /api/tos/complete**（新增）

> **v1.2 变更**：前端直传 TOS 后的上传完成通知接口，形成上传闭环

请求体:
```json
{
  "fileKey": "string, 必填, TOS中存储的文件唯一Key",
  "businessId": "long, 必填, 关联业务ID，如项目ID、资产ID",
  "fileSize": "long, 必填, 文件大小（字节）",
  "originalName": "string, 必填, 原始文件名"
}
```

> **v1.7 变更**：请求体改为 `fileKey` + `businessId` + `fileSize` + `originalName`，移除 `projectId`、`fileType`、`metadata` 嵌套结构

后端处理逻辑:
1. HEAD 请求校验文件确实存在于 TOS
2. 校验文件大小是否匹配
3. 生成并返回公网访问 URL
4. 后续业务关联逻辑（如更新数据库记录）再完善
5. 如果 HEAD 校验失败，返回错误码 `51102 文件校验失败：文件不存在或链接已过期`

#### 4.7.2 Service: `TosService`

| 方法 | 说明 |
|------|------|
| `PresignResult generatePresignUrl(String fileName, String contentType, String projectDir)` | 生成 PUT 预签名 URL，TTL=1h |
| `void completeUpload(TosCompleteRequest req)` | 上传完成通知，校验 TOS + 更新数据库 |
| `String uploadFromUrl(String sourceUrl, String targetKey)` | 从第三方URL下载到TOS（AI结果回传场景） |
| `String uploadFromBytes(byte[] data, String targetKey)` | 直接上传字节数据 |
| `void deleteFile(String key)` | 删除 TOS 文件 |

> **Phase 2 预留**：分片上传接口 `POST /api/tos/presign-multipart`、`POST /api/tos/presign-part`、`POST /api/tos/complete-multipart`

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
    │       │       │
    │       │       ├── 每个 Node 内部按 sub_step 执行：
    │       │       │   sub_step = "submit" → 提交任务到第三方AI
    │       │       │   sub_step = "polling" → 轮询AI任务结果
    │       │       │   sub_step = "download" → 下载结果到本地
    │       │       │   sub_step = "upload_tos" → 上传结果到TOS
    │       │       │
    │       │       └── 每个 sub_step 完成后更新 output_data
    │       │
    │       ├── 2e. 执行成功:
    │       │       ├── step.review == true:
    │       │       │   → 标记 status=4(待审核), 释放当前线程
    │       │       │   → Redis 锁不释放，由 Redisson 看门狗续期（最长2h）
    │       │       │   → 等待前端 review 接口
    │       │       └── step.review == false:
    │       │           → 标记 status=2(成功), 继续下一步
    │       └── 2f. 执行失败:
    │               → 标记 status=3(失败), 记录 error_msg, 终止执行
    └── 3. 所有步骤完成:
            → project.status=2(已完成), execution_lock=0
            → 释放 Redisson 锁
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

> **v1.2 变更：幂等性保证**
> - 每个 NodeExecutor 内部实现幂等逻辑，支持断点续跑不重复创建数据
> - `ImportNodeExecutor`：创建 episode 前先查 `project_id + title` 是否已存在
> - `AssetExtractNodeExecutor`：创建 asset 前先查 `project_id + name + asset_type` 是否已存在
> - `ShotGenNodeExecutor`：创建 shot 前先查 `scene_id + sort_order` 是否已存在
> - 在 `output_data` 中保存已创建的子实体 ID 列表，恢复时跳过已创建的

### 5.3 Redisson 分布式锁实现（v1.2 变更）

> **v1.2 变更**：使用 Redisson RLock 替代原生 RedisTemplate setIfAbsent，自带看门狗自动续期 + 安全释放

```java
// WorkflowService.startWorkflow()
public void startWorkflow(Long projectId) {
    String lockKey = "workflow:lock:" + projectId;
    RLock lock = redissonClient.getLock(lockKey);

    // 尝试获取锁，等待0秒，最大锁时间2小时（看门狗自动续期）
    boolean acquired = lock.tryLock(0, 2, TimeUnit.HOURS);
    if (!acquired) {
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
    RLock lock = redissonClient.getLock(lockKey);

    // Redisson 自带 Lua 脚本校验归属，安全释放
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
    projectMapper.updateExecutionLock(projectId, 0);
}
```

**Redis 锁关键特性：**
- **看门狗续期**：RLock 自带看门狗，默认 30s 续期周期，锁持有期间自动续期
- **最大 TTL**：`tryLock(0, 2, TimeUnit.HOURS)` 设置最大锁时间 2 小时，防止持有者 crash 后锁永不释放
- **安全释放**：通过 Lua 脚本校验锁归属，避免误删其他线程的锁
- **审核节点处理**：审核节点不在线程内阻塞等待，设置 status=4(待审核) 后释放当前线程，Redis 锁由看门狗持续续期

### 5.4 状态机驱动的断点续跑（v1.2 变更）

> **v1.2 变更**：将 WorkflowEngine 改造为状态机驱动，支持细粒度断点续跑

```java
// WorkflowService.recoverWorkflow() - 增强版
public void recoverWorkflow(Long projectId) {
    // 1. 查找最后一个 status=1(执行中) 的 workflow_task
    WorkflowTask runningTask = workflowTaskMapper.selectLatestRunning(projectId);
    if (runningTask == null) {
        // 无待恢复任务，可能是上次异常未清理
        forceUnlock(projectId);
        return;
    }

    // 2. 根据 sub_step 决定恢复策略
    String subStep = runningTask.getSubStep();
    switch (subStep) {
        case "submit":
            // 提交阶段中断 → 重新提交 AI 任务
            retrySubmit(runningTask);
            break;
        case "polling":
            // 轮询阶段中断 → 通过 provider_task_id 重新查询第三方 API 状态
            // 不重新提交，避免重复任务
            resumePolling(runningTask);
            break;
        case "download":
            // 下载阶段中断 → 重新下载
            resumeDownload(runningTask);
            break;
        case "upload_tos":
            // 上传阶段中断 → 重新上传
            resumeUpload(runningTask);
            break;
        default:
            // 无 sub_step 记录 → 从头重新执行该步骤
            reexecuteStep(runningTask);
    }
}
```

**恢复流程核心逻辑：**
1. 查询 workflow_task 获取 `sub_step` 和 `output_data`
2. 根据 `sub_step` 决定恢复策略（如上）
3. 如果是 AI 任务，通过 `provider_task_id` 查第三方 API 状态
4. 如果 AI 任务已完成（服务中断期间第三方已完成），直接走 download → upload_tos
5. 恢复完成后正常释放 Redis 锁 + 清空 execution_lock

### 5.5 execution_lock 与 Redis 锁分层职责（v1.2 变更）

> **v1.2 变更**：明确分层职责，解决双重状态不一致

| 锁机制 | 用途 | 权威性 |
|--------|------|--------|
| **Redis 锁（Redisson RLock）** | 防止并发执行 | **权威锁**，带看门狗续期 + 最大 TTL 兜底 |
| **execution_lock（数据库字段）** | 查询展示 + RecoveryRunner 恢复触发信号 | 冗余字段，不作为锁判断依据 |

**RecoveryRunner 恢复逻辑增强：**
```
1. 查询 execution_lock=1 的项目
2. 尝试获取 Redis 锁（非阻塞 tryLock）
3. 如果获取成功 → 说明原锁已过期/丢失，执行恢复流程
4. 如果获取失败 → 说明有其他实例正在执行，跳过
5. 恢复完成后，正常释放 Redis 锁 + 清空 execution_lock
6. 兜底清理：如果 execution_lock=1 但没有任何 running 的 workflow_task，强制解锁
```

### 5.6 执行时序图（v1.2 更新）

```
Frontend                     Backend                      Redisson         Seedance
   │                            │                            │                │
   ├── POST /workflow/start ──→│                            │                │
   │                            ├── tryLock workflow:lock ──→│                │
   │                            ├── UPDATE execution_lock    │                │
   │                            ├── @Async execute ──┐       │                │
   │                            │                    │       │                │
   │←──── {code:0} ────────────│                    │       │                │
   │                            │                    │       │                │
   │                            │  遍历步骤...        │       │                │
   │                            ├── INSERT workflow_task      │                │
   │                            ├── 执行 NodeExecutor         │                │
   │                            │   (按 sub_step 推进)        │                │
   │                            │                             │                │
   │                            │                             │  调用AI ─────→│
   │                            │                             │                │
   │  (前端3s轮询)              │                             │                │
   ├── GET /workflow/status ──→│                             │                │
   │←──── {steps:[...]} ───────│                             │                │
   │                            │                             │                │
   │  (审核节点到达时)           │                             │                │
   │                            ├── UPDATE status=4(待审核)    │                │
   │                            ├── 释放当前线程               │                │
   │                            ├── RLock 看门狗持续续期       │                │
   │                            │                             │                │
   ├── POST /workflow/review ──→│                             │                │
   │                            ├── UPDATE status=2(成功)      │                │
   │                            ├── 新 @Async 线程继续执行     │                │
   │                            │                             │                │
   │                            ├── unlock workflow:lock ────→│                │
```

---

## 6. 异步任务与线程池

### 6.1 线程池配置（v1.2 变更）

> **v1.2 变更**：调整线程池参数 + 调度器独立线程池 + 拒绝策略改为 AbortPolicy

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("workflowExecutor")
    public ThreadPoolTaskExecutor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);          // v1.1: 3 → v1.2: 5
        executor.setMaxPoolSize(8);           // v1.1: 5 → v1.2: 8
        executor.setQueueCapacity(20);        // v1.1: 10 → v1.2: 20
        executor.setThreadNamePrefix("workflow-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);         // v1.1: 5 → v1.2: 10
        executor.setMaxPoolSize(20);          // v1.1: 10 → v1.2: 20
        executor.setQueueCapacity(50);        // v1.1: 20 → v1.2: 50
        executor.setThreadNamePrefix("ai-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
```

**拒绝策略变更**：`CallerRunsPolicy` → `AbortPolicy`
- CallerRunsPolicy 会导致 Tomcat 工作线程被阻塞执行任务，前端请求超时
- AbortPolicy 直接抛 `RejectedExecutionException`，全局异常处理器捕获后返回 `42900 请求频率过高`

### 6.2 调度器独立线程池（v1.2 新增）

> **v1.2 变更**：@Scheduled 默认单线程会阻塞后续定时任务，需独立线程池

```java
@Configuration
@EnableScheduling
public class ScheduleConfig {

    // 调度器独立线程池，避免阻塞
    @Bean("schedulerExecutor")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("scheduler-");
        return scheduler;
    }
}
```

### 6.3 定时任务

```java
@Component
public class AiTaskScheduler {

    // 指数退避轮询处理中的 AI 任务结果
    @Scheduled(fixedDelay = 5000, scheduler = "schedulerExecutor")
    public void pollAiTaskResults() {
        // 按 next_poll_time 排序查询，只轮询到期任务
        // SELECT * FROM ai_task WHERE status=1 AND next_poll_time <= NOW()
        // ORDER BY next_poll_time LIMIT 50
    }

    // 每 10min 清理过期的 Redis 状态缓存
    @Scheduled(fixedDelay = 600000, scheduler = "schedulerExecutor")
    public void cleanExpiredCache() {
        // Redis TTL 自动过期为主，此为兜底
    }
}
```

---

## 7. Redis 与分布式锁规范

### 7.1 Key 规范

| 用途 | Key 格式 | TTL | 说明 |
|------|----------|-----|------|
| Sa-Token session | `satoken:token:{token}` | Sa-Token 默认(24h) | 框架自动管理 |
| 流程执行锁 | `workflow:lock:{projectId}` | 最大 2h（Redisson RLock） | 防重复执行，看门狗自动续期 |
| 流程停止标记 | `workflow:stop:{projectId}` | 2h | 用户停止后写入，引擎检查此标记 |
| AI任务状态缓存 | `ai:task:{taskId}` | 24h | 缓存最新状态，减少DB查询 |
| API限流 | `api:rate:{userId}` | 1min | 滑动窗口限流（可选） |

### 7.2 Redisson 配置

```yaml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://${spring.redis.host}:${spring.redis.port}"
          password: "${spring.redis.password:}"
          connectionPoolSize: 64
          connectionMinimumIdleSize: 10
          idleConnectionTimeout: 10000
          timeout: 3000
```

---

## 8. AI 任务轮询与熔断

### 8.1 指数退避轮询策略（v1.2 变更）

> **v1.2 变更**：v1.1 每 5s 全量扫描所有 status=1 任务 → v1.2 指数退避 + 按 next_poll_time 查询

**轮询策略：**

| 轮询次数 | 间隔 | 说明 |
|---------|------|------|
| 首次（任务提交后） | 10s | 给 AI 模型留出初始处理时间 |
| 第 1 次轮询未完成 | 20s | 指数退避 |
| 第 2 次轮询未完成 | 40s | 指数退避 |
| 第 3 次及以后 | 60s（最大间隔） | 封顶 |
| poll_count > 30 | 标记超时失败 | 约 30 分钟无结果 |

**定时查询 SQL：**
```sql
SELECT * FROM ai_task
WHERE status = 1
  AND next_poll_time <= NOW()
ORDER BY next_poll_time
LIMIT 50;
```

**next_poll_time 更新逻辑：**
```java
private void updateNextPollTime(AiTask task) {
    long pollCount = task.getPollCount();
    long delaySeconds;
    if (pollCount == 0) {
        delaySeconds = 10;
    } else if (pollCount == 1) {
        delaySeconds = 20;
    } else if (pollCount == 2) {
        delaySeconds = 40;
    } else {
        delaySeconds = 60; // 封顶
    }
    task.setNextPollTime(LocalDateTime.now().plusSeconds(delaySeconds));
    task.setPollCount(pollCount + 1);
    task.setLastPollTime(LocalDateTime.now());
    aiTaskMapper.updateById(task);
}
```

> **Phase 2 预留**：Webhook 回调（替代轮询），待 Seedance 支持 webhook 后切换。

### 8.2 AI 任务重试与熔断

- **重试**：Spring Retry @Retryable，maxAttempts=3，backoff delay=5s, multiplier=2
- **熔断**：连续失败 5 次 → 熔断 30s → 半开试探 1 次 → 成功则恢复
- **超时**：poll_count > 30 标记失败

---

## 9. AI 一致性保障

### 9.1 资产强绑定

分镜生成时，通过 `shot_asset_ref` 表查出关联资产，取每个资产的 `reference_images[0]`（主图），作为 Seedance 调用的 `reference_image` 参数传入。

### 9.2 Seedance 调用参数映射

```java
// ImageGenParams 构建
{
    "prompt": shot.getPromptEn(),
    "first_frame_image": null,
    "reference_images": [assetRef.getPrimaryImage(), ...],
    "style": project.getStylePreset().getArtStyle(),
    "resolution": "1024x1024",
    "seed": null
}

// VideoGenParams 构建
{
    "prompt": shot.getPromptEn(),
    "first_frame_image": shot.getGeneratedImageUrl(),
    "reference_images": [...],
    "duration": 5,
    "resolution": "1024x1024"
}
```

### 9.3 TOS Key 命名规范

```
projects/{projectId}/assets/{assetId}/{timestamp}_{fileName}
projects/{projectId}/episodes/{episodeId}/shots/{shotId}/image_v{version}.png
projects/{projectId}/episodes/{episodeId}/shots/{shotId}/video_v{version}.mp4
projects/{projectId}/output/final_{projectId}_{timestamp}.mp4
```

---

## 10. 安全保护

### 10.1 API Key 运行时保护（v1.2 新增）

> **v1.2 变更**：API Key 通过环境变量注入，需防止运行时泄露

1. **日志脱敏**：在 `application.yml` 中配置 Logback 脱敏规则，对包含 `key`、`secret`、`token` 的日志字段自动脱敏
2. **Actuator 保护**：
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info
     endpoint:
       env:
         show-values: NEVER
   ```
3. **SeedanceClient 覆写 toString()**：
   ```java
   @Override
   public String toString() {
       return "SeedanceClient{apiUrl='" + apiUrl + "', apiKey='***'}";
   }
   ```

### 10.2 密码加密

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // strength=12
}
```

---

## 11. Phase 2 预留功能

> 以下功能不在 MVP Sprint 中排期，但已设计好接口契约，后续可直接启用。

| 功能 | 接口 | 说明 |
|------|------|------|
| SSE 进度推送 | `GET /api/project/{id}/workflow/stream` | Spring WebFlux SseEmitter，替代 3s 轮询 |
| 分片上传 | `POST /api/tos/presign-multipart`<br>`POST /api/tos/presign-part`<br>`POST /api/tos/complete-multipart` | 大文件分片上传（>10MB） |
| Webhook AI 回调 | — | 替代轮询，待 Seedance 支持 webhook |

---

## 12. 进度追踪清单

### Sprint 1: 基础骨架 (Day 1-3)

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 1.1 | 数据库建表 + init.sql (v1.2 DDL) | 10张完整DDL + 索引 | `[ ]` |
| 1.2 | SpringBoot 骨架搭建 | 项目结构、pom.xml、application.yml | `[ ]` |
| 1.3 | 全局响应 + 异常处理 | Result<T>、PageResult<T>、GlobalExceptionHandler | `[ ]` |
| 1.4 | Sa-Token 配置 + 登录模块 | SaTokenConfig、UserController、UserService | `[ ]` |
| 1.5 | MyBatis Plus 配置 + TypeHandler | MybatisPlusConfig、JacksonTypeHandler、乐观锁插件 | `[ ]` |
| 1.6 | Redis + Redisson 配置 | RedisConfig、RedissonConfig、连接池配置 | `[ ]` |
| 1.7 | 火山 TOS 封装 + 预签名URL + 上传完成通知 | TosConfig、TosService、TosController | `[ ]` |
| 1.8 | AsyncConfig 线程池 (v1.2) | workflowExecutor + aiTaskExecutor + schedulerExecutor | `[ ]` |

### Sprint 2: 核心业务 (Day 4-7)

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 2.1 | 项目模块 CRUD + 乐观锁 | ProjectController/Service/Mapper + @Version | `[ ]` |
| 2.2 | 小说上传TOS闭环 | TosService.completeUpload | `[ ]` |
| 2.3 | 资产模块 CRUD | AssetController/Service/Mapper | `[ ]` |
| 2.4 | 资产确认 + 引用查询 | confirmAsset + getAssetReferences | `[ ]` |
| 2.5 | 分集 CRUD | Episode Controller/Service/Mapper | `[ ]` |
| 2.6 | 分场 CRUD | Scene Controller/Service/Mapper | `[ ]` |
| 2.7 | 分镜 CRUD (分页) | Shot Controller/Service/Mapper + PageResult | `[ ]` |
| 2.8 | 分镜-资产关联 CRUD | ShotAssetRef Controller/Service/Mapper | `[ ]` |
| 2.9 | 批量审核 (带明细) | batchReviewShots → BatchReviewResult | `[ ]` |
| 2.10 | 项目级分镜聚合查询 | GET /api/project/{projectId}/shots | `[ ]` |

### Sprint 3: 流程引擎 + AI集成 (Day 8-12)

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 3.1 | WorkflowEngine 状态机驱动引擎 | WorkflowEngine.java + sub_step 机制 | `[ ]` |
| 3.2 | 6种 NodeExecutor 实现 (幂等) | Import/AssetExtract/ShotGen/ImageGen/VideoGen/Export | `[ ]` |
| 3.3 | Redisson 分布式锁 | RLock + 看门狗 + 安全释放 | `[ ]` |
| 3.4 | 断点续跑 + 启动恢复 | recoverWorkflow (基于 sub_step) + RecoveryRunner | `[ ]` |
| 3.5 | 流程 Controller 层 | WorkflowController (start/status/review/stop) | `[ ]` |
| 3.6 | AiTaskService 异步处理 | submit + 指数退避 poll + @Async | `[ ]` |
| 3.7 | SeedanceClient 封装 + 重试 | HTTP 调用 + @Retryable + 熔断器 | `[ ]` |
| 3.8 | AI任务指数退避轮询 | @Scheduled + next_poll_time 策略 | `[ ]` |
| 3.9 | PromptEngine 提示词生成 | 强模型翻译+生成 prompt_en | `[ ]` |
| 3.10 | FFmpeg 导出 | ExportService (合并视频) | `[ ]` |

### Sprint 4: 联调测试 (Day 13-15)

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 4.1 | API消耗记录 + 统计 | api_call_log 写入 + 聚合查询接口 | `[ ]` |
| 4.2 | 预算控制 | 调用前余额检查 | `[ ]` |
| 4.3 | 全流程联调 | 导入→资产→分镜→生图→生视频→导出 | `[ ]` |
| 4.4 | 边界测试 | 断点续跑、并发锁、重试、熔断、权限校验 | `[ ]` |
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

# Actuator 安全
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    env:
      show-values: NEVER
```

## 附录 B: 项目包结构

```
com.lanyan.aidrama/
├── AiDramaApplication.java
├── common/
│   ├── Result.java                  # 统一响应
│   ├── PageResult.java              # 分页响应 (v1.2 新增)
│   ├── BusinessException.java       # 业务异常
│   └── GlobalExceptionHandler.java  # 全局异常处理
├── config/
│   ├── SaTokenConfig.java
│   ├── MybatisPlusConfig.java       # + 乐观锁插件
│   ├── RedisConfig.java
│   ├── RedissonConfig.java          # v1.2 新增
│   ├── AsyncConfig.java             # v1.2 调整
│   ├── ScheduleConfig.java          # v1.2 新增独立调度器
│   ├── TosConfig.java
│   └── RetryConfig.java             # v1.2 新增
├── entity/                          # 数据库实体（对应10张表）
│   ├── SysUser.java
│   ├── Project.java                 # + version 乐观锁
│   ├── Asset.java
│   ├── Episode.java
│   ├── Scene.java
│   ├── Shot.java
│   ├── ShotAssetRef.java
│   ├── WorkflowTask.java            # + sub_step
│   ├── AiTask.java                  # + next_poll_time, poll_count, last_poll_time
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
│   │   │   ├── WorkflowEngine.java    # v1.2 状态机驱动
│   │   │   └── RecoveryRunner.java    # v1.2 增强恢复
│   │   ├── executor/
│   │   │   ├── NodeExecutor.java
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
│   │       ├── SeedanceClient.java    # v1.2 + 重试/熔断/toString
│   │       └── dto/
│   └── storage/
│       ├── controller/TosController.java  # v1.2 + complete
│       └── service/TosService.java        # v1.2 + completeUpload
├── aspect/
│   ├── ApiCallLogAspect.java
│   └── CircuitBreakerAspect.java    # v1.2 新增(手动熔断)
└── scheduler/
    └── AiTaskScheduler.java         # v1.2 指数退避轮询
```

## 附录 C: DTO 命名规范

| 后缀 | 用途 | 示例 |
|------|------|------|
| `Request` | 接收前端请求体 | `LoginRequest`, `ProjectCreateRequest` |
| `VO` | 返回前端视图对象 | `UserInfoVO`, `ProjectVO`, `ShotVO` |
| `DTO` | 内部数据传输 | `ImageGenParams`, `TaskResult` |
| `Result` | 批量操作结果 | `BatchReviewResult` |

## 附录 D: 接口变更汇总（v1.1 → v1.2）

### 新增接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 上传完成通知 | POST | `/api/tos/complete` | 前端直传 TOS 后通知后端入库 |
| 项目级分镜查询 | GET | `/api/project/{projectId}/shots` | 跨分场聚合查询分镜（分页） |
| 资产引用查询 | GET | `/api/asset/{assetId}/references` | 查询资产被哪些分镜引用（分页） |
| 查询分镜最新AI任务 | GET | `/api/ai/task/latest` | 按 shotId 查最新 AI 任务 |

### 接口变更

| 接口 | 变更内容 |
|------|---------|
| `GET /api/project/list` | 响应改为 `PageResult<ProjectVO>`，增加 page/size/hasNext |
| `GET /api/scene/{sceneId}/shots` | 增加分页参数 page/size/status，响应改为 `PageResult<ShotVO>` |
| `GET /api/project/{id}/workflow/status` | 增加 overallProgress/totalShots/processedShots/estimatedRemainingSeconds/currentEpisode 等字段 |
| `POST /api/shot/batch-review` | 响应增加 BatchReviewResult（successCount/failedCount/failedDetails） |
| `POST /api/tos/presign` | 响应增加 maxFileSize + allowedContentTypes |
| `PUT /api/project/{id}/workflow` | 请求体增加 version 字段（乐观锁） |

---

## 评审问题处理总结

| 评审人 | P0 处理 | P1 处理 | P2 处理 |
|--------|---------|---------|---------|
| 小欧（前端） | 4/4 全部接受并落地 | 10/10 全部接受（1条折中方案） | 暂不排入 MVP |
| 阿典（后端） | 3/3 全部接受并落地 | 5/5 全部接受并落地 | 暂不排入 MVP |

**所有 P0/P1 问题均已在 v1.2 终版中落地。**

---

*文档版本: v1.4 | 作者: 老克 | 日期: 2026-04-20*
*评审状态: 已通过（小欧 ✓ 阿典 ✓）*
