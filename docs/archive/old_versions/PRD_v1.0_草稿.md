# AI漫剧生产平台 — PRD v1.0 (MVP)

## 文档信息

| 项目 | 内容 |
|------|------|
| 产品名 | AI漫剧生产平台 |
| 版本 | v1.0 MVP |
| 作者 | 小赫 (协助蓝烟老师) |
| 评审人 | 蓝烟老师 |
| 创建日期 | 2026-04-19 |
| 状态 | 已确认（待开发） |

---

## 1. 背景与目标

### 1.1 业务背景
个人创作者（蓝烟老师及朋友）需要将网络小说快速转化为 AI 漫剧（视频形式的短剧）。目前行业缺乏一个轻量、可灵活配置流程的生产工具。本项目定位为**个人/小团队使用的工程化平台**，核心目标是：**保证视频质量的前提下，跑通「小说→完整漫剧视频」的全流程**。

### 1.2 目标

| 目标类型 | 描述 | 衡量指标 | 当前值 | 目标值 |
|----------|------|----------|--------|--------|
| 业务目标 | 小说→漫剧视频完整流程跑通 | 一部小说→完整视频输出 | 无 | MVP可产出视频 |
| 用户目标 | 降低AI漫剧制作门槛，流程可视化 | 线性流水线+人工审核 | 无 | 拖拽可配，审核可控 |
| 质量目标 | 人物/场景一致性保障 | 资产引用机制 | 无 | 每帧强一致性 |
| 技术目标 | 单体架构，轻量可维护 | 简化流程引擎 | 无 | 线性执行+跳过/重试 |

---

## 2. 用户与场景

### 2.1 目标用户
| 用户类型 | 特征 | 核心需求 |
|----------|------|----------|
| 主创作者（蓝烟老师） | 技术背景，有剧本资源 | 全流程控制、质量保障、成本记录 |
| 协作者（朋友） | 可能非技术背景 | 按流程操作，审核确认即可 |

### 2.2 核心用户场景

**场景1：创建项目 & 提取资产**
1. 登录 → 新建项目 → 上传小说文本
2. 系统自动拆分章节
3. **进入资产库**：提取/编辑角色、场景、物品、声音资产
4. 确认每个资产的参考图和描述（用于后续生成一致性）

**场景2：配置流程 & 执行**
1. 流程编辑器中拖拽配置步骤（线性流水线）
2. 设置哪些步骤需要人工审核
3. 点击开始执行 → 用户手动刷新查看进度
4. 到达审核节点 → 人工确认质量 → 继续

**场景3：分镜工作台 & 质量把控**
1. 查看分镜列表，每个分镜关联资产（角色/场景）
2. AI 生成图片/视频时**强制引用已确认资产**
3. 打回重做 / 批量通过
4. 导出最终视频

---

## 3. 功能需求

### 3.1 功能清单

| 编号 | 功能 | 描述 | 优先级 | 复杂度 |
|------|------|------|--------|--------|
| F-001 | 用户登录 | Sa-Token 账号密码登录 | P0 | S |
| F-002 | 项目管理 | 创建/编辑/删除项目，小说导入 | P0 | M |
| F-003 | 资产管理 | 角色/场景/物品/声音 资产库，参考图管理 | P0 | M |
| F-004 | 层级结构 | 项目→分集→分场→分镜 四级管理 | P0 | M |
| F-005 | 流程编辑器 | 线性流水线可视化配置（拖拽排序） | P0 | M |
| F-006 | 流程执行 | 后端线性执行，支持跳过/重试/审核暂停 | P0 | M |
| F-007 | 分镜工作台 | 分镜查看/编辑，触发AI生成，审核打回 | P0 | M |
| F-008 | AI模型集成 | Seedance 2.0 API对接（生图+生视频） | P0 | M |
| F-009 | 视频存储 | 火山 TOS 对象存储 | P0 | S |
| F-010 | API消耗记录 | 简单记录每次调用，前端可查看 | P1 | S |

### 3.2 核心功能详述

#### F-003 资产管理（质量保障核心）
- **资产类型**：character（角色）、scene（场景）、prop（物品/道具）、voice（声音/配音）
- **资产字段**：名称、描述、参考图、风格预设、状态（草稿/已确认/废弃）
- **一致性机制**：分镜生成时，强制绑定已确认的资产。Seedance 调用时传入 `reference_image` + `character_prompt`
- **前端页面**：资产库页面，卡片式展示，支持上传参考图、编辑描述、确认/废弃

#### F-005 流程编辑器（简化版）
- 线性流水线（非 DAG），步骤可排序、可启用/禁用
- 节点类型：导入剧本 → 资产提取 → 提示词生成 → 首帧生图 → 视频生成 → 合并导出
- 每个节点可配置 `review: true/false`（是否需要人工审核）
- 前端保存为 JSON，后端按顺序执行

#### F-007 分镜工作台
- 左侧：分镜列表（带状态：待处理/生成中/待审核/已通过/已打回）
- 右侧：分镜详情（提示词、关联资产、生成的图片/视频预览）
- 操作：生成、预览、通过、打回（填写意见）、重新生成
- 版本管理：保留历史版本，支持对比

---

## 4. 非功能需求

| 类型 | 要求 | 具体指标 |
|------|------|----------|
| 性能 | 业务API响应 | P99 < 500ms |
| 性能 | AI生成任务 | 异步处理，用户手动查看进度 |
| 安全 | 认证 | Sa-Token JWT，BCrypt加密密码 |
| 安全 | API Key | 存在项目 YAML 配置文件中，不暴露前端 |
| 存储 | 文件存储 | 火山 TOS 对象存储（图片/视频） |
| 兼容性 | 浏览器 | Chrome/Edge 最新版 |

---

## 5. 数据模型（MySQL）

### 5.1 `sys_user` 用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| username | VARCHAR(50) UNIQUE | 用户名 |
| password | VARCHAR(100) | BCrypt密码 |
| nickname | VARCHAR(50) | 昵称 |
| status | TINYINT | 0:禁用 1:启用 |
| create_time | DATETIME | |

### 5.2 `project` 项目表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 创建人 |
| name | VARCHAR(200) | 项目名称 |
| description | TEXT | 描述 |
| novel_content | LONGTEXT | 小说原文 |
| workflow_config | JSON | 流程配置（线性步骤数组） |
| style_preset | JSON | 全局风格预设（画风/色调） |
| config_yaml_path | VARCHAR(500) | 项目YAML配置文件路径（含API Key等） |
| status | TINYINT | 0:草稿 1:进行中 2:已完成 |
| create_time | DATETIME | |

### 5.3 `asset` 资产表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| project_id | BIGINT | 所属项目 |
| asset_type | VARCHAR(20) | character/scene/prop/voice |
| name | VARCHAR(100) | 资产名称 |
| description | TEXT | AI描述文本 |
| reference_image_url | VARCHAR(500) | 参考图URL（TOS） |
| style_preset | JSON | 风格预设 |
| status | TINYINT | 0:草稿 1:已确认 2:已废弃 |
| create_time | DATETIME | |

### 5.4 `episode` 分集表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| project_id | BIGINT | 所属项目 |
| title | VARCHAR(200) | 标题 |
| sort_order | INT | 排序 |
| content | TEXT | 分集剧本内容 |
| status | TINYINT | 0:待处理 1:进行中 2:已完成 |

### 5.5 `scene` 分场表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| episode_id | BIGINT | 所属分集 |
| title | VARCHAR(200) | 标题 |
| sort_order | INT | 排序 |
| content | TEXT | 分场描述 |
| status | TINYINT | 0:待处理 1:进行中 2:已完成 |

### 5.6 `shot` 分镜表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| scene_id | BIGINT | 所属分场 |
| sort_order | INT | 排序 |
| prompt | TEXT | AI生图提示词 |
| asset_refs | JSON | 关联资产ID数组 [{"type":"character","id":1}, {"type":"scene","id":2}] |
| reference_image_url | VARCHAR(500) | 参考图URL |
| generated_image_url | VARCHAR(500) | 生成图片URL |
| generated_video_url | VARCHAR(500) | 生成视频URL |
| status | TINYINT | 0:待处理 1:生成中 2:待审核 3:已通过 4:已打回 5:已完成 |
| review_comment | TEXT | 审核意见 |
| version | INT | 版本号 |
| create_time | DATETIME | |

### 5.7 `workflow_task` 流程任务表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| project_id | BIGINT | 所属项目 |
| step_type | VARCHAR(50) | 步骤类型 |
| status | TINYINT | 0:未执行 1:执行中 2:成功 3:失败 4:待审核 |
| input_data | JSON | 输入 |
| output_data | JSON | 输出 |
| error_msg | TEXT | 错误信息 |
| create_time | DATETIME | |

### 5.8 `api_call_log` API调用记录表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户ID |
| project_id | BIGINT | 项目ID |
| api_provider | VARCHAR(50) | 提供商 |
| api_endpoint | VARCHAR(200) | 接口 |
| request_params | JSON | 请求摘要 |
| token_usage | INT | Token消耗 |
| cost | DECIMAL(10,4) | 费用（元） |
| status | TINYINT | 0:失败 1:成功 |
| create_time | DATETIME | |

---

## 6. 技术方案

### 6.1 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 17+, Spring Boot 3.2, MyBatis Plus 3.5, Sa-Token 1.37, Redis |
| 前端 | Vue 3 + TS, Vite, Element Plus, AntV X6（流程编辑器） |
| 数据库 | MySQL 8.0 |
| 缓存/状态 | Redis |
| 存储 | **火山 TOS**（图片/视频/资产参考图） |
| AI调用 | Seedance 2.0 API（HTTP） |

### 6.2 项目结构

```
ai-drama-platform/
├── backend/
│   ├── src/main/java/com/lanyan/aidrama/
│   │   ├── common/          # Result, ExceptionHandler, Constants
│   │   ├── config/          # SaToken, MybatisPlus, Redis, TosClient, YamlConfigLoader
│   │   ├── module/
│   │   │   ├── user/        # 用户登录
│   │   │   ├── project/     # 项目CRUD
│   │   │   ├── asset/       # 资产管理（角色/场景/物品/声音）
│   │   │   ├── content/     # 分集/分场/分镜
│   │   │   ├── workflow/    # 简化流程引擎（线性执行）
│   │   │   ├── aicall/      # AI调用（SeedanceClient）
│   │   │   └── storage/     # 火山TOS封装
│   │   └── AiDramaApplication.java
│   └── pom.xml
│
├── frontend/
│   └── src/
│       ├── views/
│       │   ├── Login.vue
│       │   ├── ProjectList.vue
│       │   ├── ProjectDetail.vue      # 项目详情（资产库+分镜列表）
│       │   ├── AssetLibrary.vue       # 资产管理页面
│       │   ├── WorkflowEditor.vue     # 流程编辑器
│       │   ├── ShotWorkbench.vue      # 分镜工作台
│       │   └── ApiCost.vue            # API消耗看板
│       └── ...
│
├── sql/init.sql
└── projects/                          # 项目YAML配置文件目录
    └── {projectId}/config.yaml        # 含API Key、模型配置等
```

### 6.3 简化流程引擎设计

**执行逻辑：**
1. 前端保存线性步骤数组到 `project.workflow_config`
2. 后端按数组顺序执行，跳过 `enabled: false` 的步骤
3. 每个步骤执行完后更新 `workflow_task.status`
4. 遇到 `review: true` 的步骤，标记为 `WAITING_REVIEW`，暂停执行
5. **用户手动刷新前端查看进度**，确认后调用接口继续
6. AI生成任务为异步（提交任务ID → 轮询结果 → 存入TOS）

**步骤类型清单：**

| 步骤 | 说明 | 输入 | 输出 |
|------|------|------|------|
| import | 导入并拆分剧本 | 小说全文 | 分集列表 |
| asset_extract | 提取资产（可AI辅助） | 分集内容 | 角色/场景资产草稿 |
| shot_gen | 分镜提示词生成 | 分场+资产 | 分镜prompt |
| image_gen | 首帧生图 | prompt+资产参考图 | 图片URL（TOS） |
| video_gen | 视频生成(Seedance) | 首帧图+prompt+音频参考 | 视频URL（TOS） |
| export | 合并导出 | 所有视频片段 | 最终视频URL（TOS） |

### 6.4 关键接口

```
POST   /api/user/login              # 登录
GET    /api/project/list            # 项目列表
POST   /api/project                 # 创建项目
GET    /api/project/{id}            # 项目详情
PUT    /api/project/{id}/workflow   # 保存流程配置
POST   /api/project/{id}/workflow/start   # 开始执行
POST   /api/project/{id}/workflow/review  # 审核通过/打回

# 资产
GET    /api/project/{id}/assets     # 资产列表
POST   /api/project/{id}/assets     # 创建资产
PUT    /api/asset/{id}              # 更新资产

# 内容
GET    /api/project/{id}/episodes   # 分集列表
POST   /api/episode/{id}/scenes     # 分场列表
POST   /api/scene/{id}/shots        # 分镜列表
PUT    /api/shot/{id}               # 更新分镜（含审核操作）

# AI调用
POST   /api/ai/image-gen            # 触发图片生成
POST   /api/ai/video-gen            # 触发视频生成
GET    /api/ai/cost-report          # 消耗统计

# 存储
POST   /api/storage/upload          # 上传文件到TOS
```

### 6.5 一致性保障机制

1. **资产强绑定**：每个分镜 `shot.asset_refs` 记录关联的资产ID。生成时强制传入资产参考图
2. **Seedance参数**：
   - `first_frame_image`: 分镜首帧图
   - `reference_images`: 角色/场景参考图数组
   - `prompt`: 包含角色名、场景描述的完整提示词
   - `audio`: 可选配音音频
3. **风格锁定**：项目级 `style_preset` 控制全局画风、色调参数
4. **审核把关**：关键节点（生图、生视频）默认开启审核，人工确认一致性

---

## 7. 里程碑

| 里程碑 | 时间 | 交付物 |
|--------|------|--------|
| 技术方案确认 | Day 1 | 本PRD |
| 后端基础 | Day 2-3 | 数据库、SpringBoot骨架、Sa-Token、TOS集成 |
| 项目管理+资产 | Day 4-5 | 项目/资产 CRUD |
| 流程编辑器前端 | Day 6-8 | 线性流水线配置 |
| 流程执行引擎 | Day 9-11 | 线性执行+审核暂停 |
| AI集成+分镜工作台 | Day 12-14 | Seedance对接+分镜审核 |
| 联调测试 | Day 15-17 | 完整流程跑通 |

---

## 8. 风险

| 风险 | 影响 | 应对 |
|------|------|------|
| AI生成质量不稳定 | 高 | 强制审核节点，支持多次重试 |
| Seedance API费用 | 高 | API消耗记录，限制并发 |
| 火山TOS成本 | 中 | 定期清理废弃资产/版本 |

---

## 附录
- [AntV X6 流程图](https://x6.antv.antgroup.com/)
- [火山引擎 TOS 文档](https://www.volcengine.com/docs/6349)
- Seedance 2.0 API 文档：待补充
