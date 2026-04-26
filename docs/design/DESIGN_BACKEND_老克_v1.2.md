# AI漫剧生产平台 — 后端系统设计规格书

## 文档信息

| 项目 | 内容 |
|------|------|
| 产品名 | AI漫剧生产平台 |
| 版本 | v1.2 |
| 基于 | PRD v1.2 本期上线终版 |
| 修订日期 | 2026-04-25 |
| 状态 | 待评审 |

## 版本变更记录

### v1.2（2026-04-25）
1. 完全删除自动流程引擎、成本统计、项目状态、分场层级相关设计。
2. 后端数据模型统一收敛为“项目 → 分集 → 分镜”三级。
3. 新增剧本分析、分镜拆分、资产提取、统一任务、个人中心修改能力。
4. 所有 prompt 改为可配置；AI 返回模板改为静态资源文件维护，不写入业务代码。
5. token 过期仅约定后端返回 401；前端交互表现不属于本后端设计范围。
6. 资产删除限制仍遵守 PRD；但按业务调整，资产被分镜绑定后允许继续修改。

---

## 1. 本期范围

本期只做以下后端能力：

1. 登录/登出/获取用户信息/修改用户名/修改密码/更新头像
2. 项目 CRUD
3. 小说文本解析与项目绑定
4. 剧本分析生成分集
5. 分镜 CRUD、拆分、排序、草稿保存、生成提示词、生成图片、生成视频
6. 资产 CRUD、提取、重复检测、关系树、生成图片、引用查询
7. 统一任务提交与状态查询
8. TOS 预签名上传与上传完成通知

以下事项不属于本后端设计范围，由前端或展示层处理：

1. 项目缩略图按风格映射
2. 生成按钮文案与 loading 展示
3. token 过期后的弹窗、停留当前页等交互策略
4. 枚举值的展示文案与页面样式编排

本期明确不做：

1. 自动流程引擎
2. 成本统计
3. 项目状态
4. 分场
5. workflow_task 启用
6. api_call_log 启用

---

## 2. 全局约束

1. 所有文件全部前端直传 TOS，后端不接收文件流，只保存路径。
2. 所有删除都为软删。
3. 所有可配置 prompt 不允许硬编码在业务代码中。
4. AI 返回模板不写在业务代码里，统一放静态资源目录维护。
5. 所有可选参数必须可枚举，避免自由输入。
6. 生图、生视频必须强制带绑定资产主参考图参数。
7. 分镜承接开启时，视频生成必须传上一分镜尾帧参数。

---

## 3. 技术栈

| 层 | 技术 |
|----|------|
| 语言 | Java 17+ |
| 框架 | Spring Boot 3.2.x |
| ORM | MyBatis Plus 3.5.5+ |
| 鉴权 | Sa-Token 1.37+ |
| 缓存 | Redis |
| 数据库 | MySQL 8.0+ |
| 存储 | 火山 TOS（AWS S3 兼容） |
| 文档解析 | 按文件类型解析器（txt/md 纯文本、Word、PDF） |
| AI 调用 | 文本大模型 + Seedance |

---

## 4. 统一约定

### 4.1 响应格式

所有接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1713500000000
}
```

分页统一返回：

```json
{
  "total": 100,
  "page": 1,
  "size": 20,
  "hasNext": true,
  "list": []
}
```

### 4.2 鉴权口径

1. 登录接口免鉴权，其余业务接口都鉴权。
2. token 过期返回 401。
3. 前端收到 401 后的交互处理不属于本后端设计范围。

### 4.3 错误处理口径

| 场景 | 处理 |
|------|------|
| 参数错误 | HTTP 200 + 业务码 |
| 业务冲突 | HTTP 200 + 业务码 |
| 未登录/过期 | HTTP 401 |
| 无权限 | HTTP 403 |
| 系统异常 | HTTP 500 |

### 4.4 静态模板与配置

1. prompt 内容放数据库表 `prompt_config`。
2. AI 返回模板放静态资源目录，供解析器读取。
3. 业务代码中只引用 `prompt_key` / 模板文件名，不直接硬编码模板正文。

---

## 5. 数据模型

### 5.1 sys_user

字段：
- `id`
- `username`
- `password`
- `nickname`
- `avatar_url`
- `status`
- `deleted`
- `create_time`
- `update_time`

说明：
- 支持修改用户名、密码、头像。
- 登录失败次数与锁定时间放 Redis，不落表。

### 5.2 project

字段：
- `id`
- `user_id`
- `name`
- `description`
- `novel_original_tos_path`
- `novel_tos_path`
- `ratio`
- `definition`
- `style`
- `style_desc`
- `deleted`
- `create_time`
- `update_time`

说明：
- 不再保留 `status`
- 不再保留 `workflow_config`
- 不再保留 `execution_lock`
- `style` 只要求可枚举；展示文案与缩略图映射由前端处理，不在后端设计中展开
- `novel_tos_path` 必须返回前端可直接读取的纯文本地址；若 TOS 桶为私有读，则项目详情接口需返回临时可读 URL

### 5.3 episode

字段：
- `id`
- `project_id`
- `title`
- `summary`
- `sort_order`
- `content`
- `asset_ids`
- `parse_status`
- `parse_error`
- `deleted`
- `create_time`
- `update_time`

解析状态枚举：
- `pending`
- `analyzing`
- `success`
- `failed`

### 5.4 shot

字段：
- `id`
- `episode_id`
- `sort_order`
- `prompt`
- `prompt_en`
- `duration`
- `scene_type`
- `camera_move`
- `lines`
- `generated_image_url`
- `generated_video_url`
- `last_frame_url`
- `follow_last`
- `draft_content`
- `prompt_status`
- `image_status`
- `video_status`
- `error_msg`
- `deleted`
- `create_time`
- `update_time`

生成状态枚举：
- `pending`
- `generating`
- `success`
- `failed`

说明：
- 不再保留 `scene_id`
- 不再保留统一 `status`

### 5.5 asset

字段：
- `id`
- `project_id`
- `asset_type`
- `name`
- `description`
- `reference_images`
- `parent_ids`
- `is_sub_asset`
- `draft_content`
- `status`
- `deleted`
- `create_time`
- `update_time`

资产状态枚举：
- `draft`
- `confirmed`
- `deprecated`

说明：
- 保留资产确认状态，因为 PRD 要求“仅已确认资产可被分镜绑定”。
- 允许资产在被分镜绑定后继续修改。
- 删除时仍需校验是否已被分镜绑定；被绑定资产禁止删除。

### 5.6 shot_asset_ref

字段：
- `id`
- `shot_id`
- `asset_id`
- `asset_type`

### 5.7 task

统一任务表，替代旧 `ai_task`。

字段：
- `id`
- `type`
- `project_id`
- `episode_id`
- `shot_id`
- `batch_id`
- `provider_task_id`
- `input_data`
- `result_data`
- `status`
- `result_url`
- `error_msg`
- `progress`
- `next_poll_time`
- `poll_count`
- `last_poll_time`
- `create_time`
- `update_time`

任务类型：
- `script_analyze`
- `shot_split`
- `asset_extract`
- `prompt_gen`
- `image_gen`
- `video_gen`

任务状态：
- `0` 待处理
- `1` 处理中
- `2` 成功
- `3` 失败

### 5.8 prompt_config

字段：
- `id`
- `prompt_key`
- `prompt_text`
- `model`
- `description`
- `version`
- `deleted`
- `create_time`
- `update_time`

---

## 6. 静态资源约定

AI 返回模板目录：

```text
backend/src/main/resources/templates/ai/
├── script_analyze_response.json
├── shot_split_response.json
└── asset_extract_response.json
```

说明：
- 模板文件可直接修改，不需要改业务代码。
- 模板只定义结构，不承载业务流程控制。

---

## 7. 模块设计

### 7.1 用户模块

接口：

| 接口 | 方法 | 路径 |
|------|------|------|
| 登录 | POST | `/api/user/login` |
| 登出 | POST | `/api/user/logout` |
| 获取当前用户信息 | GET | `/api/user/info` |
| 修改密码 | PUT | `/api/user/password` |
| 修改用户名 | PUT | `/api/user/username` |
| 更新头像 | PUT | `/api/user/avatar` |

说明：
- 连续输错 5 次锁定 10 分钟。
- 登录成功返回 `token/userId/username/nickname`。

### 7.2 项目模块

接口：

| 接口 | 方法 | 路径 |
|------|------|------|
| 项目列表 | GET | `/api/project/list` |
| 创建项目 | POST | `/api/project` |
| 项目详情 | GET | `/api/project/{id}` |
| 更新项目 | PUT | `/api/project/{id}` |
| 删除项目 | DELETE | `/api/project/{id}` |

创建项目请求体：

```json
{
  "name": "string",
  "description": "string",
  "novelFile": {
    "fileName": "string",
    "fileKey": "string",
    "fileSize": 123
  },
  "ratio": "16:9",
  "definition": "1080P",
  "style": "2D次元风",
  "styleDesc": "string"
}
```

说明：
- 小说只允许新建时上传。
- 风格创建后固定不可改。
- 项目详情返回的 `novelTosPath` 必须可直接用于剧本预览页读取纯文本内容。

### 7.3 内容模块

接口：

| 接口 | 方法 | 路径 |
|------|------|------|
| 分集列表 | GET | `/api/project/{projectId}/episodes` |
| 剧本分析 | POST | `/api/project/{projectId}/episodes/analyze` |
| 剧本分析状态 | GET | `/api/project/{projectId}/episodes/analyze/status` |
| 手动创建分集 | POST | `/api/project/{projectId}/episodes` |
| 更新分集 | PUT | `/api/episode/{id}` |
| 删除分集 | DELETE | `/api/episode/{id}` |
| 分镜列表 | GET | `/api/episode/{episodeId}/shots` |
| 分镜拆分 | POST | `/api/episode/{episodeId}/shots/split` |
| 手动创建分镜 | POST | `/api/episode/{episodeId}/shots` |
| 更新分镜 | PUT | `/api/shot/{id}` |
| 删除分镜 | DELETE | `/api/shot/{id}` |
| 分镜排序 | PUT | `/api/shot/{id}/sort` |
| 保存草稿 | POST | `/api/shot/{id}/draft` |
| 生成提示词 | POST | `/api/shot/{id}/prompt/generate` |
| 生成图片 | POST | `/api/shot/{id}/image/generate` |
| 生成视频 | POST | `/api/shot/{id}/video/generate` |
| 批量生成提示词 | POST | `/api/episode/{episodeId}/shots/batch/prompt` |
| 批量生成图片 | POST | `/api/episode/{episodeId}/shots/batch/image` |
| 批量生成视频 | POST | `/api/episode/{episodeId}/shots/batch/video` |

关键说明：

1. 剧本分析为异步任务，返回 `taskId`。
2. 剧本分析状态接口至少返回：`parseStatus / parseError / taskId`，便于前端在剧本预览页显示待解析、解析中、解析成功、解析失败和失败原因。
3. 分镜拆分支持 `10/12/15` 秒枚举。
4. 分镜列表支持按 `promptStatus/imageStatus/videoStatus` 过滤。
5. 分镜编辑支持修改提示词、景别、运镜、台词、承接开关、绑定资产。
6. 草稿保存写入 `draft_content`。
7. 视频生成时：
   - 若 `follow_last=1`，自动取上一分镜 `last_frame_url` 作为 `first_frame`
   - 自动传入绑定资产主参考图作为 `reference_image`
8. 视频生成成功后自动截尾帧，上传 TOS，并回写 `last_frame_url`。
9. 批量生成接口建议直接返回 `batchId + taskIds`，前端可做批量轮询和局部状态更新。

生成按钮文案由前端根据后端状态映射，不需要后端单独返回文案字段：
- `pending` -> 生成
- `success` -> 重新生成
- `failed` -> 重试
- `generating` -> 前端 loading

### 7.4 资产模块

接口：

| 接口 | 方法 | 路径 |
|------|------|------|
| 资产列表 | GET | `/api/project/{projectId}/assets` |
| 创建资产 | POST | `/api/project/{projectId}/assets` |
| 更新资产 | PUT | `/api/asset/{id}` |
| 删除资产 | DELETE | `/api/asset/{id}` |
| 确认资产 | PUT | `/api/asset/{id}/confirm` |
| 生成资产图 | POST | `/api/asset/{id}/image/generate` |
| 提取资产 | POST | `/api/project/{projectId}/assets/extract` |
| 提取状态 | GET | `/api/project/{projectId}/assets/extract/status` |
| 重复资产检测 | GET | `/api/project/{projectId}/assets/duplicates` |
| 资产关系树 | GET | `/api/project/{projectId}/assets/tree` |
| 更新资产关系 | PUT | `/api/asset/{id}/relations` |
| 资产引用查询 | GET | `/api/asset/{id}/references` |

关键说明：

1. 提取资产按分集执行，异步返回 `taskId`。
2. 重复资产按名称/描述相似度大于 80% 聚合。
3. 父子资产关系支持多对多。
4. 确认资产前必须至少有 1 张参考图。
5. 仅已确认资产可被分镜绑定。
6. 资产被分镜绑定后：
   - 允许修改名称、描述、参考图、关系
   - 禁止删除

### 7.5 统一任务模块

接口：

| 接口 | 方法 | 路径 |
|------|------|------|
| 查询任务状态 | GET | `/api/task/{id}` |
| 批量任务状态查询 | POST | `/api/task/batch/status` |

说明：
- 所有异步操作都落 `task` 表。
- 前端统一按 `taskId` 轮询。
- 不再保留旧 `ai_task` 接口。
- `GET /api/task/{id}` 建议统一返回：`id / type / status / progress / errorMsg / resultData / resultUrl / batchId`。
- `POST /api/task/batch/status` 建议支持按 `taskIds` 或 `batchId` 查询，便于批量生成场景使用。

### 7.6 存储模块

接口：

| 接口 | 方法 | 路径 |
|------|------|------|
| 获取预签名上传信息 | POST | `/api/storage/tos/presign` |
| 上传完成通知 | POST | `/api/storage/tos/complete` |

说明：
- 小说原始文件、小说纯文本、头像、资产图、生成图、生成视频、尾帧都走 TOS。
- 按文件类型解析器负责将 txt/word/pdf/md 提取为纯文本，纯文本结果另存 TOS。
- 若前端需要直接预览纯文本，后端需保证纯文本对象具备可读访问方式，或在详情接口中返回临时签名读取 URL。

---

## 8. 解析与生成规则

### 8.1 小说解析

流程：
1. 前端上传原始小说到 TOS
2. 后端读取原始文件
3. 按文件类型解析器提取纯文本，忽略图片和格式
4. 纯文本再上传 TOS
5. `project.novel_tos_path` 保存纯文本路径

### 8.2 剧本分析

1. 读取 `project.novel_tos_path`
2. 按 `prompt_config.prompt_key=script_split` 获取 prompt
3. 读取静态返回模板
4. 调大模型拆分为多个分集
5. 写入 `episode`
6. 更新 `parse_status`

### 8.3 分镜拆分

1. 读取分集内容
2. 按时长规则和 `shot_split` prompt 调模型
3. 写入 `shot`
4. 默认 `follow_last=1`

### 8.4 视频承接

1. 查询上一分镜
2. 若当前分镜 `follow_last=1` 且上一分镜有 `last_frame_url`
3. 则将其传给视频模型的 `first_frame`
4. 同时传绑定资产主参考图

---

## 9. 非功能说明

1. 接口文档可开 SpringDoc，但不在主业务设计中展开。
2. 本期不写流程恢复、工作流锁、成本日志等二三期内容。
3. 代码中关键业务逻辑要求中文注释。

---

## 10. 待确认事项

1. 项目风格字段最终是否直接存中文枚举，还是存英文码值并单独维护映射。
2. 资产被绑定后允许修改参考图，这会影响已生成分镜的一致性，是否需要记录“修改后仅影响后续生成”的说明。
3. 批量生成接口是同步批量建 task，还是单接口返回 batchId 供前端统一轮询。
4. 剧本分析、分镜拆分、资产提取失败时错误信息展示粒度是否需要结构化字段。
