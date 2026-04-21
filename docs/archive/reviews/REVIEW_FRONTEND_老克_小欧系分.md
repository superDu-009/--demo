# 前端系分评审报告 — 老克 评 小欧

## 文档信息

| 项目 | 内容 |
|------|------|
| 评审对象 | docs/design/DESIGN_FRONTEND_小欧_v1.0.md |
| 评审依据 | docs/design/DESIGN_BACKEND_老克_v1.2.md |
| 评审人 | 老克（后端架构师） |
| 评审日期 | 2026-04-19 |
| 评审结论 | **有条件通过**（2 项 P0 修复 + 4 项 P1 优化后可开发） |

---

## 一、接口对齐审查

### 1.1 接口清单逐项对照

| # | 前端 API 调用 | 后端接口 | 方法 | 路径 | 结论 |
|---|--------------|---------|------|------|------|
| 1 | `userApi.login` | `/api/user/login` | POST | `/user/login` | ✅ 完全一致 |
| 2 | `userApi.logout` | `/api/user/logout` | POST | `/user/logout` | ✅ 完全一致 |
| 3 | `userApi.getInfo` | `/api/user/info` | GET | `/user/info` | ✅ 完全一致 |
| 4 | `projectApi.list` | `/api/project/list` | GET | `/project/list` | ✅ 完全一致 |
| 5 | `projectApi.create` | `/api/project` | POST | `/project` | ✅ 完全一致 |
| 6 | `projectApi.getDetail` | `/api/project/{id}` | GET | `/project/${id}` | ✅ 完全一致 |
| 7 | `projectApi.update` | `/api/project/{id}` | PUT | `/project/${id}` | ✅ 完全一致 |
| 8 | `projectApi.delete` | `/api/project/{id}` | DELETE | `/project/${id}` | ✅ 完全一致 |
| 9 | `projectApi.saveWorkflow` | `/api/project/{id}/workflow` | PUT | `/project/${id}/workflow` | ✅ 完全一致 |
| 10 | `projectApi.startWorkflow` | `/api/project/{id}/workflow/start` | POST | `/project/${id}/workflow/start` | ✅ 完全一致 |
| 11 | `projectApi.getWorkflowStatus` | `/api/project/{id}/workflow/status` | GET | `/project/${id}/workflow/status` | ✅ 完全一致 |
| 12 | `projectApi.reviewWorkflow` | `/api/project/{id}/workflow/review` | POST | `/project/${id}/workflow/review` | ✅ 完全一致 |
| 13 | `projectApi.stopWorkflow` | `/api/project/{id}/workflow/stop` | POST | `/project/${id}/workflow/stop` | ✅ 完全一致 |
| 14 | `projectApi.getShots` | `/api/project/{projectId}/shots` | GET | `/project/${projectId}/shots` | ✅ 已定义，见下方说明 |
| 15 | `assetApi.list` | `/api/project/{projectId}/assets` | GET | `/project/${projectId}/assets` | ✅ 完全一致 |
| 16 | `assetApi.create` | `/api/project/{projectId}/assets` | POST | `/project/${projectId}/assets` | ✅ 完全一致 |
| 17 | `assetApi.update` | `/api/asset/{id}` | PUT | `/asset/${id}` | ✅ 完全一致 |
| 18 | `assetApi.delete` | `/api/asset/{id}` | DELETE | `/asset/${id}` | ✅ 完全一致 |
| 19 | `assetApi.confirm` | `/api/asset/{id}/confirm` | PUT | `/asset/${id}/confirm` | ✅ 完全一致 |
| 20 | `assetApi.getReferences` | `/api/asset/{assetId}/references` | GET | `/asset/${assetId}/references` | ✅ 完全一致 |
| 21 | `contentApi.listEpisodes` | `/api/project/{projectId}/episodes` | GET | `/project/${projectId}/episodes` | ✅ 完全一致 |
| 22 | `contentApi.createEpisode` | `/api/project/{projectId}/episodes` | POST | `/project/${projectId}/episodes` | ✅ 完全一致 |
| 23 | `contentApi.updateEpisode` | `/api/episode/{id}` | PUT | `/episode/${id}` | ✅ 完全一致 |
| 24 | `contentApi.deleteEpisode` | `/api/episode/{id}` | DELETE | `/episode/${id}` | ✅ 完全一致 |
| 25 | `contentApi.listScenes` | `/api/episode/{episodeId}/scenes` | GET | `/episode/${episodeId}/scenes` | ✅ 完全一致 |
| 26 | `contentApi.createScene` | `/api/episode/{episodeId}/scenes` | POST | `/episode/${episodeId}/scenes` | ✅ 完全一致 |
| 27 | `contentApi.updateScene` | `/api/scene/{id}` | PUT | `/scene/${id}` | ✅ 完全一致 |
| 28 | `contentApi.deleteScene` | `/api/scene/{id}` | DELETE | `/scene/${id}` | ✅ 完全一致 |
| 29 | `contentApi.listShots` | `/api/scene/{sceneId}/shots` | GET | `/scene/${sceneId}/shots` | ✅ 完全一致 |
| 30 | `contentApi.createShot` | `/api/scene/{sceneId}/shots` | POST | `/scene/${sceneId}/shots` | ✅ 完全一致 |
| 31 | `contentApi.updateShot` | `/api/shot/{id}` | PUT | `/shot/${id}` | ✅ 完全一致 |
| 32 | `contentApi.deleteShot` | `/api/shot/{id}` | DELETE | `/shot/${id}` | ✅ 完全一致 |
| 33 | `contentApi.batchReview` | `/api/shot/batch-review` | POST | `/shot/batch-review` | ✅ 完全一致 |
| 34 | `contentApi.bindAsset` | `/api/shot/{shotId}/assets` | POST | `/shot/${shotId}/assets` | ✅ 完全一致 |
| 35 | `contentApi.unbindAsset` | `/api/shot/{shotId}/assets/{assetId}` | DELETE | `/shot/${shotId}/assets/${assetId}` | ✅ 完全一致 |
| 36 | `aiApi.getTaskStatus` | `/api/ai/task/{taskId}` | GET | `/ai/task/${taskId}` | ✅ 完全一致 |
| 37 | `aiApi.getLatestTask` | `/api/ai/task/latest` | GET | `/ai/task/latest` | ✅ 完全一致 |
| 38 | `aiApi.getCostReport` | `/api/ai/cost-report` | GET | `/ai/cost-report` | ✅ 完全一致 |
| 39 | `tosApi.presign` | `/api/tos/presign` | POST | `/tos/presign` | ✅ 完全一致 |
| 40 | `tosApi.complete` | `/api/tos/complete` | POST | `/tos/complete` | ✅ 完全一致 |

**接口总数：40 个，全部 100% 对齐，无遗漏、无多余。**

### 1.2 请求体/响应体结构对照

| 接口 | 前端 TS 类型 | 后端 Java DTO | 结论 |
|------|------------|--------------|------|
| 登录请求 | `LoginRequest` | `LoginRequest` | ✅ 字段名一致 |
| 登录响应 | `LoginResult` | 内联定义 | ✅ token/userId/username/nickname 全匹配 |
| 用户信息 | `UserInfoVO` | 内联定义 | ✅ id/username/nickname/status 全匹配 |
| 流程配置保存 | `WorkflowConfigRequest` | 内联定义 | ✅ version/workflowConfig/stylePreset 全匹配 |
| 流程状态 | `WorkflowStatusVO` | 内联定义 | ✅ 全部字段一致（含 v1.2 新增的 overallProgress/currentEpisode 等） |
| 批量审核请求 | `BatchReviewRequest` | 内联定义 | ✅ shotIds/action/comment 全匹配 |
| 批量审核响应 | `BatchReviewResult` | 内联定义 | ✅ totalCount/successCount/failedCount/failedDetails 全匹配 |
| 分镜详情 | `ShotVO` | `ShotVO` | ✅ 含 assetRefs + currentAiTask，v1.2 变更已对齐 |
| TOS 预签名请求 | `PresignRequest` | 内联定义 | ✅ fileName/contentType/projectDir 全匹配 |
| TOS 预签名响应 | `PresignResult` | 内联定义 | ✅ 含 v1.2 新增的 maxFileSize + allowedContentTypes |
| TOS 上传完成 | `TosCompleteRequest` | 内联定义 | ✅ key/projectId/fileType/metadata 全匹配 |
| AI 任务 | `AiTaskVO` | `AiTaskVO` | ✅ id/taskType/status/resultUrl/errorMsg 全匹配 |

### 1.3 发现的问题

| 编号 | 级别 | 问题 | 说明 |
|------|------|------|------|
| A1 | **P0** | `GET /api/project/{projectId}/shots` 接口已定义但未在页面逻辑中使用 | 该接口是 v1.2 新增的**跨分场聚合查询**，设计目的是在分镜工作台实现"跨分场查看所有分镜"。当前前端分镜工作台（ShotWorkbench）只使用了 `GET /api/scene/{sceneId}/shots`，缺失跨分场视图能力。建议明确此接口的使用场景。 |
| A2 | P1 | `projectApi.getShots` 未使用 | 已在 `api/project.ts` 中定义，但在任何 Store/View/Composable 中均未调用。如果确认不需要可移除以减少维护负担。 |

---

## 二、鉴权流程审查

### 2.1 Sa-Token 配置对照

| 配置项 | 后端 (application.yml) | 前端实现 | 结论 |
|--------|----------------------|---------|------|
| Token 请求头名称 | `Authorization` | `config.headers.Authorization` | ✅ 一致 |
| Token 前缀 | `Bearer` | `Bearer ${token}` | ✅ 一致 |
| Token 有效期 | 86400s (24h) | localStorage TTL 24h | ✅ 一致 |
| 活跃超时 | 1800s (30min) | 前端无对应处理 | ⚠️ 见下方说明 |
| 并发登录 | `is-concurrent: true` | 无限制 | ✅ 无需处理 |

**活跃超时说明：** 后端 Sa-Token 配置了 `active-timeout: 1800`（30分钟无操作则 Token 失效）。前端未做主动续期处理。这是**可接受的**，因为：
1. 前端每次请求都会携带 Token，Sa-Token 会自动刷新活跃时间
2. Token 过期后由 Axios 拦截器统一处理跳转登录
3. 前端 30s 轮询 workflow/status 会持续刷新活跃时间，正常工作时不会因活跃超时而掉线

### 2.2 白名单对照

| 后端白名单（SaRouter.notMatch） | 前端路由（requiresAuth: false） | 结论 |
|-------------------------------|--------------------------------|------|
| `/api/user/login` | `/login` | ✅ 一致 |
| `/api/tos/presign` | 无对应路由（API 调用） | ✅ 合理（需鉴权的 API 调用） |
| `/error` | 无对应路由 | ✅ 合理（Spring Boot 内部错误页） |

### 2.3 发现的问题

| 编号 | 级别 | 问题 | 说明 |
|------|------|------|------|
| B1 | P1 | `40301` 前端处理与后端约定不完全一致 | 后端约定 `40301` 应"跳转无权限页"，但前端 `constants/errors.ts` 中 `40301: 'toast'`。建议：(a) 前端增加 `/403` 页面并在 ERROR_ACTION 中映射 `40301: 'redirect-403'`；(b) 或明确与老克确认 40301 的实际交互行为，统一约定 |

---

## 三、分页规范审查

### 3.1 PageResult 结构对照

| 字段 | 后端 Java (PageResult<T>) | 前端 TS (PageResult<T>) | 类型 | 结论 |
|------|-------------------------|------------------------|------|------|
| total | `long` | `number` | TS number 可表示 Java long 范围 | ✅ 一致 |
| page | `int` (1-based) | `number` | 1-based | ✅ 一致 |
| size | `int` | `number` | — | ✅ 一致 |
| hasNext | `boolean` | `boolean` | — | ✅ 一致 |
| list | `List<T>` | `T[]` | — | ✅ 一致 |

**结论：前端 `PageResult<T>` 与后端 `PageResult<T>` 完全一致，字段名、语义、分页基准全部对齐。**

### 3.2 分页参数传递

| 接口 | 后端参数定义 | 前端传参方式 | 结论 |
|------|------------|-------------|------|
| `GET /api/project/list` | `page`(int, 默认1), `size`(int, 默认10) | `{ params: { page, size } }` | ✅ 一致 |
| `GET /api/scene/{sceneId}/shots` | `page`(int, 默认1), `size`(int, 默认20) | `{ params: { page, size, status } }` | ✅ 一致 |
| `GET /api/asset/{assetId}/references` | `page`(int, 默认1), `size`(int, 默认20) | `{ params: { page, size } }` | ✅ 一致 |

### 3.3 分页组件兼容性

- Element Plus `el-pagination` 默认使用 **1-based 页码**，与后端一致 ✅
- 前端 `fetchProjectList(page=1, size=10)` 默认值与后端一致 ✅
- 前端 `fetchShots(sceneId, page=1, size=20)` 默认值与后端一致 ✅

---

## 四、错误处理审查

### 4.1 错误码覆盖完整度

后端定义的 40000-51199 范围错误码共 **21 个细分错误码**，前端在 `constants/errors.ts` 中的覆盖情况：

| 错误码 | 后端定义 | 前端 ERROR_ACTION 映射 | 前端行为 | 结论 |
|--------|---------|----------------------|---------|------|
| `40001` | 用户名或密码错误 | `'form'` | 表单高亮 | ✅ |
| `40002` | 字段校验失败（通用） | `'form'` | 表单高亮 | ✅ |
| `40003` | 文件类型不支持 | `'toast'` | Toast 提示 | ✅ |
| `40004` | 文件大小超限 | `'toast'` | Toast 提示 | ✅ |
| `40005` | 预签名 URL 已过期 | `'retry-presign'` | 自动重试 | ✅ |
| `40100` | 未登录 | `'redirect-login'` | 跳转登录 | ✅ |
| `40101` | Token 过期 | `'redirect-login'` | 跳转登录 | ✅ |
| `40300` | 无操作权限 | `'toast'` | Toast 提示 | ✅ |
| `40301` | 非项目创建者 | `'toast'` | Toast 提示 | ⚠️ 应为跳转无权限页 |
| `40400` | 资源不存在 | `'toast'` | Toast 提示 | ✅ |
| `40900` | 项目正在执行中 | `'toast'` | Toast 提示 | ✅ |
| `40901` | 资产已被引用 | `'toast'` | Toast 提示 | ✅ |
| `40902` | 分镜状态不支持操作 | `'disabled-btn'` | 禁用按钮+Toast | ✅ |
| `40903` | 数据已被修改 | `'toast'` | Toast 提示 | ✅ |
| `42900` | 请求频率过高 | `'toast'` | Toast 提示 | ✅ |
| `50000` | 服务器内部错误 | `'global-error'` | 全局错误页 | ✅ |
| `51000` | AI 调用失败 | `'toast'` | Toast 提示 | ✅ |
| `51001` | AI 模型超时/排队中 | `'toast'` | Toast 提示 | ✅ |
| `51002` | AI 生成内容为空 | `'toast'` | Toast 提示 | ✅ |
| `51100` | TOS 上传失败 | `'toast'` | Toast 提示 | ✅ |
| `51101` | TOS 存储空间不足 | `'toast'` | Toast 提示 | ✅ |

**覆盖度：21/21 = 100%**

### 4.2 HTTP 状态码处理对照

| HTTP 状态码 | 后端约定 | 前端拦截器处理 | 结论 |
|------------|---------|--------------|------|
| 401 | 401xx 范围 → 跳转登录 | `case 401: clearAuth() + 跳转 Login` | ✅ 完全一致 |
| 403 | 403xx 范围 → Toast | `case 403: Toast "无操作权限"` | ✅ 一致 |
| 200 + 业务码非0 | 根据业务码处理 | `res.code !== 0 → handleBusinessError()` | ✅ 完全一致 |
| 500 | 500xx 范围 → 全局错误页 | 走 default → 提示"网络异常" | ⚠️ 见下方说明 |
| 429 | 限流 → Toast | `case 429: Toast "请求频率过高"` | ✅ 一致 |

### 4.3 发现的问题

| 编号 | 级别 | 问题 | 说明 |
|------|------|------|------|
| C1 | **P0** | 510xx/511xx 错误码走 HTTP 200 路径的处理需确认 | 后端约定 51000-51199 返回 HTTP 200 + 业务码。前端成功拦截器中 `res.code !== 0` 会走 `handleBusinessError`，映射表中 510xx/511xx 均为 `'toast'`，行为正确。但 **51001（AI模型超时/排队中）** 后端约定前端应"展示排队状态"，而前端仅做了 Toast 提示。建议在分镜卡片中增加排队状态 UI 展示（如"AI模型排队中，请耐心等待..."）。 |
| C2 | P2 | HTTP 500 处理不够精准 | 后端约定 50000-50099 应展示全局错误页，但前端在 HTTP 状态码拦截中只有 401/403/429 三个 case，其余走 default 提示"网络异常"。如果后端返回 HTTP 500，前端不会展示全局错误页。建议增加 `case 500` 路由到全局错误页，或在 `handleBusinessError` 中兜底处理。 |

---

## 五、额外发现与建议

### 5.1 乐观锁版本号安全

`stores/workflow.ts` 中 `saveConfig` 函数：
```typescript
const version = useProjectStore().currentProject?.version || 0
```

**风险：** 如果 `currentProject` 为 null（如页面刷新后未重新加载），`version` 将取默认值 `0`，可能导致乐观锁冲突。建议在 `saveConfig` 前增加 `currentProject` 非空校验。

### 5.2 TOS 上传重试时的重复 Toast

`useTosUpload.ts` 中捕获 `40005` 后自动重试：
```typescript
if (error.response?.data?.code === 40005) {
  ElMessage.warning('上传链接已过期，正在重新获取...')
  return upload(file, options)
}
```

**问题：** Axios 响应拦截器的 `handleBusinessError` 也会先触发 `'retry-presign'` 对应的 Toast（"上传链接已过期，正在重新获取..."），然后 `useTosUpload` 再显示一次。用户会看到**两条相同的 Toast**。

**建议：** 在 `handleBusinessError` 中 `'retry-presign'` 分支不显示 Toast，或 `useTosUpload` 中不显示，只保留一处。

### 5.3 40301 无权限页面缺失

后端定义了 `40301` 错误码用于"非项目创建者无权操作"，约定前端"跳转无权限页"，但前端路由设计中没有 `/403` 页面。

**建议：**
- 方案 A：新增 `views/Forbidden.vue` 页面，路由中添加 `{ path: '/403', ... }`
- 方案 B：与老克确认，40301 是否也可用 Toast 替代（考虑到 MVP 范围）

### 5.4 前端 API 路径前缀一致性

前端 Axios `baseURL` 设置为 `/api`，各模块 API 调用路径**不包含** `/api` 前缀（如 `/user/login` 而非 `/api/user/login`），与后端实际路径拼接后完全一致。✅ 正确。

### 5.5 分镜状态枚举一致性

后端 shot 表 status 定义：`0-待处理 1-生成中 2-待审核 3-已通过 4-已打回 5-已完成`

前端 `ShotStatus` 枚举：
```typescript
Pending = 0, Generating = 1, WaitingReview = 2,
Approved = 3, Rejected = 4, Completed = 5
```

**✅ 完全一致。**

### 5.6 WorkflowTaskStatus 一致性

后端 workflow_task status：`0-未执行 1-执行中 2-成功 3-失败 4-待审核`

前端 `WorkflowTaskStatus` 枚举：
```typescript
NotStarted = 0, Running = 1, Success = 2, Failed = 3, WaitingReview = 4
```

**✅ 完全一致。**

### 5.7 AiTaskStatus 一致性

后端 ai_task status：`0-提交中 1-处理中 2-成功 3-失败`

前端 `AiTaskStatus` 枚举：
```typescript
Submitting = 0, Processing = 1, Success = 2, Failed = 3
```

**✅ 完全一致。**

### 5.8 StepType 一致性

后端 step_type 枚举值：`import/asset_extract/shot_gen/image_gen/video_gen/export`

前端 `StepType` 类型定义完全一致。✅

### 5.9 Phase 2 预留功能处理

后端 Phase 2 预留功能：
- `GET /api/project/{id}/workflow/stream` (SSE) — 前端未实现，✅ 正确
- 分片上传 (multipart) — 前端文档标注 "Phase 2"，✅ 正确

---

## 六、评审总结

### 问题汇总

| 级别 | 数量 | 问题编号 | 说明 |
|------|------|---------|------|
| **P0** | 2 | A1, C1 | 跨分场查询接口未使用；51001 排队状态 UI 缺失 |
| **P1** | 3 | B1, A2, 5.2 | 40301 处理方式不一致；getShots API 未使用；TOS 重试 Toast 重复 |
| **P2** | 2 | C2, 5.1 | HTTP 500 处理不精准；乐观锁版本号安全校验 |

### 总体评价

小欧的前端系分文档质量**很高**，具体表现在：

1. **接口对齐度极佳**：40 个 API 调用全部与后端设计 100% 匹配，URL、方法、参数名、请求体/响应体结构无一遗漏
2. **类型定义严谨**：TypeScript 类型与后端 Java DTO 字段名、类型、语义完全对齐
3. **分页规范完全兼容**：PageResult 结构、1-based 页码、默认参数全部一致
4. **错误码覆盖全面**：21 个细分错误码 100% 覆盖，映射行为合理
5. **鉴权流程正确**：Bearer Token、Sa-Token 白名单、401/403 处理均符合后端约定
6. **枚举值全部对齐**：ShotStatus、WorkflowTaskStatus、AiTaskStatus、StepType 无一偏差

2 项 P0 问题均不影响整体架构，属于实现细节优化。修复后可进入开发阶段。

---

*评审人：老克 | 日期：2026-04-19*
