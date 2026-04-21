# AI漫剧生产平台 — PRD 后端架构评审意见
**评审人**: 老克
**日期**: 2026-04-19
**PRD版本**: v1.0 MVP

---

## 一、数据库设计评审

### 1.1 缺失的通用字段（每张表都该有）

| 缺失字段 | 建议 |
|---------|------|
| `update_time` | 每张表都需要，审计/排查必备 |
| `update_by` / `created_by` | 协作者场景下必须知道谁改了什么 |
| `deleted` (逻辑删除) | 项目/资产/分镜都不该物理删除，MyBatis Plus 天然支持 |
| `tenant_id` (如果未来考虑多租户) | MVP可以先不做，但表结构留出来 |

### 1.2 各表具体问题

#### `sys_user`
- **缺少**: `last_login_time`, `password_update_time`
- **建议**: 加上 `login_ip` 字段方便安全审计

#### `project`
- `novel_content` 用 LONGTEXT 没问题，但如果小说几百万字，建议改成存 TOS 文件路径，数据库只存元信息。LONGTEXT 查询时拖慢所有涉及 project 的 SQL。
- `config_yaml_path` 指向本地文件系统的路径。如果服务重启/部署到不同机器，路径可能失效。**建议**: YAML 内容直接存到数据库 JSON 字段（`config_json`），或者统一走 TOS 存储。
- **缺少**: `deleted`, `update_time`, `total_cost`（累计 API 花费，避免每次都 join 聚合 api_call_log）

#### `asset`
- `reference_image_url` 只存一张图。实际工作中一个角色可能需要多视角参考图（正面/侧面/表情）。**建议**: 拆成 `asset_reference_images` 子表，或 JSON 数组存储多张图。
- **缺少索引**: `(project_id, asset_type)` 组合索引，前端按类型筛选资产时必用。
- **缺少**: `update_time`, `deleted`

#### `episode` / `scene`
- **缺少**: `update_time`, `deleted`
- 两张表都没有索引。查询场景通常是 `WHERE episode_id = ? ORDER BY sort_order`，需要加索引。

#### `shot`（最核心的表）
- `asset_refs` 存 JSON 数组。如果后续需要 "查找使用了某角色的所有分镜"，JSON 查询性能差且难维护。
- **强烈建议**: 增加 `shot_asset_ref` 关联表：
  ```sql
  CREATE TABLE shot_asset_ref (
    id BIGINT PK,
    shot_id BIGINT,
    asset_id BIGINT,
    asset_type VARCHAR(20),
    PRIMARY KEY (id),
    UNIQUE KEY uk_shot_asset (shot_id, asset_id)
  );
  ```
  这样既能高效查询，也方便统计资产使用频率。
- `version` 字段只是 INT。如果需要版本对比（PRD 提到了），建议设计 `shot_version` 表记录每个版本的 image_url/video_url/prompt 等，shot 表只存当前版本。或者至少 `version` 需要有唯一约束 `(scene_id, sort_order, version)`。
- **缺少**: `update_time`, `deleted`
- `generated_image_url` 和 `generated_video_url` 各只存一个。如果允许用户从多个生成结果中选最优，需要改成 JSON 数组或子表。

#### `workflow_task`
- **严重缺失**: `step_order`（步骤在流水线中的序号），没有这个无法按顺序查找下一步。
- **严重缺失**: `update_time`（需要知道步骤什么时候完成的）
- **严重缺失**: `review_comment`（审核节点需要记录审核意见）
- **严重缺失**: 没有 `episode_id`。一个项目可能有多个分集，流程是项目级别还是分集级别？PRD 没讲清楚。如果是分集级别执行，需要加 `episode_id`。
- 当前设计只能追踪"哪个步骤做了"，但无法追踪"哪个分镜的生图任务完成了"。建议在 `output_data` JSON 里明确规范结构，或者加 `target_id` / `target_type` 字段关联到具体的 shot。

#### `api_call_log`
- **缺少**: `task_id`（关联 workflow_task），否则无法追溯是哪次流程执行触发的调用
- **缺少**: `response_time_ms`（性能监控必备）
- 建议加 `(project_id, create_time)` 组合索引，消耗看板必用。

### 1.3 建议新增的表

| 表名 | 用途 |
|------|------|
| `asset_reference_images` | 一个资产多张参考图 |
| `shot_version` | 分镜多版本历史记录（如果要做版本对比） |
| `shot_asset_ref` | 分镜-资产关联（替代 JSON） |
| `system_config` | 全局配置（Seedance API Key、TOS 配置等），避免硬编码或本地 YAML |

---

## 二、技术方案坑点

### 2.1 Sa-Token 1.37 + Spring Boot 3.2

**兼容性问题**:
- Sa-Token 1.37.x 对 Spring Boot 3.x 的支持是通过 `sa-token-spring-boot3-starter` 提供的。
- **坑**: 如果你引入的是 `sa-token-spring-boot-starter`（不带 3），会与 Spring Boot 3.2 的 Jakarta EE 命名空间冲突，启动直接报错。
- **正确做法**:
  ```xml
  <dependency>
      <groupId>cn.dev33</groupId>
      <artifactId>sa-token-spring-boot3-starter</artifactId>
      <version>1.37.0</version>
  </dependency>
  ```
- Sa-Token 的 Redis 集成用 `sa-token-redis-jackson`，注意 Spring Boot 3.2 的 Redis 客户端默认从 Lettuce 切换，但 Sa-Token 的 Redis 序列化依赖的是 Jackson，版本要对齐。

**JWT 模式注意**:
- Sa-Token 默认用 Redis 存 session。如果用 JWT 无状态模式（`is-read-cookie=false`, `token-style=jwt`），则不需要 Redis，但踢人下线/强制登出就没法做了。
- **建议 MVP**: 用默认的 Redis session 模式，简单可靠。

### 2.2 MyBatis Plus 3.5 JSON 字段处理

**坑1: JSON 字段映射**
- MyBatis Plus 默认不自动处理 JSON 类型。MySQL 的 JSON 字段读出来是字符串，需要手动反序列化。
- **方案A（推荐）**: 在 Entity 字段上加 `@TableField(typeHandler = JacksonTypeHandler.class)`，同时实体类上加 `@TableName(value = "xxx", autoResultMap = true)`。
- **方案B**: 用 FastJSON2/Gson 在 Service 层手动序列化/反序列化。
- **注意**: `autoResultMap = true` 必须在 Mapper XML 中生效，如果用纯注解查询（`selectById` 等），MP 3.5.3+ 已经支持，但低版本可能有 bug。建议锁定 MP 版本 >= 3.5.5。

**坑2: JSON 字段更新**
- `updateById` 时，如果 JSON 字段内容没变（对象 equals 相同），MP 可能不会生成 UPDATE 语句（取决于 `FieldStrategy`）。如果需要强制更新 JSON 内的某个字段，需要用 `UpdateWrapper` 明确指定。

**坑3: JSON 字段查询**
- MySQL 8.0 支持 JSON 函数（`JSON_EXTRACT`, `JSON_CONTAINS`），但 MyBatis Plus 的 Wrapper 不直接支持 JSON 查询。需要写原生 SQL 或用 `apply("JSON_CONTAINS(asset_refs, ?)", value)`。

### 2.3 Redis 使用场景

PRD 没有明确 Redis 的用途。建议规划：

| 用途 | Key 设计 | TTL |
|------|---------|-----|
| Sa-Token session | `satoken:login:token:{token}` | 默认 |
| AI 任务轮询缓存 | `ai:task:{taskId}` → `{status, resultUrl}` | 24h |
| 流程执行锁 | `workflow:lock:{projectId}` | 5min |
| 资产确认状态缓存 | `asset:confirmed:{assetId}` | 长期 |
| API 调用频率限制 | `api:rate:{userId}:{endpoint}` | 1min |

**特别注意**:
- AI 任务轮询如果直接用 Redis 做状态存储，轮询频率建议 3-5 秒，避免打满 Redis QPS。
- 流程执行时需要加分布式锁（虽然是单体，但用户可能重复点击"开始执行"），用 Redis `SETNX` 即可。

### 2.4 其他技术坑

- **Spring Boot 3.2 的虚拟线程**: Java 21 才正式支持虚拟线程（Project Loom）。如果用 Java 17，不能用 `spring.threads.virtual.enabled=true`。如果未来升级 Java 21，AI 轮询任务用虚拟线程会大幅简化代码。
- **文件上传大小**: 小说 LONGTEXT + 图片/视频上传，Spring Boot 默认的 `max-file-size` 是 1MB，`max-request-size` 是 10MB。需要在 `application.yml` 中调大：
  ```yaml
  spring:
    servlet:
      multipart:
        max-file-size: 100MB
        max-request-size: 200MB
  ```
- **事务管理**: 流程执行涉及多表更新（shot 状态、workflow_task 状态、api_call_log），务必用 `@Transactional` 控制。但注意 AI 异步任务提交后等轮询结果，这个过程不能放在同一个事务里。

---

## 三、流程引擎边界情况

### 3.1 当前设计的问题

PRD 的流程引擎描述是"线性数组 + 审核暂停"，有以下边界情况没考虑到：

#### 问题1: 分集/分镜粒度的执行
- PRD 的流程步骤如 `image_gen`（首帧生图），是对所有分镜批量执行，还是逐个分镜执行？
- **建议**: 流程引擎应该是**两层结构**：
  - **外层**: 项目级别的步骤流水线（import → asset_extract → shot_gen → image_gen → video_gen → export）
  - **内层**: 每个步骤内部的批次处理（如 image_gen 步骤逐个处理所有待处理的 shot）
- `workflow_task` 表需要支持记录每个步骤处理了多少个分镜、成功/失败数量。

#### 问题2: 审核打回后的恢复
- 分镜审核打回后，是重新触发单个分镜的生成，还是从 `image_gen` 步骤重新开始？
- **建议**: 支持两种模式：
  - **单分镜重试**: 用户在工作台手动点击"重新生成"
  - **步骤级重试**: 在打回的分镜修复后，从该步骤开始继续流程

#### 问题3: 服务重启后的恢复
- 流程执行到一半（如 image_gen 正在处理第 5 个分镜，共 50 个），服务挂了重启后怎么办？
- **建议**: 
  - 每个步骤执行前检查 `workflow_task` 中已完成的子任务
  - 支持"断点续传"，从上次未完成的位置继续
  - 步骤状态需要细分：`RUNNING` → `PARTIAL_SUCCESS`（部分完成）

#### 问题4: 并发执行冲突
- 用户同时点击两次"开始执行"怎么办？
- **建议**: 
  - 用 Redis 锁 `workflow:lock:{projectId}`，5 分钟过期
  - `project.status` 也要检查，`进行中` 不允许再次启动

#### 问题5: 步骤依赖关系
- 如果 `asset_extract` 提取出的资产需要人工修正，修正后如何通知流程继续？
- **建议**: 审核节点需要有明确的 `review_action`（通过/打回/修改后通过），打回后流程暂停，用户在前端修改后手动触发"继续"。

#### 问题6: export 步骤的实现
- PRD 说"合并导出"，但视频合并（FFmpeg）是 CPU 密集型操作，不应该阻塞 HTTP 请求。
- **建议**: export 也做成异步任务，用户触发后返回 task_id，完成后推送结果。

### 3.4 流程执行的状态机建议

```
[NOT_STARTED] → [RUNNING] → [WAITING_REVIEW] → [REVIEW_PASSED] → [RUNNING] → ...
                                              → [REVIEW_REJECTED] → [PAUSED]
                                                                    → (用户修复后) → [RUNNING]
```

---

## 四、火山 TOS 集成注意事项

### 4.1 SDK 选择

- 火山引擎 TOS 兼容 S3 协议，可以用 AWS Java SDK v2（`software.amazon.awssdk:s3`），也可以用火山官方 SDK（`com.volcengine:volc-sdk-java` 中的 TOS 模块）。
- **建议**: 用 AWS SDK v2（S3 兼容模式），未来如果换存储（如阿里云 OSS、AWS S3），只需改 endpoint 和 credentials，代码零修改。

### 4.2 关键配置

```yaml
tos:
  endpoint: tos-cn-beijing.volces.com  # 注意 region
  access-key-id: ${TOS_ACCESS_KEY}
  secret-access-key: ${TOS_SECRET_KEY}
  bucket: ai-drama-prod
  cdn-domain: cdn.yourdomain.com      # 如果有 CDN 加速
```

### 4.3 文件组织规范

建议按以下结构存储：
```
ai-drama-prod/
├── projects/{projectId}/
│   ├── assets/{assetId}/
│   │   ├── ref_001.webp
│   │   └── ref_002.webp
│   ├── shots/{shotId}/
│   │   ├── v1_image.webp
│   │   ├── v1_video.mp4
│   │   ├── v2_image.webp
│   │   └── v2_video.mp4
│   └── export/
│       └── episode_01_final.mp4
```

### 4.4 注意事项

1. **预签名 URL**: 前端上传不要直接暴露 AK/SK。后端生成预签名 URL（PUT），前端直传 TOS。下载也走预签名 URL 或 CDN。
2. **大文件分片上传**: 视频文件可能几百 MB，需要用 TOS 的分片上传 API（Multipart Upload）。
3. **存储生命周期**: 配置 Bucket 的生命周期规则，自动清理 `projects/*/shots/*/v*` 下超过 30 天的旧版本文件，控制成本。
4. **Content-Type**: 上传时务必设置正确的 Content-Type（`image/webp`, `video/mp4`），否则浏览器预览会有问题。
5. **CDN 刷新**: 如果前端用了 CDN，更新文件后需要调用 CDN 刷新 API，否则用户看到的是缓存的旧图。
6. **跨域配置**: TOS Bucket 需要配置 CORS 规则，允许前端直传。

### 4.5 前端直传流程

```
1. 前端: 选择文件 → POST /api/storage/upload-signature {fileName, fileSize, contentType}
2. 后端: 调用 TOS SDK 生成预签名 PUT URL → 返回 {uploadUrl, objectKey}
3. 前端: PUT uploadUrl（直传 TOS）
4. 前端: POST /api/storage/confirm {objectKey, bizType, bizId}
5. 后端: 更新数据库中的 URL 字段
```

---

## 五、整体架构建议

### 5.1 推荐的包结构调整

```
com.lanyan.aidrama/
├── common/
│   ├── result/Result.java              # 统一响应
│   ├── exception/                      # 全局异常
│   ├── constants/                      # 常量
│   └── annotation/                     # 自定义注解
├── config/
│   ├── SaTokenConfig.java
│   ├── MybatisPlusConfig.java
│   ├── RedisConfig.java
│   ├── TosConfig.java
│   └── AsyncConfig.java                # 异步任务线程池（关键！）
├── module/
│   ├── auth/                           # 认证
│   ├── project/                        # 项目
│   ├── asset/                          # 资产
│   ├── content/                        # 分集/分场/分镜
│   ├── workflow/                       # 流程引擎
│   ├── ai/                             # AI 调用
│   ├── storage/                        # TOS 操作
│   └── cost/                           # API 消耗
└── task/                               # 定时任务/轮询
    └── AiTaskPoller.java               # AI 任务轮询器
```

### 5.2 必须引入的组件

| 组件 | 用途 | 为什么必须 |
|------|------|-----------|
| `@EnableAsync` + 自定义线程池 | AI 异步任务执行 | 默认 SimpleAsyncTaskExecutor 不复用线程 |
| Spring Retry | AI 调用失败重试 | 网络抖动、API 限流时需要自动重试 |
| Hibernate Validator | 参数校验 | 分镜 prompt、资产描述等不能为空 |
| AOP 日志 | API 调用耗时/参数记录 | 替代手动写 api_call_log |
| Quartz 或 Spring Scheduler | AI 任务轮询 | 避免用 while 循环轮询阻塞线程 |

### 5.3 API Key 管理

PRD 说"存在项目 YAML 配置文件中"。**不建议**:
1. 文件系统中的 YAML 不好管理版本，容易丢失
2. API Key 泄露风险（如果有人拿到服务器权限）

**建议方案**:
- MVP: 存在 `system_config` 数据库表，加密存储
- 进阶: 用环境变量注入，Spring Boot 的 `@Value("${seedance.api-key}")`
- AI Key 按项目隔离：在 `project` 表加 `api_key` 字段（加密），或者在 `config_json` 里存

### 5.4 异步任务设计

AI 调用（生图/生视频）的正确姿势：

```java
@Service
public class AiTaskService {
    
    // 1. 提交任务
    public String submitImageGen(Shot shot) {
        // 调用 Seedance API，拿到 taskId
        String taskId = seedanceClient.submitImage(shot.getPrompt(), ...);
        // 存入 Redis 记录状态
        redisTemplate.opsForValue().set("ai:task:" + taskId, 
            AiTaskStatus.builder().status("SUBMITTED").shotId(shot.getId()).build());
        return taskId;
    }
    
    // 2. 定时轮询（每 3 秒）
    @Scheduled(fixedDelay = 3000)
    public void pollTasks() {
        // 从 Redis 拿到所有 SUBMITTED/PROCESSING 的 taskId
        // 调用 Seedance 查询接口
        // 更新状态，如果完成则下载文件到 TOS，更新 shot 表
    }
}
```

**关键**: 轮询频率不要太低，Seedance 视频生成可能需要几分钟。

---

## 六、最可能出问题的环节及规避方案

### 6.1 TOP 5 风险点

| 排名 | 环节 | 风险描述 | 规避方案 |
|------|------|---------|---------|
| 1 | **AI 任务轮询** | Seedance API 不稳定/超时，轮询逻辑写崩导致任务丢失 | ① 任务状态持久化到 DB（不只放 Redis）；② 轮询器用 Scheduled + 异常捕获；③ 任务超时自动标记失败 |
| 2 | **资产一致性** | 分镜引用了未确认的资产，或资产被删除/修改后分镜没同步 | ① 数据库外键约束（或应用层校验）；② 资产状态变更为"废弃"时检查引用；③ 分镜生成前校验资产状态 |
| 3 | **流程中断恢复** | 服务重启/部署后流程执行状态丢失 | ① 所有状态持久化到 DB；② 启动时扫描 `RUNNING` 状态的 workflow_task，提供"恢复执行"接口 |
| 4 | **大文件上传** | 视频文件过大导致 OOM 或上传超时 | ① 前端分片上传；② 后端走预签名 URL 直传 TOS；③ 设置合理的超时时间 |
| 5 | **数据库性能** | shot 表数据量大后查询慢（一部小说可能 500+ 分镜） | ① 所有查询字段加索引；② 分镜列表用分页；③ 定期归档已完成项目 |

### 6.2 最该提前做的事

1. **第 1 天就定义好所有 JSON 字段的 Java 对象结构**，不要等到用的时候再想。比如：
   ```java
   public record WorkflowStep(String type, boolean enabled, boolean review, int order) {}
   public record AssetRef(String type, Long id) {}
   public record StylePreset(String artStyle, String colorTone, String lighting) {}
   ```

2. **第 1 天就写好 TOS 上传/下载的工具类**，验证预签名 URL 流程是否通畅。

3. **第 2 天就调通 Seedance API**，先写一个简单的 HTTP Client，确认：
   - 提交任务的响应格式
   - 查询任务状态的接口
   - 返回结果的字段结构
   - 速率限制（QPS 是多少？）

4. **流程引擎先写单元测试再写接口**，因为流程逻辑是最复杂的。

5. **数据库 DDL 先评审再执行**，避免后期加字段导致的迁移痛苦。

---

## 七、补充建议

### 7.1 日志规范

- AI 调用的完整 request/response 必须落日志（脱敏后），方便排查生成质量问题。
- 用 `MDC` 加 `traceId`，一个请求的所有日志串联起来。

### 7.2 监控

- 至少加一个 `/api/health` 健康检查接口
- API 调用失败率监控（alert 阈值：连续失败 3 次）

### 7.3 前端配合

- 流程执行时前端不能只"手动刷新"，建议加 SSE（Server-Sent Events）或 WebSocket 推送进度，用户体验好太多。
- 分镜工作台的分页：如果一集有 100 个分镜，全部加载会卡。

### 7.4 成本控制

- Seedance 生成一次视频可能几毛到几块钱，**必须加并发限制**（同一项目同时最多 N 个生成任务）。
- 增加"预览模式"：用低分辨率/短时间预览，确认效果后再用正式参数生成。

---

**总结**: PRD 整体思路清晰，适合 MVP 快速推进。最需要关注的是**流程引擎的健壮性**和**AI 任务的可靠性**，这两块在开发时要投入最多的测试精力。数据库方面建议先补上通用字段和索引，再开始编码。
