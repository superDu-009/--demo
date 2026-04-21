# AI漫剧生产平台 — 前端系统设计规格书

## 文档信息

| 项目 | 内容 |
|------|------|
| 产品名 | AI漫剧生产平台 |
| 版本 | v1.0 |
| 基于 | PRD v1.1 终版 + 后端系分 DESIGN_BACKEND_老克_v1.2 |
| 作者 | 小欧（前端架构师） |
| 评审人 | 蓝烟老师、老克、阿典 |
| 创建日期 | 2026-04-19 |
| 状态 | 待评审 |

---

## 概念图

![AI漫剧生产平台概念界面图](https://ark-acg-cn-beijing.tos-cn-beijing.volces.com/doubao-seedream-5-0/021776590329076b060a07fba38d5479db0751ac158bec52e1bff_0.png?X-Tos-Algorithm=TOS4-HMAC-SHA256&X-Tos-Credential=AKLTYWJkZTExNjA1ZDUyNDc3YzhjNTM5OGIyNjBhNDcyOTQ%2F20260419%2Fcn-beijing%2Ftos%2Frequest&X-Tos-Date=20260419T091934Z&X-Tos-Expires=86400&X-Tos-Signature=4e97fd73ba08d1fb826999346041b35606d9e4eee8e5219a462be0a347885b0a&X-Tos-SignedHeaders=host)

---

## 目录

- [1. 技术栈](#1-技术栈)
- [2. 项目结构设计](#2-项目结构设计)
- [3. 路由设计](#3-路由设计)
- [4. 核心页面设计](#4-核心页面设计)
  - [4.1 登录页](#41-登录页)
  - [4.2 项目列表页](#42-项目列表页)
  - [4.3 项目详情页（Tab 容器）](#43-项目详情页tab-容器)
  - [4.4 资产库（AssetLibrary）](#44-资产库assetlibrary)
  - [4.5 流程编辑器（WorkflowEditor）](#45-流程编辑器workfloweditor)
  - [4.6 分镜工作台（ShotWorkbench）](#46-分镜工作台shotworkbench)
  - [4.7 API 消耗看板（ApiCost）](#47-api-消耗看板apicost)
- [5. 接口对接层](#5-接口对接层)
  - [5.1 Axios 实例与拦截器](#51-axios-实例与拦截器)
  - [5.2 API 模块划分](#52-api-模块划分)
- [6. TypeScript 类型定义](#6-typescript-类型定义)
  - [6.1 通用类型](#61-通用类型)
  - [6.2 业务类型](#62-业务类型)
- [7. 状态管理（Pinia）](#7-状态管理pinia)
  - [7.1 模块划分](#71-模块划分)
  - [7.2 核心模块详细设计](#72-核心模块详细设计)
- [8. Composables 设计](#8-composables-设计)
  - [8.1 useTaskPolling（任务轮询）](#81-usetaskpolling任务轮询)
  - [8.2 useTosUpload（TOS 直传）](#82-usetosuploadtos-直传)
- [9. 常量与工具](#9-常量与工具)
- [10. UI 规范](#10-ui-规范)
- [11. 性能优化策略](#11-性能优化策略)
- [12. 错误处理策略](#12-错误处理策略)
- [13. 安全策略](#13-安全策略)
- [14. 开发环境与工程化](#14-开发环境与工程化)
- [15. Sprint 任务拆分](#15-sprint-任务拆分)

---

## 1. 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | Vue 3 (Composition API) | 3.4+ | 核心框架 |
| 语言 | TypeScript | 5.3+ | 类型安全 |
| 构建 | Vite | 5.x | 构建工具 |
| UI 组件库 | Element Plus | 2.7+ | 组件库 |
| 拖拽 | SortableJS + vue.draggable.next | 1.15+ / 2.24+ | 流程编辑器拖拽 |
| 状态管理 | Pinia | 2.1+ | 全局状态 |
| HTTP | Axios | 1.6+ | 网络请求 |
| 路由 | Vue Router | 4.2+ | 路由管理 |
| 虚拟滚动 | vue-virtual-scroller | 2.0+ | 分镜列表性能优化 |
| 图标 | @element-plus/icons-vue | 2.3+ | 图标库 |
| 日期 | dayjs | 1.11+ | 日期处理 |

---

## 2. 项目结构设计

```
frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── main.ts                          # 入口文件
│   ├── App.vue                          # 根组件
│   ├── vite-env.d.ts                    # Vite 类型声明
│   │
│   ├── api/                             # API 请求层
│   │   ├── index.ts                     # Axios 实例 + 拦截器
│   │   ├── user.ts                      # 用户模块接口
│   │   ├── project.ts                   # 项目模块接口
│   │   ├── asset.ts                     # 资产模块接口
│   │   ├── content.ts                   # 内容模块接口 (分集/分场/分镜)
│   │   ├── workflow.ts                  # 流程引擎接口
│   │   ├── ai.ts                        # AI 任务接口
│   │   └── tos.ts                       # TOS 存储接口
│   │
│   ├── types/                           # TypeScript 类型定义
│   │   ├── common.ts                    # 通用类型 (Result, PageResult, etc.)
│   │   ├── user.ts                      # 用户类型
│   │   ├── project.ts                   # 项目类型
│   │   ├── asset.ts                     # 资产类型
│   │   ├── content.ts                   # 内容类型 (Episode, Scene, Shot)
│   │   ├── workflow.ts                  # 流程类型
│   │   ├── ai.ts                        # AI 任务类型
│   │   ├── tos.ts                       # TOS 类型
│   │   └── index.ts                     # 统一导出
│   │
│   ├── stores/                          # Pinia 状态管理
│   │   ├── index.ts                     # Store 入口
│   │   ├── auth.ts                      # 认证模块
│   │   ├── project.ts                   # 项目状态
│   │   ├── asset.ts                     # 资产状态
│   │   ├── content.ts                   # 内容状态
│   │   ├── workflow.ts                  # 流程状态
│   │   └── ai.ts                        # AI 任务状态
│   │
│   ├── views/                           # 页面组件
│   │   ├── Login.vue                    # 登录页
│   │   ├── ProjectList.vue              # 项目列表
│   │   ├── ProjectDetail.vue            # 项目详情页（Tab 容器）
│   │   ├── NotFound.vue                 # 404 页
│   │   └── tabs/                        # 项目详情子 Tab 页面
│   │       ├── AssetLibrary.vue         # 资产库
│   │       ├── WorkflowEditor.vue       # 流程编辑器
│   │       ├── ShotWorkbench.vue        # 分镜工作台
│   │       └── ApiCost.vue              # API 消耗看板
│   │
│   ├── components/                      # 通用组件
│   │   ├── Layout/
│   │   │   ├── AppHeader.vue            # 顶部导航
│   │   │   └── AppSidebar.vue           # 侧边栏
│   │   ├── Asset/
│   │   │   ├── AssetCard.vue            # 资产卡片
│   │   │   ├── AssetForm.vue            # 资产表单（新建/编辑）
│   │   │   ├── ImageUploader.vue        # 图片上传组件（TOS 直传）
│   │   │   └── AudioPlayer.vue          # 声音资产播放器
│   │   ├── Workflow/
│   │   │   ├── WorkflowNode.vue         # 流程步骤卡片
│   │   │   ├── WorkflowStatus.vue       # 流程进度展示
│   │   │   └── WorkflowProgress.vue     # 进度条组件
│   │   ├── Shot/
│   │   │   ├── ShotCard.vue             # 分镜卡片
│   │   │   ├── ShotPreview.vue          # 分镜预览（图片/视频）
│   │   │   ├── ShotReviewDialog.vue     # 审核弹窗
│   │   │   └── AssetBindPanel.vue       # 资产绑定面板
│   │   └── Common/
│   │       ├── StatusTag.vue            # 状态标签（通用颜色映射）
│   │       ├── ConfirmDialog.vue        # 确认弹窗
│   │       └── EmptyState.vue           # 空状态组件
│   │
│   ├── composables/                     # 组合式函数
│   │   ├── useTaskPolling.ts            # 任务轮询（3s 间隔）
│   │   ├── useTosUpload.ts              # TOS 预签名直传
│   │   ├── useWorkflowStatus.ts         # 流程状态轮询
│   │   └── useConfirm.ts                # 确认操作封装
│   │
│   ├── router/                          # 路由配置
│   │   ├── index.ts                     # 路由定义
│   │   └── guards.ts                    # 路由守卫
│   │
│   ├── constants/                       # 常量定义
│   │   ├── status.ts                    # 状态枚举 + 颜色映射
│   │   ├── workflow.ts                  # 流程步骤定义
│   │   └── errors.ts                    # 错误码映射
│   │
│   ├── utils/                           # 工具函数
│   │   ├── request.ts                   # Axios 封装
│   │   ├── storage.ts                   # localStorage 封装
│   │   ├── format.ts                    # 日期/数字格式化
│   │   └── validators.ts                # 表单校验规则
│   │
│   ├── styles/                          # 全局样式
│   │   ├── index.scss                   # 样式入口
│   │   ├── variables.scss               # SCSS 变量
│   │   ├── reset.scss                   # 重置样式
│   │   └── dark-theme.scss              # 暗色主题定制
│   │
│   └── assets/                          # 静态资源
│       └── images/
│
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── .env.development                     # 开发环境变量
├── .env.production                      # 生产环境变量
├── .env.local                           # 本地覆盖（不提交）
└── eslint.config.ts
```

---

## 3. 路由设计

```typescript
// router/index.ts
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/components/Layout/AppLayout.vue'),
    redirect: '/projects',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'projects',
        name: 'ProjectList',
        component: () => import('@/views/ProjectList.vue'),
        meta: { title: '项目列表' }
      },
      {
        path: 'projects/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/ProjectDetail.vue'),
        meta: { title: '项目详情' },
        redirect: { name: 'AssetLibrary' },
        children: [
          {
            path: 'assets',
            name: 'AssetLibrary',
            component: () => import('@/views/tabs/AssetLibrary.vue'),
            meta: { title: '资产库' }
          },
          {
            path: 'workflow',
            name: 'WorkflowEditor',
            component: () => import('@/views/tabs/WorkflowEditor.vue'),
            meta: { title: '流程编辑器' }
          },
          {
            path: 'shots',
            name: 'ShotWorkbench',
            component: () => import('@/views/tabs/ShotWorkbench.vue'),
            meta: { title: '分镜工作台' }
          },
          {
            path: 'cost',
            name: 'ApiCost',
            component: () => import('@/views/tabs/ApiCost.vue'),
            meta: { title: 'API 消耗' }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '404' }
  }
]
```

**路由守卫逻辑（guards.ts）：**

```typescript
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 未登录且需要认证 → 跳转登录
  if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  // 已登录访问登录页 → 跳转首页
  if (to.name === 'Login' && authStore.isLoggedIn) {
    return next({ name: 'ProjectList' })
  }

  next()
})
```

---

## 4. 核心页面设计

### 4.1 登录页

**组件路径：** `views/Login.vue`

**布局：** 居中卡片，暗色背景

**功能：**
- 用户名 / 密码表单（Element Plus `el-form` + `el-input`）
- 校验规则：用户名必填，密码必填（最少6字符）
- 登录成功后存储 Token 到 `localStorage` + Pinia `auth` store
- 支持 `redirect` 参数，登录后跳回原页面

**接口：**
```
POST /api/user/login
请求: { username: string, password: string }
响应: Result<{ token: string, userId: number, username: string, nickname: string }>
```

**关键逻辑：**
```typescript
const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await userApi.login(form)
    authStore.setToken(res.data.token)
    authStore.setUserInfo({
      id: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname
    })
    ElMessage.success('登录成功')
    router.push(redirect.value || '/')
  } catch (error) {
    // 拦截器已处理错误提示
  } finally {
    loading.value = false
  }
}
```

---

### 4.2 项目列表页

**组件路径：** `views/ProjectList.vue`

**布局：**
- 顶部：搜索栏 + 「新建项目」按钮
- 主体：Element Plus `el-table` 或卡片网格（默认卡片视图，可切换表格视图）
- 分页：`el-pagination`

**数据展示字段：**
| 字段 | 来源 | 展示方式 |
|------|------|----------|
| 项目名称 | `name` | 可点击进入详情 |
| 描述 | `description` | 截断显示 |
| 状态 | `status` | `StatusTag` 组件 (草稿/进行中/已完成) |
| 执行锁 | `executionLock` | 🔒 图标标识 |
| 创建时间 | `createTime` | dayjs 格式化 |

**操作：**
| 操作 | 条件 | 接口 |
|------|------|------|
| 新建 | 始终可用 | `POST /api/project` |
| 编辑 | 草稿状态 | `PUT /api/project/{id}` |
| 删除 | 非执行中 (`executionLock=0`) | `DELETE /api/project/{id}` |
| 查看详情 | 始终可用 | 路由跳转 `/projects/{id}` |

**新建项目弹窗：**
- 项目名称（必填，最大200字符）
- 项目描述（可选，Textarea）
- 小说文件上传（`ImageUploader` 组件调用 TOS 预签名直传）

---

### 4.3 项目详情页（Tab 容器）

**组件路径：** `views/ProjectDetail.vue`

**布局：**
- 顶部面包屑：项目列表 > {项目名称}
- 左侧导航（垂直 Tab）或顶部 Tab 栏
- 右侧内容区：`<router-view>` 加载子 Tab 页面

**Tab 列表：**
| Tab 名称 | 路由 | 组件 |
|----------|------|------|
| 资产库 | `/projects/:id/assets` | AssetLibrary |
| 流程编辑器 | `/projects/:id/workflow` | WorkflowEditor |
| 分镜工作台 | `/projects/:id/shots` | ShotWorkbench |
| API 消耗 | `/projects/:id/cost` | ApiCost |

**状态管理：**
- 进入页面时调用 `projectStore.fetchProjectDetail(id)`
- 项目基本信息固定在顶部展示栏（名称、状态、执行锁标识）

---

### 4.4 资产库（AssetLibrary）

**组件路径：** `views/tabs/AssetLibrary.vue`

**布局：**
- 顶部：资产类型 Tab 栏（角色 / 场景 / 物品 / 声音）
- 工具栏：「新建资产」按钮 + 搜索框
- 主体：卡片网格布局（`el-row` + `el-col`，响应式 4/3/2 列）

**资产卡片（AssetCard）：**
```
┌─────────────────────────┐
│ [主参考图]              │
│                         │
│ 资产名称                │
│ 类型: character         │
│ 状态: ● 已确认 (绿色)    │
│                         │
│ [编辑] [删除] [确认]    │
└─────────────────────────┘
```

**资产表单（AssetForm Dialog）：**
| 字段 | 类型 | 说明 |
|------|------|------|
| 名称 | `el-input` | 必填 |
| 描述 | `el-input` (textarea) | AI 描述文本，支持手动编辑 |
| 类型 | `el-select` | character/scene/prop/voice（新建时固定） |
| 参考图 | `ImageUploader` | 多张上传，可拖拽排序，标记主图 |
| 风格预设 | `el-form` 子项 | artStyle + colorTone（可选） |

**TOS 直传流程（图片上传）：**
```
1. 用户选择图片 → ImageUploader 组件
2. 调用 POST /api/tos/presign 获取预签名 URL
   请求: { fileName, contentType: 'image/png', projectDir: 'projects/{id}/assets' }
3. 用户端 PUT 直传 TOS（携带预签名 URL）
4. 调用 POST /api/tos/complete 通知后端
   请求: { key, projectId, fileType: 'asset', metadata }
5. 后端更新数据库 + 前端更新 reference_images 列表
```

**声音资产特殊处理：**
- 使用 `AudioPlayer` 组件（`<audio>` 标签 + Element Plus 控制条）
- 支持播放/暂停/音量调节

**引用保护：**
- 删除资产前检查 `GET /api/asset/{id}/references`
- 如有关联分镜，弹出确认框展示引用关系
- 错误码 `40901` → Toast 提示「资产已被分镜引用，不可删除」

---

### 4.5 流程编辑器（WorkflowEditor）

**组件路径：** `views/tabs/WorkflowEditor.vue`

**技术：** SortableJS + `vue.draggable.next`

**布局：**
- 顶部：「保存流程配置」按钮 + 「开始执行」按钮
- 主体：垂直排列的步骤卡片列表，可拖拽排序
- 底部：流程执行状态面板（执行中时展示）

**步骤卡片（WorkflowNode）：**
```
┌────────────────────────────────────┐
│ ☰  [图标] 导入并拆分剧本 (import)  │
│                                    │
│  ☑ 启用   ☑ 需要人工审核           │
│  状态: ● 未执行                    │
│  ─────────────────────────────     │
│  配置: 无额外配置                  │
└────────────────────────────────────┘
```

**步骤类型清单：**

| stepType | 标签 | 图标 | 说明 |
|----------|------|------|------|
| import | 导入并拆分剧本 | 📥 | 读取小说 TOS 路径，AI 拆分章节 |
| asset_extract | 资产提取 | 🔍 | AI 辅助提取角色/场景资产 |
| shot_gen | 分镜提示词生成 | ✏️ | 强模型生成 prompt 中英文 |
| image_gen | 首帧生图 | 🖼️ | 根据 prompt + 资产参考图生图 |
| video_gen | 视频生成 | 🎬 | Seedance 生成视频 |
| export | 合并导出 | 📦 | FFmpeg 合并最终视频 |

**SortableJS 配置：**
```typescript
import Draggable from 'vuedraggable'

// 配置项
const sortableOptions = {
  animation: 200,         // 拖拽动画时长 ms
  handle: '.drag-handle',  // 拖拽手柄（避免误触配置项）
  ghostClass: 'sortable-ghost',
  chosenClass: 'sortable-chosen',
  dragClass: 'sortable-drag'
}
```

**数据模型：**
```typescript
interface WorkflowStep {
  stepType: StepType
  enabled: boolean
  review: boolean
  config?: Record<string, any>
}

interface WorkflowConfig {
  steps: WorkflowStep[]
}
```

**保存流程配置：**
```
PUT /api/project/{id}/workflow
请求体:
{
  version: number,          // 乐观锁版本号
  workflowConfig: { steps: WorkflowStep[] },
  stylePreset: { artStyle?: string, colorTone?: string }
}
```

**流程执行与监控：**
1. 点击「开始执行」→ `POST /api/project/{id}/workflow/start`
2. 启动 3s 轮询 `GET /api/project/{id}/workflow/status`
3. 实时更新步骤状态卡片（颜色 + 进度条）
4. 遇到审核节点 → 弹出通知 → 展示「审核通过 / 打回」按钮
5. 审核操作 → `POST /api/project/{id}/workflow/review`
6. 执行完成 → 提示 + 刷新页面

**执行状态颜色映射：**

| 状态 | 颜色 | 图标 |
|------|------|------|
| 0-未执行 | 灰色 | ⬜ |
| 1-执行中 | 蓝色（loading 动画） | 🔵 ⟳ |
| 2-成功 | 绿色 | ✅ |
| 3-失败 | 红色 | ❌ + 错误信息 |
| 4-待审核 | 橙色（脉冲动画） | 🟡 ⏸️ |

**进度条设计：**
- 整体进度：`overallProgress` (0-100)
- 步骤级进度：每个步骤卡片内嵌 `el-progress`
- 当前处理信息：`currentDetail` 文本展示（如"正在生成第45/100个分镜"）

---

### 4.6 分镜工作台（ShotWorkbench）

**组件路径：** `views/tabs/ShotWorkbench.vue`

**布局（左右分栏）：**
- 左侧（宽度 280px）：分集/分场折叠树
  - 使用 `el-tree` 或自定义递归组件
  - 每个节点展示状态摘要（分镜总数 / 已通过 / 已打回）
  - 点击节点过滤右侧分镜列表
- 右侧（自适应宽度）：分镜卡片列表
  - 顶部工具栏：状态筛选 + 「批量通过」+「批量打回」+「重新生成」
  - 列表区域：`vue-virtual-scroller` 虚拟滚动（防卡顿）
  - 底部：分页控件

**分镜卡片（ShotCard）：**
```
┌──────────────────────────────────────┐
│ #1  [状态标签]                       │
│ ┌────────────────┐                   │
│ │ [图片预览]     │  提示词: xxx       │
│ │ [视频预览]     │  版本: v2          │
│ │ [生成中遮罩]   │  尝试次数: 3       │
│ └────────────────┘                   │
│ 绑定资产: [角色A] [场景B] [+ 添加]    │
│ ───────────────────────────────      │
│ [生成] [预览] [通过] [打回] [重新生成] │
└──────────────────────────────────────┘
```

**分镜状态流转：**
```
0-待处理 → 1-生成中 → 2-待审核 → 3-已通过 / 4-已打回 → 5-已完成
```

**状态颜色映射：**

| 状态 | 颜色 | Element Plus Tag Type |
|------|------|----------------------|
| 0-待处理 | 灰色 | info |
| 1-生成中 | 蓝色 | primary (带 loading 图标) |
| 2-待审核 | 橙色 | warning |
| 3-已通过 | 绿色 | success |
| 4-已打回 | 红色 | danger |
| 5-已完成 | 深绿 | success (深色) |

**批量操作：**
```typescript
interface BatchReviewRequest {
  shotIds: number[]
  action: 'approve' | 'reject'
  comment?: string  // reject 时必填
}
// POST /api/shot/batch-review
```

**AI 任务轮询：**
- 当分镜 `status = 1（生成中）` 时，自动启动轮询
- 调用 `GET /api/ai/task/latest?shotId={id}`
- 使用 `useTaskPolling` composable，3s 间隔
- 任务完成时自动刷新分镜数据

**资产绑定：**
- 点击「+ 添加」弹出 AssetBindPanel
- 从项目资产库中选择（按类型分组）
- 提交 `POST /api/shot/{shotId}/assets`
- 解绑 `DELETE /api/shot/{shotId}/assets/{assetId}`

**虚拟滚动配置：**
```typescript
import { RecycleScroller } from 'vue-virtual-scroller'

// 配置
<RecycleScroller
  :items="filteredShots"
  :item-size="280"
  key-field="id"
  v-slot="{ item }"
>
  <ShotCard :shot="item" />
</RecycleScroller>
```

---

### 4.7 API 消耗看板（ApiCost）

**组件路径：** `views/tabs/ApiCost.vue`

**布局：**
- 顶部：日期范围选择器 + 刷新按钮
- 概览卡片行：总调用次数 / 总费用 / 平均单次费用 / 失败率
- 图表区（ECharts 可选）：按天费用趋势、按提供商分布
- 明细表格：调用记录列表（分页）

**接口：**
```
GET /api/ai/cost-report
查询参数: startDate, endDate (可选)
```

---

## 5. 接口对接层

### 5.1 Axios 实例与拦截器

**文件：** `api/index.ts`

```typescript
import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import { ERROR_MESSAGES, ERROR_ACTION } from '@/constants/errors'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// ===== 请求拦截器 =====
instance.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ===== 响应拦截器 =====
instance.interceptors.response.use(
  (response) => {
    const res = response.data

    // HTTP 200 但业务码非 0 → 业务错误
    if (res.code !== 0) {
      handleBusinessError(res.code, res.message)
      return Promise.reject(new BusinessError(res.code, res.message))
    }

    return res
  },
  (error: AxiosError<BusinessResponse>) => {
    const status = error.response?.status
    const res = error.response?.data

    switch (status) {
      case 401:
        // 未登录或 Token 过期 → 清除状态 → 跳转登录
        const authStore = useAuthStore()
        authStore.clearAuth()
        ElMessage.error('登录已过期，请重新登录')
        router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
        break

      case 403:
        ElMessage.error('无操作权限')
        break

      case 429:
        ElMessage.warning('请求频率过高，请稍后重试')
        break

      default:
        ElMessage.error(res?.message || '网络异常，请稍后重试')
    }

    return Promise.reject(error)
  }
)

// ===== 业务错误处理 =====
function handleBusinessError(code: number, message: string) {
  const action = ERROR_ACTION[code]

  switch (action) {
    case 'toast':
      ElMessage.error(message)
      break
    case 'form':
      // 由调用方处理表单高亮
      break
    case 'redirect-login':
      const authStore = useAuthStore()
      authStore.clearAuth()
      router.push({ name: 'Login' })
      break
    case 'retry-presign':
      // TOS 预签名过期 → 自动重试
      ElMessage.warning('上传链接已过期，正在重新获取...')
      break
    case 'disabled-btn':
      // 禁用对应按钮（由调用方处理）
      ElMessage.warning(message)
      break
    case 'global-error':
      // 展示全局错误页
      ElMessage.error('服务器内部错误，请稍后重试')
      break
    default:
      ElMessage.error(message)
  }
}

export default instance

// ===== 类型定义 =====
interface BusinessResponse {
  code: number
  message: string
  data: any
  timestamp: number
}

class BusinessError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'BusinessError'
  }
}
```

### 5.2 API 模块划分

**user.ts**
```typescript
import request from '@/api'
import type { LoginRequest, LoginResult, UserInfoVO } from '@/types'

export const userApi = {
  login: (data: LoginRequest) =>
    request.post<never, ApiResponse<LoginResult>>('/user/login', data),

  logout: () =>
    request.post('/user/logout'),

  getInfo: () =>
    request.get<never, ApiResponse<UserInfoVO>>('/user/info')
}
```

**project.ts**
```typescript
import request from '@/api'
import type {
  ProjectCreateRequest,
  ProjectUpdateRequest,
  ProjectVO,
  WorkflowConfigRequest,
  WorkflowStatusVO
} from '@/types'

export const projectApi = {
  list: (params?: { page?: number; size?: number }) =>
    request.get('/project/list', { params }),

  create: (data: ProjectCreateRequest) =>
    request.post<never, ApiResponse<number>>('/project', data),

  getDetail: (id: number) =>
    request.get<never, ApiResponse<ProjectVO>>(`/project/${id}`),

  update: (id: number, data: ProjectUpdateRequest) =>
    request.put(`/project/${id}`, data),

  delete: (id: number) =>
    request.delete(`/project/${id}`),

  saveWorkflow: (id: number, data: WorkflowConfigRequest) =>
    request.put(`/project/${id}/workflow`, data),

  startWorkflow: (id: number) =>
    request.post(`/project/${id}/workflow/start`),

  getWorkflowStatus: (id: number) =>
    request.get<never, ApiResponse<WorkflowStatusVO>>(`/project/${id}/workflow/status`),

  reviewWorkflow: (id: number, data: { stepType: string; action: string; comment?: string }) =>
    request.post(`/project/${id}/workflow/review`, data),

  stopWorkflow: (id: number) =>
    request.post(`/project/${id}/workflow/stop`),

  getShots: (projectId: number, params?: { sceneId?: number; status?: number; page?: number; size?: number }) =>
    request.get(`/project/${projectId}/shots`, { params })
}
```

**asset.ts**
```typescript
import request from '@/api'
import type { AssetCreateRequest, AssetUpdateRequest, AssetVO, ShotReferenceVO } from '@/types'

export const assetApi = {
  list: (projectId: number, assetType?: string) =>
    request.get<never, ApiResponse<AssetVO[]>>(`/project/${projectId}/assets`, {
      params: assetType ? { assetType } : undefined
    }),

  create: (projectId: number, data: AssetCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/project/${projectId}/assets`, data),

  update: (id: number, data: AssetUpdateRequest) =>
    request.put(`/asset/${id}`, data),

  delete: (id: number) =>
    request.delete(`/asset/${id}`),

  confirm: (id: number) =>
    request.put(`/asset/${id}/confirm`),

  getReferences: (assetId: number, params?: { page?: number; size?: number }) =>
    request.get<never, ApiResponse<PageResult<ShotReferenceVO>>>(`/asset/${assetId}/references`, { params })
}
```

**content.ts**
```typescript
import request from '@/api'
import type {
  EpisodeCreateRequest, SceneCreateRequest, ShotCreateRequest,
  ShotUpdateRequest, BatchReviewRequest, BatchReviewResult,
  EpisodeVO, SceneVO, ShotVO
} from '@/types'

export const contentApi = {
  // 分集
  listEpisodes: (projectId: number) =>
    request.get<never, ApiResponse<EpisodeVO[]>>(`/project/${projectId}/episodes`),

  createEpisode: (projectId: number, data: EpisodeCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/project/${projectId}/episodes`, data),

  updateEpisode: (id: number, data: Partial<EpisodeCreateRequest>) =>
    request.put(`/episode/${id}`, data),

  deleteEpisode: (id: number) =>
    request.delete(`/episode/${id}`),

  // 分场
  listScenes: (episodeId: number) =>
    request.get<never, ApiResponse<SceneVO[]>>(`/episode/${episodeId}/scenes`),

  createScene: (episodeId: number, data: SceneCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/episode/${episodeId}/scenes`, data),

  updateScene: (id: number, data: Partial<SceneCreateRequest>) =>
    request.put(`/scene/${id}`, data),

  deleteScene: (id: number) =>
    request.delete(`/scene/${id}`),

  // 分镜
  listShots: (sceneId: number, params?: { page?: number; size?: number; status?: number }) =>
    request.get<never, ApiResponse<PageResult<ShotVO>>>(`/scene/${sceneId}/shots`, { params }),

  createShot: (sceneId: number, data: ShotCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/scene/${sceneId}/shots`, data),

  updateShot: (id: number, data: ShotUpdateRequest) =>
    request.put(`/shot/${id}`, data),

  deleteShot: (id: number) =>
    request.delete(`/shot/${id}`),

  batchReview: (data: BatchReviewRequest) =>
    request.post<never, ApiResponse<BatchReviewResult>>('/shot/batch-review', data),

  bindAsset: (shotId: number, data: { assetId: number; assetType: string }) =>
    request.post(`/shot/${shotId}/assets`, data),

  unbindAsset: (shotId: number, assetId: number) =>
    request.delete(`/shot/${shotId}/assets/${assetId}`)
}
```

**ai.ts**
```typescript
import request from '@/api'
import type { AiTaskVO } from '@/types'

export const aiApi = {
  getTaskStatus: (taskId: number) =>
    request.get<never, ApiResponse<AiTaskVO>>(`/ai/task/${taskId}`),

  getLatestTask: (shotId: number) =>
    request.get<never, ApiResponse<AiTaskVO>>('/ai/task/latest', { params: { shotId } }),

  getCostReport: (params?: { startDate?: string; endDate?: string }) =>
    request.get('/ai/cost-report', { params })
}
```

**tos.ts**
```typescript
import request from '@/api'
import type { PresignRequest, PresignResult, TosCompleteRequest } from '@/types'

export const tosApi = {
  presign: (data: PresignRequest) =>
    request.post<never, ApiResponse<PresignResult>>('/tos/presign', data),

  complete: (data: TosCompleteRequest) =>
    request.post('/tos/complete', data)
}
```

---

## 6. TypeScript 类型定义

### 6.1 通用类型

**文件：** `types/common.ts`

```typescript
// 统一响应格式
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页响应
export interface PageResult<T = any> {
  total: number
  page: number
  size: number
  hasNext: boolean
  list: T[]
}

// 状态枚举
export enum ProjectStatus {
  Draft = 0,       // 草稿
  InProgress = 1,  // 进行中
  Completed = 2    // 已完成
}

export enum AssetStatus {
  Draft = 0,       // 草稿
  Confirmed = 1,   // 已确认
  Deprecated = 2   // 已废弃
}

export enum AssetType {
  Character = 'character',
  Scene = 'scene',
  Prop = 'prop',
  Voice = 'voice'
}

export enum EpisodeStatus {
  Pending = 0,
  InProgress = 1,
  Completed = 2
}

export enum ShotStatus {
  Pending = 0,       // 待处理
  Generating = 1,    // 生成中
  WaitingReview = 2, // 待审核
  Approved = 3,      // 已通过
  Rejected = 4,      // 已打回
  Completed = 5      // 已完成
}

export enum WorkflowTaskStatus {
  NotStarted = 0,
  Running = 1,
  Success = 2,
  Failed = 3,
  WaitingReview = 4
}

export enum AiTaskStatus {
  Submitting = 0,
  Processing = 1,
  Success = 2,
  Failed = 3
}

// 流程步骤类型
export type StepType =
  | 'import'
  | 'asset_extract'
  | 'shot_gen'
  | 'image_gen'
  | 'video_gen'
  | 'export'
```

### 6.2 业务类型

**文件：** `types/user.ts`
```typescript
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  nickname: string
}

export interface UserInfoVO {
  id: number
  username: string
  nickname: string
  status: number
}
```

**文件：** `types/project.ts`
```typescript
import type { ProjectStatus } from './common'

export interface ProjectCreateRequest {
  name: string
  description?: string
  novelTosPath?: string
}

export interface ProjectUpdateRequest {
  name?: string
  description?: string
}

export interface ProjectVO {
  id: number
  userId: number
  name: string
  description: string | null
  novelTosPath: string | null
  workflowConfig: WorkflowConfig | null
  stylePreset: StylePreset | null
  status: ProjectStatus
  executionLock: number
  version: number
  createTime: string
  updateTime: string
}

export interface WorkflowConfig {
  steps: WorkflowStep[]
}

export interface WorkflowStep {
  stepType: StepType
  enabled: boolean
  review: boolean
  config?: Record<string, any>
}

export interface StylePreset {
  artStyle?: string
  colorTone?: string
}

export interface WorkflowConfigRequest {
  version: number
  workflowConfig: WorkflowConfig
  stylePreset?: StylePreset
}

export interface WorkflowStatusVO {
  executionLock: number
  currentStep: string | null
  currentEpisodeId: number | null
  currentEpisodeTitle: string | null
  totalEpisodes: number
  overallProgress: number
  totalShots: number
  processedShots: number
  estimatedRemainingSeconds: number
  steps: WorkflowStepStatus[]
}

export interface WorkflowStepStatus {
  stepType: string
  stepOrder: number
  status: number
  progress: number
  currentDetail: string
  errorMsg: string | null
  reviewComment: string | null
}
```

**文件：** `types/asset.ts`
```typescript
import type { AssetStatus, AssetType } from './common'

export interface AssetCreateRequest {
  assetType: AssetType
  name: string
  description?: string
  referenceImages?: string[]
  stylePreset?: Record<string, any>
}

export interface AssetUpdateRequest {
  name?: string
  description?: string
  referenceImages?: string[]
  stylePreset?: Record<string, any>
}

export interface AssetVO {
  id: number
  projectId: number
  assetType: AssetType
  name: string
  description: string | null
  referenceImages: string[] | null  // 第一个为主图
  stylePreset: Record<string, any> | null
  status: AssetStatus
  createTime: string
  updateTime: string
}

export interface ShotReferenceVO {
  shotId: number
  sceneId: number
  episodeId: number
  shotStatus: number
}
```

**文件：** `types/content.ts`
```typescript
import type { ShotStatus, AiTaskStatus } from './common'

export interface EpisodeCreateRequest {
  title: string
  content?: string
  sortOrder?: number
}

export interface EpisodeVO {
  id: number
  projectId: number
  title: string
  sortOrder: number
  content: string | null
  status: number
  createTime: string
  updateTime: string
  // 前端扩展字段
  sceneCount?: number
  shotStats?: { total: number; approved: number; rejected: number }
}

export interface SceneCreateRequest {
  title: string
  content?: string
  sortOrder?: number
}

export interface SceneVO {
  id: number
  episodeId: number
  title: string
  sortOrder: number
  content: string | null
  status: number
  createTime: string
  updateTime: string
  // 前端扩展字段
  shotCount?: number
  shotStats?: { total: number; approved: number; rejected: number }
}

export interface ShotAssetRef {
  assetId: number
  assetType: string
  assetName: string
  primaryImage: string
}

export interface ShotVO {
  id: number
  sceneId: number
  sortOrder: number
  prompt: string | null
  promptEn: string | null
  generatedImageUrl: string | null
  generatedVideoUrl: string | null
  status: ShotStatus
  reviewComment: string | null
  version: number
  generationAttempts: number
  assetRefs: ShotAssetRef[]
  currentAiTask: CurrentAiTask | null
  createTime: string
  updateTime: string
}

export interface CurrentAiTask {
  taskId: number
  taskType: 'image_gen' | 'video_gen'
  status: AiTaskStatus
}

export interface ShotCreateRequest {
  sortOrder?: number
  prompt?: string
  promptEn?: string
}

export interface ShotUpdateRequest {
  prompt?: string
  promptEn?: string
  status?: ShotStatus
  reviewComment?: string
}

export interface BatchReviewRequest {
  shotIds: number[]
  action: 'approve' | 'reject'
  comment?: string
}

export interface BatchReviewResult {
  totalCount: number
  successCount: number
  failedCount: number
  failedDetails: { shotId: number; reason: string }[]
}
```

**文件：** `types/ai.ts`
```typescript
import type { AiTaskStatus } from './common'

export interface AiTaskVO {
  id: number
  taskType: 'image_gen' | 'video_gen'
  status: AiTaskStatus
  resultUrl: string | null
  errorMsg: string | null
  createTime: string
  updateTime: string
}

export interface ApiCostReport {
  totalCalls: number
  totalCost: number
  avgCostPerCall: number
  failRate: number
  dailyStats: { date: string; cost: number; calls: number }[]
  providerStats: { provider: string; cost: number; calls: number }[]
}
```

**文件：** `types/tos.ts`
```typescript
export interface PresignRequest {
  fileName: string
  contentType: string
  projectDir?: string
}

export interface PresignResult {
  uploadUrl: string
  accessUrl: string
  expiresIn: number
  maxFileSize: number
  allowedContentTypes: string[]
}

export interface TosCompleteRequest {
  key: string
  projectId: number
  fileType: 'novel' | 'asset' | 'other'
  metadata?: {
    originalName: string
    size: number
  }
}
```

---

## 7. 状态管理（Pinia）

### 7.1 模块划分

| 模块名 | Store 文件 | 职责 | 主要 State |
|--------|-----------|------|-----------|
| auth | `stores/auth.ts` | 认证状态 | token, userInfo, isLoggedIn |
| project | `stores/project.ts` | 项目状态 | projectList, currentProject, loading |
| asset | `stores/asset.ts` | 资产状态 | assetsByType, selectedAsset, loading |
| content | `stores/content.ts` | 内容状态 | episodes, scenes, shots, currentSceneId |
| workflow | `stores/workflow.ts` | 流程状态 | workflowConfig, workflowStatus, isRunning |
| ai | `stores/ai.ts` | AI 任务状态 | taskMap (taskId → AiTaskVO) |

### 7.2 核心模块详细设计

#### auth.ts
```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/user'
import { storage } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(storage.get('token') || '')
  const userInfo = ref<{ id: number; username: string; nickname: string } | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(t: string) {
    token.value = t
    storage.set('token', t, 24 * 60 * 60 * 1000) // 24h
  }

  function setUserInfo(info: typeof userInfo.value) {
    userInfo.value = info
  }

  async function fetchUserInfo() {
    const res = await userApi.getInfo()
    userInfo.value = {
      id: res.data.id,
      username: res.data.username,
      nickname: res.data.nickname
    }
  }

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    storage.remove('token')
  }

  async function logout() {
    try { await userApi.logout() } catch {}
    clearAuth()
  }

  return {
    token, userInfo, isLoggedIn,
    setToken, setUserInfo, fetchUserInfo, clearAuth, logout
  }
})
```

#### project.ts
```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { projectApi } from '@/api/project'
import type { ProjectVO, PageResult } from '@/types'

export const useProjectStore = defineStore('project', () => {
  const projectList = ref<PageResult<ProjectVO>>({
    total: 0, page: 1, size: 10, hasNext: false, list: []
  })
  const currentProject = ref<ProjectVO | null>(null)
  const loading = ref(false)

  async function fetchProjectList(page = 1, size = 10) {
    loading.value = true
    try {
      const res = await projectApi.list({ page, size })
      projectList.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchProjectDetail(id: number) {
    loading.value = true
    try {
      const res = await projectApi.getDetail(id)
      currentProject.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function createProject(data: { name: string; description?: string }) {
    await projectApi.create(data)
    await fetchProjectList()
  }

  async function deleteProject(id: number) {
    await projectApi.delete(id)
    await fetchProjectList()
  }

  return {
    projectList, currentProject, loading,
    fetchProjectList, fetchProjectDetail, createProject, deleteProject
  }
})
```

#### workflow.ts
```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { projectApi } from '@/api/project'
import type { WorkflowConfig, WorkflowStatusVO, StepType } from '@/types'

export const useWorkflowStore = defineStore('workflow', () => {
  const config = ref<WorkflowConfig>({ steps: [] })
  const status = ref<WorkflowStatusVO | null>(null)
  const isRunning = computed(() => status.value?.executionLock === 1)
  const pollingTimer = ref<number | null>(null)

  // 默认步骤配置
  const defaultSteps: { stepType: StepType; enabled: boolean; review: boolean }[] = [
    { stepType: 'import', enabled: true, review: false },
    { stepType: 'asset_extract', enabled: true, review: true },
    { stepType: 'shot_gen', enabled: true, review: true },
    { stepType: 'image_gen', enabled: true, review: true },
    { stepType: 'video_gen', enabled: true, review: true },
    { stepType: 'export', enabled: true, review: false }
  ]

  function initConfig() {
    config.value = {
      steps: defaultSteps.map(s => ({ ...s, config: {} }))
    }
  }

  async function saveConfig(projectId: number) {
    const version = useProjectStore().currentProject?.version || 0
    await projectApi.saveWorkflow(projectId, {
      version,
      workflowConfig: config.value,
      stylePreset: useProjectStore().currentProject?.stylePreset
    })
    await useProjectStore().fetchProjectDetail(projectId)
  }

  async function startExecution(projectId: number) {
    await projectApi.startWorkflow(projectId)
    startPolling(projectId)
  }

  async function reviewStep(projectId: number, stepType: string, action: 'approve' | 'reject', comment?: string) {
    await projectApi.reviewWorkflow(projectId, { stepType, action, comment })
    // 审核后继续轮询
    startPolling(projectId)
  }

  async function stopExecution(projectId: number) {
    await projectApi.stopWorkflow(projectId)
    stopPolling()
  }

  function startPolling(projectId: number) {
    stopPolling() // 清除旧定时器
    const timer = window.setInterval(async () => {
      const res = await projectApi.getWorkflowStatus(projectId)
      status.value = res.data
      // 如果不再执行中，停止轮询
      if (res.data.executionLock === 0) {
        stopPolling()
      }
    }, 3000) // 3s 轮询
    pollingTimer.value = timer as unknown as number
  }

  function stopPolling() {
    if (pollingTimer.value) {
      clearInterval(pollingTimer.value)
      pollingTimer.value = null
    }
  }

  return {
    config, status, isRunning, pollingTimer,
    initConfig, saveConfig, startExecution, reviewStep, stopExecution, startPolling, stopPolling
  }
})
```

#### asset.ts
```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { assetApi } from '@/api/asset'
import type { AssetVO, AssetType } from '@/types'

export const useAssetStore = defineStore('asset', () => {
  const assetsByType = ref<Record<AssetType, AssetVO[]>>({
    character: [],
    scene: [],
    prop: [],
    voice: []
  })
  const loading = ref(false)

  async function fetchAssets(projectId: number, assetType?: AssetType) {
    loading.value = true
    try {
      const res = await assetApi.list(projectId, assetType)
      if (assetType) {
        assetsByType.value[assetType] = res.data
      } else {
        // 按类型分组
        const grouped: Record<AssetType, AssetVO[]> = {
          character: [], scene: [], prop: [], voice: []
        }
        res.data.forEach(asset => {
          if (grouped[asset.assetType]) {
            grouped[asset.assetType].push(asset)
          }
        })
        assetsByType.value = grouped
      }
    } finally {
      loading.value = false
    }
  }

  async function createAsset(projectId: number, data: {
    assetType: AssetType
    name: string
    description?: string
    referenceImages?: string[]
  }) {
    await assetApi.create(projectId, data)
    await fetchAssets(projectId, data.assetType)
  }

  async function deleteAsset(projectId: number, id: number) {
    await assetApi.delete(id)
    // 重新获取所有类型
    await fetchAssets(projectId)
  }

  async function confirmAsset(id: number) {
    await assetApi.confirm(id)
  }

  return {
    assetsByType, loading,
    fetchAssets, createAsset, deleteAsset, confirmAsset
  }
})
```

#### content.ts
```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { contentApi } from '@/api/content'
import type { EpisodeVO, SceneVO, ShotVO, PageResult } from '@/types'

export const useContentStore = defineStore('content', () => {
  const episodes = ref<EpisodeVO[]>([])
  const scenes = ref<SceneVO[]>([])
  const shots = ref<PageResult<ShotVO>>({
    total: 0, page: 1, size: 20, hasNext: false, list: []
  })
  const currentEpisodeId = ref<number | null>(null)
  const currentSceneId = ref<number | null>(null)
  const loading = ref(false)

  async function fetchEpisodes(projectId: number) {
    const res = await contentApi.listEpisodes(projectId)
    episodes.value = res.data
  }

  async function fetchScenes(episodeId: number) {
    const res = await contentApi.listScenes(episodeId)
    scenes.value = res.data
  }

  async function fetchShots(sceneId: number, page = 1, size = 20, status?: number) {
    loading.value = true
    try {
      const res = await contentApi.listShots(sceneId, { page, size, status })
      shots.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function batchReview(data: { shotIds: number[]; action: 'approve' | 'reject'; comment?: string }) {
    return await contentApi.batchReview(data)
  }

  async function bindAsset(shotId: number, assetId: number, assetType: string) {
    await contentApi.bindAsset(shotId, { assetId, assetType })
  }

  async function unbindAsset(shotId: number, assetId: number) {
    await contentApi.unbindAsset(shotId, assetId)
  }

  return {
    episodes, scenes, shots, currentEpisodeId, currentSceneId, loading,
    fetchEpisodes, fetchScenes, fetchShots, batchReview, bindAsset, unbindAsset
  }
})
```

---

## 8. Composables 设计

### 8.1 useTaskPolling（任务轮询）

**文件：** `composables/useTaskPolling.ts`

```typescript
import { ref, onUnmounted } from 'vue'
import { aiApi } from '@/api/ai'
import type { AiTaskVO } from '@/types'

export function useTaskPolling(intervalMs = 3000) {
  const task = ref<AiTaskVO | null>(null)
  const isPolling = ref(false)
  let timer: number | null = null
  let pollCount = 0
  const maxPolls = 200 // 最多轮询约 10 分钟

  async function startPolling(shotId: number) {
    stopPolling()
    isPolling.value = true
    pollCount = 0

    const poll = async () => {
      if (!isPolling.value || pollCount >= maxPolls) {
        stopPolling()
        return
      }

      try {
        const res = await aiApi.getLatestTask(shotId)
        task.value = res.data

        // 任务完成或失败 → 停止轮询
        if (res.data.status === 2 || res.data.status === 3) {
          stopPolling()
          return
        }
      } catch (error) {
        console.error('Task polling error:', error)
      }

      pollCount++
      timer = window.setTimeout(poll, intervalMs)
    }

    poll()
  }

  function stopPolling() {
    isPolling.value = false
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  onUnmounted(() => {
    stopPolling()
  })

  return { task, isPolling, startPolling, stopPolling }
}
```

### 8.2 useTosUpload（TOS 直传）

**文件：** `composables/useTosUpload.ts`

```typescript
import { ref } from 'vue'
import { tosApi } from '@/api/tos'
import type { PresignResult } from '@/types'
import { ElMessage } from 'element-plus'

export interface UploadOptions {
  projectId: number
  fileType: 'novel' | 'asset' | 'other'
  projectDir?: string
  maxFileSize?: number  // 默认 50MB
  allowedTypes?: string[] // 默认 ['image/png', 'image/jpeg', 'video/mp4', 'text/plain']
  onSuccess?: (accessUrl: string) => void
  onError?: (error: Error) => void
}

export function useTosUpload() {
  const uploading = ref(false)
  const progress = ref(0)

  async function upload(file: File, options: UploadOptions): Promise<string> {
    // 1. 校验文件
    const maxFileSize = options.maxFileSize || 50 * 1024 * 1024
    if (file.size > maxFileSize) {
      ElMessage.error(`文件大小不能超过 ${maxFileSize / 1024 / 1024}MB`)
      throw new Error('File too large')
    }

    const allowedTypes = options.allowedTypes || ['image/png', 'image/jpeg', 'video/mp4', 'text/plain']
    if (!allowedTypes.includes(file.type)) {
      ElMessage.error(`不支持的文件类型: ${file.type}`)
      throw new Error('Unsupported file type')
    }

    uploading.value = true
    progress.value = 0

    try {
      // 2. 获取预签名 URL
      const presignRes = await tosApi.presign({
        fileName: file.name,
        contentType: file.type,
        projectDir: options.projectDir
      })
      const presignResult = presignRes.data

      // 3. 直传 TOS
      await uploadToTos(presignResult.uploadUrl, file)

      // 4. 通知后端
      await tosApi.complete({
        key: extractKeyFromUrl(presignResult.uploadUrl),
        projectId: options.projectId,
        fileType: options.fileType,
        metadata: { originalName: file.name, size: file.size }
      })

      options.onSuccess?.(presignResult.accessUrl)
      return presignResult.accessUrl
    } catch (error: any) {
      // 预签名过期自动重试一次
      if (error.response?.data?.code === 40005) {
        ElMessage.warning('上传链接已过期，正在重新获取...')
        return upload(file, options)
      }
      options.onError?.(error)
      throw error
    } finally {
      uploading.value = false
      progress.value = 0
    }
  }

  async function uploadToTos(url: string, file: File): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      xhr.open('PUT', url)
      xhr.setRequestHeader('Content-Type', file.type)

      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          progress.value = Math.round((e.loaded / e.total) * 100)
        }
      }

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve()
        } else {
          reject(new Error(`TOS upload failed: ${xhr.status}`))
        }
      }

      xhr.onerror = () => reject(new Error('TOS upload network error'))
      xhr.send(file)
    })
  }

  function extractKeyFromUrl(url: string): string {
    // 从预签名 URL 中提取 key 路径
    const urlObj = new URL(url)
    return urlObj.pathname.substring(1) // 去掉前导 /
  }

  return { uploading, progress, upload }
}
```

---

## 9. 常量与工具

### 状态颜色映射

**文件：** `constants/status.ts`

```typescript
import type { ProjectStatus, AssetStatus, ShotStatus, WorkflowTaskStatus, AiTaskStatus } from '@/types'

export const PROJECT_STATUS_MAP: Record<ProjectStatus, { label: string; type: 'info' | 'primary' | 'success' }> = {
  [ProjectStatus.Draft]: { label: '草稿', type: 'info' },
  [ProjectStatus.InProgress]: { label: '进行中', type: 'primary' },
  [ProjectStatus.Completed]: { label: '已完成', type: 'success' }
}

export const ASSET_STATUS_MAP: Record<AssetStatus, { label: string; type: 'info' | 'success' | 'danger' }> = {
  [AssetStatus.Draft]: { label: '草稿', type: 'info' },
  [AssetStatus.Confirmed]: { label: '已确认', type: 'success' },
  [AssetStatus.Deprecated]: { label: '已废弃', type: 'danger' }
}

export const SHOT_STATUS_MAP: Record<ShotStatus, { label: string; type: 'info' | 'primary' | 'warning' | 'success' | 'danger'; icon?: string }> = {
  [ShotStatus.Pending]: { label: '待处理', type: 'info' },
  [ShotStatus.Generating]: { label: '生成中', type: 'primary', icon: 'Loading' },
  [ShotStatus.WaitingReview]: { label: '待审核', type: 'warning' },
  [ShotStatus.Approved]: { label: '已通过', type: 'success' },
  [ShotStatus.Rejected]: { label: '已打回', type: 'danger' },
  [ShotStatus.Completed]: { label: '已完成', type: 'success' }
}

export const WORKFLOW_TASK_STATUS_MAP: Record<WorkflowTaskStatus, { label: string; color: string }> = {
  [WorkflowTaskStatus.NotStarted]: { label: '未执行', color: '#909399' },
  [WorkflowTaskStatus.Running]: { label: '执行中', color: '#409EFF' },
  [WorkflowTaskStatus.Success]: { label: '成功', color: '#67C23A' },
  [WorkflowTaskStatus.Failed]: { label: '失败', color: '#F56C6C' },
  [WorkflowTaskStatus.WaitingReview]: { label: '待审核', color: '#E6A23C' }
}

export const AI_TASK_STATUS_MAP: Record<AiTaskStatus, { label: string; type: 'info' | 'primary' | 'success' | 'danger' }> = {
  [AiTaskStatus.Submitting]: { label: '提交中', type: 'info' },
  [AiTaskStatus.Processing]: { label: '处理中', type: 'primary' },
  [AiTaskStatus.Success]: { label: '成功', type: 'success' },
  [AiTaskStatus.Failed]: { label: '失败', type: 'danger' }
}
```

### 错误码处理映射

**文件：** `constants/errors.ts`

```typescript
export type ErrorAction =
  | 'toast'           // Toast 提示
  | 'form'            // 表单高亮（调用方处理）
  | 'redirect-login'  // 跳转登录
  | 'retry-presign'   // 自动重试预签名
  | 'disabled-btn'    // 禁用按钮
  | 'global-error'    // 全局错误页

export const ERROR_ACTION: Record<number, ErrorAction> = {
  // 参数校验
  40001: 'form',    // 用户名或密码错误
  40002: 'form',    // 字段校验失败
  40003: 'toast',   // 文件类型不支持
  40004: 'toast',   // 文件大小超限
  40005: 'retry-presign', // 预签名 URL 过期

  // 认证鉴权
  40100: 'redirect-login', // 未登录
  40101: 'redirect-login', // Token 过期

  // 权限
  40300: 'toast',   // 无操作权限
  40301: 'toast',   // 非项目创建者

  // 资源
  40400: 'toast',   // 资源不存在

  // 业务冲突
  40900: 'toast',   // 项目正在执行中
  40901: 'toast',   // 资产被引用不可删除
  40902: 'disabled-btn', // 分镜状态不支持操作
  40903: 'toast',   // 数据已被修改

  // 限流
  42900: 'toast',   // 请求频率过高

  // 系统异常
  50000: 'global-error', // 服务器内部错误

  // AI/存储
  51000: 'toast',   // AI 调用失败
  51001: 'toast',   // AI 模型超时
  51002: 'toast',   // AI 生成内容为空
  51100: 'toast',   // TOS 上传失败
  51101: 'toast'    // TOS 空间不足
}
```

### 流程步骤定义

**文件：** `constants/workflow.ts`

```typescript
export const WORKFLOW_STEP_CONFIG: Record<string, {
  label: string
  icon: string
  description: string
  defaultReview: boolean
}> = {
  import: {
    label: '导入并拆分剧本',
    icon: 'Download',
    description: '读取小说文本，AI 自动拆分章节',
    defaultReview: false
  },
  asset_extract: {
    label: '资产提取',
    icon: 'Search',
    description: 'AI 分析剧本，提取角色/场景资产',
    defaultReview: true
  },
  shot_gen: {
    label: '分镜提示词生成',
    icon: 'EditPen',
    description: '根据分场+资产，生成中英文提示词',
    defaultReview: true
  },
  image_gen: {
    label: '首帧生图',
    icon: 'Picture',
    description: '根据提示词和资产参考图生成首帧',
    defaultReview: true
  },
  video_gen: {
    label: '视频生成',
    icon: 'VideoCamera',
    description: 'Seedance 模型根据首帧生成视频',
    defaultReview: true
  },
  export: {
    label: '合并导出',
    icon: 'Box',
    description: 'FFmpeg 合并所有分镜视频为最终作品',
    defaultReview: false
  }
}
```

---

## 10. UI 规范

### 主题
- **暗色主题**：默认使用 Element Plus dark theme
- 主色调：`#409EFF`（Element Plus 默认蓝）
- 背景色：`#141414`（深灰）
- 卡片背景：`#1d1e1f`
- 边框色：`#363637`

### 布局规范
| 元素 | 规范 |
|------|------|
| 页面内边距 | 24px |
| 卡片间距 | 16px |
| 表单 label 宽度 | 100px |
| 按钮最小宽度 | 80px |
| 图片卡片比例 | 16:9 |

### 响应式断点
| 断点 | 宽度 | 列数 |
|------|------|------|
| xs | < 768px | 1 列 |
| sm | 768-1024px | 2 列 |
| md | 1024-1440px | 3 列 |
| lg | > 1440px | 4 列 |

---

## 11. 性能优化策略

| 优化项 | 方案 | 适用场景 |
|--------|------|----------|
| 路由懒加载 | `() => import(...)` | 所有页面组件 |
| 虚拟滚动 | `vue-virtual-scroller` | 分镜列表（可能上千条） |
| 图片懒加载 | Element Plus `el-image` lazy | 资产卡片、分镜预览 |
| 组件按需引入 | `unplugin-vue-components` | Element Plus 组件 |
| 防抖/节流 | `lodash-es` | 搜索输入、窗口 resize |
| 请求缓存 | Pinia 持久化 / SWR 策略 | 资产列表、项目列表 |
| 大文件分片 | Phase 2（multipart upload） | 视频文件上传 > 10MB |
| Web Worker | 提示词格式校验 | 复杂文本处理（Phase 2） |

---

## 12. 错误处理策略

### 全局异常捕获
```typescript
// main.ts
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue error:', err, info)
  ElMessage.error('页面出现异常，已自动上报')
  // TODO: 接入 Sentry 或类似错误追踪
}
```

### 网络异常处理
- 请求超时：30s，超时后 Toast 提示
- 网络断开：Axios 拦截器捕获，Toast 提示「网络连接异常」
- 401/403：自动跳转登录页或 Toast 提示
- 业务错误码：根据 `ERROR_ACTION` 映射执行对应操作

### 表单校验
- 使用 Element Plus `el-form` 内置校验
- 自定义校验规则：`utils/validators.ts`
- 校验失败时高亮对应字段（错误码 40001/40002）

---

## 13. 安全策略

| 安全措施 | 实现方式 |
|----------|----------|
| Token 存储 | localStorage（24h TTL），敏感操作前校验 |
| XSS 防护 | Vue 3 自动转义，v-html 严格限制 |
| CSRF | Sa-Token header 认证（`Authorization: Bearer ***`），无 Cookie 认证 |
| 文件上传 | 前端校验类型+大小 → TOS 预签名 URL 限制 contentType |
| API Key | 前端不接触，所有 AI 调用由后端代理 |
| 路由守卫 | 未登录自动重定向登录页 |
| 敏感操作确认 | 删除、执行流程等二次确认弹窗 |

---

## 14. 开发环境与工程化

### Vite 配置要点

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts'
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;`
      }
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### ESLint 配置
- `@vue/eslint-config-typescript`
- `eslint-plugin-vue`
- `prettier` 集成
- `vue/define-macros-order`（`<script setup>` 宏排序）

### Git Hooks
- `lint-staged`：提交前自动 lint + format
- `husky`：pre-commit 钩子

### 环境变量

```bash
# .env.development
VITE_API_BASE_URL=/api
VITE_APP_TITLE=AI漫剧生产平台 (开发)

# .env.production
VITE_API_BASE_URL=https://api.example.com
VITE_APP_TITLE=AI漫剧生产平台
```

---

## 15. Sprint 任务拆分

### Sprint 1: 基础骨架（Day 1-3）

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 1.1 | Vue 3 + Vite + TS 项目初始化 | package.json, vite.config.ts, tsconfig.json | `[ ]` |
| 1.2 | Element Plus 集成 + 暗色主题 | styles/dark-theme.scss, 组件自动导入 | `[ ]` |
| 1.3 | Vue Router 配置 + 路由守卫 | router/index.ts, guards.ts | `[ ]` |
| 1.4 | Pinia 初始化 + auth store | stores/auth.ts | `[ ]` |
| 1.5 | Axios 实例 + 拦截器 | api/index.ts + 类型定义 | `[ ]` |
| 1.6 | TS 类型定义（全模块） | types/*.ts | `[ ]` |
| 1.7 | 登录页面 | views/Login.vue | `[ ]` |
| 1.8 | 基础 Layout（Header + Sidebar） | components/Layout/*.vue | `[ ]` |
| 1.9 | 常量与工具函数 | constants/*.ts, utils/*.ts | `[ ]` |

### Sprint 2: 核心业务页面（Day 4-7）

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 2.1 | 项目列表页（CRUD + 分页） | views/ProjectList.vue, api/project.ts | `[ ]` |
| 2.2 | 项目详情页（Tab 容器） | views/ProjectDetail.vue, stores/project.ts | `[ ]` |
| 2.3 | 资产库页面（Tab 分组 + 卡片网格） | views/tabs/AssetLibrary.vue | `[ ]` |
| 2.4 | 资产卡片 + 表单组件 | components/Asset/*.vue | `[ ]` |
| 2.5 | TOS 直传 composable + ImageUploader | composables/useTosUpload.ts | `[ ]` |
| 2.6 | Asset Store 完整实现 | stores/asset.ts | `[ ]` |
| 2.7 | 分集/分场/分镜 API 层 | api/content.ts | `[ ]` |
| 2.8 | Content Store 实现 | stores/content.ts | `[ ]` |

### Sprint 3: 流程编辑器 + 分镜工作台（Day 8-12）

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 3.1 | 流程编辑器（SortableJS 拖拽） | views/tabs/WorkflowEditor.vue | `[ ]` |
| 3.2 | 流程步骤卡片组件 | components/Workflow/*.vue | `[ ]` |
| 3.3 | Workflow Store + 状态轮询 | stores/workflow.ts | `[ ]` |
| 3.4 | useTaskPolling composable | composables/useTaskPolling.ts | `[ ]` |
| 3.5 | 分镜工作台（左右分栏 + 虚拟滚动） | views/tabs/ShotWorkbench.vue | `[ ]` |
| 3.6 | 分镜卡片 + 预览组件 | components/Shot/*.vue | `[ ]` |
| 3.7 | 资产绑定面板 | components/Shot/AssetBindPanel.vue | `[ ]` |
| 3.8 | 批量审核功能 | ShotWorkbench 内实现 | `[ ]` |
| 3.9 | AI 任务状态轮询集成 | ShotCard 内集成 useTaskPolling | `[ ]` |

### Sprint 4: 联调测试（Day 13-15）

| # | 任务 | 交付物 | 状态 |
|---|------|--------|------|
| 4.1 | API 消耗看板 | views/tabs/ApiCost.vue, api/ai.ts | `[ ]` |
| 4.2 | 全流程联调（导入→资产→流程→分镜→生成→导出） | 端到端测试 | `[ ]` |
| 4.3 | 边界情况测试（断点续跑前端展示、并发锁提示、重试） | 测试报告 | `[ ]` |
| 4.4 | 响应式适配（移动端 / 平板） | CSS 适配 | `[ ]` |
| 4.5 | 性能优化（虚拟滚动验证、图片懒加载） | 性能报告 | `[ ]` |

---

## 附录 A: 前后端接口对接总表

| 前端调用 | 后端接口 | 方法 | 状态 |
|----------|----------|------|------|
| `userApi.login` | `/api/user/login` | POST | `[ ]` |
| `userApi.logout` | `/api/user/logout` | POST | `[ ]` |
| `userApi.getInfo` | `/api/user/info` | GET | `[ ]` |
| `projectApi.list` | `/api/project/list` | GET | `[ ]` |
| `projectApi.create` | `/api/project` | POST | `[ ]` |
| `projectApi.getDetail` | `/api/project/{id}` | GET | `[ ]` |
| `projectApi.update` | `/api/project/{id}` | PUT | `[ ]` |
| `projectApi.delete` | `/api/project/{id}` | DELETE | `[ ]` |
| `projectApi.saveWorkflow` | `/api/project/{id}/workflow` | PUT | `[ ]` |
| `projectApi.startWorkflow` | `/api/project/{id}/workflow/start` | POST | `[ ]` |
| `projectApi.getWorkflowStatus` | `/api/project/{id}/workflow/status` | GET | `[ ]` |
| `projectApi.reviewWorkflow` | `/api/project/{id}/workflow/review` | POST | `[ ]` |
| `projectApi.stopWorkflow` | `/api/project/{id}/workflow/stop` | POST | `[ ]` |
| `projectApi.getShots` | `/api/project/{projectId}/shots` | GET | `[ ]` |
| `assetApi.list` | `/api/project/{projectId}/assets` | GET | `[ ]` |
| `assetApi.create` | `/api/project/{projectId}/assets` | POST | `[ ]` |
| `assetApi.update` | `/api/asset/{id}` | PUT | `[ ]` |
| `assetApi.delete` | `/api/asset/{id}` | DELETE | `[ ]` |
| `assetApi.confirm` | `/api/asset/{id}/confirm` | PUT | `[ ]` |
| `assetApi.getReferences` | `/api/asset/{assetId}/references` | GET | `[ ]` |
| `contentApi.listEpisodes` | `/api/project/{projectId}/episodes` | GET | `[ ]` |
| `contentApi.createEpisode` | `/api/project/{projectId}/episodes` | POST | `[ ]` |
| `contentApi.updateEpisode` | `/api/episode/{id}` | PUT | `[ ]` |
| `contentApi.deleteEpisode` | `/api/episode/{id}` | DELETE | `[ ]` |
| `contentApi.listScenes` | `/api/episode/{episodeId}/scenes` | GET | `[ ]` |
| `contentApi.createScene` | `/api/episode/{episodeId}/scenes` | POST | `[ ]` |
| `contentApi.updateScene` | `/api/scene/{id}` | PUT | `[ ]` |
| `contentApi.deleteScene` | `/api/scene/{id}` | DELETE | `[ ]` |
| `contentApi.listShots` | `/api/scene/{sceneId}/shots` | GET | `[ ]` |
| `contentApi.createShot` | `/api/scene/{sceneId}/shots` | POST | `[ ]` |
| `contentApi.updateShot` | `/api/shot/{id}` | PUT | `[ ]` |
| `contentApi.deleteShot` | `/api/shot/{id}` | DELETE | `[ ]` |
| `contentApi.batchReview` | `/api/shot/batch-review` | POST | `[ ]` |
| `contentApi.bindAsset` | `/api/shot/{shotId}/assets` | POST | `[ ]` |
| `contentApi.unbindAsset` | `/api/shot/{shotId}/assets/{assetId}` | DELETE | `[ ]` |
| `aiApi.getTaskStatus` | `/api/ai/task/{taskId}` | GET | `[ ]` |
| `aiApi.getLatestTask` | `/api/ai/task/latest` | GET | `[ ]` |
| `aiApi.getCostReport` | `/api/ai/cost-report` | GET | `[ ]` |
| `tosApi.presign` | `/api/tos/presign` | POST | `[ ]` |
| `tosApi.complete` | `/api/tos/complete` | POST | `[ ]` |

---

## 附录 B: 关键技术决策

| 决策项 | 选择 | 原因 |
|--------|------|------|
| 流程拖拽方案 | SortableJS + vue.draggable.next | PRD 明确要求轻量方案，不用 AntV X6 |
| 大列表渲染 | vue-virtual-scroller | 分镜可能上千条，虚拟滚动防卡顿 |
| 状态管理 | Pinia | Vue 3 官方推荐，TS 支持好，轻量 |
| HTTP 客户端 | Axios | 拦截器成熟，取消请求方便 |
| 文件上传 | 前端预签名直传 TOS | 减轻后端压力，后端只存 URL |
| 任务轮询 | 3s 轮询（非 SSE） | MVP 阶段简单可靠，Phase 2 可切 SSE |
| 暗色主题 | Element Plus dark theme + 自定义变量 | 创作者场景适合暗色，减少眼部疲劳 |
| 类型安全 | 完整 TS 类型定义 | 后端系分已有详细 DTO 规范，前端严格对齐 |

---

*文档版本: v1.0 | 作者: 小欧 | 日期: 2026-04-19*
*评审状态: 待评审*
