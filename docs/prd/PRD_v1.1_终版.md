# AI漫剧生产平台 — PRD v1.1 (MVP 终版)

## 文档信息

| 项目 | 内容 |
|------|------|
| 产品名 | AI漫剧生产平台 |
| 版本 | v1.1 MVP 终版 |
| 作者 | 小赫 (协助蓝烟老师) |
| 评审人 | 蓝烟老师、老克、小欧、阿典 |
| 创建日期 | 2026-04-19 |
| 状态 | 已确认（待开发） |

---

## 1. 背景与目标

### 1.1 业务背景
个人创作者（蓝烟老师及朋友）需要将网络小说快速转化为 AI 漫剧（视频形式的短剧）。核心目标：**保证视频质量的前提下，跑通「小说→完整漫剧视频」的全流程**。

### 1.2 目标

| 目标类型 | 描述 | 衡量指标 | 当前值 | 目标值 |
|----------|------|----------|--------|--------|
| 业务目标 | 小说→漫剧视频完整流程跑通 | 一部小说→完整视频输出 | 无 | MVP可产出视频 |
| 用户目标 | 降低AI漫剧制作门槛，流程可视化 | 线性流水线+人工审核 | 无 | 拖拽可配，审核可控 |
| 质量目标 | 人物/场景一致性保障 | 资产引用机制 + 审核把关 | 无 | 每帧强一致性 |
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
1. 登录 → 新建项目 → 上传小说文本（存TOS路径）
2. 系统自动拆分章节
3. **进入资产库**：提取/编辑角色、场景、物品、声音资产
4. 确认每个资产的多视角参考图和描述（用于后续生成一致性）

**场景2：配置流程 & 执行**
1. 流程编辑器中拖拽配置步骤（线性流水线）
2. 设置哪些步骤需要人工审核
3. 点击开始执行 → 前端 3s 轮询查看进度
4. 到达审核节点 → 人工确认质量 → 继续

**场景3：分镜工作台 & 质量把控**
1. 查看分镜列表，每个分镜关联资产（角色/场景）
2. AI 生成图片/视频时**强制引用已确认资产**
3. 打回重做 / 批量通过
4. 导出最终视频

---

## 3. 功能需求

### 3.1 功能清单

| 编号 | 功能 | 描述 | 优先级 |
|------|------|------|--------|
| F-001 | 用户登录 | Sa-Token 账号密码登录 | P0 |
| F-002 | 项目管理 | 创建/编辑/删除项目，小说导入（存TOS） | P0 |
| F-003 | 资产管理 | 角色/场景/物品/声音，**多张参考图** | P0 |
| F-004 | 层级结构 | 项目→分集→分场→分镜 四级管理 | P0 |
| F-005 | 流程编辑器 | **SortableJS 线性流水线**（非X6） | P0 |
| F-006 | 流程执行 | 后端线性执行，跳过/重试/审核暂停，**断点续跑** | P0 |
| F-007 | 分镜工作台 | 分镜查看/编辑，AI生成，**批量审核** | P0 |
| F-008 | AI模型集成 | Seedance 2.0 API对接（生图+生视频） | P0 |
| F-009 | 火山TOS存储 | **前端预签名直传**，后端只存URL | P0 |
| F-010 | API消耗记录 | 简单记录每次调用，前端可查看 | P1 |
| F-011 | 预算控制 | API调用前余额检查，可设上限 | P1 |

### 3.2 核心功能详述

#### F-003 资产管理（质量保障核心）
- **资产类型**：character（角色）、scene（场景）、prop（物品/道具）、voice（声音/配音）
- **多参考图**：每个资产支持上传多张图片（正面/侧面/特写），标记一张主图
- **一致性机制**：分镜生成时，强制绑定已确认的资产。Seedance 调用时传入主参考图 + 角色描述
- **前端页面**：Tab 按类型分组，卡片网格展示，声音资产带音频播放器

#### F-005 流程编辑器（轻量版）
- **SortableJS + Element Plus 卡片**（不用 AntV X6，轻量）
- 步骤类型固定：导入剧本 → 资产提取 → 提示词生成 → 首帧生图 → 视频生成 → 合并导出
- 每个步骤可配置：启用/禁用、审核开关
- 拖拽排序，保存为 JSON

#### F-007 分镜工作台
- 左侧：分集/分场折叠树 + 状态摘要
- 右侧：分镜卡片列表（虚拟滚动防卡顿）
- 操作：生成、预览、通过、打回（填写意见）、重新生成、**批量通过/打回**
- 版本管理：保留历史版本

---

## 4. 非功能需求

| 类型 | 要求 | 具体指标 |
|------|------|----------|
| 性能 | 业务API响应 | P99 < 500ms |
| 性能 | AI生成任务 | @Async 异步处理，前端 3s 轮询 |
| 安全 | 认证 | Sa-Token JWT（`sa-token-spring-boot3-starter`），BCrypt加密 |
| 安全 | API Key | **环境变量存储**，不暴露前端，不写死代码 |
| 存储 | 文件存储 | 火山 TOS，**前端预签名直传** |
| 兼容性 | 浏览器 | Chrome/Edge 最新版 |

---

## 5. 数据模型（MySQL 8.0）

### 5.1 `sys_user` 用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| username | VARCHAR(50) UNIQUE | 用户名 |
| password | VARCHAR(100) | BCrypt密码 |
| nickname | VARCHAR(50) | 昵称 |
| status | TINYINT | 0:禁用 1:启用 |
| deleted | TINYINT | 0:正常 1:删除 |
| create_time | DATETIME | |
| update_time | DATETIME | |

### 5.2 `project` 项目表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 创建人 |
| name | VARCHAR(200) | 项目名称 |
| description | TEXT | 描述 |
| novel_tos_path | VARCHAR(500) | 小说文件TOS路径（不再存LONGTEXT） |
| workflow_config | JSON | 流程配置（线性步骤数组） |
| style_preset | JSON | 全局风格预设（画风/色调） |
| status | TINYINT | 0:草稿 1:进行中 2:已完成 |
| execution_lock | TINYINT | 0:未执行 1:执行中（防重复点击） |
| deleted | TINYINT | 0:正常 1:删除 |
| create_time | DATETIME | |
| update_time | DATETIME | |

### 5.3 `asset` 资产表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| project_id | BIGINT | 所属项目 |
| asset_type | VARCHAR(20) | character/scene/prop/voice |
| name | VARCHAR(100) | 资产名称 |
| description | TEXT | AI描述文本 |
| reference_images | JSON | 参考图URL数组 `["url1", "url2"]`，第一个为主图 |
| style_preset | JSON | 风格预设 |
| status | TINYINT | 0:草稿 1:已确认 2:已废弃 |
| deleted | TINYINT | 0:正常 1:删除 |
| create_time | DATETIME | |
| update_time | DATETIME | |

### 5.4 `episode` 分集表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| project_id | BIGINT | 所属项目 |
| title | VARCHAR(200) | 标题 |
| sort_order | INT | 排序 |
| content | TEXT | 分集剧本内容 |
| status | TINYINT | 0:待处理 1:进行中 2:已完成 |
| deleted | TINYINT | 0:正常 1:删除 |
| create_time | DATETIME | |
| update_time | DATETIME | |

### 5.5 `scene` 分场表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| episode_id | BIGINT | 所属分集 |
| title | VARCHAR(200) | 标题 |
| sort_order | INT | 排序 |
| content | TEXT | 分场描述 |
| status | TINYINT | 0:待处理 1:进行中 2:已完成 |
| deleted | TINYINT | 0:正常 1:删除 |
| create_time | DATETIME | |
| update_time | DATETIME | |

### 5.6 `shot` 分镜表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| scene_id | BIGINT | 所属分场 |
| sort_order | INT | 排序 |
| prompt | TEXT | AI生图提示词（中文） |
| prompt_en | TEXT | AI生图提示词（英文，翻译后） |
| generated_image_url | VARCHAR(500) | 生成图片URL |
| generated_video_url | VARCHAR(500) | 生成视频URL |
| status | TINYINT | 0:待处理 1:生成中 2:待审核 3:已通过 4:已打回 5:已完成 |
| review_comment | TEXT | 审核意见 |
| version | INT | 版本号 |
| generation_attempts | INT | 生成尝试次数 |
| deleted | TINYINT | 0:正常 1:删除 |
| create_time | DATETIME | |
| update_time | DATETIME | |

### 5.7 `shot_asset_ref` 分镜-资产关联表（拆出JSON）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| shot_id | BIGINT | 分镜ID |
| asset_id | BIGINT | 资产ID |
| asset_type | VARCHAR(20) | character/scene/prop |
| **UNIQUE(shot_id, asset_id)** | | 防止重复关联 |

### 5.8 `workflow_task` 流程任务表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| project_id | BIGINT | 所属项目 |
| episode_id | BIGINT | 当前处理的分集ID（可为空） |
| step_type | VARCHAR(50) | 步骤类型 |
| step_order | INT | 步骤顺序 |
| status | TINYINT | 0:未执行 1:执行中 2:成功 3:失败 4:待审核 |
| input_data | JSON | 输入 |
| output_data | JSON | 输出 |
| review_comment | TEXT | 审核意见 |
| error_msg | TEXT | 错误信息 |
| create_time | DATETIME | |
| update_time | DATETIME | |

### 5.9 `ai_task` AI任务表（独立于workflow_task）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| project_id | BIGINT | 项目ID |
| shot_id | BIGINT | 关联分镜 |
| task_type | VARCHAR(20) | image_gen / video_gen |
| provider_task_id | VARCHAR(100) | 第三方API返回的任务ID |
| status | TINYINT | 0:提交中 1:处理中 2:成功 3:失败 |
| result_url | VARCHAR(500) | 结果URL（TOS） |
| error_msg | TEXT | 错误信息 |
| create_time | DATETIME | |
| update_time | DATETIME | |

### 5.10 `api_call_log` API调用记录表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户ID |
| project_id | BIGINT | 项目ID |
| api_provider | VARCHAR(50) | 提供商 |
| api_endpoint | VARCHAR(200) | 接口 |
| request_params | JSON | 请求摘要（脱敏） |
| token_usage | INT | Token消耗 |
| cost | DECIMAL(10,4) | 费用（元） |
| status | TINYINT | 0:失败 1:成功 |
| create_time | DATETIME | |

---

## 6. 技术方案

### 6.1 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 17+, Spring Boot 3.2, MyBatis Plus 3.5.5+, Sa-Token 1.37 (spring-boot3-starter), Redis |
| 前端 | Vue 3 + TS, Vite, Element Plus, **SortableJS**（流程拖拽）, vue-virtual-scroller |
| 数据库 | MySQL 8.0 |
| 缓存/状态 | Redis（session、AI任务状态缓存、流程执行锁） |
| 存储 | **火山 TOS**（AWS S3 兼容 SDK） |
| AI调用 | Seedance 2.0 API（HTTP）, 文本模型（GPT-4o/Claude） |
| 视频合并 | FFmpeg（后端调用） |

### 6.2 项目结构

```
ai-drama-platform/
├── backend/
│   ├── src/main/java/com/lanyan/aidrama/
│   │   ├── common/          # Result, GlobalExceptionHandler, Constants, BusinessException
│   │   ├── config/
│   │   │   ├── SaTokenConfig.java           # sa-token-spring-boot3-starter
│   │   │   ├── MybatisPlusConfig.java       # JacksonTypeHandler 配置
│   │   │   ├── RedisConfig.java
│   │   │   ├── AsyncConfig.java             # @EnableAsync + 自定义线程池（3核心）
│   │   │   └── TosConfig.java               # AWS S3 兼容配置
│   │   ├── module/
│   │   │   ├── user/        # 用户登录
│   │   │   ├── project/     # 项目CRUD + novel上传TOS
│   │   │   ├── asset/       # 资产管理（多参考图）
│   │   │   ├── content/     # 分集/分场/分镜 CRUD + 批量审核
│   │   │   ├── workflow/    # 流程执行引擎（线性）
│   │   │   │   ├── WorkflowEngine.java      # 线性执行 + 断点续跑
│   │   │   │   └── NodeExecutor.java        # 各步骤执行器
│   │   │   ├── aitask/      # AI任务管理
│   │   │   │   ├── AiTaskService.java       # 异步提交 + 轮询结果
│   │   │   │   └── client/
│   │   │   │       └── SeedanceClient.java  # Seedance API封装
│   │   │   └── storage/     # TOS封装
│   │   │       └── TosService.java          # 预签名URL生成
│   │   └── AiDramaApplication.java
│   └── pom.xml
│
├── frontend/
│   └── src/
│       ├── views/
│       │   ├── Login.vue
│       │   ├── ProjectList.vue
│       │   ├── ProjectDetail.vue      # 路由容器
│       │   ├── tabs/
│       │   │   ├── AssetLibrary.vue   # 资产管理（Tab分组）
│       │   │   ├── WorkflowEditor.vue # SortableJS 线性流程
│       │   │   ├── ShotWorkbench.vue  # 分镜工作台（虚拟滚动）
│       │   │   └── ApiCost.vue        # API消耗看板
│       │   └── ...
│       ├── api/                     # Axios 封装 + 拦截器
│       ├── types/                   # TS 类型定义
│       ├── stores/                  # Pinia: auth, project, asset, content, ai
│       ├── composables/             # useTaskPolling, useTosUpload
│       └── constants/               # status.ts（状态颜色映射）
│
├── sql/init.sql
└── .env                             # API Key 环境变量
```

### 6.3 简化流程引擎设计

**执行逻辑：**
1. 前端保存线性步骤数组到 `project.workflow_config`
2. 后端 `@Async` 按顺序执行，跳过 `enabled: false` 的步骤
3. 每步执行前加 Redis 分布式锁（防重复点击 `execution_lock`）
4. 每步执行完后更新 `workflow_task.status` + `update_time`
5. 遇到 `review: true` 的步骤，标记为 `WAITING_REVIEW`，暂停执行
6. **前端 3s 轮询** `/api/project/{id}/workflow/status` 获取进度
7. 用户确认后调用 `/api/project/{id}/workflow/review` 继续
8. **服务重启恢复**：启动时扫描 `status=1(执行中)` 的任务，提供恢复接口

**步骤类型清单：**

| 步骤 | 说明 | 输入 | 输出 |
|------|------|------|------|
| import | 导入并拆分剧本 | 小说TOS路径 | 分集列表 |
| asset_extract | 提取资产（AI辅助） | 分集内容 | 角色/场景资产草稿 |
| shot_gen | 分镜提示词生成（强模型） | 分场+资产 | 分镜prompt（中英文） |
| image_gen | 首帧生图 | prompt+资产参考图 | 图片URL（TOS） |
| video_gen | 视频生成(Seedance) | 首帧图+prompt | 视频URL（TOS） |
| export | 合并导出（FFmpeg） | 所有视频URL | 最终视频URL（TOS） |

### 6.4 关键接口

```
POST   /api/user/login              # 登录
GET    /api/user/info               # 当前用户信息

# 项目
GET    /api/project/list            # 项目列表
POST   /api/project                 # 创建项目
GET    /api/project/{id}            # 项目详情
PUT    /api/project/{id}            # 更新项目

# TOS 预签名
POST   /api/tos/presign             # 获取上传预签名URL

# 资产
GET    /api/project/{id}/assets     # 资产列表（按类型分组）
POST   /api/project/{id}/assets     # 创建资产
PUT    /api/asset/{id}              # 更新资产

# 内容（分集/分场/分镜）
GET    /api/project/{id}/episodes   # 分集列表
GET    /api/episode/{id}/scenes     # 分场列表
GET    /api/scene/{id}/shots        # 分镜列表
PUT    /api/shot/{id}               # 更新分镜
POST   /api/shot/batch-review       # 批量审核通过/打回

# 流程
PUT    /api/project/{id}/workflow         # 保存流程配置
POST   /api/project/{id}/workflow/start   # 开始执行（加锁）
GET    /api/project/{id}/workflow/status  # 查询进度（轮询用）
POST   /api/project/{id}/workflow/review  # 审核通过/打回
POST   /api/project/{id}/workflow/stop    # 停止执行

# AI 任务
POST   /api/ai/image-gen            # 触发图片生成
POST   /api/ai/video-gen            # 触发视频生成
GET    /api/ai/task/{taskId}        # 查询AI任务状态（轮询用）

# 消耗
GET    /api/ai/cost-report          # 消耗统计
```

### 6.5 一致性保障机制

1. **资产强绑定**：`shot_asset_ref` 表记录关联，生成时强制传入资产主参考图
2. **Seedance参数**：
   - `first_frame_image`: 分镜首帧图
   - `reference_image`: 角色/场景主参考图
   - `prompt`: 包含角色名、场景描述的完整英文提示词
3. **风格锁定**：项目级 `style_preset` 控制全局画风参数
4. **提示词质量**：shot_gen 使用强模型生成后，默认开启人工审核，确认后再进入生图
5. **审核把关**：生图、生视频节点默认开启审核，人工确认一致性

### 6.6 Redis 使用场景
| 用途 | Key 格式 | TTL |
|------|----------|-----|
| Sa-Token session | `satoken:token:{token}` | 默认 |
| AI 任务状态缓存 | `ai:task:{taskId}` | 24h |
| 流程执行锁 | `workflow:lock:{projectId}` | 30min |
| API 限流 | `api:rate:{userId}` | 1min |

### 6.7 AI 异步处理流程
```
前端提交生成请求 → 后端创建 ai_task 记录（status=0） → @Async 提交到第三方API
→ 更新 provider_task_id + status=1 → 定时任务轮询第三方API（每5s）
→ 获取结果 → 上传TOS → 更新 ai_task status=2 + result_url
→ 前端轮询 /api/ai/task/{taskId} 获取最新状态
```

---

## 7. 任务拆分

### Sprint 1: 基础骨架（Day 1-3）
| 任务 | 负责人 | 交付物 |
|------|--------|--------|
| 数据库建表 + init.sql | 老克 | 10 张表完整 DDL |
| SpringBoot 骨架搭建 | 老克 | 项目结构、pom.xml、配置文件 |
| Sa-Token 登录模块 | 老克 | 登录/登出/获取用户信息 |
| 火山 TOS 封装 + 预签名URL | 老克 | TosService（上传/预签名） |
| 前端基础工程 | 小欧 | Vue3 + Vite + Element Plus + 路由 + Pinia + Axios拦截器 |

### Sprint 2: 核心业务（Day 4-7）
| 任务 | 负责人 | 交付物 |
|------|--------|--------|
| 项目管理 CRUD + 小说上传TOS | 老克 | 项目接口 |
| 资产管理 CRUD（多参考图） | 老克 | 资产接口 |
| 分集/分场/分镜 CRUD + 批量审核 | 老克 | 内容接口 |
| 前端：项目列表 + 项目详情 Tab 页 | 小欧 | 页面+路由 |
| 前端：资产管理页面 | 小欧 | Tab分组+卡片+图片上传直传TOS |

### Sprint 3: 流程引擎 + AI集成（Day 8-12）
| 任务 | 负责人 | 交付物 |
|------|--------|--------|
| 流程执行引擎（线性+断点续跑+锁） | 老克 | WorkflowEngine |
| AI 任务异步处理 + 轮询 | 阿典 | AiTaskService + SeedanceClient |
| shot_gen 提示词生成（强模型） | 阿典 | PromptEngine |
| 前端：流程编辑器（SortableJS） | 小欧 | 拖拽排序+审核开关配置 |
| 前端：分镜工作台（虚拟滚动+批量操作） | 小欧 | 分镜列表+审核操作 |
| 前端：任务轮询 composable | 小欧 | useTaskPolling（3s轮询） |

### Sprint 4: 联调测试（Day 13-15）
| 任务 | 负责人 | 交付物 |
|------|--------|--------|
| API 消耗记录 + 看板 | 老克 | api_call_log 记录 + 前端展示 |
| FFmpeg 视频合并导出 | 阿典 | ExportService |
| 全流程联调（导入→资产→分镜→生图→生视频→导出） | 三人 | 跑通完整流程 |
| 边界情况测试（断点续跑、并发锁、重试） | 三人 | 测试报告 |

---

## 8. 风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| AI生成质量不稳定 | 高 | 强制审核节点，支持多次重试，shot_gen 必须人工确认 |
| Seedance API费用 | 高 | 预算上限+单次调用前检查，沙盒低分辨率预览 |
| 火山TOS成本 | 中 | Bucket 生命周期规则自动清理旧版本 |
| AI 任务状态丢失 | 高 | 状态持久化到 DB（ai_task 表），不依赖 Redis |
| 服务重启流程中断 | 中 | 启动时扫描 RUNNING 任务，提供恢复接口 |
| 分镜引用已删除资产 | 高 | 数据库约束 + 应用层校验 |

---

## 附录
- [AntV X6 流程图](https://x6.antv.antgroup.com/)（不采用，改用 SortableJS）
- [火山引擎 TOS 文档](https://www.volcengine.com/docs/6349)
- [SortableJS](https://sortablejs.github.io/Sortable/)
- Seedance 2.0 API 文档：待补充
