# AI漫剧生产平台 — MVP 测试用例 (Test Cases)

## 文档信息

| 项目 | 内容 |
|------|------|
| 产品名 | AI漫剧生产平台 |
| 版本 | **v1.1** (评审修订版) |
| 基于 | PRD v1.1 + DESIGN_BACKEND_老克_v1.4 + DESIGN_FRONTEND_小欧_v1.2 + DESIGN_AI_阿典_v1.2 |
| 编写人 | 小赫 (产品经理兼测试) |
| 评审人 | 蓝烟老师、老克、小欧、阿典 |
| 创建日期 | 2026-04-20 |
| 修订日期 | 2026-04-20 |
| 状态 | 评审修订完成，待 CEO 审批 |

**v1.1 变更说明：** 根据老克/小欧/阿典三方评审意见，修正接口不一致问题（移除已废弃 AI 接口、Webhook 移至 Phase 2），补充 4 个 v1.2 新增接口、分页、乐观锁、路由守卫、Pinia 状态、TOS 直传、任务状态机、Prompt 安全等 68 条用例，修正 8 条错误描述。详见 `REPLY_小赫_REVIEWS.md`。

---

## 目录

- [1. 测试范围与策略](#1-测试范围与策略)
- [2. 用户模块测试](#2-用户模块测试)
- [3. 项目管理测试](#3-项目管理测试)
- [4. 资产管理测试](#4-资产管理测试)
- [5. 内容管理测试](#5-内容管理测试)
- [6. 流程引擎测试](#6-流程引擎测试)
- [7. AI 任务测试](#7-ai-任务测试)
- [8. Prompt 工程测试](#8-prompt-工程测试)
- [9. 一致性保障测试](#9-一致性保障测试)
- [10. TOS 存储测试](#10-tos-存储测试)
- [11. 安全与鉴权测试](#11-安全与鉴权测试)
- [12. 并发与异常测试](#12-并发与异常测试)
- [13. 前端集成测试](#13-前端集成测试)
- [14. 端到端全流程测试](#14-端到端全流程测试)
- [附录 A. Phase 2 测试计划](#附录-a-phase-2-测试计划)

---

## 1. 测试范围与策略

### 1.1 测试分层

| 层级 | 测试类型 | 工具 | 范围 |
|------|---------|------|------|
| L1 | 单元测试 | JUnit 5 | Service 层业务逻辑 |
| L2 | API 接口测试 | SpringBootTest + MockMvc / Postman | Controller 层 |
| L3 | 集成测试 | SpringBootTest + MySQL | 数据库交互、Redis、TOS |
| L4 | 前端 E2E | Playwright / Cypress | 用户操作流程 |

### 1.2 MVP 测试重点

- **P0 核心流程**：登录→创建项目→资产确认→流程执行→分镜审核→视频导出
- **P0 一致性**：资产引用机制、Seedance 调用参数正确性
- **P1 安全**：Sa-Token 鉴权、API Key 不暴露、路由守卫
- **P1 并发**：流程执行锁、轮询幂等、乐观锁

---

## 2. 用户模块测试

### TC-USER-001: 正常登录 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/user/login` |
| 输入 | `{ "username": "testuser", "password": "Test123!" }` |
| 预期 | 返回 `code=0`，包含 `token`，sa-token cookie 设置成功 |
| 验证 | 使用 token 调用 `/api/user/info` 返回正确用户信息 |

### TC-USER-002: 错误密码登录 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/user/login` |
| 输入 | `{ "username": "testuser", "password": "wrongpwd" }` |
| 预期 | 返回 `code=40100`，不返回 token，不区分用户名/密码错误 |

### TC-USER-003: 不存在用户登录 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `POST /api/user/login` |
| 输入 | `{ "username": "nouser", "password": "123456" }` |
| 预期 | 返回 `code=40100`，提示信息模糊（防枚举） |

### TC-USER-004: 空参数登录 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `POST /api/user/login` |
| 输入 | `{}` 或缺少必填字段 |
| 预期 | 返回 `code=40000`，参数校验错误 |

### TC-USER-005: 获取用户信息（已登录） ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/user/info` |
| 前置 | 已登录，携带 sa-token |
| 预期 | 返回 `code=0`，包含 username、nickname、status |

### TC-USER-006: 获取用户信息（未登录） ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/user/info` |
| 前置 | 不携带 token |
| 预期 | 返回 `code=40100` |

### TC-USER-007: 登出 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `POST /api/user/logout` |
| 前置 | 已登录 |
| 预期 | token 失效，再次调用 `/api/user/info` 返回 `code=40100` |

---

## 3. 项目管理测试

### TC-PRJ-001: 创建项目 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/project` |
| 输入 | `{ "name": "测试项目", "description": "描述", "novel_tos_path": "tos://bucket/novel.txt" }` |
| 预期 | 返回 `code=0`，`data` 包含项目 id、status=0(草稿) |
| DB验证 | `project` 表新增记录，`user_id` 正确，`execution_lock=0` |

### TC-PRJ-002: 获取项目列表（分页） ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/project/list?page=1&size=10` |
| 预期 | 返回 `PageResult<ProjectVO>`，含 `total/page/size/hasNext`，按 `create_time` 倒序 |

### TC-PRJ-003: 分页参数校验 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/project/list?page=0&size=1000` |
| 预期 | page=0 走默认值 1，size>100 限制为 100 或返回校验错误 |

### TC-PRJ-004: 获取项目详情 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/project/{id}` |
| 预期 | 返回完整项目信息，包含 `workflow_config`、`style_preset`、`status`、`execution_lock` |

### TC-PRJ-005: 更新项目 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `PUT /api/project/{id}` |
| 输入 | `{ "name": "新名称", "version": 1 }` |
| 预期 | 返回 `code=0`，DB 已更新，乐观锁 version 检查通过 |

### TC-PRJ-006: 删除项目（软删） ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `DELETE /api/project/{id}` |
| 预期 | `deleted=1`，列表和详情不再返回 |

### TC-PRJ-007: 无权访问他人项目 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `GET /api/project/{otherUserIdProjectId}` |
| 预期 | 返回 `code=40300` |

---

## 4. 资产管理测试

### TC-ASSET-001: 创建角色资产 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/project/{id}/assets` |
| 输入 | `{ "asset_type": "character", "name": "主角小明", "description": "少年形象...", "reference_images": ["url1", "url2"] }` |
| 预期 | 返回 `code=0`，第一个 URL 为主图，status=0(草稿) |

### TC-ASSET-002: 创建场景/物品/声音资产 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/project/{id}/assets` |
| 输入 | 分别传入 `asset_type: scene/prop/voice` |
| 预期 | 各类资产均能正常创建 |

### TC-ASSET-003: 获取资产列表（按类型分组） ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/project/{id}/assets` |
| 预期 | 返回资产列表，可按 `asset_type` 过滤或分组 |

### TC-ASSET-004: 更新资产 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `PUT /api/asset/{id}` |
| 输入 | `{ "description": "修改后的描述", "reference_images": ["new_url1", "new_url2", "new_url3"] }` |
| 预期 | 更新成功，参考图数量变化 |

### TC-ASSET-005: 确认资产 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `PUT /api/asset/{id}` |
| 输入 | `{ "status": 1 }` (已确认) |
| 预期 | 资产状态变为已确认，可被分镜引用 |

### TC-ASSET-006: 查询资产引用关系 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `GET /api/asset/{assetId}/references` |
| 预期 | 返回引用该资产的分镜列表，含 shot_id、shot 状态 |

### TC-ASSET-007: 删除被引用资产 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 资产已被分镜引用，尝试删除 |
| 预期 | 返回 `code=40901`（资产被引用不可删） |

---

## 5. 内容管理测试

### TC-CONTENT-001: 获取分集列表 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/project/{id}/episodes` |
| 预期 | 按 `sort_order` 排序返回分集列表 |

### TC-CONTENT-002: 获取分场列表 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/episode/{id}/scenes` |
| 预期 | 返回该分集下的所有分场 |

### TC-CONTENT-003: 获取分镜列表（分页） ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/scene/{id}/shots?page=1&size=20` |
| 预期 | 返回 `PageResult<ShotVO>`，含 `assetRefs`（关联资产列表）和 `currentAiTask`（当前AI任务状态）内嵌字段 |

### TC-CONTENT-004: 项目级分镜聚合查询 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/project/{projectId}/shots` |
| 预期 | 返回该项目所有分镜的聚合列表，可过滤状态 |

### TC-CONTENT-005: 更新分镜 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `PUT /api/shot/{id}` |
| 输入 | `{ "prompt": "新的提示词", "prompt_en": "new prompt in English" }` |
| 预期 | 更新成功，返回更新后的分镜信息 |

### TC-CONTENT-006: 分镜关联资产（单条绑定） ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/shot/{shotId}/assets` |
| 输入 | `{ "asset_id": 1 }` |
| 预期 | `shot_asset_ref` 表插入记录，`UNIQUE(shot_id, asset_id)` 约束生效，重复绑定返回 `code=40902` |

### TC-CONTENT-007: 分镜解绑资产 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `DELETE /api/shot/{shotId}/assets/{assetId}` |
| 预期 | 关联记录删除成功 |

### TC-CONTENT-008: 批量审核通过 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/shot/batch-review` |
| 输入 | `{ "shot_ids": [1, 2, 3], "action": "approve" }` |
| 预期 | 返回 `{ "successCount": 3, "failedCount": 0, "failedDetails": [] }`，所有分镜 status=3 |

### TC-CONTENT-009: 批量审核打回 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/shot/batch-review` |
| 输入 | `{ "shot_ids": [4, 5], "action": "reject", "comment": "角色形象不一致" }` |
| 预期 | 分镜 status=4，`review_comment` 写入，返回 `successCount` 和 `failedCount` |

### TC-CONTENT-010: 批量审核部分失败 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 部分分镜状态不允许审核（如 status=0） |
| 预期 | 返回 `successCount` 和 `failedDetails`，成功的已更新，失败的保留原状态 |

---

## 6. 流程引擎测试

### TC-WF-001: 保存流程配置（含乐观锁） ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `PUT /api/project/{id}/workflow` |
| 输入 | `{ "steps": [...], "version": 1 }` |
| 预期 | `project.workflow_config` 已更新，version 递增，返回 `code=0` |

### TC-WF-002: 乐观锁并发冲突 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 两个请求同时用 version=1 更新 workflow_config |
| 预期 | 第一个成功，第二个返回 `code=40903`（数据已被修改） |

### TC-WF-003: 开始执行流程 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/project/{id}/workflow/start` |
| 预期 | `execution_lock=1`，`workflow_task` 表创建记录，异步执行开始 |

### TC-WF-004: 重复点击开始（并发锁） ✅ P0

| 项 | 值 |
|---|---|
| 接口 | 连续两次 `POST /api/project/{id}/workflow/start` |
| 预期 | 第二次返回 `code=40900`（项目正在执行中） |

### TC-WF-005: 查询执行进度 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/project/{id}/workflow/status` |
| 预期 | 返回 `overallProgress`、`totalShots`、`processedShots`、`estimatedRemainingSeconds`、`currentEpisodeId/Title`、各 step 状态 |

### TC-WF-006: 审核节点暂停 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 流程执行到 `review: true` 的步骤 |
| 预期 | 该 step status=4(待审核)，后续步骤不执行，`execution_lock=1` 保持 |

### TC-WF-007: 审核通过继续执行 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/project/{id}/workflow/review` |
| 输入 | `{ "stepType": "asset_extract", "action": "approve" }` |
| 预期 | 该 step status=2(成功)，后续步骤继续执行 |

### TC-WF-008: 审核打回 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `POST /api/project/{id}/workflow/review` |
| 输入 | `{ "stepType": "asset_extract", "action": "reject", "comment": "xxx" }` |
| 预期 | 流程停止或回退，step status=3(失败) |

### TC-WF-009: 停止执行 ✅ P1

| 项 | 值 |
|---|---|
| 接口 | `POST /api/project/{id}/workflow/stop` |
| 预期 | 正在执行的步骤完成后停止，`execution_lock=0` |

### TC-WF-010: 断点续跑 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 流程执行到 step 3 时服务重启 |
| 操作 | 重启后再次调用 start 或恢复接口 |
| 预期 | 从 step 4 继续，step 1-2 不重复 |

### TC-WF-011: 跳过禁用步骤 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 流程配置中某 step `enabled: false` |
| 预期 | 执行时跳过，直接执行下一个 |

### TC-WF-012: RecoveryRunner 启动恢复 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 服务启动时扫描 `execution_lock=1` 但 Redis 锁已丢失的项目 |
| 预期 | 自动恢复流程执行，不丢失进度 |

---

## 7. AI 任务测试

### TC-AI-001: WorkflowEngine 内部驱动图片生成 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 流程执行到 image_gen 步骤 |
| 预期 | WorkflowEngine 自动创建 ai_task 记录（status=0），异步提交到 Seedance，不依赖前端手动触发 |

### TC-AI-002: WorkflowEngine 内部驱动视频生成 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 流程执行到 video_gen 步骤 |
| 预期 | WorkflowEngine 自动创建 ai_task，验证首帧图存在才提交，无首帧图返回 `code=51002` |

### TC-AI-003: 查询 AI 任务状态 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/ai/task/{taskId}` |
| 预期 | 返回 task 状态，成功后包含 `result_url` |

### TC-AI-004: 查询分镜最新 AI 任务 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `GET /api/ai/task/latest?shotId=xxx` |
| 预期 | 返回该分镜最新的 ai_task 记录 |

### TC-AI-005: AI 任务失败处理 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | Seedance API 返回错误 |
| 预期 | `ai_task.status=3`，`error_msg` 记录错误，`generation_attempts++` |

### TC-AI-006: AI 任务重试上限 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | AI 任务失败 3 次（`generation_attempts >= 3`） |
| 预期 | 不再重试，保持失败状态 |

### TC-AI-007: AI 任务结果上传 TOS ✅ P0

| 项 | 值 |
|---|---|
| 场景 | Seedance 返回结果 |
| 预期 | 后端下载结果并上传到 TOS，`ai_task.result_url` 为 TOS 路径（格式 `projects/{id}/output/...`） |

### TC-AI-008: AI 任务熔断 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | Seedance 连续失败 5 次 |
| 预期 | 熔断器 CLOSED → OPEN，新任务快速拒绝返回 `code=51000` |

### TC-AI-009: AI 任务熔断恢复 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 熔断 30s 后 |
| 预期 | OPEN → HALF_OPEN，试探请求成功则 CLOSED，失败则重新 OPEN |

### TC-AI-010: AI 任务限流 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | QPS > 5 |
| 预期 | 等待 5 秒后仍无法获取 token，返回 `code=42900` |

### TC-AI-011: Seedance API 调用参数正确性 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 验证请求体 |
| 预期 | `req_key` 正确（图片=high_aes_general_v20_L，视频=seedance_1_0_pro_i2v），`reference_image_urls` ≤ 3 张，`duration`/`resolution`/`watermark` 默认值正确 |

### TC-AI-012: 任务状态机完整流转 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 验证所有 8 条状态流转 |
| 预期 | 0→1（提交成功）、0→3（提交失败）、1→2（完成）、1→3（失败/超时）、3→0（重试）、3→终态（超限） |

### TC-AI-013: 终态任务不再轮询 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | ai_task status >= 2 |
| 预期 | `next_poll_time` 为 null，轮询任务跳过 |

---

## 8. Prompt 工程测试

### TC-PROMPT-001: Prompt 生成正常流程 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | LLM 返回合法 JSON |
| 预期 | `prompt`（中文）和 `prompt_en`（英文）正确生成，`shotNumber` 连续，`durationEstimate` 在 3-8 秒范围内 |

### TC-PROMPT-002: Prompt 超长截断 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | `prompt_en` 超过 500 字符 |
| 预期 | 截断并添加 "..." |

### TC-PROMPT-003: 空 Prompt 处理 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | `prompt_en` 为空或全空白 |
| 预期 | shot status=4（打回），记录 error_msg |

### TC-PROMPT-004: 非英文 Prompt 重试 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | LLM 返回非英文 prompt_en |
| 预期 | 重试 LLM 生成 |

### TC-PROMPT-005: LLM 返回非 JSON ✅ P1

| 项 | 值 |
|---|---|
| 场景 | LLM 返回纯文本而非 JSON |
| 预期 | 解析失败，触发重试或记录错误 |

### TC-PROMPT-006: 风格预设注入验证 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 系统 Prompt 中包含项目 style_preset |
| 预期 | 生成结果包含风格描述 |

### TC-PROMPT-007: LLM 超时处理 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | LLM 响应超时（>30s） |
| 预期 | 抛出超时异常，任务标记失败 |

---

## 9. 一致性保障测试

### TC-CONSIST-001: 正常一致性（Level 1） ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 分镜绑定了角色+场景参考图 |
| 预期 | Seedance 调用时全量传入参考图 + 角色描述 |

### TC-CONSIST-002: 部分降级（Level 2） ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 只有角色参考图，无场景参考图 |
| 预期 | 加强 prompt 中的场景描述，日志记录降级 |

### TC-CONSIST-003: 严重降级（Level 3） ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 无参考图 |
| 预期 | 纯 prompt 驱动，warning 日志，降低一致性保证 |

### TC-CONSIST-004: AssetRefResolver 参考图解析 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 分镜关联多个资产 |
| 预期 | 按 character > scene > prop 优先级排序，最多 4 张截断，空 reference_images 的资产跳过 |

### TC-CONSIST-005: 首帧图校验 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | video_gen 时 shot.generatedImageUrl 为空 |
| 预期 | 返回 `code=51002` "分镜首帧图不存在，无法生成视频" |

### TC-CONSIST-006: extraRefs 去重与上限 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | extraRefs 包含 firstFrameUrl 或超过 3 张 |
| 预期 | 去重过滤，上限截断为 3 张 |

---

## 10. TOS 存储测试

### TC-TOS-001: 获取预签名上传 URL（需鉴权） ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/tos/presign` |
| 输入 | `{ "filename": "test.jpg", "content_type": "image/jpeg" }` |
| 前置 | 需登录（sa-token 鉴权） |
| 预期 | 返回预签名 URL |

### TC-TOS-002: 前端直传 TOS ✅ P0

| 项 | 值 |
|---|---|
| 操作 | 前端使用预签名 URL PUT 上传文件 |
| 预期 | 文件上传成功，可通过 URL 访问 |

### TC-TOS-003: 后端上传到 TOS ✅ P0

| 项 | 值 |
|---|---|
| 场景 | AI 生成结果下载到后端后上传 TOS |
| 预期 | TosService 上传成功，返回 TOS URL |

### TC-TOS-004: 上传完成通知 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `POST /api/tos/complete` |
| 输入 | `{ "tos_key": "projects/1/output/test.jpg" }` |
| 预期 | HEAD 校验文件存在，更新 DB 记录，返回 `code=0` |

### TC-TOS-005: 预签名 URL 过期 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 预签名 URL 过期后尝试上传 |
| 预期 | TOS 返回签名过期错误 |

---

## 11. 安全与鉴权测试

### TC-SEC-001: 未登录访问受保护接口 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 不带 token 调用 `/api/project/list` |
| 预期 | 返回 `code=40100` |

### TC-SEC-002: Token 过期 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 使用过期 token 访问接口 |
| 预期 | 返回 `code=40101` |

### TC-SEC-003: 公开接口无需登录 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `/api/user/login`、`/api/webhook/seedance`（Phase 2） |
| 预期 | 不携带 token 也能正常访问 |

### TC-SEC-004: 需鉴权接口必须登录 ✅ P0

| 项 | 值 |
|---|---|
| 接口 | `/api/tos/presign`（v1.2 变更为需鉴权） |
| 预期 | 不携带 token 返回 `code=40100` |

### TC-SEC-005: API Key 不暴露 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 检查前端网络请求、后端响应体 |
| 预期 | 找不到任何 API Key |

### TC-SEC-006: API Key 从环境变量读取 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 检查代码中是否有硬编码 API Key |
| 预期 | 所有 Key 从 `.env` / 环境变量读取 |

### TC-SEC-007: Swagger 环境控制 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | Prod 环境访问 `/swagger-ui.html` |
| 预期 | Swagger UI 不可访问 |

### TC-SEC-008: Actuator 安全 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 访问 `/actuator/env` |
| 预期 | 仅暴露 health/info，env 值隐藏 |

---

## 12. 并发与异常测试

### TC-CONC-001: 流程执行并发锁 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 同一项目同时触发两次 workflow start |
| 预期 | Redis 分布式锁拦截，只有一个执行 |

### TC-CONC-002: 纯轮询并发更新 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 两个轮询线程同时更新同一个 ai_task（MVP 纯轮询场景） |
| 预期 | 分布式锁保护，不会并发覆盖，终态检查幂等 |

### TC-CONC-003: 线程池满载 + 拒绝策略 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 提交超过 `aiTaskExecutor` 容量的任务 |
| 预期 | AbortPolicy 拒绝，返回 `code=42900`，不阻塞不崩溃 |

### TC-CONC-004: 服务重启后任务恢复 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 流程执行中（`execution_lock=1`）服务重启 |
| 预期 | 重启后扫描到 RUNNING 任务，自动恢复 |

### TC-CONC-005: 数据库连接池耗尽 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 大量并发请求 |
| 预期 | 连接池排队，超时返回错误 |

### TC-EXC-001: Seedance API 超时 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | Seedance API 响应超时 |
| 预期 | 捕获异常，标记失败或进入重试 |

### TC-EXC-002: TOS 上传失败 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | AI 结果上传到 TOS 时网络异常 |
| 预期 | 重试机制生效 |

### TC-EXC-003: 数据库异常 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | MySQL 连接断开 |
| 预期 | 全局异常处理器返回友好错误，不暴露堆栈 |

### TC-EXC-004: 指数退避轮询 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 验证 poll_count 递增时 next_poll_time 延迟 |
| 预期 | poll_count 0-2: 3s，3-9: 10s，10-29: 30s，>=30: 超时失败 |

### TC-EXC-005: FFmpeg 导出正常 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 合并已通过分镜的视频 |
| 预期 | FFmpeg 执行成功，输出最终视频 URL |

### TC-EXC-006: FFmpeg 无通过分镜 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 执行导出但无 status=3 的分镜 |
| 预期 | 返回 `code=51002` |

### TC-EXC-007: FFmpeg 超时强杀 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | FFmpeg 执行超过 600s |
| 预期 | 强杀进程，标记失败 |

---

## 13. 前端集成测试

### TC-FE-001: 登录页面 ✅ P0

| 项 | 值 |
|---|---|
| 操作 | 输入正确用户名密码 → 点击登录 |
| 预期 | 跳转到 ProjectList，token 写入 localStorage + Pinia authStore |

### TC-FE-002: 登录表单校验 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 空用户名/密码 < 6 字符 |
| 预期 | 高亮对应输入框，阻止提交 |

### TC-FE-003: 已登录访问登录页 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 已登录用户访问 `/login` |
| 预期 | 自动跳转 `/projects` |

### TC-FE-004: 项目列表页 ✅ P0

| 项 | 值 |
|---|---|
| 操作 | 进入项目列表页 |
| 预期 | 卡片视图默认展示，分页正常，搜索功能生效，executionLock=1 时显示 🔒 |

### TC-FE-005: 资产管理页 ✅ P0

| 项 | 值 |
|---|---|
| 操作 | 进入资产管理 → 按类型 Tab 切换 → 上传参考图 |
| 预期 | Tab 分组正常（角色/场景/物品/声音），参考图拖拽排序正常，卡片展示主图+名称+状态 |

### TC-FE-006: 流程编辑器 ✅ P0

| 项 | 值 |
|---|---|
| 操作 | 拖拽步骤排序 → 开关审核选项 → 保存 |
| 预期 | SortableJS 拖拽正常（handle 拖拽 + animation），6 种步骤类型展示正确，保存时含 version 乐观锁 |

### TC-FE-007: 分镜工作台（分页模式） ✅ P0

| 项 | 值 |
|---|---|
| 操作 | 查看分镜列表 → 切换分场/全部 → 分页浏览 |
| 预期 | 左右分栏布局正常，el-pagination 交互正常（page-size=20），切换过滤条件时重置到第 1 页 |

### TC-FE-008: 前端任务轮询（三档退避） ✅ P0

| 项 | 值 |
|---|---|
| 操作 | 触发 AI 生成后等待 |
| 预期 | 轮询间隔 3s→10s→30s 三档退避，超时保护（maxPolls=2400），onDone 回调刷新分镜数据，组件卸载自动停止 |

### TC-FE-009: 工作流轮询（useWorkflowPolling） ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 开始执行流程后 |
| 预期 | 启动轮询，pollingStatus='polling'，executionLock=0 时停止，超时 Toast 提示 |

### TC-FE-010: 流程审核弹窗 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 执行到审核节点 |
| 预期 | 弹出通知，展示通过/打回按钮，操作后进度条更新 |

### TC-FE-011: 路由守卫：未登录跳转 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 未登录访问 `/projects` |
| 预期 | 跳转 `/login?redirect=/projects` |

### TC-FE-012: 路由守卫：redirect 回跳 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 登录后 |
| 预期 | 跳回 redirect 指定路径 |

### TC-FE-013: Pinia auth store 持久化与恢复 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 页面刷新后 |
| 预期 | 从 localStorage 读取 token 恢复登录态，clearAuth 清理 token + userInfo + localStorage |

### TC-FE-014: Axios 401 拦截处理 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 后端返回 401 |
| 预期 | authStore.clearAuth() → ElMessage.error('登录已过期') → 跳转登录页 |

### TC-FE-015: Axios 403 拦截处理 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 后端返回 403 |
| 预期 | Toast 提示，不跳转 |

### TC-FE-016: Axios 业务错误码映射 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 后端返回不同业务错误码（40001~51101） |
| 预期 | ERROR_ACTION 映射正确处理（toast/form/redirect-login/retry-presign/disabled-btn） |

### TC-FE-017: TOS 直传：正常流程 ✅ P0

| 项 | 值 |
|---|---|
| 场景 | 选择文件 → presign → PUT 直传 → complete |
| 预期 | 完整链路成功，后端 DB 记录更新 |

### TC-FE-018: TOS 直传：文件校验 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 文件大小 > 50MB / 不支持类型 |
| 预期 | 前端拦截，阻止上传 |

### TC-FE-019: TOS 直传：预签名过期重试 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | presign URL 过期返回 40005 |
| 预期 | useTosUpload 自动重新获取 presign URL 并重试 |

### TC-FE-020: TOS 直传：上传进度展示 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | PUT 上传中 |
| 预期 | progress ref 实时更新，loading 状态阻断重复提交 |

### TC-FE-021: SortableJS 拖拽边界 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 拖拽到首位/末位/执行中禁止拖拽/保存失败回滚 |
| 预期 | 各边界场景处理正确 |

### TC-FE-022: 批量操作边界 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 未选择分镜/部分失败/打回缺 comment |
| 预期 | 对应拦截和提示 |

### TC-FE-023: 资产引用保护删除 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 删除被分镜引用的资产 |
| 预期 | 后端返回 40901，Toast 提示"资产已被分镜引用，不可删除" |

### TC-FE-024: 排队状态展示 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | currentAiTask.status=0/1 |
| 预期 | 遮罩文案正确（"AI模型排队中"/"🖼️ 图片生成中"/"🎬 视频生成中"） |

### TC-FE-025: 表单校验：新建项目/资产 ✅ P1

| 项 | 值 |
|---|---|
| 场景 | 缺少必填字段 |
| 预期 | 阻止提交，高亮对应输入框 |

### TC-FE-026: 暗色主题渲染 ✅ P2

| 项 | 值 |
|---|---|
| 场景 | 页面加载 |
| 预期 | Element Plus dark theme 生效，背景 #141414，卡片 #1d1e1f |

---

## 14. 端到端全流程测试

### TC-E2E-001: 小说→视频完整流程 ✅ P0

| 项 | 值 |
|---|---|
| 步骤 | 1.登录 → 2.创建项目 → 3.上传小说 → 4.拆分章节 → 5.提取/确认资产 → 6.配置流程 → 7.开始执行 → 8.审核分镜 → 9.AI生图 → 10.AI生视频 → 11.合并导出 |
| 预期 | 全流程跑通，最终输出完整视频 URL |

### TC-E2E-002: 流程中断恢复 ✅ P0

| 项 | 值 |
|---|---|
| 步骤 | 执行到中间步骤 → 停止/重启 → 恢复 |
| 预期 | 从断点继续，不重复已完成步骤 |

### TC-E2E-003: 分镜打回重做 ✅ P0

| 项 | 值 |
|---|---|
| 步骤 | 生图完成 → 审核打回 → 重新生成 → 审核通过 |
| 预期 | 打回意见记录，重新生成成功，版本递增 |

### TC-E2E-004: 预算控制 ✅ P1

| 项 | 值 |
|---|---|
| 步骤 | 设置预算上限 → 触发大量 AI 调用 → 达到上限 |
| 预期 | 超出预算后阻止新 AI 调用 |

---

## 附录 A. Phase 2 测试计划

以下用例在 MVP 阶段**不可执行**（Webhook 为 Phase 2 预留功能），移至 Phase 2 测试计划：

### Phase 2: Webhook 回调测试

| 编号 | 用例名称 | 优先级 |
|------|---------|--------|
| TC-WH-001 | Webhook 回调正常处理 | P0 |
| TC-WH-002 | Webhook 签名校验通过 | P0 |
| TC-WH-003 | Webhook 签名校验失败 | P0 |
| TC-WH-004 | Webhook 重放攻击防御（5 分钟时间窗口） | P0 |
| TC-WH-005 | Webhook 回调异步处理（@Async） | P1 |
| TC-WH-006 | Webhook 异常 payload 处理 | P1 |
| TC-WH-007 | 防重放时间窗口边界值 | P0 |
| TC-WH-008 | Webhook IP 白名单校验 | P1 |
| TC-WH-009 | Webhook 与轮询并发冲突 | P0 |

### Phase 2: FFmpeg 转场效果详细测试

| 编号 | 用例名称 | 优先级 |
|------|---------|--------|
| TC-FF-001 | 带转场效果合并（fade 等） | P1 |
| TC-FF-002 | 背景音乐叠加 | P1 |
| TC-FF-003 | 片头/片尾黑场 | P2 |

---

## 测试用例汇总

| 模块 | P0 数量 | P1 数量 | P2 数量 | 总计 |
|------|---------|---------|---------|------|
| 用户模块 | 3 | 3 | 0 | 6 |
| 项目管理 | 3 | 3 | 0 | 6 |
| 资产管理 | 3 | 3 | 0 | 6 |
| 内容管理 | 6 | 3 | 0 | 9 |
| 流程引擎 | 6 | 5 | 0 | 11 |
| AI 任务 | 8 | 4 | 0 | 12 |
| Prompt 工程 | 3 | 4 | 0 | 7 |
| 一致性保障 | 3 | 3 | 0 | 6 |
| TOS 存储 | 4 | 1 | 0 | 5 |
| 安全与鉴权 | 6 | 2 | 0 | 8 |
| 并发与异常 | 3 | 6 | 0 | 9 |
| 前端集成 | 11 | 12 | 1 | 24 |
| 端到端流程 | 3 | 1 | 0 | 4 |
| Phase 2（预留） | 6 | 4 | 1 | 11 |
| **合计** | **65** | **54** | **2** | **121** (+11 Phase 2) |

---

## 附录 B. 状态码映射

| 错误码 | 含义 |
|--------|------|
| 40000 | 参数校验错误 |
| 40100 | 未登录 |
| 40101 | Token 过期 |
| 40300 | 无操作权限 |
| 40301 | 无权访问 |
| 40400 | 资源不存在 |
| 40900 | 项目正在执行中 |
| 40901 | 资产被引用不可删 |
| 40902 | 重复操作/数据冲突 |
| 40903 | 数据已被修改（乐观锁） |
| 42900 | 请求频率过高 |
| 51000 | AI 服务异常 |
| 51002 | AI 参数错误（缺少首帧图等） |
| 51003 | Webhook 签名无效（Phase 2） |
