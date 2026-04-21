# 后端评审报告 — TESTCASES_PM_小赫_v1.0.md

## 文档信息

| 项目 | 内容 |
|------|------|
| 评审文档 | TESTCASES_PM_小赫_v1.0.md |
| 对照文档 | DESIGN_BACKEND_老克_v1.4（即 DESIGN_BACKEND_老克_v1.2.md 文件内 v1.4 内容） |
| 评审人 | 老克（后端架构师） |
| 评审日期 | 2026-04-20 |
| 评审结论 | **有条件通过** |

---

## 一、P0 用例逐条比对（接口路径 / 输入输出 / 预期结果）

### 1.1 用户模块

| 用例 | 比对结果 | 说明 |
|------|----------|------|
| TC-USER-001 正常登录 | 一致 | 接口、输入、输出均匹配系分 4.1.1 |
| TC-USER-002 错误密码 | 一致 | 匹配 |
| TC-USER-005 获取用户信息(已登录) | 一致 | 匹配 |
| TC-USER-006 获取用户信息(未登录) | 一致 | 匹配 |

### 1.2 项目管理

| 用例 | 比对结果 | 说明 |
|------|----------|------|
| TC-PRJ-001 创建项目 | **部分偏差** | 预期中 `execution_lock=0` 正确，但系分中创建项目时 `status=0` 是默认值而非显式返回，`execution_lock` 也不在创建响应中返回（设计 4.2.1 响应体不含 execution_lock），仅查询时返回 |
| TC-PRJ-002 获取项目列表 | **偏差** | 系分 v1.2 已改为**分页响应** `PageResult<ProjectVO>`（含 page/size/hasNext/total），测试用例未验证分页参数和分页响应结构 |
| TC-PRJ-003 获取项目详情 | 一致 | 匹配 |

### 1.3 资产管理

| 用例 | 比对结果 | 说明 |
|------|----------|------|
| TC-ASSET-001 创建角色资产 | 一致 | 匹配 |
| TC-ASSET-002 创建其他类型资产 | 一致 | 匹配 |
| TC-ASSET-003 获取资产列表 | 一致 | 匹配 |

### 1.4 内容管理

| 用例 | 比对结果 | 说明 |
|------|----------|------|
| TC-CONTENT-001 获取分集列表 | 一致 | 匹配 |
| TC-CONTENT-002 获取分场列表 | 一致 | 匹配 |
| TC-CONTENT-003 获取分镜列表 | **偏差** | 系分 v1.2 已改为**分页响应** `PageResult<ShotVO>`，含 `assetRefs` + `currentAiTask` 内嵌字段，测试用例未验证分页结构和新增字段 |
| TC-CONTENT-004 更新分镜 | **偏差** | 系分中 updateShot 不保证 version 递增（version 是 shot 表固定版本号字段，非乐观锁），测试用例预期 "version 递增" 与系分不符 |
| TC-CONTENT-005 分镜关联资产 | **偏差** | 系分 v1.2 明确为**单条绑定** `POST /api/shot/{shotId}/assets`（bindAssetToShot 方法签名为单 assetId），非批量。测试用例输入 `{ "asset_ids": [1, 2, 3] }` 为批量形式，与系分不一致 |
| TC-CONTENT-006 批量审核通过 | 一致 | 接口路径和输入匹配 |
| TC-CONTENT-007 批量审核打回 | 一致 | 接口路径和输入匹配 |

### 1.5 流程引擎

| 用例 | 比对结果 | 说明 |
|------|----------|------|
| TC-WF-001 保存流程配置 | **偏差** | 系分要求请求体必须传入 `version` 字段（乐观锁），测试用例输入未包含 version |
| TC-WF-002 开始执行流程 | 一致 | 匹配 |
| TC-WF-003 重复点击开始 | 一致 | 匹配 |
| TC-WF-004 查询执行进度 | **偏差** | 系分 v1.2 响应增加 `overallProgress`、`totalShots`、`processedShots`、`estimatedRemainingSeconds`、`currentEpisodeId/Title` 等字段，测试用例未覆盖这些新增字段 |
| TC-WF-005 审核节点暂停 | 一致 | 匹配 |
| TC-WF-006 审核通过继续执行 | **偏差** | 系分审核请求体使用 `stepType`（string）而非 `step_id`，测试用例输入 `{ "action": "approve", "step_id": xxx }` 参数名错误 |
| TC-WF-009 断点续跑 | 一致 | 场景描述匹配 |

### 1.6 AI 任务

| 用例 | 比对结果 | 说明 |
|------|----------|------|
| TC-AI-001 触发图片生成 | **严重偏差** | 系分 v1.2 **已移除** `POST /api/ai/image-gen` 接口，明确标注"移除手动触发 image-gen/video-gen 接口（由 WorkflowEngine 内部驱动）"，仅保留查询接口。该用例对应接口不存在 |
| TC-AI-002 触发视频生成 | **严重偏差** | 同上，`POST /api/ai/video-gen` 已移除 |
| TC-AI-003 查询 AI 任务状态 | 一致 | 匹配 |
| TC-AI-005 AI 任务结果上传 TOS | 一致 | 场景描述匹配 |
| TC-AI-007 Seedance API 参数正确性 | 一致 | 匹配系分 9.2 参数映射 |

### 1.7 Webhook 回调

| 用例 | 比对结果 | 说明 |
|------|----------|------|
| TC-WH-001 ~ TC-WH-006 | **严重偏差** | 系分明确标注 Webhook 为 **Phase 2 预留功能**（"待 Seedance 支持 webhook 后切换"），MVP 阶段使用**轮询机制**替代。这 6 个用例在 MVP 范围内**无法执行**，接口不存在 |

### 1.8 TOS 存储

| 用例 | 比对结果 | 说明 |
|------|----------|------|
| TC-TOS-001 获取预签名上传 URL | **偏差** | 系分 v1.2 变更明确标注"预签名接口鉴权修正为'是'"，但 SaTokenConfig 代码未同步移除 `.notMatch("/api/tos/presign")`，存在设计文档内部矛盾。测试用例跟随了旧版代码行为写"无需鉴权"，与 v1.2 变更意图不符 |
| TC-TOS-002 前端直传 TOS | 一致 | 场景描述匹配 |
| TC-TOS-003 后端上传到 TOS | 一致 | 场景描述匹配 |

### 1.9 安全与鉴权

| 用例 | 比对结果 | 说明 |
|------|----------|------|
| TC-SEC-001 未登录访问 | 一致 | 匹配 |
| TC-SEC-002 Token 过期 | 一致 | 匹配 |
| TC-SEC-003 公开接口无需登录 | **偏差** | 列出了 `/api/tos/presign` 为公开接口，与 v1.2 变更意图（需鉴权）矛盾 |
| TC-SEC-004 API Key 不暴露 | 一致 | 匹配 |
| TC-SEC-005 API Key 从环境变量读取 | 一致 | 匹配 |

### 1.10 并发与异常

| 用例 | 比对结果 | 说明 |
|------|----------|------|
| TC-CONC-001 流程执行并发锁 | 一致 | 匹配 |
| TC-CONC-002 Webhook+轮询并发 | **偏差** | 涉及 Webhook 的并发场景在 MVP 中不适用（Webhook 为 Phase 2），应改为纯轮询并发场景 |
| TC-CONC-004 服务重启后任务恢复 | 一致 | 匹配 |

---

## 二、遗漏的后端关键场景

### P0 级别遗漏

| # | 遗漏场景 | 系分依据 | 建议 |
|---|----------|----------|------|
| L-01 | **v1.2 新增接口完全缺失测试** | 系分附录 D 列出 4 个新增接口 | 必须补充以下用例：<br>• `POST /api/tos/complete`（上传完成通知）<br>• `GET /api/project/{projectId}/shots`（项目级分镜聚合查询）<br>• `GET /api/asset/{assetId}/references`（资产引用查询）<br>• `GET /api/ai/task/latest?shotId=xxx`（查询分镜最新 AI 任务） |
| L-02 | **乐观锁并发冲突测试** | 系分 4.2.1 saveWorkflowConfig 要求带 version 字段，并发时返回 40903 | 补充用例：两个请求同时更新 workflow_config，第二个返回 40903 |
| L-03 | **分页参数校验测试** | 系分多个列表接口使用 PageResult，含 page/size 参数 | 补充用例：page=0、size>100、page/size 缺失（应走默认值） |

### P1 级别遗漏

| # | 遗漏场景 | 系分依据 | 建议 |
|---|----------|----------|------|
| L-04 | **线程池满载 + 拒绝策略** | 系分 6.1：AbortPolicy 拒绝策略，抛 RejectedExecutionException 返回 42900 | 补充用例：提交超过 aiTaskExecutor 容量的任务，验证返回 42900 而非阻塞 |
| L-05 | **AI 熔断器测试** | 系分 4.6.4/8.2：连续失败 5 次 → 熔断 30s → 半开试探 | 补充用例：模拟 Seedance 连续 5 次失败，验证新任务被快速拒绝；30s 后恢复 |
| L-06 | **指数退避轮询测试** | 系分 6.3/8.1：next_poll_time 驱动轮询，退避策略 | 补充用例：验证 poll_count 递增、next_poll_time 延迟递增 |
| L-07 | **RecoveryRunner 启动恢复** | 系分 5.5：服务启动时扫描 execution_lock=1 的项目 | 补充用例：模拟 execution_lock=1 但无 Redis 锁状态，验证自动恢复 |
| L-08 | **TOS complete 上传闭环** | 系分 4.7.1：POST /api/tos/complete 通知接口 | 补充用例：前端直传后调用 complete，验证 HEAD 校验 + DB 更新 |
| L-09 | **分镜-资产解绑接口** | 系分 4.4.1：DELETE /api/shot/{shotId}/assets/{assetId} | 补充解绑测试用例 |
| L-10 | **批量审核结果明细验证** | 系分 4.4.1：响应返回 successCount/failedCount/failedDetails | 测试用例 TC-CONTENT-006/007 未验证响应中的失败明细结构 |
| L-11 | **调度器独立线程池** | 系分 6.2：@Scheduled 使用独立 schedulerExecutor | 补充用例：验证定时任务不阻塞 Tomcat 线程 |
| L-12 | **Actuator 安全配置** | 系分 10.1：management endpoints 仅暴露 health,info，env 值隐藏 | 补充用例：验证 /actuator/env 不泄露配置值 |

### P2 级别遗漏

| # | 遗漏场景 | 系分依据 | 建议 |
|---|----------|----------|------|
| L-13 | **FFmpeg 导出节点** | 系分 5.2 ExportNodeExecutor | 补充视频合并导出测试 |
| L-14 | **PromptEngine 提示词生成** | 系分 Sprint 3.9 | 补充 prompt_en 生成质量验证 |
| L-15 | **TOS Key 命名规范验证** | 系分 9.3 | 验证上传文件路径符合命名规范 |
| L-16 | **JSON TypeHandler 序列化** | 系分 2.5 | 验证 workflowConfig/stylePreset JSON 字段正确读写 |

---

## 三、测试用例设计建议

### 3.1 接口映射修正

| 原用例 | 问题 | 修正建议 |
|--------|------|----------|
| TC-AI-001 | 接口 `POST /api/ai/image-gen` 不存在 | 删除或改为：验证 WorkflowEngine 内部自动触发 image_gen 任务 |
| TC-AI-002 | 接口 `POST /api/ai/video-gen` 不存在 | 删除或改为：验证 WorkflowEngine 内部自动触发 video_gen 任务 |
| TC-WH-001~006 | Webhook 为 Phase 2，MVP 无此接口 | 全部移至 Phase 2 测试计划，MVP 中替换为轮询相关用例 |
| TC-WF-006 | 参数 `step_id` 应为 `stepType` | 修正输入为 `{ "stepType": "asset_extract", "action": "approve" }` |
| TC-WF-001 | 缺少 `version` 字段 | 补充 version 到输入体 |
| TC-CONTENT-005 | 批量绑定的接口不存在 | 改为逐个绑定测试，或确认是否需要新增批量接口 |
| TC-TOS-001 / TC-SEC-003 | presign 鉴权状态矛盾 | 明确 presign 是否需要鉴权后统一修正 |

### 3.2 状态码一致性

测试用例中部分预期结果使用了模糊描述如 `code != 0`，建议与系分 2.2 错误码规范对齐：

| 场景 | 应使用的错误码 |
|------|---------------|
| 未登录 | 40100 |
| Token 过期 | 40101 |
| 无操作权限 | 40300 / 40301 |
| 项目不存在 | 40400 |
| 项目正在执行中 | 40900 |
| 资产被引用不可删 | 40901 |
| 数据已被修改 | 40903 |
| 请求频率过高 | 42900 |
| AI 调用失败 | 51000 |
| Webhook 签名无效 | 51003（但 Webhook 为 Phase 2，暂不需要） |

---

## 四、问题汇总

| 优先级 | 数量 | 说明 |
|--------|------|------|
| P0 | 8 | 接口不存在（AI 手动触发/Webhook）、分页遗漏、乐观锁遗漏、v1.2 新增接口全部缺失 |
| P1 | 12 | 鉴权矛盾、参数名错误、线程池/熔断/轮询/恢复等关键场景缺失 |
| P2 | 4 | FFmpeg 导出、PromptEngine、TOS Key 命名、JSON TypeHandler |

---

## 五、评审结论

### **有条件通过**

**理由：**
1. 测试用例整体结构清晰，模块覆盖较全面，P0 核心流程的测试思路正确
2. 但存在以下必须修正的问题：
   - **TC-AI-001/002** 对应的接口在系分 v1.2 中已被移除，必须替换为 WorkflowEngine 内部驱动的测试场景
   - **TC-WH-001~006**（6 个 Webhook 用例）在 MVP 中不可执行，需移至 Phase 2
   - **4 个 v1.2 新增接口**（tos/complete、project/shots、asset/references、ai/task/latest）完全缺失测试
   - **分页**和**乐观锁**作为系分 v1.2 重点变更，测试用例未覆盖
   - **熔断器**和**线程池拒绝策略**作为核心稳定性保障，缺少测试

**通过条件（修复后方可进入开发/测试阶段）：**
1. 删除或替换 TC-AI-001/002（接口不存在）
2. 将 TC-WH-001~006 移至 Phase 2 测试计划
3. 补充 4 个 v1.2 新增接口的测试用例
4. 补充分页参数和乐观锁并发测试用例
5. 修正 TC-WF-006 参数名（step_id → stepType）
6. 明确并统一 /api/tos/presign 的鉴权状态

**设计文档自身问题（需老克修正）：**
- SaTokenConfig 中 `.notMatch("/api/tos/presign")` 与 4.7.1 表格中"鉴权: 是"矛盾，需同步更新代码或文档

---

*评审人: 老克 | 日期: 2026-04-20 | 状态: 有条件通过*
