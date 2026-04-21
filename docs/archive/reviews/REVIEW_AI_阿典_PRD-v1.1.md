# AI漫剧生产平台 — PRD 技术评审报告

**评审人：阿典（资深AI工程师）**
**评审日期：2026-04-19**
**PRD版本：v1.0 MVP**

---

## 总评

PRD 整体思路清晰，功能清单完整，流程设计合理。但在以下方面需要重点加强：
1. AI 异步任务处理机制不够具体
2. 角色一致性保障手段偏薄弱
3. API Key 管理存在安全隐患
4. 缺少成本控制的防御性设计

---

## 1. Seedance 2.0 能力边界与 Java 调用注意事项

### 能力边界

| 维度 | 限制 |
|------|------|
| 分辨率 | 通常最高 768p/1080p，超出会降采样或报错 |
| 时长 | 单次 5 秒，部分版本支持 10 秒。长视频需分段拼接 |
| 帧率 | 固定 24fps，不可自定义 |
| 并发 | QPS 限制，同时运行任务数 3-5 个 |
| 参考图 | reference_images 数量上限（通常 3-5 张） |
| 生成时间 | 30-120 秒/次 |

### Java 调用注意事项

**异步模式（必须）**：视频生成需要 30-120 秒，绝不能同步阻塞 HTTP 调用。
推荐方案：WebClient (Spring WebFlux) 或 OkHttp + CompletableFuture。

**重试策略**：

```
HTTP 429 (限流)     → 指数退避重试: 10s → 20s → 40s
HTTP 5xx (服务端)   → 最多重试 3 次，间隔 5s
业务错误 (生成失败) → 不自动重试，标记失败由用户手动处理
```

**超时设置**：
- 提交请求：连接超时 10s，读取超时 30s
- 轮询查询：每个查询超时 15s

**结果下载**：视频文件较大（5-30MB），必须使用流式下载，不要一次性加载到内存。

---

## 2. 角色/场景一致性技术方案评审

### 当前方案评价：及格线，但不够

PRD 的"资产强绑定 + reference_images + 风格锁定"是基础做法，实际效果往往不够稳定。AI 生成的一致性是业界公认难题。

### 建议增强手段

**【1】分镜级「角色多角度定位图」**
- 每个角色至少 3 个角度参考图（正面/侧面/背面）
- prompt 中明确具体特征："黑色长发、圆眼镜、白衬衫" 而非 "张三"

**【2】首帧图一致性校验（Phase 2）**
- 将生成的首帧图与资产参考图做 CLIP embedding 相似度比对
- 相似度低于阈值时标记警告，人工审核介入
- MVP 可先用纯人工审核兜底

**【3】分镜间「连续帧」策略**
- 同一分场相邻分镜，使用前一分镜末帧作为下一分镜的参考图
- 减少场景跳跃感

**【4】提示词模板化**
- 使用结构化模板而非自由文本：
  `[风格] + [场景描述] + [角色A动作] + [角色B动作] + [镜头运动] + [光线氛围]`
- 确保每次生成提示词结构一致

**【5】数据库字段建议增加**

```
asset 表增加：
- face_embedding (TEXT): 人脸特征向量，用于一致性校验
- appearance_keywords (JSON): 外观关键词数组，自动注入 prompt

shot 表增加：
- prompt_en (TEXT): 英文生成提示词（Seedance 对英文理解更好）
- consistency_score (DECIMAL): 一致性校验得分
```

---

## 3. 分镜提示词生成（shot_gen）模型选择

### 推荐方案

**不要用一个模型干所有活，建议分层：**

| Layer | 任务 | 推荐模型 | 原因 |
|-------|------|----------|------|
| L1 | 章节拆分 | DeepSeek V3 | 轻量便宜，中文够用 |
| L2 | 资产提取 | 通义千问 Max / GPT-4o-mini | 中等成本，实体识别准 |
| L3 | 分镜提示词 | GPT-4o / Claude Sonnet | 提示词质量直接影响视频质量，值得花钱 |

### 提示词工程最佳实践

**系统提示词模板示例：**

```
你是专业影视分镜师。请根据以下分场内容生成 3-5 个分镜。

【风格约束】
{style_preset}

【已有资产】
角色：{角色列表，含名称+外观描述}
场景：{场景列表，含名称+描述}

【分场内容】
{scene_content}

【输出要求 - JSON 格式】
每个分镜包含：
- camera_angle: 镜头角度（特写/中景/全景/俯拍/仰拍）
- description: 画面描述（中文，50字以内）
- characters: 出现角色列表
- action: 角色动作描述
- mood: 氛围/光线
- prompt_en: 英文生成提示词（用于AI生图）
```

**关键原则：**
1. 英文 prompt 效果远好于中文，在 shot_gen 阶段就生成英文 prompt
2. Few-shot 示例：给 1-2 个高质量分镜示例，大幅提升输出稳定性
3. temperature=0.3-0.5，降低随机性，保证输出格式稳定
4. **重要：shot_gen 的输出应该增加人工审核环节**，确认 prompt 质量后再调用生图

---

## 4. 视频合并导出（export）技术方案

### 推荐方案：FFmpeg + 进程管理

**Java 集成方式（三种）：**

```
方式1：ProcessBuilder 直接调用（推荐 MVP）
  - 简单可靠，零依赖
  - 示例：
    ProcessBuilder pb = new ProcessBuilder(
      "ffmpeg", "-y",
      "-f", "concat", "-safe", "0",
      "-i", "file_list.txt",
      "-c:v", "libx264", "-preset", "medium",
      "-crf", "23", "output.mp4"
    );

方式2：Jaffree 库（Java FFmpeg wrapper）
  - Maven: com.github.kokorin.jaffree
  - 类型安全，但学习成本略高

方式3：Spring Batch + FFmpeg
  - 适合大批量处理，MVP 不需要
```

**导出流程：**
1. 将分镜按 episode → scene → shot 排序
2. 从 TOS 下载所有视频片段到临时目录
3. 生成 FFmpeg concat 文件列表
4. 执行 FFmpeg 合并
5. 上传合并后的视频到 TOS
6. 清理临时文件

**必须考虑的细节：**
- 视频分辨率不一致时需统一（加 scale filter）
- 音频轨道处理（有配音 vs 无配音）
- 合并失败时回滚机制
- FFmpeg 需要在服务器预装，Docker 推荐 jrottenberg/ffmpeg 基础镜像

**数据库建议增加导出任务表：**

```
export_task 表：
- id, project_id, status
- input_shot_ids (JSON)
- output_url, ffmpeg_log
- create_time, finish_time
```

---

## 5. AI 异步处理流程与重试机制

### 当前方案问题

1. "用户手动刷新查看进度" 体验差
2. 没有明确的任务队列设计
3. 多分镜同时生成时并发控制缺失
4. 失败后重试策略不明确

### 推荐架构

```
用户提交生成请求
     │
     ▼
┌─────────────┐
│  Redis 队列  │  ← 限流器控制并发数（同时最多 3 个任务）
│  (TaskQueue) │
└──────┬──────┘
       │
┌──────▼──────┐     ┌──────────────┐
│  @Async 消费 │────→│  Seedance API │
│  线程池      │     │  提交+轮询    │
└──────┬──────┘     └──────────────┘
       │
┌──────▼──────┐
│  更新 DB 状态│
│  + TOS 存储 │
└──────┬──────┘
       │
┌──────▼──────┐
│  SSE 推送    │  ← 主动推送进度到前端
│  进度通知    │
└─────────────┘
```

### 具体实现

**线程池配置：**
```
核心线程数: 3（对应 API 并发限制）
最大线程数: 5
队列容量: 100（超出则拒绝，返回友好提示）
```

**失败重试策略：**
```
HTTP 层失败（超时/5xx）：自动重试最多 3 次，指数退避
业务层失败（生成质量差）：不自动重试，标记"生成失败"
                         用户在分镜工作台手动点击"重新生成"
连续失败 N 次（如 3 次）：升级告警，暂停整个流程
```

**进度推送（三选一）：**

| 方案 | 复杂度 | 推荐阶段 |
|------|--------|----------|
| 前端轮询 (每3s) | 低 | MVP |
| Server-Sent Events (SSE) | 中 | Phase 1 |
| WebSocket | 高 | Phase 2 |

MVP 建议先用轮询，后续升级为 SSE。

**任务状态机：**
```
PENDING → QUEUED → RUNNING → [SUCCESS | FAILED | CANCELLED]
FAILED 需区分：API_ERROR / TIMEOUT / QUALITY_REJECTED
```

---

## 6. API Key 管理安全性

### 当前方案风险

PRD 将 API Key 存在项目 YAML 配置文件中：
1. YAML 文件若误提交 Git，Key 泄露
2. 文件权限管理不当，其他进程可读取
3. 没有 Key 轮换机制

### 推荐方案（按阶段演进）

**阶段1 — MVP（环境变量方案，推荐直接使用）：**

```yaml
# application.yml
seedance:
  api-key: ${SEEDANCE_API_KEY}
  api-url: https://api.seedance.com/v1

volcengine:
  tos:
    access-key: ${TOS_ACCESS_KEY}
    secret-key: ${TOS_SECRET_KEY}
```

```bash
# 启动脚本或 .env 文件（不提交 Git）
export SEEDANCE_API_KEY="sk-xxxxx"
export TOS_ACCESS_KEY="xxxxx"
export TOS_SECRET_KEY="xxxxx"
```

**安全 Checklist：**
- [ ] application.yml 中敏感值全部改为环境变量引用
- [ ] .gitignore 包含所有 secrets 相关文件
- [ ] 日志中禁止打印 API Key（配置 logback 过滤器）
- [ ] API 调用日志中脱敏处理（只存 key 后 4 位）
- [ ] 定期轮换 API Key

---

## 7. 火山 TOS 大文件存储最佳实践

### 上传策略

| 文件类型 | 大小 | 方案 |
|----------|------|------|
| 图片/小文件 | < 10MB | 直接上传 (PutObject) |
| 视频/大文件 | > 50MB | 分片上传 (MultipartUpload) |

**前端直传（强烈推荐）：**
```
1. 前端请求 POST /api/storage/upload-url → 后端生成预签名 PUT URL（有效期 15 分钟）
2. 前端直接 PUT 上传到 TOS
3. 上传完成后通知后端记录 URL

好处：减轻后端压力，不消耗应用服务器带宽
```

### 存储目录规范

```
{bucket}/
├── projects/
│   └── {projectId}/
│       ├── assets/
│       │   ├── character/{assetId}/reference.png
│       │   ├── scene/{assetId}/reference.png
│       │   └── prop/{assetId}/reference.png
│       ├── generations/
│       │   └── {taskId}/
│       │       ├── image.png
│       │       └── video.mp4
│       └── exports/
│           └── {exportTaskId}/final.mp4
```

### CDN 与成本优化

- TOS 绑定自定义域名 + CDN 加速
- CDN 缓存策略：图片 7 天，视频 3 天
- 使用 TOS 低频存储存放历史版本
- 开启存储桶生命周期管理，自动删除 30 天以上的废弃版本

### Maven 依赖

```xml
<dependency>
  <groupId>com.volcengine</groupId>
  <artifactId>volc-sdk-java</artifactId>
  <version>1.0.241</version>
</dependency>
```

---

## 8. 最可能出问题的 AI 环节及规避方案

### 风险排序（从高到低）

| 排名 | 风险 | 影响 | 严重度 |
|------|------|------|--------|
| 1 | 分镜提示词质量差 | 生成的图/视频完全不可用 | 致命 |
| 2 | 角色一致性失败 | 同一角色在不同分镜中长相不同 | 严重 |
| 3 | Seedance API 不稳定 | 任务失败，流程中断 | 中等 |
| 4 | FFmpeg 合并失败 | 最终导出失败 | 中等 |
| 5 | API 费用失控 | 预算耗尽 | 严重 |

### 规避方案

**风险1 — 分镜提示词质量差（最致命）：**
- shot_gen 使用强模型（GPT-4o / Claude Sonnet）
- 系统提示词充分测试，包含大量 Few-shot 示例
- **增加人工审核环节**：分镜 prompt 生成后先让人审核再调 AI 生图
- 建立 prompt 模板库，积累成功模式

**风险2 — 角色一致性失败：**
- 参考图至少 3 张（多角度），质量要高
- prompt 中固定角色外观描述
- 生成后人工逐帧审核
- 预留"重新生成"能力

**风险3 — Seedance API 不稳定：**
- 完善的异常处理和重试机制
- 并发控制（线程池 + Redis 队列）
- 失败任务不阻塞整个流程，允许跳过/重试
- **准备备选模型**（如 Kling / Runway 作为 fallback）

**风险4 — FFmpeg 合并失败：**
- FFmpeg 日志完整记录
- 每个视频片段上传前校验格式/分辨率/编码
- 合并在独立目录操作，不影响源文件
- 提供"手动下载片段自行合并"的降级方案

**风险5 — API 费用失控：**
- 每次 AI 调用前检查预算余额
- 设置单项目/单日 API 费用上限
- 批量生成时限制并发数和总量
- 实时费用看板，超过 80% 阈值告警
- **沙盒模式**：低分辨率预览 → 确认后全量生成

---

## 综合评分

| 维度 | 评分 | 说明 |
|------|------|------|
| PRD 完整性 | 7.5/10 | 功能清单清晰，技术细节需补充 |
| 一致性方案 | 6.0/10 | 基础框架有，需增强手段 |
| 异步设计 | 5.5/10 | 缺少任务队列和进度推送 |
| 安全性 | 5.0/10 | API Key 管理方案不安全 |
| 风险控制 | 7.0/10 | 识别了主要风险，应对需具体化 |

---

## 关键建议优先级

| 优先级 | 事项 | 预计工作量 |
|--------|------|-----------|
| **P0** | 环境变量管理 API Key | 1h |
| **P0** | 异步任务队列（线程池 + Redis） | 4h |
| **P0** | 失败重试机制 | 3h |
| **P1** | SSE 进度推送（或先用轮询） | 3h |
| **P1** | 分镜 prompt 人工审核环节 | 2h |
| **P1** | 沙盒预览模式 | 4h |
| **P2** | CLIP 一致性校验 | 6h |
| **P2** | 前端直传 TOS | 3h |
| **P2** | 预算告警 | 2h |

---

## 数据库修改建议汇总

```sql
-- asset 表增加
ALTER TABLE asset ADD COLUMN appearance_keywords JSON COMMENT '外观关键词数组';
ALTER TABLE asset ADD COLUMN face_embedding TEXT COMMENT '人脸特征向量';

-- shot 表增加
ALTER TABLE shot ADD COLUMN prompt_en TEXT COMMENT '英文生成提示词';
ALTER TABLE shot ADD COLUMN consistency_score DECIMAL(5,2) COMMENT '一致性校验得分';
ALTER TABLE shot ADD COLUMN generation_attempts INT DEFAULT 0 COMMENT '生成尝试次数';

-- 新增导出任务表
CREATE TABLE export_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    status TINYINT DEFAULT 0 COMMENT '0:待处理 1:处理中 2:成功 3:失败',
    input_shot_ids JSON COMMENT '参与合并的分镜ID列表',
    output_url VARCHAR(500) COMMENT '最终视频URL',
    ffmpeg_log TEXT COMMENT 'FFmpeg执行日志',
    error_msg TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    finish_time DATETIME
);
```

---

**评审结论：PRD 框架良好，建议采纳上述改进后进入开发阶段。重点关注异步任务处理和一致性保障两个核心领域。**
