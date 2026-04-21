# 回复评审意见书 — 老克

> **系分文档**: `DESIGN_BACKEND_老克_v1.1.md`
> **评审人**: 小欧（前端架构师）、阿典（后端/架构评审）
> **回复人**: 老克（后端架构师）
> **日期**: 2026-04-19
> **状态**: 评审回复

---

## 总体态度

感谢小欧和阿典的严格审查。整体来说，你们提的问题质量很高，尤其是阿典在流程引擎健壮性和 AI 任务轮询方面指出的 P0 问题，确实是我设计中的盲区。小欧从前端交互角度提出的分页、进度精度、上传闭环等问题也非常切中要害。

以下逐条表态。

---

## 一、小欧评审回复

### 小欧 P0 问题（共 4 条）

---

#### 小欧 1.1 [P0] 分镜列表缺少分页参数

**【接受】**

**理由**: 确实是我的疏忽。分镜工作台一个分场可能包含几十甚至上百个分镜，全量返回在首屏加载和内存上都是问题。前端虚拟滚动或分页都需要后端分页支撑。

**修改方案**:
- 将 `GET /api/scene/{sceneId}/shots` 改为支持分页参数：`?page=1&size=20&status=xxx(可选)`
- 响应体统一使用 `PageResult<ShotVO>` 包装，包含 `total`、`page`、`size`、`hasNext` 字段
- Service 层 `listShots` 方法签名改为 `PageResult<ShotVO> listShots(Long sceneId, int page, int size, Integer status)`

---

#### 小欧 1.2 [P0] 项目列表分页参数响应缺少标准字段

**【接受】**

**理由**: 当前响应只返回 `{total, list}`，确实缺少驱动前端翻页器所需的 `page`、`size`、`hasNext` 等标准字段。统一 `PageResult` 规范是必要的。

**修改方案**:
- 定义统一 `PageResult<T>` 类：
  ```java
  public class PageResult<T> {
      private long total;
      private int page;
      private int size;
      private boolean hasNext;
      private List<T> list;
  }
  ```
- 项目列表、分镜列表、资产列表等所有分页接口统一使用 `PageResult` 包装
- `GET /api/project/list` 响应 data 改为 `PageResult<ProjectVO>`

---

#### 小欧 3.1 [P0] progress 字段精度不足

**【接受】**

**理由**: 我原先设计的 `progress` 只是"已完成步骤数/总步骤数"，粒度太粗。对于 AI 漫剧生产这种长流程（可能持续数十分钟），前端进度条确实需要更细粒度的数据：步骤内百分比、分镜级统计、预估剩余时间等。

**修改方案**:
- 扩展 `workflow/status` 响应体，新增字段：
  ```json
  {
    "executionLock": 1,
    "currentStep": "image_gen",
    "overallProgress": 45,
    "totalShots": 100,
    "processedShots": 45,
    "estimatedRemainingSeconds": 180,
    "steps": [{
      "stepType": "image_gen",
      "status": 1,
      "progress": 60,
      "currentDetail": "正在生成第 45/100 个分镜",
      "errorMsg": null
    }]
  }
  ```
- `overallProgress` 计算逻辑：综合各步骤完成度和子任务进度加权计算
- 在 `image_gen`/`video_gen` 步骤执行时，实时更新 `processedShots` 计数器
- `estimatedRemainingSeconds` 根据历史执行时长动态估算（初始可简化为固定值）

---

#### 小欧 4.1 [P0] 缺少上传完成回调/通知机制

**【接受】**

**理由**: 这确实是一个数据一致性风险。前端直传 TOS 后，如果第二步 `PUT /api/project/{id}` 设置 `novelTosPath` 失败，文件已上传但数据库无记录。两步操作非原子。

**修改方案**:
- 采用"上传完成通知接口"方案（比 TOS 回调更可控，不需要配置 TOS 回调 URL）：
  ```
  POST /api/tos/complete
  {
    "key": "projects/123/novel_xxx.txt",
    "projectId": 123,
    "fileType": "novel",
    "metadata": { "originalName": "xxx.txt", "size": 12345 }
  }
  ```
- 后端收到通知后：
  1. HEAD 请求校验文件确实存在于 TOS
  2. 根据 `fileType` 和 `projectId` 更新对应数据库记录（如 `project.novel_tos_path`）
  3. 写入 `api_call_log` 记录
- 如果 HEAD 校验失败，返回错误码 `40005 预签名URL已过期或文件不存在`
- 前端在直传成功后必须调用此接口，形成闭环

---

### 小欧 P1 问题（共 10 条）

---

#### 小欧 1.3 [P1] 缺少项目级分镜聚合查询

**【接受】**

**理由**: 分镜工作台确实可能需要跨分场查看分镜状态，当前只能通过 `sceneId` 查询，缺少项目维度聚合。

**修改方案**:
- 新增接口 `GET /api/project/{projectId}/shots?sceneId=xxx(可选)&status=xxx(可选)&page=1&size=20`
- 支持按分场过滤（sceneId 可选），不传则返回项目下全部分镜
- 返回 `PageResult<ShotVO>`，内嵌 `assetRefs`

---

#### 小欧 1.4 [P1] 批量审核接口缺少前置校验信息和结果明细

**【接受】**

**理由**: 批量操作需要明确的反馈，前端需要知道哪些成功、哪些失败及原因。

**修改方案**:
- `POST /api/shot/batch-review` 响应体改为：
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
- Service 层逐个校验状态，记录失败明细，不抛异常而是收集结果

---

#### 小欧 2.1 [P1] shot_asset_ref 需要批量查询优化 — 缺少资产引用反向查询接口

**【接受】**

**理由**: 资产详情页需要展示"被哪些分镜引用"，反向查询是合理需求。

**修改方案**:
- 新增接口 `GET /api/asset/{assetId}/references?page=1&size=20`
- 通过 `shot_asset_ref` 表的 `idx_asset_id` 索引查询，JOIN `shot` 表获取分镜信息
- 返回 `PageResult<ShotReferenceVO>`，包含 shotId、sceneId、episodeId、shotStatus

---

#### 小欧 2.2 [P1] shot 表缺少 ai_task_id 关联字段

**【反驳】**

**理由**: 
1. 一个分镜可能先后触发多次 AI 任务（生成→打回→重新生成），如果只在 shot 表上挂 `image_task_id`，只能关联最后一个任务，历史任务就丢失了关联关系。当前的 `ai_task` 表通过 `shot_id` 反向关联是正确的设计——它天然支持一对多关系。
2. 前端分镜卡片需要显示"生成中"状态时，不需要知道具体的 `taskId`。前端只需要知道 `shot.status = 1(生成中)` 即可展示 loading 状态。如果需要查 AI 任务详情，可以通过 `GET /api/ai/task/latest?shotId=xxx` 按 shotId 查最新任务，而不是在 shot 表上挂字段。
3. 在 shot 表上加 `image_task_id` 和 `video_task_id` 会带来额外的状态同步复杂度（任务完成/失败/重试时都要更新 shot 表），且当 image 和 video 任务同时进行时，两个字段的状态管理容易出错。

**替代方案（折中）**:
- 在 `GET /api/scene/{sceneId}/shots` 的 ShotVO 中新增 `currentAiTask` 字段，后端在查询时通过 LEFT JOIN `ai_task`（WHERE shot_id = ? ORDER BY id DESC LIMIT 1）内嵌当前最新 AI 任务状态：
  ```json
  {
    "id": 1,
    "status": 1,
    "currentAiTask": {
      "taskId": 456,
      "taskType": "image_gen",
      "status": 1
    }
  }
  ```
- 这样前端既有任务信息可做状态展示，又不需要在 shot 表上加冗余字段

---

#### 小欧 3.2 [P1] 缺少 SSE/WebSocket 替代方案

**【接受（MVP 后预留）】**

**理由**: MVP 阶段 3s 轮询完全够用，引入 SSE/WebSocket 会增加架构复杂度。但预留接口是合理的，后续体验优化时可以直接启用。

**修改方案**:
- 文档中预留 `GET /api/project/{id}/workflow/stream` 接口路径
- 标注为"Phase 2 功能，MVP 阶段暂不实现"
- 技术预研：使用 Spring WebFlux 的 `SseEmitter` 实现 EventStream 推送
- 不在 MVP Sprint 中排期

---

#### 小欧 3.3 [P1] workflow/status 缺少当前执行的分集信息

**【接受】**

**理由**: 前端确实需要展示"正在处理第 X 集 / 共 Y 集"的信息。当前 `workflow_task` 表有 `episode_id` 字段但响应中没有透出。

**修改方案**:
- `workflow/status` 响应中增加 `currentEpisodeId` 和 `currentEpisodeTitle` 字段
- 后端在返回时从 workflow_task 表中获取当前执行步骤的 episode_id，JOIN episode 表取 title
- 同时增加 `totalEpisodes` 字段，方便前端显示分集进度

---

#### 小欧 4.2 [P1] 缺少上传进度支持 — PresignResult 缺少 maxFileSize

**【接受】**

**理由**: 前端需要在上传前校验文件大小，展示限制提示。

**修改方案**:
- `PresignResult` 增加 `maxFileSize` 字段（从配置 `spring.servlet.multipart.max-file-size` 读取）
- 同时增加 `allowedContentTypes` 数组，前端可在上传前做 MIME 类型校验
  ```json
  {
    "uploadUrl": "...",
    "accessUrl": "...",
    "expiresIn": 3600,
    "maxFileSize": 52428800,
    "allowedContentTypes": ["image/png", "image/jpeg", "video/mp4", "text/plain"]
  }
  ```

---

#### 小欧 4.3 [P1] 缺少分片上传支持

**【接受（MVP 后实现）】**

**理由**: 对于 MVP 阶段，小说文件通常 < 10MB，50MB 限制够用。视频文件由系统生成（非用户上传），走后端上传链路。但分片上传确实是长期需求。

**修改方案**:
- 文档中标注为"Phase 2 功能"
- 预留接口设计：
  - `POST /api/tos/presign-multipart` 初始化分片上传
  - `POST /api/tos/presign-part` 获取单个分片预签名 URL
  - `POST /api/tos/complete-multipart` 合并分片
- MVP 不在 Sprint 中排期，但设计好接口契约

---

#### 小欧 5.1 [P1] 缺少前端高频交互场景的细分错误码

**【接受】**

**理由**: 当前错误码覆盖确实不够细，前端在表单校验、上传校验、业务冲突等场景需要细分错误码来做不同的 UI 反馈。

**修改方案**:
- 在 2.2 节错误码规范表中补充：
  | 错误码 | 场景 | 前端交互 |
  |--------|------|----------|
  | `40002` | 字段校验失败 | 表单高亮对应字段 |
  | `40003` | 文件类型不支持 | Toast 提示允许的类型 |
  | `40004` | 文件大小超限 | Toast 提示最大限制 |
  | `40005` | 预签名 URL 已过期 | 自动重新请求 presign |
  | `40301` | 非项目创建者无权操作 | 跳转无权限页 |
  | `40901` | 资产已被分镜引用，不可删除 | Toast 提示引用关系 |
  | `40902` | 分镜状态不支持当前操作 | 禁用对应按钮 |
  | `42900` | 请求频率过高（AI 调用限流） | 倒计时后重试 |
  | `51001` | AI 模型超时/排队中 | 展示排队状态 |
  | `51002` | AI 生成内容为空/不合格 | 提示重新生成 |
  | `51101` | TOS 存储空间不足 | 联系管理员 |

---

#### 小欧 5.2 [P1] 缺少全局错误处理约定

**【接受】**

**理由**: 前后端需要对错误处理行为达成共识，否则前端拦截器逻辑不一致。

**修改方案**:
- 在文档中新增"前端错误处理约定"小节：
  - `40100/40101` → HTTP 401 + 前端自动跳转登录页
  - `40300/40301` → HTTP 403 + 前端 Toast "无操作权限"
  - `40000-40099` → HTTP 200 + 前端根据错误码做对应交互（表单高亮/Toast 等）
  - `40900-40999` → HTTP 200 + 前端 Toast 提示业务冲突原因
  - `50000-50099` → HTTP 500 + 前端展示全局错误页
  - `51000-51199` → HTTP 200 + 前端 Toast 提示服务异常
- 鉴权相关错误（401xx、403xx）返回对应 HTTP 状态码，其他业务错误统一 HTTP 200 + 业务码

---

## 二、阿典评审回复

### 阿典 P0 问题（共 4 条）

---

#### 阿典 1.1 [P0] Redis 锁 + @Async 无法实现真正的断点续跑

**【接受】**

**理由**: 阿典指出的三个子问题（a/b/c）都是真实存在的致命缺陷：

**(a) 锁无归属标识，无法安全释放**: 我承认这个设计有严重问题。直接用 `delete(lockKey)` 释放锁，在锁超时后被其他线程获取的情况下，原线程 delete 会误删别人的锁。这是经典的 Redis 分布式锁缺陷。

**修改方案**: 引入 Redisson 替代原生 RedisTemplate 操作锁：
- 加依赖 `redisson-spring-boot-starter`
- 使用 `RLock` 替代 `setIfAbsent`，自带看门狗自动续期 + 安全释放（Lua 脚本校验归属）
- 删除原来手写的 `setIfAbsent` + `delete` 代码

**(b) 30 分钟 TTL 不够用**: 多分集 + 生视频 + FFmpeg 导出的完整流程确实可能超过 30 分钟。

**修改方案**: Redisson 的 `RLock` 自带看门狗（默认 30s 续期周期，锁持有期间自动续期），不再需要手动设置 TTL。对于 workflow 锁，使用 `lock(2, TimeUnit.HOURS)` 设置最大锁时间 2 小时（防止持有者 crash 后锁永不释放），同时看门狗在持锁期间持续续期。

**(c) @Async 线程中的状态无法被 RecoveryRunner 恢复**: 这个批评完全正确。我原先的恢复逻辑太粗糙——直接把当前步骤标记为失败跳下一步，丢失了 AI 任务的中间状态。

**修改方案**: 将 WorkflowEngine 改造为**状态机驱动**架构：
- 每个 Node 执行前在 `workflow_task` 中记录 `sub_step` 状态（如 `image_gen` 节点有 `submit → polling → download → upload_tos` 四个子步骤）
- 每个子步骤完成后将中间结果写入 `output_data` JSON
- 恢复时：
  1. 查询 workflow_task 获取 `sub_step` 和 `output_data`
  2. 根据 `sub_step` 决定恢复策略：
     - `submit` 阶段中断 → 重新提交 AI 任务
     - `polling` 阶段中断 → 通过 `provider_task_id` 重新查询第三方 API 状态（不重新提交）
     - `download` 阶段中断 → 重新下载
     - `upload_tos` 阶段中断 → 重新上传
  3. 如果是 AI 任务，通过 `provider_task_id` 查第三方 API 状态，如果已完成则直接走 download → upload_tos

---

#### 阿典 1.2 [P0] 审核节点暂停时，Redis 锁未续期

**【接受】**

**理由**: 这是我最初设计中的一个关键缺陷。审核等待时间不可控（用户可能几小时后才审核），Redis 锁在等待期间会过期，导致并发安全问题。

**修改方案**:
- 审核节点**不在线程内等待**，改为异步回调驱动：
  1. NodeExecutor 执行完业务逻辑后，设置 `workflow_task.status = 4(待审核)`
  2. **释放当前 workflow 线程**（不阻塞）
  3. Redis 锁**不释放**，由 Redisson 看门狗持续续期（直到最大 2 小时）
  4. 前端 `POST /api/project/{id}/workflow/review` 触发"继续执行"
  5. Review 逻辑中重新从 `workflow_task` 读取下一步，分配新的 `@Async` 线程继续执行
  6. 如果用户长时间不审核（超过 2 小时），Redisson 锁自动释放，`execution_lock` 通过 RecoveryRunner 清理
- 关键变化：审核步骤不持锁 → 而是通过 `execution_lock` 字段防止并发 start。Redis 锁只在步骤执行期间持有

---

#### 阿典 1.3 [P0] execution_lock 和 Redis 锁的双重状态不一致

**【接受】**

**理由**: 两套锁机制生命周期不同步确实会导致状态不一致。服务重启后 Redis 锁丢失但 `execution_lock` 仍然是 1，如果没有兜底机制会永久卡死。

**修改方案**:
- 明确分层职责：
  - **Redis 锁（Redisson RLock）**：权威锁，用于防止并发执行。带看门狗续期 + 最大 TTL 兜底
  - **execution_lock（数据库字段）**：仅作为查询展示的冗余字段 + RecoveryRunner 的恢复触发信号
- RecoveryRunner 恢复逻辑增强：
  ```
  1. 查询 execution_lock=1 的项目
  2. 尝试获取 Redis 锁（非阻塞 tryLock）
  3. 如果获取成功 → 说明原锁已过期/丢失，执行恢复流程
  4. 如果获取失败 → 说明有其他实例正在执行，跳过
  5. 恢复完成后，正常释放 Redis 锁 + 清空 execution_lock
  ```
- 增加兜底清理：RecoveryRunner 如果发现 `execution_lock=1` 但没有任何 running 的 workflow_task，强制解锁（可能是上次异常导致未清理）

---

#### 阿典 2.1 [P0] 定时轮询每 5s 全量扫描 — 必然触发 API 频率限制

**【接受】**

**理由**: 这个批评一针见血。100 个分镜同时生图，每 5s 全量轮询 100 次 API，一个项目就可能触发 Seedance 的限流。多项目同时执行时情况更糟。

**修改方案**: 采用**指数退避轮询 + 按 next_poll_time 排序查询**：
- `ai_task` 表新增字段：
  - `next_poll_time` DATETIME：下次轮询时间
  - `poll_count` INT：已轮询次数
  - `last_poll_time` DATETIME：上次轮询时间
- 轮询策略：
  - 任务提交后：`next_poll_time = now + 10s`（首次 10s 后查）
  - 第 1 次轮询未完成：`next_poll_time = last_poll_time + 20s`
  - 第 2 次轮询未完成：`next_poll_time = last_poll_time + 40s`
  - 第 3 次及以后：`next_poll_time = last_poll_time + 60s`（最大间隔 60s）
  - 如果 `poll_count > 30`（约 30 分钟无结果），标记为超时失败
- 定时查询改为：`SELECT * FROM ai_task WHERE status=1 AND next_poll_time <= NOW() ORDER BY next_poll_time LIMIT 50`
- 这样大幅减少无效 API 调用，且新提交的任务优先轮询

**补充**：在文档中标注 Webhook 回调为 Phase 2 优化方向，待 Seedance 支持 webhook 后切换。

---

### 阿典 P1 问题（共 7 条）

---

#### 阿典 2.2 [P1] @Async 线程池配置不合理

**【接受】**

**理由**: 我原设计的 `aiTaskExecutor`（core=5, max=10, queue=20）和 `CallerRunsPolicy` 确实有问题。线程池满时 Tomcat 工作线程被阻塞执行任务，会导致前端请求超时。`@Scheduled` 默认单线程也会阻塞后续定时任务。

**修改方案**:
- `workflowExecutor`：core=3 → **5**，max=5 → **8**，queue=10 → **20**
- `aiTaskExecutor`：core=5 → **10**，max=10 → **20**，queue=20 → **50**
- 拒绝策略：`CallerRunsPolicy` → `AbortPolicy` + 全局异常处理器捕获 `RejectedExecutionException` 并返回 `42900 请求频率过高`
- 调度器独立线程池：
  ```java
  @Bean("schedulerExecutor")
  public ThreadPoolTaskScheduler taskScheduler() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setPoolSize(3);
      return scheduler;
  }
  ```

---

#### 阿典 2.3 [P1] 缺少 AI 任务的重试和熔断机制

**【接受】**

**理由**: 第三方 API 调用必须有重试和熔断，否则单次超时或限流会导致任务直接失败，不可接受。

**修改方案**:
- 引入 Spring Retry（比 Resilience4j 更轻量，MVP 够用）：
  ```java
  @Retryable(
      value = {ApiException.class, TimeoutException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 5000, multiplier = 2)
  )
  public TaskResult queryTask(String providerTaskId) { ... }
  ```
- 添加熔断器逻辑（手动实现，不引入额外依赖）：
  - 记录 Seedance API 连续失败次数
  - 连续失败 5 次后进入熔断状态（30s 内不再提交新任务）
  - 熔断期间新任务直接标记为 `3-失败`，错误信息 "AI 服务暂时不可用，请稍后重试"
  - 30s 后进入半开状态，允许 1 个请求试探
- 添加任务超时处理：`poll_count > 30` 时自动标记失败

---

#### 阿典 3.1 [P1] JSON 字段更新为全量覆盖，无增量修改能力

**【接受】**

**理由**: 虽然当前限制了"仅草稿可修改"，并发冲突概率低，但没有乐观锁保护始终是隐患。后续扩展时容易出问题。

**修改方案**:
- `project` 表增加 `version INT NOT NULL DEFAULT 0` 字段
- Entity 中加 `@Version` 注解（MyBatis Plus 自动处理乐观锁）
- `saveWorkflowConfig` 接口请求体中增加 `version` 字段，前端传入当前版本号
- 并发更新时抛出 `OptimisticLockException`，返回错误码 `40903 数据已被修改，请刷新后重试`
- 这个改动量很小，在 Sprint 2 中一并完成

---

#### 阿典 3.2 [P1] 工作流任务缺少幂等性保证

**【接受】**

**理由**: 特别是 `import` 节点（AI 拆分章节创建 episode），如果重试会导致重复数据。幂等性是分布式系统的基本要求。

**修改方案**:
- `workflow_task` 表增加唯一索引：
  ```sql
  UNIQUE KEY uk_project_step_episode (project_id, step_type, episode_id)
  ```
- 每个 NodeExecutor 内部实现幂等逻辑：
  - `ImportNodeExecutor`：创建 episode 前先查 `project_id + title` 是否已存在
  - `AssetExtractNodeExecutor`：创建 asset 前先查 `project_id + name + asset_type` 是否已存在
  - `ShotGenNodeExecutor`：创建 shot 前先查 `scene_id + sort_order` 是否已存在
- 在 `output_data` 中保存已创建的子实体 ID 列表，恢复时跳过已创建的

---

#### 阿典 4.1 [P1] shot 表查询缺少组合索引

**【接受】**

**理由**: 最常见的查询是"查某分场下的所有分镜，按排序号排序"，组合索引可以避免回表 + filesort。

**修改方案**:
- 在 DDL 中新增组合索引：
  ```sql
  ALTER TABLE shot ADD INDEX idx_scene_deleted_sort (scene_id, deleted, sort_order);
  ALTER TABLE episode ADD INDEX idx_project_deleted_sort (project_id, deleted, sort_order);
  ALTER TABLE scene ADD INDEX idx_episode_deleted_sort (episode_id, deleted, sort_order);
  ```
- 同时可以删除原有的单列 `idx_sort_order` 索引（被组合索引覆盖）

---

#### 阿典 4.2 [P1] ai_task 轮询查询缺少合适索引

**【接受】**

**理由**: 轮询查询 `WHERE status=1 ORDER BY next_poll_time` 需要组合索引支撑。

**修改方案**:
- `ai_task` 表新增组合索引：
  ```sql
  ALTER TABLE ai_task ADD INDEX idx_status_next_poll (status, next_poll_time);
  ```
- 同时保留原有 `idx_status`（其他场景可能用到）

---

#### 阿典 5.1 [P1] API Key 通过环境变量注入，但未做运行时保护

**【接受】**

**理由**: 日志泄露和 Actuator 暴露都是真实风险。

**修改方案**:
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

---

## 三、修改计划汇总

### 文档修改（DESIGN_BACKEND_老克_v1.2.md）

| 序号 | 修改内容 | 对应评审 |
|------|---------|---------|
| 1 | 统一 `PageResult<T>` 分页响应规范 | 小欧 1.1, 1.2 |
| 2 | 分镜列表增加分页参数 | 小欧 1.1 |
| 3 | `workflow/status` 响应体增加精细进度字段 | 小欧 3.1 |
| 4 | 新增 `POST /api/tos/complete` 上传完成通知接口 | 小欧 4.1 |
| 5 | 新增项目级分镜聚合查询接口 | 小欧 1.3 |
| 6 | 批量审核响应增加结果明细 | 小欧 1.4 |
| 7 | 新增资产引用反向查询接口 | 小欧 2.1 |
| 8 | ShotVO 增加 `currentAiTask` 内嵌字段（非 DDL 变更） | 小欧 2.2 |
| 9 | `workflow/status` 增加分集信息字段 | 小欧 3.3 |
| 10 | `PresignResult` 增加 `maxFileSize` 和 `allowedContentTypes` | 小欧 4.2 |
| 11 | 错误码规范表补充 11 个细分错误码 | 小欧 5.1 |
| 12 | 新增"前端错误处理约定"小节 | 小欧 5.2 |
| 13 | Redis 锁改用 Redisson RLock（附代码示例） | 阿典 1.1a, 1.2, 1.3 |
| 14 | WorkflowEngine 改为状态机驱动架构（附 sub_step 设计） | 阿典 1.1c |
| 15 | execution_lock + Redis 锁分层职责说明 | 阿典 1.3 |
| 16 | AI 任务轮询改为指数退避策略（附 next_poll_time 设计） | 阿典 2.1 |
| 17 | 线程池配置调整 + 调度器独立线程池 | 阿典 2.2 |
| 18 | AI 任务重试 + 熔断机制设计 | 阿典 2.3 |
| 19 | project 表增加 version 乐观锁字段 | 阿典 3.1 |
| 20 | workflow_task 幂等性保证设计 | 阿典 3.2 |
| 21 | 组合索引补充（shot/episode/scene/ai_task） | 阿典 4.1, 4.2 |
| 22 | API Key 运行时保护措施 | 阿典 5.1 |

### DDL 变更汇总

```sql
-- 新增字段
ALTER TABLE project ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE ai_task ADD COLUMN next_poll_time DATETIME DEFAULT NULL COMMENT '下次轮询时间';
ALTER TABLE ai_task ADD COLUMN poll_count INT NOT NULL DEFAULT 0 COMMENT '轮询次数';
ALTER TABLE ai_task ADD COLUMN last_poll_time DATETIME DEFAULT NULL COMMENT '上次轮询时间';

-- 新增索引
ALTER TABLE shot ADD INDEX idx_scene_deleted_sort (scene_id, deleted, sort_order);
ALTER TABLE episode ADD INDEX idx_project_deleted_sort (project_id, deleted, sort_order);
ALTER TABLE scene ADD INDEX idx_episode_deleted_sort (episode_id, deleted, sort_order);
ALTER TABLE ai_task ADD INDEX idx_status_next_poll (status, next_poll_time);

-- 新增唯一约束
ALTER TABLE workflow_task ADD UNIQUE KEY uk_project_step_episode (project_id, step_type, episode_id);
```

### 新增 pom.xml 依赖

```xml
<!-- Redisson (替代原生 RedisTemplate 分布式锁) -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.25.0</version>
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
```

### 新增接口汇总

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 上传完成通知 | POST | `/api/tos/complete` | 前端直传 TOS 后通知后端入库 |
| 项目级分镜查询 | GET | `/api/project/{projectId}/shots` | 跨分场聚合查询分镜 |
| 资产引用查询 | GET | `/api/asset/{assetId}/references` | 查询资产被哪些分镜引用 |

### Phase 2 预留（MVP 不实现）

| 功能 | 说明 |
|------|------|
| SSE 进度推送 | `GET /api/project/{id}/workflow/stream` |
| 分片上传 | `POST /api/tos/presign-multipart` 等 |
| Webhook AI 回调 | 替代轮询，待 Seedance 支持 |

---

## 四、总结

感谢小欧和阿典的高质量评审。我的系分文档在以下几个方面确实存在不足：

1. **流程引擎健壮性**（阿典指出）：Redis 锁安全、断点续跑细粒度、审核等待锁续期——这些都是可能导致生产环境数据不一致的核心问题，必须修复后才能进入开发。
2. **AI 任务轮询策略**（阿典指出）：5s 全量轮询在任务量大时必然触发限流，指数退避是最低成本的修复方案。
3. **前端交互支撑**（小欧指出）：分页缺失、进度粒度过粗、上传闭环不完整——这些直接影响用户体验，需要在设计阶段补齐。
4. **幂等性和乐观锁**（阿典指出）：MVP 阶段可能碰不到并发问题，但设计上必须预留，否则后期重构成本极高。

**所有 P0 问题均已接受并给出修改方案，所有 P1 问题均已接受（1 条部分反驳但给出折中方案）。**

下一步我将输出 `DESIGN_BACKEND_老克_v1.2.md`，将上述修改方案落地到文档中。

---

*回复人：老克 | 2026-04-19*
