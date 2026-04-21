# AI 模块系分评审报告

| 项目 | 内容 |
|------|------|
| 评审人 | 老克（后端架构师） |
| 被评审文档 | `docs/design/DESIGN_AI_阿典_v1.0.md` |
| 评审日期 | 2026-04-19 |
| 结论 | ⚠️ **有条件通过，存在 3 个问题需修复** |

---

## 1. Webhook 接口

### 1.1 路径澄清

> 用户提问中提及的路径为 `/api/ai/callback/seedance`，但阿典文档实际定义的路径是 `/api/webhook/seedance`。以下按实际路径审查。

### 1.2 🔴 P0: Sa-Token 拦截冲突

**问题**：后端系分 `SaTokenConfig` (行 293-299) 的全局拦截规则为：

```java
SaRouter.match("/**")
    .notMatch("/api/user/login")
    .notMatch("/api/tos/presign")
    .notMatch("/error")
    .check(r -> StpUtil.checkLogin());
```

**`/api/webhook/seedance` 未被排除**，意味着火山引擎的 Webhook 回调请求会被 Sa-Token 拦截，要求携带登录 Token，导致回调 401 失败。

**修复方案**：在 `SaTokenConfig` 中增加排除路径：
```java
.notMatch("/api/webhook/**")  // 第三方回调接口不需要鉴权
```

### 1.3 🟡 P1: 签名校验实现风险

阿典的 `WebhookSignatureVerifier` 存在以下隐患：

1. **Authorization Header 解析过于脆弱**：使用 `split(", ")` 硬分割，若火山引擎返回的 Header 格式有空格变化（如 `",  "` 或 `","`）会直接越界崩溃。建议改用正则或更健壮的解析。

2. **签名算法可能不完整**：当前实现只计算了 `HTTPMethod + CanonicalURI + CanonicalQueryString + HashedPayload`，但火山引擎 HMAC-SHA256 签名通常还需要加入 Credential 中的日期、区域、服务信息构建 `StringToSign`。**需在对接时验证实际签名格式。**

3. **未启用防重放**：`timestampHeader` 传了 `null`（行 492），防重放功能实际未启用。建议在 MVP 阶段至少开启 5 分钟窗口校验。

### 1.4 🟡 P1: 错误码语义不当

Webhook 签名校验失败返回 `Result.fail(40100, "签名校验失败")`。按后端系分错误码规范，`40100` 是"未登录"，会触发前端跳转登录页。对于第三方 Webhook 签名失败，应使用 `51000`（AI服务异常）或新增 `51003`（Webhook签名无效），避免语义混淆。

---

## 2. 异步线程池

### 2.1 ✅ 线程池配置一致

阿典使用的线程池与后端系分完全对齐：
- `@Scheduled(scheduler = "schedulerExecutor")` — 使用独立的调度器线程池（poolSize=3），与后端系分 6.2 一致。
- `@Async("aiTaskExecutor")` — 使用 `aiTaskExecutor`（core=10, max=20, queue=50），与后端系分 6.1 一致。

### 2.2 🟡 P1: Webhook 回调同步处理阻塞 Tomcat 线程

阿文档注释写"异步处理回调 (避免阻塞火山引擎)"（行 500），但实际代码中 `handleWebhookCallback(payload)` 是**同步调用**：

```java
// WebhookController line 501
aiTaskService.handleWebhookCallback(payload);  // 同步!
```

虽然方法内部使用了 Redisson 分布式锁，但整个处理流程（查库、更新、可能的下载上传触发）都在 Tomcat 工作线程中执行。如果火山引擎回调响应慢（如 TOS 上传耗时），Tomcat 线程会被占用。

**建议**：改为 `@Async` 调用或 `ApplicationEventPublisher` 异步发布事件，先快速返回 200 给火山引擎：
```java
@Async("aiTaskExecutor")
public void handleWebhookCallback(WebhookPayload payload) { ... }
```

### 2.3 ✅ 不会耗尽 @Async 线程池

- `AiTaskScheduler.pollAiTaskResults()` 每次最多拉取 50 个到期任务，但每个任务的轮询操作是 IO 密集型（HTTP 调用 Seedance），不会长时间占用线程。
- `downloadAndUploadAsync` 虽然也走 `aiTaskExecutor`，但频率受限于 AI 任务完成频率，在 MVP 规模下不会打满 20 个 max 线程。

---

## 3. 数据库交互

### 3.1 ✅ 并发更新安全

阿典对 `ai_task` 表的并发更新设计了**双重保护**：

1. **Redisson 分布式锁**：
   - Webhook 回调: `webhook:duplicate:{taskId}`
   - 轮询: `poll:lock:{taskId}`

2. **幂等检查**：
   - Webhook: 获取锁后检查 `status >= 2`（终态），已终态则忽略
   - 轮询: 获取锁后双重检查 `fresh.getStatus() >= 2`，防止与 Webhook 冲突

3. **互不冲突**：Webhook 和轮询各自使用不同的锁 key，但通过状态终态检查互斥，不会出现并发覆盖。

### 3.2 🟢 无 version 乐观锁但可接受

`ai_task` 表没有 `version` 乐观锁字段（不像 `project` 表）。在当前设计中，Redisson 分布式锁已经提供了足够的并发保护，因此不需要乐观锁。但未来如果去除分布式锁（如改用消息队列），则需要补充 `version` 字段。

### 3.3 ✅ 与后端 DDL 一致

阿典使用的 `ai_task` 表字段（`next_poll_time`, `poll_count`, `last_poll_time`）与后端系分 DDL（行 527-547）完全一致，索引 `idx_status_next_poll` 也已包含。

---

## 总结

| 检查项 | 状态 | 说明 |
|--------|------|------|
| Webhook 路径 | ⚠️ 需修复 | 需在 Sa-Token 中排除 `/api/webhook/**` |
| 签名校验逻辑 | ⚠️ 有风险 | Header 解析脆弱、签名算法需验证、防重放未启用 |
| 错误码使用 | ⚠️ 需调整 | 40100 用于 Webhook 签名失败语义不当 |
| 线程池配置 | ✅ 一致 | 与后端系分线程池参数完全对齐 |
| 线程池耗尽风险 | 🟢 低风险 | Webhook 回调建议改为异步处理 |
| DB 并发安全 | ✅ 安全 | Redisson 锁 + 幂等检查双重保护 |
| DDL 一致性 | ✅ 一致 | 字段、索引与后端系分完全匹配 |

### 必须修复 (P0)
1. **Sa-Token 排除 `/api/webhook/**`** — 否则火山引擎回调全部 401 失败

### 建议修复 (P1)
2. Webhook 回调改为 `@Async` 异步处理，避免阻塞 Tomcat 线程
3. 修复签名校验的 Header 解析逻辑，增加防重放校验
4. Webhook 签名失败使用 `51000` 系列错误码而非 `40100`
