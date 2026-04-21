# 后端系分评审报告 — AI漫剧生产平台

| 项目 | 内容 |
|------|------|
| 被评审文档 | DESIGN_BACKEND_老克_v1.1.md |
| 评审人 | 阿典 |
| 评审日期 | 2026-04-19 |
| 评审结论 | **有条件通过** — 存在 3 个 P0 级问题、5 个 P1 级问题，修复后可进入开发 |

---

## 评审结论总览

| 严重程度 | 数量 | 说明 |
|---------|------|------|
| P0 (必须修) | 3 | 不改会导致数据不一致或流程中断 |
| P1 (强烈建议) | 5 | 不改会导致生产环境高频故障或性能瓶颈 |
| P2 (建议优化) | 4 | 不改不影响运行，但长期维护成本增加 |

---

## 1. 流程引擎健壮性 — P0 问题

### 1.1 [P0] Redis 锁 + @Async 无法实现真正的断点续跑

**问题描述：**

文档 5.3 节中 Redis 锁的实现：
```java
redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.MINUTES);
```

存在以下致命缺陷：

**(a) 锁无归属标识，无法安全释放**

当前释放逻辑：
```java
redisTemplate.delete(lockKey);  // 第1176行
```

没有使用 Lua 脚本校验锁持有者身份。如果线程 A 的锁超时后线程 B 获取了锁，A 执行完毕后执行 delete 会把 B 的锁删掉。这是经典的 Redis 分布式锁缺陷。

**修复方案：**
```java
// 加锁时使用唯一标识
String lockValue = UUID.randomUUID().toString();
Boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, lockValue, 30, TimeUnit.MINUTES);

// 释放时用 Lua 脚本原子校验 + 删除
String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then "
           + "return redis.call('del', KEYS[1]) else return 0 end";
redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class),
    Collections.singletonList(lockKey), lockValue);
```

或者直接引入 Redisson，使用 `RLock`，自带看门狗自动续期 + 安全释放。

**(b) 30 分钟 TTL 不够用**

一个项目的完整流程可能包含：
- 生图节点：Seedance 生图可能需要数分钟
- 生视频节点：Seedance 生视频可能需要 5-10 分钟
- 导出节点：FFmpeg 合并多个视频可能需要数分钟
- 如果项目有多个 episode 循环执行，总时间可能超过 30 分钟

**修复方案：** 引入看门狗续期机制（Redisson 自带），或者将 TTL 设为 2 小时 + 定期续期。

**(c) @Async 线程中的状态无法被 RecoveryRunner 恢复**

当服务重启时：
1. `@Async("workflowExecutor")` 启动的线程随 JVM 销毁，**正在执行的 Node 直接中断**
2. RecoveryRunner 只能看到 `execution_lock=1` 的 project 和 `status=1` 的 workflow_task
3. 但文档 5.4 节的恢复逻辑只是把当前步骤标记为失败、跳到下一步：
```java
// 将当前执行中步骤标记为失败
workflowTaskMapper.updateStatus(runningTask.getId(), 3, "服务中断");
// 从下一个步骤继续执行
int nextOrder = runningTask.getStepOrder() + 1;
workflowEngine.resumeFrom(projectId, nextOrder);
```

**核心缺陷：**
- 如果中断的是 `image_gen` 步骤，此时 Seedance 上可能有一个正在生成的任务（provider_task_id 已记录），但系统直接跳过了它
- 如果中断的是 `asset_extract` 步骤，AI 已经提取了部分资产但未保存，直接跳到下一步会导致数据不一致
- 如果中断的是 `export` 步骤（FFmpeg 合并中），生成的半成品文件无人清理

**修复方案（推荐）：**

将 WorkflowEngine 改造为**状态机驱动**的架构：

```
每个 Node 执行前先将 workflow_task 状态标记为 1(执行中) + 记录当前 sub_step
Node 内部按 sub_step 执行：
  - image_gen: sub_step = "submit" -> "polling" -> "download" -> "upload_tos"
  
恢复时：
  1. 查询 workflow_task 记录（包含 output_data 中的中间状态）
  2. 根据 sub_step 决定是重新提交还是继续轮询还是直接下载
  3. 如果是 AI 任务，通过 provider_task_id 重新查询第三方 API 状态
```

### 1.2 [P0] 审核节点暂停时，Redis 锁未续期

**问题描述：**

文档 5.1 节时序图显示，当 `step.review == true` 时：
```
→ 标记 status=4(待审核), 暂停执行
→ 等待前端 review 接口
```

此时 `@Async` 线程是**阻塞等待**还是**退出线程**？

- 如果是阻塞等待（while 循环等），线程会被长期占用，workflowExecutor 的 3 个核心线程很快耗尽
- 如果是退出线程等 review 回调后继续，那锁的 30 分钟 TTL 可能在用户审核期间过期
- 审核通过后的"继续执行"需要从新的线程重新启动，但此时 Redis 锁可能已经释放或被其他请求获取

**修复方案：**

审核节点不应该在线程内等待。改为：
```
1. NodeExecutor 执行完业务逻辑后，设置 status=4(待审核)
2. 释放当前 workflow 线程（但保留 Redis 锁，使用更长的 TTL 或看门狗续期）
3. 前端 review 接口触发"继续执行"逻辑
4. 从 workflow_task 表读取下一个步骤，重新分配 @Async 线程继续
```

或者更好的做法是：**审核步骤不持锁**，而是通过 `execution_lock` 字段 + `workflow:stop` 标记来防止并发启动。Redis 锁只用于防止同项目短时间内被重复 start。

### 1.3 [P0] `execution_lock` 和 Redis 锁的双重状态不一致

**问题描述：**

文档中有两套锁机制：
1. Redis 锁：`workflow:lock:{projectId}`，TTL 30min
2. 数据库锁：`project.execution_lock`，无 TTL

两者生命周期不同步的场景：
- Redis 锁过期自动消失，但 `execution_lock` 仍然是 1
- 服务重启后，Redis 锁丢失，但 `execution_lock` 仍然是 1
- RecoveryRunner 只检查了 `execution_lock=1`，没有检查 Redis 锁是否存在

这意味着在某些异常场景下，`execution_lock` 可能永远卡在 1（除非有兜底清理机制）。

**修复方案：**

建议以 Redis 锁为权威，`execution_lock` 仅作为查询展示的冗余字段。或者在 `execution_lock` 上加乐观锁版本号 + 更新时间戳，RecoveryRunner 判断如果 `update_time` 超过 TTL 则强制解锁。

---

## 2. AI 任务异步处理 — P0/P1 问题

### 2.1 [P0] 定时轮询每 5s 全量扫描 — 必然触发 API 频率限制

**问题描述：**

文档 6.3 节：
```java
@Scheduled(fixedDelay = 5000)
public void pollAiTaskResults() {
    // 查 ai_task WHERE status=1, 批量查第三方API结果
}
```

假设一个项目有 100 个分镜需要生图，同时提交 100 个任务：
- 每 5 秒，`pollAiTaskResults()` 查出所有 100 个 status=1 的任务
- 逐个调用 Seedance 的 queryTask API
- Seedance 的 API 通常有速率限制（如 100 req/min）
- **仅一个项目就可能触发限流**

如果有多个项目同时执行，情况更糟。而且每次轮询都要遍历所有 status=1 的任务，无论它们是否已经快完成了。

**修复方案（推荐）：**

```
方案 A：指数退避轮询
- 每个 task 记录 last_poll_time 和 poll_count
- 第1次轮询：提交后 10s
- 第2次轮询：上次后 20s
- 第3次轮询：上次后 40s
- ...最大间隔 120s
- 按 next_poll_time 排序查询，减少无效 API 调用

方案 B（最优）：Webhook 回调
- 提交 AI 任务时传入回调 URL
- 第三方任务完成后主动通知后端
- 后端收到回调后更新 ai_task 状态
- 完全消除轮询
```

### 2.2 [P1] @Async 线程池配置不合理

**问题描述：**

```
workflowExecutor: core=3, max=5, queue=10
aiTaskExecutor: core=5, max=10, queue=20
```

- `aiTaskExecutor` 核心线程 5 个，但 `pollAiTaskResults()` 本身也需要 HTTP 调用第三方 API
- 如果同时有多个 workflow 在运行，每个 workflow 中的 ImageGen/VideoGen 都通过 `@Async` 提交任务
- 线程池满后触发 `CallerRunsPolicy`，调用线程（通常是 Tomcat 工作线程）会被阻塞执行任务
- 这意味着 API 请求线程可能因为线程池满而卡住，导致前端请求超时

**修复方案：**

```java
@Bean("aiTaskExecutor")
public ThreadPoolTaskExecutor aiTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);           // 提高到 10
    executor.setMaxPoolSize(20);            // 提高到 20
    executor.setQueueCapacity(50);          // 提高到 50
    executor.setThreadNamePrefix("ai-task-");
    // 不要用 CallerRunsPolicy！使用 AbortPolicy + 监控告警
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
}
```

同时：`@Scheduled` 定时任务默认使用单线程，`pollAiTaskResults` 会阻塞后续定时任务。建议：
```java
@Bean("schedulerExecutor")
public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(3);  // 给调度器单独线程池
    return scheduler;
}
```

### 2.3 [P1] 缺少 AI 任务的重试和熔断机制

**问题描述：**

文档中没有提到：
- 第三方 API 调用失败后的重试策略（超时？限流？503？）
- 熔断器：如果 Seedance 持续不可用，应该停止提交新任务并告警
- 最大重试次数和退避策略
- 任务超时处理（一个任务提交了但 30 分钟都没结果怎么办？）

**修复方案：**

引入 Resilience4j 或 Spring Retry：
```java
@Retryable(
    value = {ApiException.class, TimeoutException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 5000, multiplier = 2)
)
public TaskResult queryTask(String providerTaskId) { ... }

@CircuitBreaker(name = "seedance", fallbackMethod = "fallback")
public String submitImage(ImageGenParams params) { ... }
```

### 2.4 [P2] 定时轮询未考虑多实例部署

**问题描述：**

如果部署了 2+ 个应用实例，每个实例都会执行 `@Scheduled` 的 `pollAiTaskResults()`，导致：
- 同一个任务被多个实例重复查询
- 成倍的 API 调用量

**修复方案：**

- 使用 `@SchedulerLock` (ShedLock) 确保只有一个实例执行轮询
- 或者将轮询逻辑改为按任务 ID 取模分配到不同实例

### 2.5 [P2] AiTaskService 提交和轮询的职责耦合

**问题描述：**

`submitImageGenTask()` 方法同时负责：
1. 创建 ai_task 记录
2. @Async 调用 Seedance
3. 更新 provider_task_id
4. 轮询结果
5. 上传 TOS
6. 更新 status

这个链路太长，任何一个环节失败都会导致状态不一致。建议拆分为：
- `submitTask()`：只负责创建记录 + 调用提交 API
- `pollAndProcessResult()`：只负责查询结果 + 下载 + 上传 + 更新状态

---

## 3. 数据一致性 — P1 问题

### 3.1 [P1] JSON 字段更新为全量覆盖，无增量修改能力

**问题描述：**

`project.workflow_config` 和 `project.style_preset` 是 JSON 类型字段。文档 4.2.2 中：
```
saveWorkflowConfig: 保存 workflow_config JSON，仅允许 status=草稿 时修改
```

使用 MyBatis Plus 的 `JacksonTypeHandler` 做 JSON 序列化，`UPDATE` 语句会**全量覆盖**整个 JSON 字段。

**风险：**
- 如果有多个地方同时修改 workflow_config（比如一个改 steps，一个改全局 config），后写入的会覆盖先写入的
- 虽然当前限制了"仅草稿可修改"，但后续扩展时容易出问题
- 没有乐观锁（version 字段）保护并发更新

**修复方案：**

在 project 表增加 `version` 字段做乐观锁：
```sql
ALTER TABLE project ADD COLUMN version INT NOT NULL DEFAULT 0;
```

Entity 中加 `@Version` 注解。或者使用 MySQL 8.0 的 `JSON_SET` 做增量更新（但这需要在 Service 层处理）。

### 3.2 [P1] 工作流任务缺少幂等性保证

**问题描述：**

`workflow_task` 表在流程启动时初始化，但文档没有说明：
- 如果用户连续两次点击"开始执行"（虽然 409 拦截），是否会创建重复的 workflow_task 记录
- 如果 NodeExecutor 执行了一半失败，重新执行时是否会重复创建 episode/asset/shot 记录
- 特别是 `import` 节点（AI 拆分章节创建 episode），如果重试会导致重复数据

**修复方案：**

每个 NodeExecutor 内部需要幂等逻辑：
- 通过 `project_id + episode_id + step_type` 唯一索引判断是否已执行
- 或者在 workflow_task 记录中保存已创建的子实体 ID 列表，恢复时跳过已创建的

### 3.3 [P2] workflow_task 表缺少项目-步骤的唯一约束

**问题描述：**

```sql
KEY idx_project_id (project_id),
KEY idx_step_order (step_order),
```

没有 `(project_id, step_type)` 的唯一约束。可能出现同一个项目的同一个 step_type 有多条记录（正常流程不会，但异常情况可能）。

**修复方案：**

添加唯一索引：
```sql
UNIQUE KEY uk_project_step (project_id, step_type, episode_id)
```

---

## 4. 性能隐患 — P1 问题

### 4.1 [P1] shot 表查询缺少组合索引

**问题描述：**

`shot` 表当前索引：
```sql
KEY idx_scene_id (scene_id),
KEY idx_status (status),
KEY idx_sort_order (sort_order),
```

最常见的查询场景是"查某分场下的所有分镜，按排序号排序"：
```sql
SELECT * FROM shot WHERE scene_id = ? AND deleted = 0 ORDER BY sort_order;
```

虽然有 `idx_scene_id`，但如果分镜数量大（一个分场可能有几十个分镜），MySQL 需要回表 + filesort。

**修复方案：**

```sql
-- 组合索引覆盖最常用的查询
ALTER TABLE shot ADD INDEX idx_scene_deleted_sort (scene_id, deleted, sort_order);
```

同样，`episode` 和 `scene` 表也需要类似的组合索引：
```sql
ALTER TABLE episode ADD INDEX idx_project_deleted_sort (project_id, deleted, sort_order);
ALTER TABLE scene ADD INDEX idx_episode_deleted_sort (episode_id, deleted, sort_order);
```

### 4.2 [P1] ai_task 轮询查询缺少合适索引

**问题描述：**

`pollAiTaskResults()` 需要查 `status=1` 的任务：
```sql
SELECT * FROM ai_task WHERE status = 1;
```

当前只有单列 `idx_status`，但轮询还需要按 `create_time` 排序（优先查最早提交的任务），且需要 `project_id` 过滤（可选）。

**修复方案：**

```sql
ALTER TABLE ai_task ADD INDEX idx_status_created (status, create_time);
```

### 4.3 [P2] workflow_task 查询可能成为慢查询

**问题描述：**

`getWorkflowStatus()` 需要查一个项目的所有 workflow_task 并按 step_order 排序。如果多次重启 + 恢复，同一个项目可能有多批次的 task 记录。

**修复方案：**

查询时加 `ORDER BY step_order DESC LIMIT N` 或者在查询中加批次标识。

### 4.4 [P2] JSON 字段上的查询无法利用索引

**问题描述：**

如果需要按 `workflow_config` 中的某个条件查询（比如查某个 stepType 的项目），MySQL JSON 字段上的查询会走全表扫描。

虽然 MVP 阶段不太需要，但如果后续有"查所有包含 image_gen 步骤的项目"这类需求，会非常慢。

**修复方案：**

使用 MySQL 8.0 的生成列（Generated Column）+ 虚拟索引，或者在应用层维护冗余的 tag 字段。

---

## 5. 安全性 — P1/P2 问题

### 5.1 [P1] API Key 通过环境变量注入，但未做运行时保护

**问题描述：**

```yaml
seedance:
  api-key: ${SEEDANCE_API_KEY}
```

```java
@Value("${seedance.api-key}")
private String apiKey;
```

问题点：
1. **日志泄露风险**：如果异常堆栈或 debug 日志打印了 `SeedanceClient` 的实例，apiKey 可能被输出到日志
2. **Actuator 泄露**：Spring Boot Actuator 的 `/env` 端点可能暴露环境变量（虽然 `$` 引用通常只显示占位符，但配置不当会泄露）
3. **无轮换机制**：API Key 写死在环境变量中，轮换需要重启服务
4. **无多租户隔离**：所有用户共用同一个 API Key，无法按项目/用户做配额控制

**修复方案：**

1. 日志脱敏：配置 Jackson 或 Logback 对包含 `key`/`secret`/`token` 的字段脱敏
2. Actuator 保护：
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info  # 不要暴露 env
  endpoint:
    env:
      show-values: NEVER      # 永远不显示变量值
```
3. 在 SeedanceClient 中覆写 `toString()` 防止意外打印：
```java
@Override
public String toString() {
    return "SeedanceClient{apiUrl='" + apiUrl + "', apiKey='***'}";
}
```

### 5.2 [P2] 登录密码 BCrypt 但未配置强度

**问题描述：**

文档提到 "BCrypt加密密码"，但没有说明 BCrypt 的 strength（默认是 10）。对于 MVP 够用，但建议在配置中明确：

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // strength=12 更安全
}
```

### 5.3 [P2] Sa-Token 配置允许同端并发登录

**问题描述：**

```java
.setIsConcurrent(true)  // 允许同端并发登录
```

这意味着同一用户可以同时在多个设备/浏览器登录。对于 MVP 可以接受，但后续如果要做"付费用户单设备限制"或"登录设备管理"，需要提前规划。

### 5.4 [P2] 缺少接口级别的防重放攻击保护

**问题描述：**

`batchReviewShots`、`startWorkflow` 等写操作没有防重放机制。如果前端请求超时重试，可能导致：
- 同一个分镜被审核两次
- 同一个工作流被启动两次（虽然有锁保护，但时序窗口存在）

**修复方案：**

- 对敏感接口加 nonce/timestamp + 签名校验
- 或者至少在 Service 层做幂等检查

---

## 6. 其他发现

### 6.1 [P2] DDL 中缺少字符集一致性声明

所有表使用了 `utf8mb4_unicode_ci`，这是正确的。但 `api_call_log` 的 `request_params` 是 JSON 类型，MySQL 8.0 的 JSON 类型对 utf8mb4 支持良好，无需额外处理。

### 6.2 [P2] 逻辑删除未考虑外键关系

`episode`、`scene`、`shot` 都做了逻辑删除，但 `shot_asset_ref` 没有 `deleted` 字段。当 shot 被逻辑删除后，`shot_asset_ref` 中的关联记录仍然是"有效"的，查询时可能产生脏数据。

**修复方案：**

给 `shot_asset_ref` 也加 `deleted` 字段，或者在删除 shot 时级联删除关联记录。

### 6.3 [P2] `api_call_log` 表没有逻辑删除

其他表都有 `deleted` 字段做逻辑删除，但 `api_call_log` 没有。这是合理的（日志表通常不需要逻辑删除），但文档中应明确说明。

### 6.4 [P2] 缺少全局唯一 ID 生成策略

所有表使用 `AUTO_INCREMENT`。在单实例部署下没问题，但如果后续需要分库分表或多实例写入，会有 ID 冲突。建议预留雪花算法（Snowflake）或 UUID 的迁移方案。

---

## 7. 修复优先级建议

| 优先级 | 问题 | 修改量 | 影响范围 |
|--------|------|--------|---------|
| P0-1 | Redis 锁安全释放 + 看门狗续期 | 中 | 流程引擎 |
| P0-2 | 断点续改造为状态机驱动 | 大 | 流程引擎核心 |
| P0-3 | AI 任务轮询改为指数退避或 Webhook | 中 | AI 任务模块 |
| P1-1 | 线程池扩容 + 调度器独立线程池 | 小 | 全局 |
| P1-2 | 添加缺失的组合索引 | 小 | 数据库 |
| P1-3 | 审核节点改为异步回调驱动 | 中 | 流程引擎 |
| P1-4 | JSON 字段加乐观锁 | 小 | 项目模块 |
| P1-5 | AI 任务重试 + 熔断 | 中 | AI 任务模块 |
| P2 | 其余优化项 | 小-中 | 各模块 |

---

## 8. 总结

老克的系分文档整体结构清晰，模块划分合理，技术栈选择恰当（Spring Boot 3 + MyBatis Plus + Sa-Token 是当前 Java 生态的成熟组合）。DDL 设计规范，索引覆盖了大部分查询场景。

**最大的风险点在于流程引擎的断点续跑机制**：当前的 Redis 锁 + @Async 方案只能做到"粗粒度"的故障恢复（跳过当前步骤继续），无法做到"细粒度"的断点续跑（从子步骤恢复）。这在 MVP 阶段可能勉强可用，但如果用户项目较大（多分集、多分镜），服务重启可能导致大量已做的工作丢失。

**AI 任务轮询是最容易在生产环境炸掉的部分**：5 秒全量轮询在任务量大时必定触发第三方 API 限流，建议优先改为指数退避策略，后续再升级为 Webhook 回调。

建议在 Sprint 3 开发前优先修复 3 个 P0 问题，否则后期重构成本会更高。

---

*评审人：阿典 | 2026-04-19*
