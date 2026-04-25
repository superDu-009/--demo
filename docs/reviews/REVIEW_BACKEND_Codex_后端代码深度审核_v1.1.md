# AI漫剧平台后端代码深度评审结果

**评审人**: Codex  
**评审日期**: 2026-04-23  
**评审范围**: `/Users/mac/Desktop/workspace/agent/ai-drama-platform/backend`  
**评审类型**: 后端安全、隐藏 Bug、并发一致性、测试可运行性、SDK 重复实现专项检查  
**验证命令**: `mvn test`  
**验证结果**: 编译阶段通过，测试阶段失败。失败原因是测试启动 Spring 上下文时 `RedissonConfig` 直接连接 `localhost:6379`，本机 Redis 不可用导致 14 个测试全部 `ApplicationContext` 加载失败。

---

## 一、总体结论

本次审查发现后端当前最大风险集中在三类：

1. **多租户越权风险严重**：项目接口有部分归属校验，但内容、资产、工作流、TOS 存储接口大量缺少“当前登录用户是否拥有该资源”的校验。登录用户只要枚举或猜到 ID，就可能读取、修改、删除或审核其他用户的资源。
2. **工作流与异步任务存在实质性逻辑 Bug**：`@Async` 同类自调用不生效、审核恢复逻辑不完整、Redisson 锁语义使用错误、任务轮询多实例下可能重复处理。
3. **配置和测试隔离问题突出**：构建产物里出现 TOS AK/SK 明文；测试依赖真实 Redis、真实 TOS、甚至本机文件路径，无法稳定在 CI 或其他机器运行。

上一版后端 review 中提到 “TOS 接口未鉴权”。当前代码通过 `SaTokenConfig` 已经把 `/api/tos/presign` 纳入登录鉴权范围，但问题没有完全解决：它仍然缺少业务归属校验、fileKey 前缀校验、文件类型/大小可信校验，因此仍属于高风险接口。

---

## 二、严重问题（必须优先修）

### 1. 构建产物包含 TOS AK/SK 明文

**位置**

- `target/classes/application.yml:59-66`
- `config/TosConfig.java:22-35`

**现象**

`target/classes/application.yml` 中存在：

- `tos.access-key`
- `tos.secret-key`
- `tos.bucket`
- Redis 默认密码
- MySQL 默认密码

虽然 `src/main/resources` 当前没有看到 `application.yml`，但 `target/classes` 是 Maven 构建输出，会被打入 jar 或进入镜像上下文。只要制品被上传、拷贝或部署，这组密钥就有泄露风险。

**影响**

- TOS Bucket 读写凭证泄露。
- 攻击者可绕过业务系统直接操作对象存储。
- 后续即使代码删除，已经泄露的 AK/SK 仍需轮换。

**建议**

- 立即轮换当前 TOS AK/SK。
- 禁止将真实密钥写入任何源码、构建产物、测试配置样例。
- 使用环境变量、KMS、部署平台 Secret 或配置中心注入。
- CI 增加 secret scan，例如扫描 `AKLT`、`secret-key`、`access-key`、Base64 形式密钥等模式。
- `target/` 应确认在 `.gitignore` 中，并避免被 Docker build context 或部署脚本误带入。

---

### 2. 内容模块缺少资源归属校验，存在横向越权

**位置**

- `module/content/controller/ContentController.java:32-160`
- `module/content/service/ContentServiceImpl.java:47-451`

**现象**

内容模块大部分接口只按传入 ID 查询资源，没有追溯到 `project.user_id` 校验当前登录用户是否是资源所有者。例如：

- `listEpisodes(projectId)`
- `createEpisode(projectId, req)`
- `updateEpisode(id, req)`
- `deleteEpisode(id)`
- `listScenes(episodeId)`
- `createScene(episodeId, req)`
- `updateScene(id, req)`
- `deleteScene(id)`
- `listShots(sceneId, ...)`
- `createShot(sceneId, req)`
- `updateShot(id, req)`
- `deleteShot(id)`
- `batchReviewShots(shotIds, ...)`
- `bindAssetToShot(shotId, assetId, assetType)`
- `unbindAssetFromShot(shotId, assetId)`

这些方法只要拿到 `episodeId`、`sceneId`、`shotId`、`assetId` 就可以操作，不需要是该项目 owner。

**影响**

- 用户 A 可以读取用户 B 的分集、分场、分镜。
- 用户 A 可以修改、删除用户 B 的内容。
- 用户 A 可以审核或打回用户 B 的分镜。
- 用户 A 可以把自己的资产绑定到别人的分镜，或把别人的资产绑定到自己的分镜，造成数据污染。

**建议**

- 在 Service 层建立统一的归属校验方法，而不是只在 Controller 做登录校验。
- 对不同 ID 做向上追溯：
  - `episodeId -> episode.project_id -> project.user_id`
  - `sceneId -> scene.episode_id -> episode.project_id -> project.user_id`
  - `shotId -> shot.scene_id -> scene.episode_id -> episode.project_id -> project.user_id`
  - `assetId -> asset.project_id -> project.user_id`
- `bindAssetToShot` 必须同时校验 `shot` 和 `asset` 属于同一个项目，且该项目属于当前用户。
- 对 `batchReviewShots` 中每个 shot 做归属校验，不允许跨项目混审。
- 建议抽出 `ProjectAccessService` 或 `ResourceOwnershipGuard`，减少复制粘贴。

---

### 3. 资产模块除创建外基本无归属校验

**位置**

- `module/asset/controller/AssetController.java:29-80`
- `module/asset/service/AssetServiceImpl.java:41-209`

**现象**

`createAsset` 中有项目 owner 校验：

- `AssetServiceImpl.java:61-70`

但以下方法只查资源存在，不查当前用户是否拥有资源所属项目：

- `listAssets(projectId, assetType)` 只校验项目存在。
- `updateAsset(id, req)` 只按 assetId 更新。
- `deleteAsset(id)` 只检查是否被引用。
- `confirmAsset(id)` 只按 assetId 改状态。
- `getAssetReferences(assetId, page, size)` 只按 assetId 查引用。

**影响**

- 任意登录用户可查看其他项目资产列表。
- 任意登录用户可修改、删除、确认其他用户资产。
- 任意登录用户可通过引用查询推断其他项目结构。

**建议**

- 复用统一归属校验。
- `listAssets(projectId)` 应校验 `project.user_id == currentUserId`。
- `update/delete/confirm/references(assetId)` 应先查 `asset.project_id`，再校验项目 owner。

---

### 4. 工作流接口缺少项目归属校验

**位置**

- `module/workflow/controller/WorkflowController.java:32-66`
- `module/workflow/service/WorkflowService.java:50-203`

**现象**

工作流接口按 `projectId` 操作，但没有校验当前登录用户是否拥有该项目：

- `startWorkflow(projectId)`
- `getWorkflowStatus(projectId)`
- `reviewStep(projectId, stepType, action, comment)`
- `stopWorkflow(projectId)`

**影响**

- 用户 A 可以启动用户 B 的工作流，消耗 AI 资源。
- 用户 A 可以停止用户 B 正在运行的工作流。
- 用户 A 可以审核通过或打回用户 B 的流程节点。
- 用户 A 可以读取用户 B 的生产进度。

**建议**

- `WorkflowService` 所有外部入口先校验 project owner。
- 审核接口还应校验当前待审核任务属于当前项目，且 action 合法。
- 如果未来存在协作者/管理员角色，应明确 RBAC/ABAC 权限模型，不应只依赖登录态。

---

### 5. TOS 预签名与 complete 接口仍存在越权和对象确认风险

**位置**

- `module/storage/controller/TosController.java:33-55`
- `module/storage/service/TosService.java:67-163`
- `module/storage/dto/TosPresignRequest.java:18-41`
- `module/storage/dto/TosCompleteRequest.java:17-40`

**现象**

当前 `SaTokenConfig` 已要求 `/api/tos/presign`、`/api/tos/complete` 登录，但业务校验不足：

- `presign` 信任请求体中的 `source` 和 `businessId`，没有校验该 businessId 是否属于当前用户。
- `complete` 只对 `fileKey` 做 `headObject` 和大小校验，没有校验这个 key 是否由当前用户申请。
- `complete` 中 `businessId` 和 `originalName` 基本没有参与安全校验。
- `fileKey` 可由客户端任意传入，已知 key 的用户可尝试确认并拿到公开 URL。

**影响**

- 用户可为不属于自己的项目生成上传 key。
- 用户可确认任意已知对象 key。
- 如果 Bucket 或对象公开读，`complete` 会帮助返回访问 URL。
- 如果预签名 URL 泄露，其有效期内可被反复上传覆盖同一 key。

**建议**

- 预签名时服务端生成并持久化一条 `upload_session` 或类似记录，包含：
  - `user_id`
  - `business_type`
  - `business_id`
  - `file_key`
  - `content_type`
  - `max_size`
  - `expire_time`
  - `status`
- `complete` 必须根据 `fileKey` 查上传会话，校验 `user_id`、`business_id`、状态、大小、Content-Type。
- 不允许客户端指定 `source=backend`；后端内部上传不应暴露给前端接口。
- `fileKey` 前缀建议包含 user/project 维度，例如 `users/{userId}/projects/{projectId}/...`，并在 complete 时强校验。
- 服务端应对文件类型和大小做二次校验，不可信任前端传入的 `contentType` 和 `fileSize`。

---

## 三、工作流与异步任务 Bug

### 1. `@Async` 同类自调用导致工作流不异步

**位置**

- `module/workflow/service/WorkflowService.java:49-85`
- `module/workflow/service/WorkflowService.java:208-216`

**现象**

`startWorkflow` 内部直接调用 `executeWorkflowAsync(projectId)`。这是同一个 Spring Bean 内部方法调用，不会经过 Spring AOP 代理，因此 `@Async("workflowExecutor")` 不生效。

**影响**

- HTTP 请求线程会同步执行工作流。
- 长流程可能导致请求超时。
- Tomcat 工作线程被占用，影响其他请求。

**建议**

- 把异步执行方法移动到独立 Bean，例如 `WorkflowAsyncRunner`。
- 或通过 `ApplicationEventPublisher` 发布事件，由监听器异步处理。
- 不建议通过 `AopContext.currentProxy()` 修补，耦合度高且容易误用。

---

### 2. 审核节点判断使用了错误的分集索引

**位置**

- `module/workflow/engine/WorkflowEngine.java:71-148`

**现象**

`episodeIndex` 在步骤循环外定义，但在外层 step 循环末尾递增：

```java
int episodeIndex = 0;
for (StepConfig step : steps) {
    for (Episode episode : episodes) {
        if (step.isReview() && episodeIndex == episodes.size() - 1) {
            ...
        }
    }
    episodeIndex++;
}
```

这个变量实际代表“步骤循环次数”，却被拿来判断“是否最后一个分集”。

**影响**

- 审核节点可能不会在最后一个分集触发。
- 步骤数量和分集数量接近时，触发时机更混乱。
- 后续流程可能提前继续或卡住。

**建议**

- 使用内层循环下标判断分集位置。
- 更进一步：不要用“最后一个分集”隐式判断审核节点，应明确 workflow task 粒度和状态模型。

---

### 3. Redisson 锁语义使用不正确，审核等待期间锁不会被看门狗续期

**位置**

- `module/workflow/service/WorkflowService.java:66`
- `module/workflow/engine/WorkflowEngine.java:124-129`
- `module/workflow/engine/WorkflowEngine.java:267-274`

**现象**

`tryLock(0, 2, TimeUnit.HOURS)` 设置了固定 `leaseTime=2h`。Redisson 只有在不传 leaseTime 的情况下才使用看门狗自动续期。代码注释中说“锁由看门狗续期”，与实际 API 语义不一致。

进入审核节点时直接 `return`，没有释放锁，也没有明确把锁交给审核恢复逻辑管理。

**影响**

- 审核等待超过 2 小时后，Redis 锁自动过期，但数据库 `execution_lock` 可能仍为 1。
- 其他线程可能重新获取 Redis 锁，导致同一个项目状态不一致。
- 审核恢复路径再加锁，可能和旧锁状态冲突。

**建议**

- 如果审核等待是持久状态，不应该长期持有 Redis 锁。
- 到达待审核节点时：
  - 写 `workflow_task.status=REVIEWING`
  - 写 `project.execution_lock=0` 或独立状态 `WAITING_REVIEW`
  - 释放 Redis 锁
- 审核通过后重新抢锁，从明确的 checkpoint 恢复。
- 数据库状态和 Redis 锁职责要拆开：数据库保存业务状态，Redis 只保护短时间临界区。

---

### 4. 审核恢复逻辑不完整

**位置**

- `module/workflow/engine/WorkflowEngine.java:162-246`

**现象**

`continueAfterReview` 找到最后一个 `status != 2` 的任务后调用 `recoverFromTask`，但：

- `executeStep` 只重新执行当前 step。
- `executeStep` TODO 中没有解析原始 `workflow_config`。
- 成功后没有继续后续步骤。
- 成功后没有完整更新 `workflow_task`。
- 部分路径没有释放锁。

**影响**

- 审核通过后流程可能停在当前步骤。
- 可能重复执行已完成节点。
- 任务状态和实际产物不一致。

**建议**

- 使用统一的 `executeFromCheckpoint(projectId, stepOrder, episodeId)`。
- checkpoint 应包含：
  - 当前 step_order
  - 当前 episode_id
  - sub_step
  - 当前 task 状态
- 审核通过后应把待审核 task 标记成功，然后从下一个 step/episode 继续。
- 所有入口使用 `try/finally` 保证锁释放和 `execution_lock` 修正。

---

### 5. `WorkflowTask` 每次 insert，状态查询会出现重复历史任务

**位置**

- `module/workflow/engine/WorkflowEngine.java:251-261`
- `module/workflow/service/WorkflowService.java:100-139`

**现象**

方法名叫 `createOrUpdateTask`，实际只 `insert`。同一项目、同一步骤、同一分集重复执行会产生多条任务。`getWorkflowStatus` 又直接按 `step_order` 查全部任务。

**影响**

- 页面进度中同一步骤重复出现。
- 整体进度计算被历史任务污染。
- 审核恢复查“最后一个非成功任务”容易选错历史记录。

**建议**

- 建立唯一键：`project_id + episode_id + step_type + step_order + run_id`，或明确 workflow run 概念。
- 当前运行态只查最新 `run_id`。
- `createOrUpdateTask` 应真正 upsert 当前运行记录，历史记录另建表或用 run_id 隔离。

---

## 四、AI 任务与外部 API 调用问题

### 1. AI 提交方法 `@Async` 返回 `Long` 不可靠

**位置**

- `module/aitask/service/AiTaskServiceImpl.java:52-93`
- `module/aitask/service/AiTaskServiceImpl.java:97-127`

**现象**

`submitImageGenTask` 和 `submitVideoGenTask` 标注 `@Async("aiTaskExecutor")`，但返回值是普通 `Long`。Spring 异步方法若需要返回结果，通常应使用 `Future` / `CompletableFuture`。普通返回值对调用方并不可靠。

**影响**

- 调用方拿不到确定的 taskId。
- 如果线程池拒绝任务，任务记录可能没有创建，调用方也不易处理。
- 任务提交和任务记录创建耦合在异步线程里，失败恢复困难。

**建议**

- 同步创建 `ai_task`，立即返回 taskId。
- 后台异步只负责调用 provider 并更新 task 状态。
- 或将方法签名改为 `CompletableFuture<Long>`，但业务上更推荐“先落库再异步”。

---

### 2. 多实例部署下 AI 任务轮询可能重复处理

**位置**

- `scheduler/AiTaskScheduler.java:31-45`
- `resources/mapper/AiTaskMapper.xml:7-14`
- `module/aitask/service/AiTaskServiceImpl.java:153-209`

**现象**

轮询 SQL：

```sql
SELECT *
FROM ai_task
WHERE status = 1
  AND next_poll_time <= NOW()
ORDER BY next_poll_time
LIMIT 50
```

它只查询，不抢占。多个应用实例、或者同实例多调度线程同时执行时，可能拿到同一批任务。

**影响**

- 多次请求第三方查询接口。
- 同一任务重复更新。
- 生成结果可能被重复写入分镜。
- 失败/成功状态可能被后到请求覆盖。

**建议**

- 查询后先原子抢占，例如 `UPDATE ai_task SET polling_lock = ?, lock_expire_time = ? WHERE id = ? AND status = 1 AND next_poll_time <= NOW()`。
- MySQL 8 可考虑 `SELECT ... FOR UPDATE SKIP LOCKED`，但要放在事务内。
- 或用 Redis 分布式锁按 `ai_task:{id}` 锁住单个任务。
- 更新成功/失败时加条件：`WHERE id = ? AND status = 1`，避免覆盖终态。

---

### 3. prompt 为空会触发 NPE

**位置**

- `module/aitask/client/ImageGenClient.java:50-51`
- `module/aitask/client/VideoGenClient.java:48-49`
- `module/aitask/service/AiTaskServiceImpl.java:71`
- `module/aitask/service/AiTaskServiceImpl.java:116`

**现象**

客户端日志直接调用 `prompt.length()`。如果 `shot.promptEn` 没有生成或为空，会直接 NPE。

**影响**

- 单个分镜缺少英文提示词会导致整个 AI 任务失败。
- 失败信息不清晰，不利于用户修复。

**建议**

- 提交前校验 `promptEn` 非空。
- 如果为空，应先调用翻译逻辑或返回明确业务错误。
- 日志中使用 `prompt != null ? prompt.length() : 0`。

---

### 4. `@Retryable` 没有启用

**位置**

- `module/aitask/client/DoubaoClient.java:39-43`
- `module/aitask/client/ImageGenClient.java:44-48`
- `module/aitask/client/VideoGenClient.java:42-46`
- `module/aitask/client/VideoGenClient.java:114-118`
- `config/AsyncConfig.java:21`

**现象**

项目引入了 `spring-retry`，方法也标注了 `@Retryable`，但没有看到 `@EnableRetry`。

**影响**

- 外部 API 短暂网络抖动不会自动重试。
- 代码注解给人“已重试”的错觉。

**建议**

- 在配置类加 `@EnableRetry`。
- 确认 `spring-aspects` / AOP 依赖已生效。
- 注意同类内部调用同样不会触发 retry 代理。

---

### 5. 轮询次数上限 off-by-one

**位置**

- `module/aitask/service/AiTaskServiceImpl.java:160-166`

**现象**

当前判断是：

```java
if (task.getPollCount() > 30) {
    ...
}
```

如果业务含义是“最多轮询 30 次”，应使用 `>= 30`。

**影响**

- 实际最多会进入第 31 次轮询。
- 进度和超时估算不一致。

**建议**

- 明确 `pollCount` 是“已轮询次数”还是“下一次轮询次数”。
- 使用常量 `MAX_POLL_COUNT`，并补单元测试覆盖边界。

---

## 五、数据一致性与状态模型问题

### 1. 分镜审核状态使用不一致

**位置**

- `common/ShotStatus.java:10-21`
- `module/content/service/ContentServiceImpl.java:386-404`
- `module/aitask/service/AiTaskServiceImpl.java:260-276`

**现象**

状态定义：

- `0 PENDING`
- `1 GENERATING`
- `2 REVIEWING`
- `3 APPROVED`
- `4 REJECTED`
- `5 COMPLETED`

但 `batchReviewShots` 中 approve 直接设为 `COMPLETED`，没有使用 `APPROVED`。视频生成完成也直接设为 `COMPLETED`。

**影响**

- `APPROVED` 状态可能永远不用。
- “审核通过”和“视频完成”无法区分。
- 前端筛选、统计、流程恢复可能语义混乱。

**建议**

- 重新定义状态机，例如：
  - 图片生成完成 -> `REVIEWING`
  - 审核通过 -> `APPROVED`
  - 视频生成完成 -> `COMPLETED`
  - 审核打回 -> `REJECTED`
- 所有入口只能按允许的状态迁移更新。
- 用枚举替代裸 int 常量，减少误用。

---

### 2. 内容创建时没有校验父资源存在和归属

**位置**

- `module/content/service/ContentServiceImpl.java:58-76`
- `module/content/service/ContentServiceImpl.java:158-176`
- `module/content/service/ContentServiceImpl.java:305-324`

**现象**

创建分集、分场、分镜时：

- `createEpisode(projectId, req)` 没有校验项目存在和归属。
- `createScene(episodeId, req)` 没有校验分集存在和归属。
- `createShot(sceneId, req)` 没有校验分场存在和归属。

**影响**

- 可能插入孤儿数据。
- 可向他人资源下创建内容。
- 依赖数据库外键时可能抛 SQL 异常；无外键时则污染数据。

**建议**

- 创建前必须查询父资源并校验归属。
- 对不存在父资源返回业务错误，不要依赖数据库异常。
- 数据库层也建议保留外键或至少增加索引和一致性检查任务。

---

### 3. sort_order 自动递增存在并发冲突

**位置**

- `module/content/service/ContentServiceImpl.java:58-65`
- `module/content/service/ContentServiceImpl.java:158-165`
- `module/content/service/ContentServiceImpl.java:305-312`

**现象**

创建时查询当前最大 `sort_order`，然后 `+1`。并发创建同一父资源下的内容时，两个请求可能拿到相同最大值。

**影响**

- 同一项目/分集/分场下排序号重复。
- 前端排序不稳定。

**建议**

- 如果允许重复排序，则前端需二级排序。
- 如果不允许重复，数据库加唯一键，例如 `(project_id, sort_order, deleted)` 或 `(scene_id, sort_order, deleted)`。
- 并发创建时用事务 + 行锁，或服务端生成更稳定的顺序值。

---

### 4. `listProjectShots` 的 sceneId 可跨项目

**位置**

- `module/project/service/ProjectServiceImpl.java:132-155`

**现象**

方法先校验 `projectId` 属于当前用户，但如果请求带了 `sceneId`，代码直接 `wrapper.eq(Shot::getSceneId, sceneId)`，没有校验该 scene 是否属于这个 project。

**影响**

- 用户可以调用自己项目的接口，但传入别人项目的 `sceneId`，从而查询别人分场下的分镜。

**建议**

- 如果传入 `sceneId`，必须校验：
  - scene 存在
  - scene -> episode -> projectId 等于路径参数 projectId
  - project owner 是当前用户

---

## 六、存储模块实现问题

### 1. `uploadFromUrl` 整体读入内存，存在堆压力

**位置**

- `module/storage/service/TosService.java:194-200`

**现象**

`uploadFromUrl` 使用：

```java
byte[] data = in.readNBytes(MAX_DOWNLOAD_SIZE);
return uploadFromBytes(data, targetKey);
```

最大读取 100MB 到堆内存。

**影响**

- 多个并发 URL 上传容易造成堆内存飙升。
- 大文件处理性能差。
- 只通过 `data.length >= MAX_DOWNLOAD_SIZE` 判断大小，无法根据 `Content-Length` 提前拒绝。

**建议**

- 优先读取 `Content-Length`，超过上限直接拒绝。
- 使用流式上传：`RequestBody.fromInputStream(inputStream, contentLength)`。
- 如果没有 `Content-Length`，使用限流 InputStream 包装器，超过上限中断。

---

### 2. `getPublicUrl` 手拼 TOS URL，容易生成错误 URL

**位置**

- `module/storage/service/TosService.java:335-339`

**现象**

当前通过：

```java
return String.format("%s/%s/%s",
        tosConfig.getEndpoint().replace("https://", "https://" + tosConfig.getBucket() + "."),
        tosConfig.getRegion(), key);
```

手动拼接公开 URL。

**影响**

- endpoint 是否包含协议不一致时会拼错。
- key 中特殊字符没有统一 URL 编码。
- 私有桶不应该返回公开 URL。
- endpoint 结构变化时 URL 不稳定。

**建议**

- 如果桶私有，统一返回短期下载预签名 URL。
- 如果桶公共读，配置明确的 `publicBaseUrl` 或 CDN 域名。
- 不要从 S3 endpoint 反推公开 URL。

---

### 3. Content-Type 校验过宽

**位置**

- `module/storage/service/TosService.java:80-89`
- `module/storage/service/TosService.java:234-239`

**现象**

`application/*` 全部被归为 document，并允许从 URL 下载。`application/javascript`、`application/x-msdownload`、`application/octet-stream` 等都可能被放行。

**影响**

- 用户可上传或转存可执行/脚本类文件。
- 如果后续被前端直接预览或 CDN 错误配置，可能引入 XSS 或下载风险。

**建议**

- 建立白名单，例如：
  - 图片：`image/png`、`image/jpeg`、`image/webp`
  - 视频：`video/mp4`、`video/webm`
  - 文本：`text/plain`
  - 文档：按业务确需列出
- 禁止 SVG 直接作为图片预览，除非做安全清洗。
- complete 时用 TOS headObject 的 Content-Type 与上传会话中记录的 Content-Type 对比。

---

## 七、测试与工程化问题

### 1. `mvn test` 依赖真实 Redis，测试不可稳定运行

**位置**

- `config/RedissonConfig.java:31-44`
- `src/test/java/.../TosControllerTest.java:26`
- `src/test/java/.../TosServiceTest.java:18`

**验证结果**

执行 `mvn test`：

- 编译通过。
- 测试启动 Spring Context 失败。
- 失败根因：`RedissonConfig` 创建 `RedissonClient` 时连接 `localhost:6379`，本机 Redis 不可用。
- 最终结果：14 tests，0 failures，14 errors。

**影响**

- CI 环境无法稳定跑测试。
- 本地开发者必须启动 Redis 才能跑存储模块测试。
- 和当前测试目标无关的工作流 Bean 阻塞了 TOS 测试。

**建议**

- Controller 层测试改用 `@WebMvcTest(TosController.class)`，只加载 MVC 切片，并 `@MockBean TosService`。
- Service 层测试对 `S3Client`、`S3Presigner` mock，不连真实 TOS。
- Redisson 可使用 test profile mock Bean，或用 Testcontainers 启 Redis。
- 对需要真实外部服务的测试标注 integration profile，不放入默认 `mvn test`。

---

### 2. 测试依赖本机绝对路径和真实 TOS

**位置**

- `src/test/java/com/lanyan/aidrama/module/storage/service/TosServiceTest.java:31-34`
- `src/test/java/com/lanyan/aidrama/module/storage/controller/TosControllerTest.java:121-150`

**现象**

`TosServiceTest` 读取 `/Users/mac/Downloads/111.jpeg` 并上传真实 TOS。`TosControllerTest` 也会调用 `tosService.uploadFromBytes` 上传并删除真实对象。

**影响**

- 换机器必失败。
- 无网络或无凭证必失败。
- 测试可能污染真实 Bucket。
- 并发跑测试时 key 冲突或清理失败会留下垃圾数据。

**建议**

- 单元测试不要访问真实文件和真实 TOS。
- 使用 `MockMultipartFile`、内存字节数组和 mock S3 响应。
- 真实 TOS 测试单独放 `IT` 后缀，用 Maven profile 控制。

---

### 3. Spring Boot 3.2.5 与 Java 25 运行有潜在兼容噪音

**位置**

- `pom.xml:20` 指定 Java 17
- 测试输出显示运行 Java 25.0.2

**现象**

测试输出中出现 Netty/Unsafe deprecation 警告。项目声明 Java 17，但本机测试使用 Java 25。

**影响**

- 未来 JDK 行为变化可能造成依赖兼容问题。
- CI 与本地环境不一致时排查困难。

**建议**

- 开发和 CI 使用 Java 17 LTS，与 `pom.xml` 保持一致。
- Maven Enforcer 加 JDK 版本约束。

---

## 八、SDK 与重复实现建议

### 1. TOS 可考虑使用火山官方 ve-tos-java-sdk 替代部分手写适配

**当前实现**

- `pom.xml` 使用 AWS S3 SDK：`software.amazon.awssdk:s3`
- `TosConfig` 手动配置 S3 endpoint。
- `TosService` 自行处理预签名、公开 URL、上传、删除。

**可替代内容**

火山官方 `ve-tos-java-sdk` 提供：

- `TOSV2Client`
- `putObject`
- `deleteObject`
- `PreSignedURLInput`
- `PreSignedURLOutput`
- 原生 TOS endpoint/region 语义

**建议**

- 如果当前 S3 兼容模式稳定，可暂不强制迁移。
- 但建议至少不要手拼 public URL；使用官方 SDK 或配置明确 public/CDN base URL。
- 对 TOS 特有能力、错误码、签名行为要求高时，迁移官方 SDK 更稳。

**参考**

- 火山 TOS Java 普通预签名文档：`https://www.volcengine.com/docs/6349/79910`
- 火山 TOS Java SDK：`https://github.com/volcengine/ve-tos-java-sdk`

---

### 2. Doubao / Ark 客户端手写 JSON 和 RestTemplate，建议引入 typed client 层

**当前实现**

- `DoubaoClient.java`
- `ImageGenClient.java`
- `VideoGenClient.java`

这些类手写 JSON 请求体、手动解析响应字段、手动处理 URL 和状态。

**问题**

- 字段名变化时编译期无法发现。
- 响应结构缺字段容易 NPE。
- 三个 client 重复构造 header、RestTemplate、错误处理、日志、retry。
- `ImageGenClient` 硬编码了 `IMAGE_API_URL`，没有使用 `DoubaoConfig.getImageApiUrl()`。

**建议**

- 抽出统一 `ArkHttpClient` 或 `ArkApiClient`：
  - 统一 Bearer Auth。
  - 统一错误响应解析。
  - 统一 timeout/retry/log。
- 请求/响应使用 DTO，而不是大量 `ObjectNode` 和 `JsonNode`。
- 如果后续引入火山官方 Java SDK，可逐步替换手写 HTTP。

**参考**

- 火山 Java SDK：`https://github.com/volcengine/volcengine-java-sdk`

---

## 九、优先级修复路线

### P0：立即处理

1. 轮换 TOS AK/SK，清理构建产物中的密钥。
2. 为内容、资产、工作流、TOS 接口补齐资源归属校验。
3. 修复 `@Async` 同类自调用，避免工作流阻塞请求线程。
4. 修复 TOS complete 的 fileKey 归属校验。
5. 让默认 `mvn test` 不依赖真实 Redis/TOS。

### P1：短期处理

1. 重构工作流锁和审核等待模型。
2. 修复审核恢复逻辑和 workflow task 重复 insert 问题。
3. AI 任务改成“先落库返回 taskId，再异步提交 provider”。
4. 轮询任务增加抢占锁，支持多实例部署。
5. 启用 `@EnableRetry` 或删除无效 retry 注解。

### P2：中期优化

1. 用枚举和状态机替代裸 int 状态常量。
2. 内容创建增加父资源存在性、归属、并发排序保护。
3. 存储模块改用上传会话表，统一校验 content-type、大小、owner。
4. TOS public URL 改为配置化 CDN/base-url 或统一预签名下载。
5. Doubao/Ark client 抽 typed API 层，减少手写 JSON。

---

## 十、建议补充测试

### 权限测试

- 用户 A 不能访问用户 B 的项目详情。
- 用户 A 不能创建、更新、删除用户 B 项目下的 episode/scene/shot。
- 用户 A 不能更新、删除、确认用户 B 的 asset。
- 用户 A 不能启动、停止、审核用户 B 的 workflow。
- 用户 A 不能 complete 用户 B 的 TOS fileKey。

### 工作流测试

- `startWorkflow` 返回后异步执行，不阻塞请求线程。
- 到达审核节点时释放 Redis 锁并写入待审核状态。
- 审核通过后从下一个 checkpoint 继续。
- 审核拒绝后项目进入失败/暂停状态，锁释放。
- 重启后 recovery 不重复执行已完成步骤。

### AI 任务测试

- `promptEn` 为空返回明确业务错误。
- provider 查询成功时只允许从 processing 状态转 success。
- provider 查询失败时按退避更新时间。
- 多线程同时 poll 同一个 task 时只有一个线程抢占成功。
- pollCount 在边界 30 次时按预期超时。

### 存储测试

- presign 不允许非 owner 的 businessId。
- complete 不允许未知 fileKey。
- complete 不允许其他用户 fileKey。
- complete 校验 Content-Type 和文件大小。
- SVG、可执行文件、未知 application 类型被拒绝。

---

## 十一、本次审查未覆盖或需后续确认

- 未连接真实数据库检查 DDL、索引、唯一键和外键。
- 未检查 Dockerfile、部署脚本、CI 配置。
- 未压测工作流和 AI 任务线程池。
- 未验证火山 Ark 最新 Java SDK 对图片/视频生成接口的完整覆盖度；这里只给出迁移方向。
- 未执行真实 Redis/TOS 集成测试，因为默认 `mvn test` 已被 Redis 依赖阻断。

