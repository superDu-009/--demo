# AI 模块测试用例评审报告

## 文档信息

| 项目 | 内容 |
|------|------|
| 评审文档 | `TESTCASES_PM_小赫_v1.0.md` |
| 对照基线 | `DESIGN_AI_阿典_v1.0.md` (v1.2 代码注释规范+评审修订版) |
| 评审人 | 阿典（AI 工程师） |
| 评审日期 | 2026-04-20 |
| 评审结论 | **⚠️ 有条件通过** |

---

## 一、评审结论摘要

测试用例文档整体结构完整，覆盖了核心业务流程。但 **AI 模块相关用例存在较多遗漏**，尤其在 Prompt 工程、一致性保障、签名算法细节验证、轮询策略、熔断限流、FFmpeg 导出等方面缺少可执行的测试场景。

**建议修复后重新评审通过。**

---

## 二、逐条对照评审

### 2.1 AI 任务测试（第 7 章：TC-AI-001 ~ TC-AI-007）

| 用例 | 状态 | 评审意见 |
|------|------|---------|
| TC-AI-001 触发图片生成 | ✅ 基本覆盖 | 需补充：Seedance 提交失败时的状态流转（0→3），provider_task_id 记录验证 |
| TC-AI-002 触发视频生成 | ✅ 基本覆盖 | 需补充：无首帧图时的异常处理（应抛 51002），reference_image_urls 上限 3 张验证 |
| TC-AI-003 查询 AI 任务状态 | ✅ 覆盖 | 建议补充：终态任务不再轮询的验证（next_poll_time 为 null） |
| TC-AI-004 AI 任务失败处理 | ⚠️ 不完整 | 缺少：失败后 generation_attempts++ 验证、重试 3 次上限验证、失败后状态机流转验证 |
| TC-AI-005 AI 结果上传 TOS | ✅ 覆盖 | 建议补充：TOS 路径格式验证（projects/{id}/output/...） |
| TC-AI-006 AI 任务熔断/限流 | ⚠️ 不完整 | 缺少：具体阈值验证（连续 5 次失败触发熔断、QPS=5 限流）、熔断后 30 秒半开恢复验证 |
| TC-AI-007 Seedance API 调用参数正确性 | ⚠️ 不完整 | 缺少：req_key 值验证（图片=high_aes_general_v20_L，视频=seedance_1_0_pro_i2v）、duration/resolution/watermark 默认值验证 |

### 2.2 Webhook 回调测试（第 8 章：TC-WH-001 ~ TC-WH-006）

| 用例 | 状态 | 评审意见 |
|------|------|---------|
| TC-WH-001 Webhook 正常处理 | ✅ 覆盖 | 需补充：回调 payload 字段完整性验证（taskId/status/resultUrl/costTokens/callbackTime） |
| TC-WH-002 签名校验通过 | ✅ 覆盖 | 需补充：HMAC-SHA256 签名构造方式的具体验证（StringToSign 格式） |
| TC-WH-003 签名校验失败 | ✅ 覆盖 | 错误码 51003 验证正确 ✅ |
| TC-WH-004 重放攻击防御 | ⚠️ 不完整 | **缺少关键场景**：仅提到"短时间内重复发送"，未覆盖设计文档中的 **5 分钟时间窗口** 验证。需补充：时间戳过期（>5min）、时间戳超前、空时间戳、时间戳格式非法等场景 |
| TC-WH-005 回调异步处理 | ✅ 覆盖 | 需补充：@Async 线程池名称验证（"aiTaskExecutor"），非默认线程池 |
| TC-WH-006 Webhook 与轮询并发 | ✅ 覆盖 | 需补充：具体并发时序场景测试方案 |

---

## 三、遗漏的 AI 关键场景

### 🔴 P0 级别（必须补充）

#### GAP-001: 防重放时间窗口验证
**对应设计：** DESIGN_AI 第 2.6 节 `isTimestampValid()` — 5 分钟窗口（300 秒）
**遗漏场景：**
- 时间戳在窗口内（now - ts = 200s）→ 应通过
- 时间戳超出窗口（now - ts = 400s）→ 应拒绝
- 时间戳超前（ts > now + 300s）→ 应拒绝
- 时间戳为空 → 应通过（降级不拦截）
- 时间戳格式非法（非数字字符串）→ 应拒绝

#### GAP-002: Prompt 安全与长度控制
**对应设计：** DESIGN_AI 第 3.4 节
**遗漏场景：**
- prompt_en 超过 500 字符 → 应截断并添加 "..."
- prompt_en 为空或全空白 → 应标记 shot status=4（打回），记录 error_msg
- prompt_en 非英文 → 应重试 LLM 生成
- 敏感词（暴力/色情/政治）→ 应调用内容安全 API 检测

#### GAP-003: 任务状态机完整流转
**对应设计：** DESIGN_AI 第 6.5 节状态机（8 条流转规则）
**遗漏场景：**
- 0→1：成功提交到 Seedance，记录 provider_task_id，设置 next_poll_time
- 0→3：提交失败，记录 error_msg
- 1→2：Webhook/轮询返回 done，下载结果，上传 TOS
- 1→3：Webhook/轮询返回 failed
- 1→3：poll_count > 30 超时标记
- 3→0：generation_attempts < 3 时重试
- 3→终态：generation_attempts >= 3 不再重试

#### GAP-004: Webhook 异常 payload 处理
**对应设计：** DESIGN_AI 第 2.7 节 WebhookController
**遗漏场景：**
- 回调 body 不是合法 JSON → 应 graceful 处理
- 回调缺少 taskId 字段 → 应记录错误日志
- 回调 taskId 在本地找不到对应 ai_task → 应记录 error 并返回（不应崩溃）

### 🟠 P1 级别（建议补充）

#### GAP-005: 指数退避轮询策略
**对应设计：** DESIGN_AI 第 6.4 节 `PollBackoffStrategy`
**遗漏场景：**
- poll_count 0-2：延迟 3s
- poll_count 3-9：延迟 10s
- poll_count 10-29：延迟 30s
- poll_count >= 30：标记超时失败
- next_poll_time 计算正确性验证

#### GAP-006: 一致性降级策略
**对应设计：** DESIGN_AI 第 4.3 节（4 个 Level）
**遗漏场景：**
- Level 1（正常）：有角色+场景参考图 → 全量传入
- Level 2（部分降级）：只有角色参考图 → 加强 prompt 场景描述
- Level 3（严重降级）：无参考图 → 纯 prompt 驱动 + warning 日志
- Level 4（生成失败）：Seedance 结果质量不达标 → 打回 + generation_attempts++

#### GAP-007: 熔断器状态流转
**对应设计：** DESIGN_AI 第 8 节 CircuitBreaker
**遗漏场景：**
- CLOSED → OPEN：连续 5 次失败触发
- OPEN → HALF_OPEN：30 秒后自动切换
- HALF_OPEN → CLOSED：试探请求成功
- HALF_OPEN → OPEN：试探请求失败
- 熔断开启时提交任务应返回 51000 错误码

#### GAP-008: 限流器行为验证
**对应设计：** DESIGN_AI 第 8 节 RateLimiter（QPS=5）
**遗漏场景：**
- QPS ≤ 5：正常通过
- QPS > 5：等待 5 秒后仍无法获取 → 抛 42900 错误码
- 限流与熔断的组合场景

#### GAP-009: 服务重启后任务恢复（RecoveryRunner）
**对应设计：** DESIGN_AI 第 6.3 节 RecoveryRunner
**遗漏场景：**
- 服务启动时扫描 status=0/1 的 pending 任务
- 查询 Seedance 发现任务已 done → 走下载+上传流程
- 查询发现任务 running/submitted → 重置 next_poll_time 继续轮询
- 查询发现任务 failed → 更新状态
- 查询 Seedance 本身失败 → 标记需人工检查

#### GAP-010: API 调用日志记录完整性
**对应设计：** DESIGN_AI 第 7.2 节 AOP 切面
**遗漏场景：**
- Seedance 成功调用 → 记录 status=1，包含 cost_tokens
- Seedance 失败调用 → 记录 status=0（@AfterThrowing 切面）
- LLM 调用 → 记录 tokenUsage（估算）和 cost
- 请求参数脱敏验证（API Key 不暴露）

#### GAP-011: FFmpeg 导出测试
**对应设计：** DESIGN_AI 第 5 节
**遗漏场景：**
- 无已通过分镜 → 抛 51002 错误
- 正常合并（无转场）
- 带转场效果合并（fade 等）
- 背景音乐叠加
- 片头/片尾黑场
- FFmpeg 超时（>600s）→ 强杀进程
- FFmpeg 执行失败（exit code != 0）

#### GAP-012: 视频首帧图校验
**对应设计：** DESIGN_AI 第 4.2 节 `buildVideoGenParams`
**遗漏场景：**
- shot.generatedImageUrl 为空 → 抛 51002 "分镜首帧图不存在，无法生成视频"
- extraRefs 上限 3 张验证
- 去重过滤（extraRefs 不包含 firstFrameUrl）

#### GAP-013: PromptEngine LLM 调用
**对应设计：** DESIGN_AI 第 3.1 节
**遗漏场景：**
- LLM 返回非 JSON → 应解析失败并处理
- LLM 超时（>30s）→ 应超时异常
- 系统 Prompt 中风格预设注入验证
- 用户 Prompt 中资产描述注入验证
- shotNumber 连续性验证
- durationEstimate 范围验证（3-8 秒）

#### GAP-014: AssetRefResolver 参考图解析
**对应设计：** DESIGN_AI 第 4.1 节
**遗漏场景：**
- 参考图按优先级排序验证（character > scene > prop）
- 最多 4 张参考图截断验证
- 空 reference_images 数组的资产处理
- 批量预加载（N+1 优化）

#### GAP-015: 前端轮询与后端轮询对齐
**对应设计：** DESIGN_AI 第 6.4 节（与前端三档退避对齐）
**遗漏场景：**
- 前端 3s/10s/30s 三档退避策略
- 前端轮询与后端慢轮询不会重复更新同一任务

#### GAP-016: Webhook IP 白名单
**对应设计：** DESIGN_AI 第 2.7 节"白名单备注"
**遗漏场景：**
- 非火山引擎 IP 请求 /api/webhook/seedance → 应拒绝
- 白名单校验在签名校验之前执行

---

## 四、现有用例的问题修正建议

| 用例编号 | 问题 | 建议 |
|---------|------|------|
| TC-AI-004 | "Seedance API 返回错误" 场景太模糊 | 拆分为多个子用例：超时/限流(42900)/参数错误/熔断(51000) |
| TC-AI-006 | "短时间内大量请求" 缺乏量化标准 | 明确阈值：QPS>5 触发限流，连续 5 次失败触发熔断 |
| TC-WH-004 | 重放防御仅提"短时间重复" | 明确 5 分钟时间窗口，补充时间戳边界值测试 |
| TC-CONC-004 | "重启后扫描 RUNNING 任务" 描述不准确 | 应扫描 status=0(提交中) 和 status=1(处理中) 且 next_poll_time 过期的任务 |

---

## 五、测试用例统计

| 模块 | 现有用例数 | 建议补充数 | 补充后总数 |
|------|-----------|-----------|-----------|
| AI 任务（第 7 章） | 7 | 8 | 15 |
| Webhook 回调（第 8 章） | 6 | 4 | 10 |
| Prompt 工程（新增） | 0 | 5 | 5 |
| 一致性保障（新增） | 0 | 3 | 3 |
| 熔断限流（新增） | 0 | 3 | 3 |
| FFmpeg 导出（新增） | 0 | 5 | 5 |
| 恢复机制（新增） | 0 | 3 | 3 |
| **合计** | **13** | **31** | **44** |

---

## 六、评审结论

### ⚠️ 有条件通过

**通过条件：**
1. 补充 GAP-001（防重放时间窗口）的 5 个边界测试用例
2. 补充 GAP-002（Prompt 安全与长度）的 4 个测试用例
3. 补充 GAP-003（任务状态机）的状态流转验证
4. 补充 GAP-004（Webhook 异常 payload）的 3 个异常场景
5. 修正 TC-AI-004、TC-AI-006、TC-WH-004 的模糊描述

**可延后到 Phase 2 的场景（不阻塞 MVP）：**
- GAP-011 FFmpeg 转场效果详细测试
- GAP-014 批量预加载 N+1 优化验证
- GAP-015 前端轮询策略对齐
- GAP-016 IP 白名单（依赖运维配置）

---

*评审人：阿典 | 日期：2026-04-20 | 版本：v1.0*
